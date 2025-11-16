import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  HttpCode,
  HttpStatus,
  UseGuards,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiParam,
  ApiBadRequestResponse,
  ApiUnauthorizedResponse,
  ApiNotFoundResponse,
  ApiForbiddenResponse,
} from '@nestjs/swagger';
import { InscriptionService } from './inscription.service';
import { CreateInscriptionDto } from './dto/create-inscription.dto';
import { UpdateInscriptionDto } from './dto/update-inscription.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { Public } from '../common/decorators/public.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@ApiTags('Inscriptions')
@Controller('inscriptions')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth('JWT-auth')
export class InscriptionController {
  constructor(private readonly inscriptionService: InscriptionService) {}

  @Post()
  @Public()
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Créer une nouvelle inscription (Public - pour les parents)' })
  @ApiResponse({
    status: 201,
    description: 'Inscription créée avec succès',
    schema: {
      example: {
        _id: '507f1f77bcf86cd799439011',
        tournoiId: '507f1f77bcf86cd799439012',
        enfantPrenom: 'Lucas',
        enfantNom: 'Martin',
        enfantDateNaissance: '2012-05-15T00:00:00.000Z',
        parentPrenom: 'Sophie',
        parentNom: 'Martin',
        parentTelephone: '+33612345678',
        montantInscription: 25.5,
        besoinsParticuliers: 'Allergie aux arachides',
        createdAt: '2024-01-15T10:00:00.000Z',
        updatedAt: '2024-01-15T10:00:00.000Z',
      },
    },
  })
  @ApiBadRequestResponse({
    description: 'Données invalides ou tournoi non trouvé',
    schema: {
      example: {
        statusCode: 400,
        message: ['Le prénom de l\'enfant doit contenir au moins 2 caractères'],
        error: 'Bad Request',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Tournoi non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Tournoi non trouvé',
        error: 'Not Found',
      },
    },
  })
  create(@Body() createInscriptionDto: CreateInscriptionDto) {
    return this.inscriptionService.create(createInscriptionDto);
  }


  @Get(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Récupérer une inscription par ID (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'inscription (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Inscription trouvée',
    schema: {
      example: {
        _id: '507f1f77bcf86cd799439011',
        tournoiId: '507f1f77bcf86cd799439012',
        enfantPrenom: 'Lucas',
        enfantNom: 'Martin',
        enfantDateNaissance: '2012-05-15T00:00:00.000Z',
        parentPrenom: 'Sophie',
        parentNom: 'Martin',
        parentTelephone: '+33612345678',
        montantInscription: 25.5,
        besoinsParticuliers: 'Allergie aux arachides',
        createdAt: '2024-01-15T10:00:00.000Z',
        updatedAt: '2024-01-15T10:00:00.000Z',
      },
    },
  })
  @ApiBadRequestResponse({
    description: 'ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID invalide',
        error: 'Bad Request',
      },
    },
  })
  @ApiUnauthorizedResponse({
    description: 'Token JWT manquant ou invalide',
    schema: {
      example: {
        statusCode: 401,
        message: 'Token invalide ou expiré',
        error: 'Unauthorized',
      },
    },
  })
  @ApiForbiddenResponse({
    description: 'Accès refusé : rôle insuffisant',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Inscription non trouvée',
    schema: {
      example: {
        statusCode: 404,
        message: 'Inscription non trouvée',
        error: 'Not Found',
      },
    },
  })
  findOne(@Param('id') id: string) {
    return this.inscriptionService.findOne(id);
  }

  @Patch(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Mettre à jour une inscription (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'inscription (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Inscription mise à jour avec succès',
    schema: {
      example: {
        _id: '507f1f77bcf86cd799439011',
        tournoiId: '507f1f77bcf86cd799439012',
        enfantPrenom: 'Lucas',
        enfantNom: 'Martin',
        enfantDateNaissance: '2012-05-15T00:00:00.000Z',
        parentPrenom: 'Sophie',
        parentNom: 'Martin',
        parentTelephone: '+33612345678',
        montantInscription: 30.0,
        besoinsParticuliers: 'Allergie aux arachides, besoin d\'un accompagnateur',
        createdAt: '2024-01-15T10:00:00.000Z',
        updatedAt: '2024-01-15T11:00:00.000Z',
      },
    },
  })
  @ApiBadRequestResponse({
    description: 'Données invalides ou ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: ['Le montant d\'inscription ne peut pas être négatif'],
        error: 'Bad Request',
      },
    },
  })
  @ApiUnauthorizedResponse({
    description: 'Token JWT manquant ou invalide',
    schema: {
      example: {
        statusCode: 401,
        message: 'Token invalide ou expiré',
        error: 'Unauthorized',
      },
    },
  })
  @ApiForbiddenResponse({
    description: 'Accès refusé : rôle insuffisant',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Inscription non trouvée',
    schema: {
      example: {
        statusCode: 404,
        message: 'Inscription non trouvée',
        error: 'Not Found',
      },
    },
  })
  update(@Param('id') id: string, @Body() updateInscriptionDto: UpdateInscriptionDto) {
    return this.inscriptionService.update(id, updateInscriptionDto);
  }

  @Delete(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Supprimer une inscription (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'inscription (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Inscription supprimée avec succès',
    schema: {
      example: {
        message: 'Inscription supprimée avec succès',
      },
    },
  })
  @ApiBadRequestResponse({
    description: 'ID invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID invalide',
        error: 'Bad Request',
      },
    },
  })
  @ApiUnauthorizedResponse({
    description: 'Token JWT manquant ou invalide',
    schema: {
      example: {
        statusCode: 401,
        message: 'Token invalide ou expiré',
        error: 'Unauthorized',
      },
    },
  })
  @ApiForbiddenResponse({
    description: 'Accès refusé : rôle insuffisant',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Inscription non trouvée',
    schema: {
      example: {
        statusCode: 404,
        message: 'Inscription non trouvée',
        error: 'Not Found',
      },
    },
  })
  remove(@Param('id') id: string) {
    return this.inscriptionService.remove(id);
  }
}

