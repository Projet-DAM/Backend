import { Controller, Post, Body, Get, Param, Patch, Delete, Query, HttpCode, HttpStatus } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiBadRequestResponse,
  ApiUnauthorizedResponse,
  ApiForbiddenResponse,
  ApiNotFoundResponse,
  ApiParam,
  ApiBody,
  ApiQuery,
} from '@nestjs/swagger';
import { MatchesService } from './matches.service';
import { CreateMatchDto } from './dto/create-match.dto';
import { UpdateMatchDto } from './dto/update-match.dto';
import { MatchResponseDto } from './dto/match-response.dto';
import { BracketResponseDto } from './dto/bracket-response.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Matches')
@Controller('matches')
@ApiBearerAuth('JWT-auth')
export class MatchesController {
  constructor(private readonly matchesService: MatchesService) {}

  @Post()
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Créer un nouveau match (Coach ou Académie uniquement)' })
  @ApiBody({ type: CreateMatchDto })
  @ApiResponse({
    status: 201,
    description: 'Match créé avec succès',
    type: MatchResponseDto,
  })
  @ApiBadRequestResponse({
    description: 'Données invalides ou équipes identiques',
    schema: {
      example: {
        statusCode: 400,
        message: 'Les équipes A et B doivent être différentes',
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
    description: 'Accès refusé : rôle insuffisant (Coach ou Académie requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Tournoi ou équipe non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Tournoi non trouvé',
        error: 'Not Found',
      },
    },
  })
  async create(@Body() createMatchDto: CreateMatchDto) {
    return this.matchesService.create(createMatchDto);
  }

  @Post('tournoi/:tournoiId/generate-bracket')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Générer automatiquement l\'arbre de tournoi depuis les équipes (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'tournoiId', description: 'ID du tournoi (MongoDB ObjectId)' })
  @ApiResponse({
    status: 201,
    description: 'Arbre généré avec succès',
    type: [MatchResponseDto],
  })
  @ApiBadRequestResponse({
    description: 'Nombre d\'équipes insuffisant ou arbre déjà existant',
    schema: {
      example: {
        statusCode: 400,
        message: 'Un tournoi doit avoir au moins 2 équipes pour générer un arbre',
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
    description: 'Accès refusé : rôle insuffisant (Coach ou Académie requis)',
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
  async generateBracket(@Param('tournoiId') tournoiId: string) {
    return this.matchesService.generateBracket(tournoiId);
  }

  @Get('tournoi/:tournoiId')
  @Roles(UserRole.COACH, UserRole.ACADEMIE, UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer tous les matchs d\'un tournoi (Coach/Académie/Parent)' })
  @ApiParam({ name: 'tournoiId', description: 'ID du tournoi (MongoDB ObjectId)' })
  @ApiQuery({
    name: 'showFuture',
    required: false,
    type: Boolean,
    description: 'Afficher les matchs à venir (true) ou seulement les matchs en cours/terminés (false, défaut)',
  })
  @ApiResponse({
    status: 200,
    description: 'Liste des matchs du tournoi',
    type: [MatchResponseDto],
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
  async findByTournoi(@Param('tournoiId') tournoiId: string, @Query('showFuture') showFuture?: string) {
    const showFutureBool = showFuture === 'true';
    return this.matchesService.findByTournoi(tournoiId, showFutureBool);
  }

  @Get('tournoi/:tournoiId/bracket')
  @Roles(UserRole.COACH, UserRole.ACADEMIE, UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer l\'arbre de tournoi organisé par phases (Coach/Académie/Parent)' })
  @ApiParam({ name: 'tournoiId', description: 'ID du tournoi (MongoDB ObjectId)' })
  @ApiQuery({
    name: 'showFuture',
    required: false,
    type: Boolean,
    description: 'Afficher les matchs à venir (true) ou seulement les matchs en cours/terminés (false, défaut pour parents)',
  })
  @ApiResponse({
    status: 200,
    description: 'Arbre de tournoi organisé par phases',
    type: BracketResponseDto,
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
  async getBracket(@Param('tournoiId') tournoiId: string, @Query('showFuture') showFuture?: string) {
    const showFutureBool = showFuture === 'true';
    return this.matchesService.getBracket(tournoiId, showFutureBool);
  }

  @Get(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE, UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer un match par ID (Coach/Académie/Parent)' })
  @ApiParam({ name: 'id', description: 'ID du match (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Match trouvé',
    type: MatchResponseDto,
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
    description: 'Match non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Match non trouvé',
        error: 'Not Found',
      },
    },
  })
  async findOne(@Param('id') id: string) {
    return this.matchesService.findOne(id);
  }

  @Patch(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Mettre à jour un match (scores, statut) (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID du match (MongoDB ObjectId)' })
  @ApiBody({ type: UpdateMatchDto })
  @ApiResponse({
    status: 200,
    description: 'Match mis à jour avec succès',
    type: MatchResponseDto,
  })
  @ApiBadRequestResponse({
    description: 'Données invalides ou scores manquants pour terminer un match',
    schema: {
      example: {
        statusCode: 400,
        message: 'Les scores sont requis pour terminer un match',
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
    description: 'Accès refusé : rôle insuffisant (Coach ou Académie requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Match non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Match non trouvé',
        error: 'Not Found',
      },
    },
  })
  async update(@Param('id') id: string, @Body() updateMatchDto: UpdateMatchDto) {
    return this.matchesService.update(id, updateMatchDto);
  }

  @Delete(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Supprimer un match (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID du match (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Match supprimé',
    schema: {
      example: {
        message: 'Match supprimé avec succès',
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
    description: 'Accès refusé : rôle insuffisant (Coach ou Académie requis)',
    schema: {
      example: {
        statusCode: 403,
        message: 'Accès refusé : rôle insuffisant',
        error: 'Forbidden',
      },
    },
  })
  @ApiNotFoundResponse({
    description: 'Match non trouvé',
    schema: {
      example: {
        statusCode: 404,
        message: 'Match non trouvé',
        error: 'Not Found',
      },
    },
  })
  async remove(@Param('id') id: string) {
    await this.matchesService.remove(id);
    return { message: 'Match supprimé avec succès' };
  }
}
