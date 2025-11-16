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
  UseInterceptors,
  UploadedFile,
  ParseFilePipe,
  MaxFileSizeValidator,
  Inject,
  forwardRef,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiParam,
  ApiBody,
  ApiConsumes,
  ApiBadRequestResponse,
  ApiUnauthorizedResponse,
  ApiNotFoundResponse,
  ApiForbiddenResponse,
} from '@nestjs/swagger';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { TournoiService } from './tournoi.service';
import { CreateTournoiDto } from './dto/create-tournoi.dto';
import { UpdateTournoiDto } from './dto/update-tournoi.dto';
import { TournoiResponseDto } from './dto/tournoi-response.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { Public } from '../common/decorators/public.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { ImageFileValidator } from '../users/validators/image-file.validator';
import { InscriptionService } from '../inscriptions/inscription.service';

@ApiTags('Tournois')
@Controller('tournois')
@ApiBearerAuth('JWT-auth')
export class TournoiController {
  constructor(
    private readonly tournoiService: TournoiService,
    @Inject(forwardRef(() => InscriptionService))
    private readonly inscriptionService: InscriptionService,
  ) {}

  @Post()
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @HttpCode(HttpStatus.CREATED)
  @UseInterceptors(
    FileInterceptor('image', {
      storage: diskStorage({
        destination: './uploads/tournois',
        filename: (req, file, cb) => {
          const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
          cb(null, `tournoi-${uniqueSuffix}${extname(file.originalname)}`);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Créer un nouveau tournoi (Coach ou Académie uniquement)' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        nom: {
          type: 'string',
          example: 'Tournoi de Printemps U12',
          description: 'Nom du tournoi',
        },
        description: {
          type: 'string',
          example: 'Tournoi amical pour les jeunes joueurs de la région',
          description: 'Détails sur le tournoi',
        },
        sport: {
          type: 'string',
          example: 'football',
          description: 'Type de sport',
        },
        categorieAge: {
          type: 'string',
          example: 'U12',
          description: 'Tranche d\'âge concernée',
        },
        dateDebut: {
          type: 'string',
          format: 'date-time',
          example: '2024-05-15T09:00:00.000Z',
          description: 'Date de début du tournoi',
        },
        dateFin: {
          type: 'string',
          format: 'date-time',
          example: '2024-05-17T18:00:00.000Z',
          description: 'Date de fin du tournoi',
        },
        lieu: {
          type: 'string',
          example: 'Stade Municipal de la Ville',
          description: 'Lieu où se déroule le tournoi',
        },
        nombreParticipantsMax: {
          type: 'number',
          example: 16,
          description: 'Nombre maximal de participants',
        },
        fraisParticipation: {
          type: 'number',
          example: 25.5,
          description: 'Frais d\'inscription',
        },
        etat: {
          type: 'string',
          enum: ['ouvert', 'fermé', 'terminé'],
          example: 'ouvert',
          description: 'Statut du tournoi',
        },
        niveau: {
          type: 'string',
          enum: ['débutant', 'intermédiaire', 'avancé'],
          example: 'intermédiaire',
          description: 'Niveau du tournoi',
        },
        recompense: {
          type: 'string',
          example: 'Trophée et médailles pour les 3 premiers',
          description: 'Description de la récompense',
        },
        image: {
          type: 'string',
          format: 'binary',
          description: 'Image du tournoi (JPG, PNG, GIF, max 20MB)',
        },
      },
      required: ['nom', 'sport', 'categorieAge', 'dateDebut', 'dateFin', 'lieu'],
    },
  })
  @ApiResponse({
    status: 201,
    description: 'Tournoi créé avec succès',
    type: TournoiResponseDto,
  })
  @ApiBadRequestResponse({
    description: 'Données invalides, dates incorrectes ou fichier invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'La date de début doit être antérieure à la date de fin',
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
  async create(
    @Body() createTournoiDto: any,
    @UploadedFile(
      new ParseFilePipe({
        validators: [
          new MaxFileSizeValidator({ maxSize: 20000000 }), // 20MB
          new ImageFileValidator(),
        ],
        fileIsRequired: false,
      }),
    )
    file?: Express.Multer.File,
  ) {
    if (file) {
      const image = `/uploads/tournois/${file.filename}`;
      return this.tournoiService.create({ ...createTournoiDto, image });
    }
    return this.tournoiService.create(createTournoiDto);
  }

  @Get()
  @Public()
  @ApiOperation({ summary: 'Récupérer tous les tournois (Public)' })
  @ApiResponse({
    status: 200,
    description: 'Liste des tournois',
    type: [TournoiResponseDto],
  })
  findAll() {
    return this.tournoiService.findAll();
  }

  @Get(':id')
  @Public()
  @ApiOperation({ summary: 'Récupérer un tournoi par ID (Public)' })
  @ApiParam({ name: 'id', description: 'ID du tournoi (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Tournoi trouvé',
    type: TournoiResponseDto,
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
  findOne(@Param('id') id: string) {
    return this.tournoiService.findById(id);
  }

  @Get(':tournoiId/participants')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Récupérer tous les participants d\'un tournoi (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'tournoiId', description: 'ID du tournoi (MongoDB ObjectId)', example: '507f1f77bcf86cd799439012' })
  @ApiResponse({
    status: 200,
    description: 'Liste des participants du tournoi',
    schema: {
      example: [
        {
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
      ],
    },
  })
  @ApiBadRequestResponse({
    description: 'ID du tournoi invalide',
    schema: {
      example: {
        statusCode: 400,
        message: 'ID du tournoi invalide',
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
  getParticipants(@Param('tournoiId') tournoiId: string) {
    return this.inscriptionService.findByTournoi(tournoiId);
  }

 @Patch(':id')
@Roles(UserRole.COACH, UserRole.ACADEMIE)
@UseInterceptors(
  FileInterceptor('image', {
    storage: diskStorage({
      destination: './uploads/tournois',
      filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
        cb(null, `tournoi-${uniqueSuffix}${extname(file.originalname)}`);
      },
    }),
  }),
)
@ApiOperation({ summary: 'Mettre à jour un tournoi (Coach ou Académie uniquement)' })
@ApiParam({ name: 'id', description: 'ID du tournoi (MongoDB ObjectId)' })
@ApiConsumes('multipart/form-data')
async update(
  @Param('id') id: string,
  @Body() updateTournoiDto: any, // ⚠️ CHANGER ICI : utiliser 'any' au lieu de 'UpdateTournoiDto'
  @UploadedFile(
    new ParseFilePipe({
      validators: [
        new MaxFileSizeValidator({ maxSize: 20000000 }), // 20MB
        new ImageFileValidator(),
      ],
      fileIsRequired: false,
    }),
  )
  file?: Express.Multer.File,
) {
  if (file) {
    const imageUrl = `/uploads/tournois/${file.filename}`;
    return this.tournoiService.update(id, { ...updateTournoiDto, image: imageUrl });
  }
  return this.tournoiService.update(id, updateTournoiDto);
}

  @Delete(':id')
  @Roles(UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Supprimer un tournoi (Coach ou Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID du tournoi (MongoDB ObjectId)' })
  @ApiResponse({
    status: 200,
    description: 'Tournoi supprimé',
    schema: {
      example: {
        message: 'Tournoi supprimé avec succès',
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
  remove(@Param('id') id: string) {
    return this.tournoiService.remove(id);
  }
}

