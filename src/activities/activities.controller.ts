import { Body, Controller, Delete, Get, Param, Patch, Post, Query, Req } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiParam, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { ActivitiesService } from './activities.service';
import { CreateActivityDto } from './dto/create-activity.dto';
import { UpdateActivityDto } from './dto/update-activity.dto';
import { QueryActivityDto } from './dto/query-activity.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Activities')
@ApiBearerAuth('JWT-auth')
@Controller('activities')
export class ActivitiesController {
  constructor(private readonly activitiesService: ActivitiesService) {}

  @Post()
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Créer une activité' })
  @ApiResponse({ status: 201, description: 'Activité créée' })
  @ApiBody({
    description: 'Corps de la requête pour créer une activité',
    examples: {
      example1: {
        summary: 'Séance de football',
        value: {
          nom_activite: 'Football U10',
          description: "Entraînement hebdomadaire",
          categorie: 'Football',
          date: '2025-11-15',
          heure: '14:30',
          duree: 90,
          capacite_max: 20,
          prix: 10,
          statut: 'ACTIVE',
        } as CreateActivityDto,
      },
    },
  })
  create(@Body() dto: CreateActivityDto, @Req() req: any) {
    return this.activitiesService.create(dto, req.user);
  }

  @Get()
  @ApiOperation({ summary: 'Lister les activités' })
  @ApiQuery({ name: 'categorie', required: false })
  @ApiQuery({ name: 'date', required: false, description: 'YYYY-MM-DD' })
  @ApiQuery({ name: 'coach', required: false })
  @ApiQuery({ name: 'academie', required: false })
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



