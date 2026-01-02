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
  Request,
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
  ApiBadRequestResponse,
  ApiUnauthorizedResponse,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
} from '@nestjs/swagger';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { CreateChildDto } from './dto/create-child.dto';
import { UserResponseDto } from './dto/user-response.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { UserRole } from './interfaces/user-role.enum';
import { Roles } from '../common/decorators/roles.decorator';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { v4 as uuidv4 } from 'uuid';
import * as fs from 'fs';
import { ImageFileValidator } from './validators/image-file.validator';

@ApiTags('Users')
@Controller('users')
@ApiBearerAuth('JWT-auth')
export class UsersController {
  constructor(private readonly usersService: UsersService) { }

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



  // Helper pour transformer UserDocument en format compatible Android
  private transformUserForResponse(user: any): any {
    if (!user) return null;

    const transformed: any = {
      id: user._id?.toString() || user.id,
      email: user.email || null,
      nom: user.nom,
      prenom: user.prenom,
      role: user.role,
      photoProfil: user.photoProfil || null,
    };

    // Transformer dateNaissance de Date à string (YYYY-MM-DD)
    if (user.dateNaissance) {
      const date = user.dateNaissance instanceof Date
        ? user.dateNaissance
        : new Date(user.dateNaissance);
      transformed.dateNaissance = date.toISOString().split('T')[0];
    } else {
      transformed.dateNaissance = null;
    }

    // Transformer parent (ObjectId) en parentId (string)
    if (user.parent) {
      if (typeof user.parent === 'object' && user.parent._id) {
        transformed.parentId = user.parent._id.toString();
      } else if (typeof user.parent === 'object' && user.parent.toString) {
        transformed.parentId = user.parent.toString();
      } else {
        transformed.parentId = user.parent.toString();
      }
    } else {
      transformed.parentId = null;
    }

    transformed.sexe = user.sexe || null;

    return transformed;
  }

  // Helper pour transformer un tableau d'utilisateurs
  private transformUsersForResponse(users: any[]): any[] {
    return users.map(user => this.transformUserForResponse(user));
  }

  @Post()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Créer un nouvel utilisateur' })
  @ApiResponse({ status: 201, description: 'Utilisateur créé avec succès' })
  @ApiResponse({ status: 409, description: 'Email déjà utilisé' })
  @ApiResponse({ status: 403, description: 'Accès refusé : permissions insuffisantes' })
  async create(@Body() createUserDto: CreateUserDto, @Request() req) {
    const currentUserRole = req.user?.role;
    // Le JWT strategy retourne userId, pas sub
    const currentUserId = req.user?.userId || req.user?.sub;

    if (!currentUserId) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    // Si on crée un enfant
    if (createUserDto.role === UserRole.ENFANT) {
      // Seuls les parents peuvent créer des enfants
      if (currentUserRole !== UserRole.PARENT) {
        throw new ForbiddenException('Seuls les parents peuvent créer des enfants');
      }
      // Associer automatiquement au parent connecté
      createUserDto.parentId = currentUserId.toString();
      // Email optionnel pour les enfants (générer un email temporaire unique si non fourni)
      if (!createUserDto.email) {
        createUserDto.email = `enfant_${Date.now()}_${Math.random().toString(36).substring(7)}@temp.com`;
      }
    } else {
      // Pour créer d'autres types d'utilisateurs, seul 'academie' peut le faire
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seul le rôle academie peut créer des utilisateurs autres que des enfants');
      }
    }

