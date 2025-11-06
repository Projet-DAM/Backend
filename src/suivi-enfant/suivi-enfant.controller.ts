import { Controller, Post, Body, Get, Param, Patch, Delete } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth } from '@nestjs/swagger';
import { SuiviEnfantService } from './suivi-enfant.service';
import { CreateSuiviEnfantDto } from './dto/create-suivi-enfant.dto';
import { UpdateSuiviEnfantDto } from './dto/update-suivi-enfant.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('SuiviEnfant')
@ApiBearerAuth('JWT-auth')
@Controller('suivi-enfant')
export class SuiviEnfantController {
  constructor(private readonly suiviEnfantService: SuiviEnfantService) {}

  @Post()
  @ApiBearerAuth('JWT-auth')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Créer un suivi pour un enfant (COACH uniquement)' })
  @ApiResponse({ status: 201, description: 'Suivi créé' })
  @ApiResponse({ status: 403, description: 'Accès refusé : rôle COACH requis' })
  create(@Body() dto: CreateSuiviEnfantDto) {
    return this.suiviEnfantService.create(dto);
  }

  @Get()
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ENFANT, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Lister tous les suivis (PARENT, COACH, ENFANT, ACADEMIE)' })
  @ApiResponse({ status: 200, description: 'Liste des suivis' })
  findAll() {
    return this.suiviEnfantService.findAll();
  }

  @Get(':id')
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ENFANT, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Obtenir un suivi par son id (PARENT, COACH, ENFANT, ACADEMIE)' })
  @ApiResponse({ status: 200, description: 'Détails du suivi' })
  findOne(@Param('id') id: string) {
    return this.suiviEnfantService.findOne(id);
  }

  @Get('enfant/:enfantId')
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ENFANT, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Lister tous les suivis d\'un enfant donné (PARENT, COACH, ENFANT, ACADEMIE)' })
  @ApiResponse({ status: 200, description: 'Liste des suivis de l\'enfant' })
  findByEnfant(@Param('enfantId') enfantId: string) {
    return this.suiviEnfantService.findByEnfant(enfantId);
  }

  @Patch(':id')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Mettre à jour un suivi (COACH uniquement)' })
  @ApiResponse({ status: 200, description: 'Suivi mis à jour' })
  @ApiResponse({ status: 403, description: 'Accès refusé : rôle COACH requis' })
  update(@Param('id') id: string, @Body() dto: UpdateSuiviEnfantDto) {
    return this.suiviEnfantService.update(id, dto);
  }

  @Delete(':id')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Supprimer un suivi (COACH uniquement)' })
  @ApiResponse({ status: 200, description: 'Suivi supprimé' })
  @ApiResponse({ status: 403, description: 'Accès refusé : rôle COACH requis' })
  remove(@Param('id') id: string) {
    return this.suiviEnfantService.remove(id);
  }
}
