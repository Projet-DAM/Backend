import { Body, Controller, Delete, Get, Param, Patch, Post, Query, Req, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, ValidationPipe } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiParam, ApiQuery, ApiResponse, ApiTags, ApiConsumes } from '@nestjs/swagger';
import { ActivitiesService } from './activities.service';
import { CreateActivityDto } from './dto/create-activity.dto';
import { UpdateActivityDto } from './dto/update-activity.dto';
import { QueryActivityDto } from './dto/query-activity.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { ImageFileValidator } from '../users/validators/image-file.validator';

@ApiTags('Activities')
@ApiBearerAuth('JWT-auth')
@Controller('activities')
export class ActivitiesController {
  constructor(private readonly activitiesService: ActivitiesService) {}

  @Post()
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @UseInterceptors(
    FileInterceptor('image', {
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
          cb(null, `activity-${uniqueSuffix}${extname(file.originalname)}`);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Créer une activité' })
  @ApiConsumes('multipart/form-data')
  @ApiResponse({ status: 201, description: 'Activité créée' })
  @ApiBody({
    description: 'Corps de la requête pour créer une activité',
    schema: {
      type: 'object',
      properties: {
        nom_activite: { type: 'string', example: 'Football U10' },
        description: { type: 'string', example: 'Entraînement hebdomadaire' },
        categorie: { type: 'string', example: 'Football' },
        date: { type: 'string', format: 'date', example: '2025-11-15' },
        heure: { type: 'string', example: '14:30' },
        duree: { type: 'number', example: 90 },
        capacite_max: { type: 'number', example: 20 },
        prix: { type: 'number', example: 10 },
        statut: { type: 'string', enum: ['ACTIVE', 'ANNULEE', 'TERMINEE', 'BROUILLON'], example: 'ACTIVE' },
        image: {
          type: 'string',
          format: 'binary',
          description: 'Image de l\'activité',
        },
      },
      required: ['nom_activite'],
    },
  })
  create(
    @Body(new ValidationPipe({ transform: true, transformOptions: { enableImplicitConversion: true } })) dto: CreateActivityDto,
    @UploadedFile(
      new ParseFilePipe({
        validators: [
          new MaxFileSizeValidator({ maxSize: 20000000 }), // 20MB
          new ImageFileValidator(),
        ],
        fileIsRequired: false,
      }),
    )
    file: Express.Multer.File | undefined,
    @Req() req: any,
  ) {
    if (file) {
      dto.image = `/uploads/${file.filename}`;
    }
    // Convertir les strings en nombres pour multipart/form-data
    if (dto.duree !== undefined && typeof dto.duree === 'string') {
      dto.duree = dto.duree === '' ? undefined : Number(dto.duree);
    }
    if (dto.capacite_max !== undefined && typeof dto.capacite_max === 'string') {
      dto.capacite_max = dto.capacite_max === '' ? undefined : Number(dto.capacite_max);
    }
    if (dto.prix !== undefined && typeof dto.prix === 'string') {
      dto.prix = dto.prix === '' ? undefined : Number(dto.prix);
    }
    return this.activitiesService.create(dto, req.user);
  }

  @Get()
  @ApiOperation({ summary: 'Lister les activités' })
  @ApiQuery({ name: 'categorie', required: false })
  @ApiQuery({ name: 'date', required: false, description: 'YYYY-MM-DD' })
  @ApiQuery({ name: 'coach', required: false })
  @ApiQuery({ name: 'academie', required: false })
  @ApiQuery({ name: 'programme', required: false })
  @ApiQuery({ name: 'statut', required: false })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'sortBy', required: false })
  @ApiQuery({ name: 'order', required: false })
  @ApiResponse({ status: 200, description: 'Liste paginée des activités' })
  findAll(@Query() query: QueryActivityDto) {
    return this.activitiesService.findAll(query);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Récupérer une activité par ID' })
  @ApiParam({ name: 'id', description: "ID de l'activité" })
  @ApiResponse({ status: 200, description: 'Activité trouvée' })
  @ApiResponse({ status: 404, description: 'Activité non trouvée' })
  findOne(@Param('id') id: string) {
    return this.activitiesService.findOne(id);
  }

  @Patch(':id')
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Mettre à jour une activité' })
  @ApiParam({ name: 'id', description: "ID de l'activité" })
  @ApiResponse({ status: 200, description: 'Activité mise à jour' })
  update(@Param('id') id: string, @Body() dto: UpdateActivityDto, @Req() req: any) {
    return this.activitiesService.update(id, dto, req.user);
  }

  @Delete(':id')
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Supprimer une activité' })
  @ApiParam({ name: 'id', description: "ID de l'activité" })
  @ApiResponse({ status: 200, description: 'Activité supprimée' })
  remove(@Param('id') id: string, @Req() req: any) {
    return this.activitiesService.remove(id, req.user);
  }
}





