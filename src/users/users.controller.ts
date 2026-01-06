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
import { Roles } from '../common/decorators/roles.decorator';
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

    if (user.dateNaissance) {
      const date = user.dateNaissance instanceof Date
        ? user.dateNaissance
        : new Date(user.dateNaissance);
      transformed.dateNaissance = date.toISOString().split('T')[0];
    } else {
      transformed.dateNaissance = null;
    }

    if (user.parent) {
      if (typeof user.parent === 'object' && user.parent._id) {
        transformed.parentId = user.parent._id.toString();
      } else {
        transformed.parentId = user.parent.toString();
      }
    } else {
      transformed.parentId = null;
    }

    transformed.sexe = user.sexe || null;
    return transformed;
  }

  private transformUsersForResponse(users: any[]): any[] {
    return users.map(user => this.transformUserForResponse(user));
  }

  @Post(':id/children')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Créer un enfant et le lier au parent (PARENT uniquement)' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  async createChild(@Param('id') id: string, @Body() createChildDto: CreateChildDto, @Req() req: any) {
    const tokenUserId = req?.user?.userId;
    if (!tokenUserId) throw new UnauthorizedException({ message: 'Token manquant ou invalide' });

    const decoded = decodeURIComponent(id).trim();
    if (!Types.ObjectId.isValid(decoded)) throw new BadRequestException({ message: 'parentId invalide' });

    if (req.user.role === UserRole.PARENT && tokenUserId !== decoded) {
      throw new ForbiddenException({ message: 'Un parent ne peut créer un enfant que pour lui-même' });
    }

    return this.usersService.createChild(decoded, createChildDto);
  }

  @Post('children')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Créer un enfant pour le parent authentifié' })
  async createChildForSelf(@Req() req: any, @Body() createChildDto: CreateChildDto) {
    const tokenUserId = req?.user?.userId;
    if (!tokenUserId) throw new UnauthorizedException({ message: 'Token manquant ou invalide' });
    return this.usersService.createChild(tokenUserId, createChildDto);
  }

  @Post()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Créer un nouvel utilisateur' })
  async create(@Body() createUserDto: CreateUserDto, @Request() req) {
    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.userId;
    if (!currentUserId) throw new ForbiddenException('Utilisateur non authentifié');

    if (createUserDto.role === UserRole.ENFANT) {
      if (currentUserRole !== UserRole.PARENT) {
        throw new ForbiddenException('Seuls les parents peuvent créer des enfants');
      }
      createUserDto.parentId = currentUserId.toString();
      if (!createUserDto.email) {
        createUserDto.email = `enfant_${Date.now()}_${Math.random().toString(36).substring(7)}@temp.com`;
      }
    } else {
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seul le rôle academie peut créer des utilisateurs');
      }
    }

    const user = await this.usersService.create(createUserDto);
    return this.transformUserForResponse(user);
  }

  @Get()
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Récupérer les utilisateurs (avec filtres optionnels)' })
  async findAll(
    @Query('role') role?: UserRole,
    @Query('parentId') parentId?: string,
    @Request() req?: any
  ) {
    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.userId;
    if (!currentUserId) throw new ForbiddenException('Utilisateur non authentifié');

    if (role === UserRole.ENFANT) {
      if (!parentId && currentUserRole === UserRole.PARENT) {
        parentId = currentUserId.toString();
      }
      if (currentUserRole === UserRole.PARENT && parentId !== currentUserId.toString()) {
        throw new ForbiddenException('Vous ne pouvez voir que vos propres enfants');
      }
    } else {
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seul le rôle academie peut consulter tous les utilisateurs');
      }
    }

    const users = await this.usersService.findAll({ role, parentId });
    return this.transformUsersForResponse(users);
  }

  @Get('enfants')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.COACH, UserRole.ACADEMIE, UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer la liste compacte des enfants' })
  async getChildrenCompact(@Req() req: any) {
    const role = req?.user?.role;
    const tokenUserId = req?.user?.userId;
    if (!tokenUserId) throw new UnauthorizedException('Token manquant ou invalide');

    if (role === UserRole.PARENT) {
      const list = await this.usersService.findChildrenCompactByParent(tokenUserId);
      return list.map(c => ({ ...c, ownedByRequester: true }));
    }

    if (role === UserRole.COACH || role === UserRole.ACADEMIE) {
      const list = await this.usersService.findAllChildrenCompact();
      return list.map(c => ({ ...c, ownedByRequester: false }));
    }

    throw new ForbiddenException('Accès refusé');
  }

  @Get(':id')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Récupérer un utilisateur par ID' })
  async findOne(@Param('id') id: string, @Request() req) {
    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.userId;
    if (!currentUserId) throw new ForbiddenException('Utilisateur non authentifié');

    const user = await this.usersService.findById(id);
    if (!user) throw new NotFoundException('Utilisateur non trouvé');

    const userIdStr = user._id?.toString() || user.id;
    if (userIdStr === currentUserId.toString()) return this.transformUserForResponse(user);

    if (user.role === UserRole.ENFANT) {
      if (currentUserRole === UserRole.PARENT && user.parent?.toString() !== currentUserId.toString()) {
        throw new ForbiddenException('Vous ne pouvez voir que vos propres enfants');
      }
    } else if (currentUserRole !== UserRole.ACADEMIE) {
      throw new ForbiddenException('Vous ne pouvez voir que votre propre profil');
    }

    return this.transformUserForResponse(user);
  }

  @Patch(':id')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Mettre à jour un utilisateur' })
  async update(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto, @Request() req) {
    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.userId;
    if (!currentUserId) throw new ForbiddenException('Utilisateur non authentifié');

    const user = await this.usersService.findById(id);
    if (!user) throw new NotFoundException('Utilisateur non trouvé');

    if (user.role === UserRole.ENFANT) {
      if (currentUserRole !== UserRole.PARENT && currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seuls les parents et academies peuvent modifier les enfants');
      }
      if (currentUserRole === UserRole.PARENT && user.parent?.toString() !== currentUserId.toString()) {
        throw new ForbiddenException('Vous ne pouvez modifier que vos propres enfants');
      }
    } else {
      if (currentUserRole !== UserRole.ACADEMIE && id !== currentUserId.toString()) {
        throw new ForbiddenException('Vous ne pouvez modifier que votre propre profil');
      }
    }

    const updatedUser = await this.usersService.update(id, updateUserDto);
    return this.transformUserForResponse(updatedUser);
  }

  @Delete(':id')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Supprimer un utilisateur' })
  async remove(@Param('id') id: string, @Req() req: any) {
    const decoded = decodeURIComponent(id).trim();
    if (!Types.ObjectId.isValid(decoded)) throw new BadRequestException('id invalide');

    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.userId;
    if (!currentUserId) throw new UnauthorizedException('Token manquant ou invalide');

    const user = await this.usersService.findById(decoded);
    if (!user) throw new NotFoundException('Utilisateur non trouvé');

    if (user.role === UserRole.ENFANT) {
      if (currentUserRole === UserRole.PARENT && user.parent?.toString() !== currentUserId.toString()) {
        throw new ForbiddenException('Vous ne pouvez supprimer que vos propres enfants');
      }
      if (currentUserRole !== UserRole.PARENT && currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seuls les parents et academies peuvent supprimer les enfants');
      }
    } else if (currentUserRole !== UserRole.ACADEMIE) {
      throw new ForbiddenException('Seul le rôle academie peut supprimer des utilisateurs');
    }

    return this.usersService.remove(decoded);
  }

  @Get(':parentId/children/:childId')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer un enfant spécifique' })
  async getChildData(@Param('parentId') parentId: string, @Param('childId') childId: string) {
    return this.usersService.getChildData(parentId, childId);
  }

  @Get(':id/children')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Récupérer les enfants d\'un parent' })
  async getChildren(@Param('id') id: string, @Req() req: any) {
    const tokenUserId = req?.user?.userId;
    const effectiveId = id && id.trim() !== '' ? id : tokenUserId;
    if (!effectiveId) return [];
    return this.usersService.getChildren(effectiveId);
  }

  @Post(':id/upload-photo')
  @UseGuards(JwtAuthGuard)
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          const ext = extname(file.originalname) || '';
          cb(null, `${uuidv4()}${ext}`);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Uploader une photo de profil' })
  async uploadPhoto(
    @Param('id') id: string,
    @UploadedFile(
      new ParseFilePipe({
        validators: [
          new MaxFileSizeValidator({ maxSize: 10 * 1024 * 1024 }),
          new ImageFileValidator(),
        ],
      }),
    )
    file: Express.Multer.File,
    @Req() req: any,
  ) {
    const tokenUserId = req?.user?.userId;
    if (!tokenUserId) throw new UnauthorizedException('Token manquant ou invalide');

    const user = await this.usersService.findById(id);
    if (!user) {
      try { fs.unlinkSync(file.path); } catch (e) { }
      throw new NotFoundException('Utilisateur non trouvé');
    }

    const requesterRole = req.user.role;
    let authorized = false;
    if (tokenUserId === id) authorized = true;
    if (!authorized && requesterRole === UserRole.PARENT && user.parent?.toString() === tokenUserId) authorized = true;
    if (!authorized && requesterRole === UserRole.ACADEMIE) authorized = true;

    if (!authorized) {
      try { fs.unlinkSync(file.path); } catch (e) { }
      throw new ForbiddenException('Non autorisé à uploader cette photo');
    }

    const publicUrl = `/uploads/${file.filename}`;
    const updated = await this.usersService.update(id, { photoProfil: publicUrl });
    return { success: true, photoProfil: publicUrl, user: updated };
  }

  @Patch(':id/fcm-token')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Mettre à jour le token FCM' })
  async updateFcmToken(@Param('id') id: string, @Body('fcmToken') fcmToken: string, @Req() req: any) {
    const tokenUserId = req?.user?.userId;
    if (tokenUserId !== id) throw new ForbiddenException('Vous ne pouvez mettre à jour que votre propre token FCM');
    return this.usersService.update(id, { fcmToken });
  }
}

