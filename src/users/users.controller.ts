import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  Query,
  UseInterceptors,
  UploadedFile,
  ParseFilePipe,
  MaxFileSizeValidator,
  Req,
  Logger,
  ForbiddenException,
  UnauthorizedException,
  BadRequestException,
  UseGuards,
  NotFoundException,
  PayloadTooLargeException,
  UnsupportedMediaTypeException,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import { Types } from 'mongoose';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiConsumes,
  ApiBody,
  ApiParam,
  ApiQuery,
} from '@nestjs/swagger';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { CreateChildDto } from './dto/create-child.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from './interfaces/user-role.enum';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { v4 as uuidv4 } from 'uuid';
import * as fs from 'fs';
import { ImageFileValidator } from './validators/image-file.validator';

@ApiTags('Users')
@Controller('users')
@ApiBearerAuth('JWT-auth')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  private readonly logger = new Logger(UsersController.name);

  @Post(':id/children')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Créer un enfant et le lier au parent (PARENT uniquement)' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  @ApiBody({
    type: CreateChildDto,
    examples: {
      simpleChild: {
        summary: 'Create a child (minimal)',
        value: {
          nom: 'Petit',
          prenom: 'Paul',
          dateNaissance: '2014-05-10'
        }
      },
      withPhoto: {
        summary: 'Create a child with photo',
        value: {
          nom: 'Petit',
          prenom: 'Paul',
          dateNaissance: '2014-05-10',
          photoProfil: 'https://example.com/paul.jpg'
        }
      }
    }
  })
  @UseGuards(JwtAuthGuard)
  async createChild(@Param('id') id: string, @Body() createChildDto: CreateChildDto, @Req() req: any) {
    // If authenticated, prefer the token's userId as the parent
    const tokenUserId = req?.user?.userId;
    if (!req || !req.user || !tokenUserId) {
      throw new UnauthorizedException({ message: 'Token manquant ou invalide' });
    }

    // sanitize and validate provided id
    const raw = id || '';
    const decoded = decodeURIComponent(raw).trim();
    if (!decoded) {
      throw new BadRequestException({ message: 'parentId manquant' });
    }
    if (!Types.ObjectId.isValid(decoded)) {
      throw new BadRequestException({ message: 'parentId invalide' });
    }

    // Ensure the caller has permission to create for this parent. If the caller is a parent,
    // they may only create children for themselves (tokenUserId must match path id).
    const callerRole = req.user.role;
    if (callerRole === UserRole.PARENT && tokenUserId !== decoded) {
      throw new ForbiddenException({ message: 'Un parent ne peut créer un enfant que pour lui-même' });
    }

    return this.usersService.createChildForParent(decoded, createChildDto);
  }


  @Post('children')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Créer un enfant pour le parent authentifié (utilise le token JWT)' })
  @ApiResponse({ status: 201, description: 'Enfant créé et lié au parent' })
  @ApiResponse({ status: 401, description: 'Token manquant ou invalide' })
  @ApiResponse({ status: 403, description: 'Rôle non autorisé' })
  async createChildForSelf(@Req() req: any, @Body() createChildDto: CreateChildDto) {
    const tokenUserId = req?.user?.userId;
    if (!req || !req.user || !tokenUserId) {
      throw new UnauthorizedException({ message: 'Token manquant ou invalide' });
    }
    // caller must be PARENT (Roles decorator enforces it) — pass tokenUserId as parent
    return this.usersService.createChildForParent(tokenUserId, createChildDto);
  }

  @Post()
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Créer un nouvel utilisateur (Académie uniquement)' })
  @ApiResponse({ status: 201, description: 'Utilisateur créé avec succès' })
  @ApiResponse({ status: 409, description: 'Email déjà utilisé' })
  create(@Body() createUserDto: CreateUserDto) {
    return this.usersService.create(createUserDto);
  }

  @Get()
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Récupérer tous les utilisateurs (Académie uniquement)' })
  @ApiQuery({ name: 'role', required: false, enum: UserRole, description: 'Filtrer par rôle' })
  @ApiResponse({ status: 200, description: 'Liste des utilisateurs' })
  findAll(@Query('role') role?: UserRole) {
    return this.usersService.findAll(role);
  }

  @Get('enfants')
  @Roles(UserRole.COACH, UserRole.ACADEMIE, UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer la liste compacte des enfants (Coach/Académie; Parent returns own children)' })
  @ApiResponse({ status: 200, description: 'Liste compacte des enfants' })
  async getChildrenCompact(@Req() req: any) {
    const role = req?.user?.role;
    const tokenUserId = req?.user?.userId;
    this.logger.debug(`GET /users/enfants called; role=${role}; tokenUserId=${tokenUserId}`);

    // Require authentication for this endpoint — do not return an unauthenticated full list
    if (!req || !req.user || !tokenUserId) {
      this.logger.warn('Unauthorized request to GET /users/enfants');
      throw new UnauthorizedException('Token manquant ou invalide');
    }

    const roleStr = (role || '').toString().toLowerCase();
    if (roleStr === UserRole.PARENT) {
      // Parent: return only children they own (by parent OR createdBy)
      const listWithParent = await this.usersService.findChildrenCompactByParent(tokenUserId);
      this.logger.debug(`Returning ${listWithParent.length} children for parent ${tokenUserId}`);
      return listWithParent.map(({ _id, prenom, nom, fullName }: any) => ({ _id, prenom, nom, fullName, ownedByRequester: true }));
    }

    if (roleStr === UserRole.COACH || roleStr === UserRole.ACADEMIE) {
      // For coach/academie, return full list but mark which are owned by the requester (rarely true for these roles)
      const list = await this.usersService.findAllChildrenCompactWithOwners();
      const mapped = list.map((c: any) => ({
        _id: c._id,
        prenom: c.prenom,
        nom: c.nom,
        fullName: c.fullName,
        ownedByRequester: c.parent === tokenUserId || c.createdBy === tokenUserId,
      }));
      this.logger.debug(`Returning ${mapped.length} total children for role=${role}`);
      return mapped;
    }

    // For any other role, forbid
    this.logger.warn(`Access denied to /users/enfants for role=${role}`);
    throw new ForbiddenException('Accès refusé : rôle insuffisant');
  }

  @Get(':id')
  @ApiOperation({ summary: 'Récupérer un utilisateur par ID' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur trouvé' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  findOne(@Param('id') id: string) {
    return this.usersService.findById(id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Mettre à jour un utilisateur' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiBody({
    type: UpdateUserDto,
    examples: {
      assignCoach: {
        summary: 'Assign a coach to a child',
        value: { coach: '507f1f77bcf86cd799439011' }
      },
      updateProfile: {
        summary: 'Update profile fields',
        value: { nom: 'Dupont', prenom: 'Jean', photoProfil: 'https://example.com/photo.jpg' }
      }
    }
  })
  @ApiResponse({ status: 200, description: 'Utilisateur mis à jour' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  update(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto) {
    return this.usersService.update(id, updateUserDto);
  }

  @Delete(':id')
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Supprimer un utilisateur (Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur supprimé' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  remove(@Param('id') id: string) {
    // sanitize and validate the incoming id (clients sometimes include trailing spaces)
    const raw = id || '';
    const decoded = decodeURIComponent(raw).trim();
    if (!decoded || !Types.ObjectId.isValid(decoded)) {
      throw new BadRequestException('id invalide');
    }
    return this.usersService.remove(decoded);
  }

  @Post(':id/link-child')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Lier un enfant à un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        childId: { type: 'string', example: '507f1f77bcf86cd799439011' },
      },
      required: ['childId'],
    },
  })
  @ApiResponse({ status: 200, description: 'Enfant lié avec succès' })
  @ApiResponse({ status: 404, description: 'Parent ou enfant non trouvé' })
  @ApiResponse({ status: 400, description: 'Erreur de validation' })
  linkChild(@Param('id') id: string, @Body('childId') childId: string) {
    return this.usersService.linkChild(id, childId);
  }

  @Get(':id/children')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer les enfants d\'un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  @ApiResponse({ status: 200, description: 'Liste des enfants' })
  async getChildren(@Param('id') id: string, @Req() req: any) {
    try {
      const tokenUserId = req?.user?.userId;
      const paramId = id && id.trim() !== '' ? id : undefined;
      const effectiveId = paramId || tokenUserId;

      this.logger.debug(`GET /users/:id/children called; paramId=${paramId}; tokenUserId=${tokenUserId}; effectiveId=${effectiveId}`);

      if (!effectiveId) {
        // No id available from param or token
        this.logger.warn('No parent id provided in path or token');
        return [];
      }

      const children = await this.usersService.getChildren(effectiveId);
      this.logger.debug(`Found ${children.length} children for parentId=${effectiveId}`);
      return children;
    } catch (err) {
      // Log and rethrow so Nest maps exceptions correctly
      this.logger.error(`Error in getChildren: ${err?.message}`);
      throw err;
    }
  }

  @Post(':id/upload-photo')
  @UseGuards(JwtAuthGuard)
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          // Use UUID for filename and preserve extension
          const ext = extname(file.originalname) || '';
          const filename = `${uuidv4()}${ext}`;
          cb(null, filename);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Uploader une photo de profil' })
  @HttpCode(HttpStatus.CREATED)
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        photo: {
          type: 'string',
          format: 'binary',
        },
      },
    },
  })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Photo uploadée avec succès' })
  async uploadPhoto(
    @Param('id') id: string,
    @UploadedFile(
      new ParseFilePipe({
        validators: [
          new MaxFileSizeValidator({ maxSize: 10 * 1024 * 1024 }), // 10MB
          new ImageFileValidator(),
        ],
      }),
    )
    file: Express.Multer.File,
    @Req() req: any,
  ) {
    // Basic checks
    const tokenUserId = req?.user?.userId;
    if (!req || !req.user || !tokenUserId) {
      throw new UnauthorizedException('Token manquant ou invalide');
    }

    // Validate target user id
    const raw = id || '';
    const targetId = decodeURIComponent(raw).trim();
    if (!targetId || !Types.ObjectId.isValid(targetId)) {
      throw new BadRequestException({ message: 'id invalide' });
    }

    // Ensure file exists
    if (!file) {
      throw new BadRequestException({ message: 'Fichier manquant' });
    }

    // Enforce file size server-side as well
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      // remove uploaded file if present
      try { fs.unlinkSync((file as any).path); } catch (e) {}
      throw new PayloadTooLargeException('Fichier trop volumineux');
    }

    // Authorization: allow if self, or parent uploading for their child, or academie
    const targetUser = await this.usersService.findById(targetId);
    if (!targetUser) throw new NotFoundException('Utilisateur non trouvé');

    const requesterRole = req.user.role;
    const requesterId = tokenUserId;

    let authorized = false;
    if (requesterId === targetId) authorized = true; // uploading own photo
    if (!authorized && requesterRole === UserRole.PARENT) {
      // parent can upload for their child
      const parentChildren = await this.usersService.getChildren(requesterId);
      if ((parentChildren || []).some((c) => (c._id && c._id.toString ? c._id.toString() : c._id) === targetId)) {
        authorized = true;
      }
    }
    if (!authorized && requesterRole === UserRole.ACADEMIE) {
      authorized = true;
    }
    if (!authorized) {
      // remove uploaded file if present
      try { fs.unlinkSync((file as any).path); } catch (e) {}
      throw new ForbiddenException('Non autorisé à uploader cette photo');
    }

    // Validate file signature (magic bytes) for additional safety
    try {
      const p = (file as any).path;
      const fd = fs.openSync(p, 'r');
      const header = Buffer.alloc(12);
      fs.readSync(fd, header, 0, 12, 0);
      fs.closeSync(fd);
      const isJpeg = header[0] === 0xff && header[1] === 0xd8 && header[2] === 0xff;
      const isPng = header[0] === 0x89 && header[1] === 0x50 && header[2] === 0x4e && header[3] === 0x47;
      const isWebp = header.toString('ascii', 0, 4) === 'RIFF' && header.toString('ascii', 8, 12) === 'WEBP';
      if (!isJpeg && !isPng && !isWebp) {
        try { fs.unlinkSync((file as any).path); } catch (e) {}
        throw new UnsupportedMediaTypeException('Type de fichier non supporté');
      }
    } catch (err) {
      try { fs.unlinkSync((file as any).path); } catch (e) {}
      if (err instanceof UnsupportedMediaTypeException) throw err;
      throw new BadRequestException('Erreur lors de la validation du fichier');
    }

    // Compute public URL (served statically from /uploads)
    const publicUrl = `/uploads/${file.filename}`;
    try {
      const updated = await this.usersService.update(targetId, { photoProfil: publicUrl });
      this.logger.log(`User ${requesterId} uploaded photo for user ${targetId} -> ${publicUrl}; originalName=${file.originalname}; saved=${file.filename}`);
      return { success: true, photoProfil: publicUrl, user: updated };
    } catch (e) {
      // remove file on error
      try { fs.unlinkSync((file as any).path); } catch (err2) {}
      this.logger.error('Error saving photo URL to user', e?.message || e);
      throw new Error('Erreur interne lors de l\'upload');
    }
  }
}

