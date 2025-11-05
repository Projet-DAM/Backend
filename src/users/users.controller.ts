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
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from './interfaces/user-role.enum';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { ImageFileValidator } from './validators/image-file.validator';

@ApiTags('Users')
@Controller('users')
@ApiBearerAuth('JWT-auth')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post()
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
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Récupérer tous les utilisateurs (Académie uniquement)' })
  @ApiQuery({ name: 'role', required: false, enum: UserRole, description: 'Filtrer par rôle' })
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
  @ApiOperation({ summary: 'Récupérer un utilisateur par ID' })
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
  @ApiOperation({ summary: 'Mettre à jour un utilisateur' })
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
  @Roles(UserRole.PARENT)
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

  @Get(':id/children')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer les enfants d\'un parent' })
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