    const user = await this.usersService.create(createUserDto);
    return this.transformUserForResponse(user);
  }

  @Get()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Récupérer les utilisateurs (avec filtres optionnels)' })
  @ApiQuery({ name: 'role', required: false, enum: UserRole, description: 'Filtrer par rôle' })
  @ApiQuery({ name: 'parentId', required: false, description: 'Filtrer par parentId (pour les enfants)' })
  @ApiResponse({ status: 200, description: 'Liste des utilisateurs' })
  async findAll(
    @Query('role') role?: UserRole,
    @Query('parentId') parentId?: string,
    @Request() req?: any
  ) {
    const currentUserRole = req.user?.role;
    // Le JWT strategy retourne userId, pas sub
    const currentUserId = req.user?.userId || req.user?.sub;

    console.log(`[UsersController] findAll - Request: role=${role}, parentId=${parentId}, currentUserRole=${currentUserRole}, currentUserId=${currentUserId}`);

    if (!currentUserId) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    // Si on demande les enfants
    if (role === UserRole.ENFANT) {
      // Si un parentId est spécifié dans la query, utiliser celui-ci
      // Sinon, si c'est un parent qui demande, utiliser son ID
      if (!parentId && currentUserRole === UserRole.PARENT) {
        parentId = currentUserId.toString();
        console.log(`[UsersController] parentId non fourni, utilisation de currentUserId: ${parentId}`);
      }

      // Les parents ne peuvent voir que leurs propres enfants
      if (currentUserRole === UserRole.PARENT) {
        const parentIdStr = parentId?.toString();
        const currentUserIdStr = currentUserId.toString();
        console.log(`[UsersController] Vérification des permissions: parentIdStr=${parentIdStr}, currentUserIdStr=${currentUserIdStr}`);
        if (parentIdStr !== currentUserIdStr) {
          throw new ForbiddenException('Vous ne pouvez voir que vos propres enfants');
        }
      }

      // Les coaches et academies peuvent voir tous les enfants
      if (currentUserRole === UserRole.COACH || currentUserRole === UserRole.ACADEMIE) {
        // Pas de restriction, peut voir tous les enfants
        parentId = undefined; // Retirer le filtre pour voir tous
        console.log(`[UsersController] Coach/Academie - Retrait du filtre parentId pour voir tous les enfants`);
      }
    } else {
      // Pour voir d'autres types d'utilisateurs
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seul le rôle academie peut consulter tous les utilisateurs');
      }
    }

    console.log(`[UsersController] Appel à usersService.findAll avec: role=${role}, parentId=${parentId}`);
    const users = await this.usersService.findAll({ role, parentId });
    console.log(`[UsersController] findAll - role: ${role}, parentId: ${parentId}, currentUserId: ${currentUserId}, found ${users.length} users`);
    if (role === UserRole.ENFANT && users.length > 0) {
      console.log(`[UsersController] Enfants trouvés:`, users.map(u => ({ id: u._id, nom: u.nom, prenom: u.prenom, parent: u.parent })));
    }
    return this.transformUsersForResponse(users);
  }

  @Get('enfants')
  @UseGuards(JwtAuthGuard)
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
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Récupérer un utilisateur par ID' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur trouvé' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  async findOne(@Param('id') id: string, @Request() req) {
    const currentUserRole = req.user?.role;
    // Le JWT strategy retourne userId, pas sub
    const currentUserId = req.user?.userId || req.user?.sub;

    if (!currentUserId) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }

    const user = await this.usersService.findById(id);
    if (!user) {
      throw new NotFoundException('Utilisateur non trouvé');
    }

    // Normaliser les IDs pour la comparaison (string)
    const userIdStr = user._id?.toString() || user.id;
    const currentUserIdStr = currentUserId.toString();

    // Si l'utilisateur demande son propre profil, toujours autoriser
    if (userIdStr === currentUserIdStr) {
      return this.transformUserForResponse(user);
    }

    // Si c'est un enfant
    if (user.role === UserRole.ENFANT) {
      // Les parents ne peuvent voir que leurs propres enfants
      if (currentUserRole === UserRole.PARENT) {
        const parentIdStr = user.parent?.toString();
        if (parentIdStr !== currentUserIdStr) {
          throw new ForbiddenException('Vous ne pouvez voir que vos propres enfants');
        }
      }
      // Les coaches et academies peuvent voir tous les enfants
    } else {
      // Pour voir d'autres utilisateurs (pas son propre profil)
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Vous ne pouvez voir que votre propre profil');
      }
    }

    return this.transformUserForResponse(user);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard)
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
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  async update(
    @Param('id') id: string,
    @Body() updateUserDto: UpdateUserDto,
    @Request() req
  ) {
    const currentUserRole = req.user?.role;
    // Le JWT strategy retourne userId, pas sub
    const currentUserId = req.user?.userId || req.user?.sub;

    // Récupérer l'utilisateur (findById peuple déjà le parent)
    const existingUser = await this.usersService.findById(id);
    if (!existingUser) {
      return null;
    }

    // Si on modifie un enfant
    if (existingUser.role === UserRole.ENFANT) {
      // Extraire l'ID du parent de l'enfant (findById peuple déjà le parent)
      let childParentId: string | null = null;
      if (existingUser.parent) {
        // Si c'est un objet peuplé avec _id
        if (typeof existingUser.parent === 'object' && existingUser.parent._id) {
          childParentId = existingUser.parent._id.toString();
        }
        // Si c'est un ObjectId (non peuplé)
        else if (existingUser.parent.toString) {
          childParentId = existingUser.parent.toString();
        }
        // Sinon convertir en string
        else {
          childParentId = String(existingUser.parent);
        }
      }

      // Normaliser currentUserId en string (s'assurer que c'est bien une string)
      const currentUserIdStr = String(currentUserId).trim();
      const childParentIdStr = childParentId ? childParentId.trim() : null;

      console.log(`[UsersController] update - childParentId: ${childParentIdStr}, currentUserIdStr: ${currentUserIdStr}, currentUserRole: ${currentUserRole}, existingUser.parent type: ${typeof existingUser.parent}`);

      // Les parents ne peuvent modifier que leurs propres enfants
      if (currentUserRole === UserRole.PARENT) {
        if (!childParentIdStr || childParentIdStr !== currentUserIdStr) {
          throw new ForbiddenException(`Vous ne pouvez modifier que vos propres enfants. Parent de l'enfant: ${childParentIdStr || 'null'}, Votre ID: ${currentUserIdStr}`);
        }
        // Autoriser le parent à modifier son enfant
        console.log(`[UsersController] update - Autorisation accordée au parent ${currentUserIdStr} pour modifier l'enfant ${id}`);
      }
      // Les academies peuvent modifier tous les enfants
      else if (currentUserRole === UserRole.ACADEMIE) {
        // Autoriser
      } else {
        throw new ForbiddenException('Seuls les parents et academies peuvent modifier les enfants');
      }
    } else {
      // Pour modifier d'autres utilisateurs
      if (currentUserRole !== UserRole.ACADEMIE && id !== currentUserId) {
        throw new ForbiddenException('Vous ne pouvez modifier que votre propre profil');
      }
    }

    const updatedUser = await this.usersService.update(id, updateUserDto);
    return this.transformUserForResponse(updatedUser);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Supprimer un utilisateur' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur supprimé' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  async remove(@Param('id') id: string, @Req() req: any) {
    // sanitize and validate the incoming id (clients sometimes include trailing spaces)
    const raw = id || '';
    const decoded = decodeURIComponent(raw).trim();
    if (!decoded || !Types.ObjectId.isValid(decoded)) {
      throw new BadRequestException('id invalide');
    }

    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.userId || req.user?.sub;

    if (!currentUserId) {
      throw new UnauthorizedException('Token manquant ou invalide');
    }

    const user = await this.usersService.findById(decoded);
    if (!user) {
      throw new NotFoundException('Utilisateur non trouvé');
    }

    // Si on supprime un enfant
    if (user.role === UserRole.ENFANT) {
      // Extraire l'ID du parent de l'enfant (peut être ObjectId ou objet peuplé)
      let childParentId: string | null = null;
      if (user.parent) {
        if (typeof user.parent === 'object' && (user.parent as any)._id) {
          childParentId = (user.parent as any)._id.toString();
        } else if (typeof user.parent === 'object' && user.parent.toString) {
          childParentId = user.parent.toString();
        } else {
          childParentId = String(user.parent);
        }
      }

      // Normaliser currentUserId en string
      const currentUserIdStr = String(currentUserId);

      // Les parents ne peuvent supprimer que leurs propres enfants
      if (currentUserRole === UserRole.PARENT) {
        if (childParentId !== currentUserIdStr) {
          throw new ForbiddenException(`Vous ne pouvez supprimer que vos propres enfants.`);
        }
        // Autoriser le parent à supprimer son enfant
      }
      // Les academies peuvent supprimer tous les enfants
      else if (currentUserRole === UserRole.ACADEMIE) {
        // Autoriser
      } else {
        throw new ForbiddenException('Seuls les parents et academies peuvent supprimer les enfants');
      }
    } else {
      // Pour supprimer d'autres utilisateurs
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seul le rôle academie peut supprimer des utilisateurs');
      }
    }

    await this.usersService.remove(decoded);
    return { success: true, id: decoded };
  }

  @Get(':id/children')
  @UseGuards(JwtAuthGuard)
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
          description: 'Fichier image (JPG, PNG, GIF, max 20MB)',
        },
      },
      required: ['photo'],
    },
  })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Photo uploadée avec succès',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({
    description: 'Fichier invalide, format non supporté, ou taille trop grande',
    schema: {
      example: {
        statusCode: 400,
        message: 'Le fichier doit être une image (JPG, PNG, GIF) et ne doit pas dépasser 20MB',
        error: 'Bad Request'
      }
    }
  })
  @ApiUnauthorizedResponse({
    description: 'Token JWT manquant ou invalide',
    schema: {
      example: {
        statusCode: 401,
        message: 'Token invalide ou expiré',
        error: 'Unauthorized'
      }
    }
  })
  @ApiNotFoundResponse({
    description: 'Utilisateur non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Utilisateur non trouvé',
        error: 'Not Found'
      }
    }
  })
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
      try { fs.unlinkSync((file as any).path); } catch (e) { }
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
      try { fs.unlinkSync((file as any).path); } catch (e) { }
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
        try { fs.unlinkSync((file as any).path); } catch (e) { }
        throw new UnsupportedMediaTypeException('Type de fichier non supporté');
      }
    } catch (err) {
      try { fs.unlinkSync((file as any).path); } catch (e) { }
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
      try { fs.unlinkSync((file as any).path); } catch (err2) { }
      this.logger.error('Error saving photo URL to user', e?.message || e);
      throw new Error('Erreur interne lors de l\'upload');
    }
  }
  @Patch(':id/fcm-token')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Mettre à jour le token FCM pour les notifications push' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        fcmToken: { type: 'string', example: 'fcm_token_string' },
      },
      required: ['fcmToken'],
    },
  })
  @ApiResponse({ status: 200, description: 'Token FCM mis à jour' })
  async updateFcmToken(@Param('id') id: string, @Body('fcmToken') fcmToken: string, @Req() req: any) {
    const tokenUserId = req?.user?.userId;
    if (!req || !req.user || !tokenUserId) {
      throw new UnauthorizedException('Token manquant ou invalide');
    }

    // Allow user to update their own token
    if (tokenUserId !== id) {
      throw new ForbiddenException('Vous ne pouvez mettre à jour que votre propre token FCM');
    }

    return this.usersService.update(id, { fcmToken });
  }
}

