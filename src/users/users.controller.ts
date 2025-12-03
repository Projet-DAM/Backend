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
  ApiBadRequestResponse,
  ApiUnauthorizedResponse,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
} from '@nestjs/swagger';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { UserResponseDto } from './dto/user-response.dto';
import { LinkChildDto } from './dto/link-child.dto';
import { CreateChildDto } from './dto/create-child.dto';
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
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Créer un nouvel utilisateur (Académie uniquement)' })
  @ApiBody({ type: CreateUserDto })
  @ApiResponse({ 
    status: 201, 
    description: 'Utilisateur créé avec succès',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({ 
    description: 'Données invalides',
    schema: {
      example: {
        statusCode: 400,
        message: ['email must be an email', 'motDePasse must be longer than or equal to 6 characters'],
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Académie requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
      }
    }
  })
  @ApiResponse({ 
    status: 409, 
    description: 'Email déjà utilisé',
    schema: {
      example: {
        statusCode: 409,
        message: 'Cet email est déjà utilisé',
        error: 'Conflict'
      }
    }
  })
  create(@Body() createUserDto: CreateUserDto) {
    return this.usersService.create(createUserDto);
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
  @ApiResponse({ 
    status: 200, 
    description: 'Liste des utilisateurs',
    type: [UserResponseDto]
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Académie requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
      }
    }
  })
  findAll(@Query('role') role?: UserRole) {
    return this.usersService.findAll(role);
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
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur (MongoDB ObjectId)' })
  @ApiResponse({ 
    status: 200, 
    description: 'Utilisateur trouvé',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({ 
    description: 'ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID invalide',
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
  findOne(@Param('id') id: string) {
    return this.usersService.findById(id);
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

  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur (MongoDB ObjectId)' })
  @ApiBody({ type: UpdateUserDto })
  @ApiResponse({ 
    status: 200, 
    description: 'Utilisateur mis à jour',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({ 
    description: 'Données invalides',
    schema: {
      example: {
        statusCode: 400,
        message: ['email must be an email'],
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
  update(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto) {
    return this.usersService.update(id, updateUserDto);
  }

  @Delete(':id')
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Supprimer un utilisateur (Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur (MongoDB ObjectId)' })
  @ApiResponse({ 
    status: 200, 
    description: 'Utilisateur supprimé',
    schema: {
      example: {
        message: 'Utilisateur supprimé avec succès'
      }
    }
  })
  @ApiBadRequestResponse({ 
    description: 'ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID invalide',
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Académie requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
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
  remove(@Param('id') id: string) {
    return this.usersService.remove(id);
  }

  @Post(':id/link-child')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Lier un enfant à un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent (MongoDB ObjectId)' })
  @ApiBody({ type: LinkChildDto })
  @ApiResponse({ 
    status: 200, 
    description: 'Enfant lié avec succès',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({ 
    description: 'Erreur de validation ou ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'childId doit être un ID MongoDB valide',
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Parent requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
      }
    }
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
  @ApiNotFoundResponse({ 
    description: 'Parent ou enfant non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Parent ou enfant non trouvé',
        error: 'Not Found'
      }
    }
  })
  linkChild(@Param('id') id: string, @Body() linkChildDto: LinkChildDto) {
    return this.usersService.linkChild(id, linkChildDto.childId);
  }

  @Get(':parentId/children/:childId')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer un enfant spécifique avec vérification d\'appartenance' })
  @ApiParam({ name: 'parentId', description: 'ID du parent (MongoDB ObjectId)' })
  @ApiParam({ name: 'childId', description: 'ID de l\'enfant (MongoDB ObjectId)' })
  @ApiResponse({ 
    status: 200, 
    description: 'Enfant trouvé',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({ 
    description: 'ID invalide ou enfant n\'appartient pas au parent',
    schema: {
      example: {
        statusCode: 400,
        message: 'Cet enfant n\'appartient pas à ce parent',
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Parent requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
      }
    }
  })
  @ApiNotFoundResponse({ 
    description: 'Parent ou enfant non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Parent ou enfant non trouvé',
        error: 'Not Found'
      }
    }
  })
  getChildData(@Param('parentId') parentId: string, @Param('childId') childId: string) {
    return this.usersService.getChildData(parentId, childId);
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
  @ApiParam({ name: 'id', description: 'ID du parent (MongoDB ObjectId)' })
  @ApiResponse({ 
    status: 200, 
    description: 'Liste des enfants',
    type: [UserResponseDto]
  })
  @ApiBadRequestResponse({ 
    description: 'ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID invalide',
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Parent requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
      }
    }
  })
  @ApiNotFoundResponse({ 
    description: 'Parent non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Parent non trouvé',
        error: 'Not Found'
      }
    }
  })
  getChildren(@Param('id') id: string) {
    return this.usersService.getChildren(id);
  }

  @Post(':id/children')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Créer un nouvel enfant pour un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent (MongoDB ObjectId)' })
  @ApiBody({ type: CreateChildDto })
  @ApiResponse({ 
    status: 201, 
    description: 'Enfant créé avec succès',
    type: UserResponseDto
  })
  @ApiBadRequestResponse({ 
    description: 'Erreur de validation ou ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: ['prenom must be a string', 'sportPratique must be a valid enum value'],
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
  @ApiForbiddenResponse({ 
    description: 'Accès refusé : rôle insuffisant (Parent requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden'
      }
    }
  })
  @ApiNotFoundResponse({ 
    description: 'Parent non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Parent non trouvé',
        error: 'Not Found'
      }
    }
  })
  createChild(@Param('id') id: string, @Body() createChildDto: CreateChildDto) {
    return this.usersService.createChild(id, createChildDto);
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

