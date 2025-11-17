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
  UseGuards,
  Request,
  ForbiddenException,
  NotFoundException,
} from '@nestjs/common';
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
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { UserRole } from './interfaces/user-role.enum';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { ImageFileValidator } from './validators/image-file.validator';

@ApiTags('Users')
@Controller('users')
@ApiBearerAuth('JWT-auth')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

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
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  async remove(@Param('id') id: string, @Request() req) {
    const currentUserRole = req.user?.role;
    // Le JWT strategy retourne userId, pas sub
    const currentUserId = req.user?.userId || req.user?.sub;

    const user = await this.usersService.findById(id);
    if (!user) {
      return null;
    }

    // Si on supprime un enfant
    if (user.role === UserRole.ENFANT) {
      // Extraire l'ID du parent de l'enfant (peut être ObjectId ou objet peuplé)
      let childParentId: string | null = null;
      if (user.parent) {
        if (typeof user.parent === 'object' && user.parent._id) {
          childParentId = user.parent._id.toString();
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
          throw new ForbiddenException(`Vous ne pouvez supprimer que vos propres enfants. Parent de l'enfant: ${childParentId}, Votre ID: ${currentUserIdStr}`);
        }
        // Autoriser le parent à supprimer son enfant
      }
      // Les academies peuvent supprimer tous les enfants
      if (currentUserRole === UserRole.ACADEMIE) {
        // Autoriser
      } else if (currentUserRole !== UserRole.PARENT) {
        throw new ForbiddenException('Seuls les parents et academies peuvent supprimer les enfants');
      }
    } else {
      // Pour supprimer d'autres utilisateurs
      if (currentUserRole !== UserRole.ACADEMIE) {
        throw new ForbiddenException('Seul le rôle academie peut supprimer des utilisateurs');
      }
    }

    return this.usersService.remove(id);
  }

  @Post(':id/link-child')
  @UseGuards(JwtAuthGuard)
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
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  linkChild(@Param('id') id: string, @Body('childId') childId: string, @Request() req) {
    const currentUserRole = req.user?.role;
    if (currentUserRole !== UserRole.PARENT) {
      throw new ForbiddenException('Seuls les parents peuvent lier des enfants');
    }
    return this.usersService.linkChild(id, childId);
  }

  @Get(':id/children')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Récupérer les enfants d\'un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  @ApiResponse({ status: 200, description: 'Liste des enfants' })
  @ApiResponse({ status: 404, description: 'Parent non trouvé' })
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  async getChildren(@Param('id') id: string, @Request() req) {
    const currentUserRole = req.user?.role;
    const currentUserId = req.user?.sub;
    
    // Les parents ne peuvent voir que leurs propres enfants
    if (currentUserRole === UserRole.PARENT && id !== currentUserId) {
      throw new ForbiddenException('Vous ne pouvez voir que vos propres enfants');
    }
    // Les coaches et academies peuvent voir tous les enfants
    const children = await this.usersService.getChildren(id);
    return this.transformUsersForResponse(children);
  }

  @Post(':id/upload-photo')
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
          cb(null, `${uniqueSuffix}${extname(file.originalname)}`);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Uploader une photo de profil' })
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
          new MaxFileSizeValidator({ maxSize: 20000000 }), // 20MB
          new ImageFileValidator(),
        ],
      }),
    )
    file: Express.Multer.File,
  ) {
    const photoUrl = `/uploads/${file.filename}`;
    return this.usersService.update(id, { photoProfil: photoUrl });
  }
}

