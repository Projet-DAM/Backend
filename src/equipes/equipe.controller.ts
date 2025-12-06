import { Controller, Post, Body, Get, Param, Patch } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiBadRequestResponse,
  ApiUnauthorizedResponse,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiConflictResponse,
  ApiParam,
  ApiBody,
} from '@nestjs/swagger';
import { EquipeService } from './equipe.service';
import { CreateEquipeDto } from './dto/create-equipe.dto';
import { EquipeResponseDto } from './dto/equipe-response.dto';
import { UpdateEquipeEnfantsDto } from './dto/update-equipe-enfants.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Equipes')
@Controller('equipes')
@ApiBearerAuth('JWT-auth')
export class EquipeController {
  constructor(private readonly equipeService: EquipeService) {}

  @Post()
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Créer une nouvelle équipe (Coach uniquement)' })
  @ApiBody({ type: CreateEquipeDto })
  @ApiResponse({
    status: 201,
    description: 'Équipe créée avec succès',
    type: EquipeResponseDto,
  })
  @ApiBadRequestResponse({
    description: 'Données invalides ou contraintes sportives non respectées',
    schema: {
      example: {
        statusCode: 400,
        message: 'Le football en 5v5 exige 5 enfants par équipe',
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
    description: 'Accès refusé : rôle insuffisant (Coach requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiConflictResponse({
    description: 'Nom de l\'équipe déjà utilisé ou enfant déjà assigné',
    schema: {
      example: {
        statusCode: 409,
        message: 'Une équipe avec ce nom existe déjà pour ce tournoi',
        error: 'Conflict',
      },
    },
  })
  async create(@Body() createEquipeDto: CreateEquipeDto) {
    return this.equipeService.create(createEquipeDto);
  }

  @Get('tournoi/:tournoiId')
  @Roles(UserRole.COACH, UserRole.ACADEMIE, UserRole.PARENT)
  @ApiOperation({ summary: 'Lister les équipes d\'un tournoi avec leurs enfants (Coach/Académie/Parent)' })
  @ApiParam({ name: 'tournoiId', description: 'ID du tournoi (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Liste des équipes pour le tournoi sélectionné',
    type: [EquipeResponseDto],
  })
  @ApiBadRequestResponse({
    description: 'ID de tournoi invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID de tournoi invalide',
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
    description: 'Tournoi non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Tournoi non trouvé',
        error: 'Not Found',
      },
    },
  })
  async findByTournoi(@Param('tournoiId') tournoiId: string) {
    return this.equipeService.findByTournoi(tournoiId);
  }

  @Patch(':id/enfants')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Ajouter ou retirer des enfants d\'une équipe (Coach uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'équipe (MongoDB ObjectId)' })
  @ApiBody({ type: UpdateEquipeEnfantsDto })
  @ApiResponse({
    status: 200,
    description: 'Équipe mise à jour avec succès',
    type: EquipeResponseDto,
  })
  @ApiBadRequestResponse({
    description: 'Contraintes invalides ou enfants non trouvés',
    schema: {
      example: {
        statusCode: 400,
        message: 'Aucun enfant à ajouter ou retirer',
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
    description: 'Accès refusé : rôle insuffisant (Coach requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Équipe non trouvée',
    schema: {
      example: {
        statusCode: 404,
        message: 'Équipe non trouvée',
        error: 'Not Found',
      },
    },
  })
  @ApiConflictResponse({
    description: 'Enfant déjà assigné à une autre équipe',
    schema: {
      example: {
        statusCode: 409,
        message: 'Un ou plusieurs enfants sont déjà assignés',
        error: 'Conflict',
      },
    },
  })
  async updateChildren(
    @Param('id') id: string,
    @Body() updateEquipeEnfantsDto: UpdateEquipeEnfantsDto,
  ) {
    return this.equipeService.updateEnfants(id, updateEquipeEnfantsDto);
  }
}

