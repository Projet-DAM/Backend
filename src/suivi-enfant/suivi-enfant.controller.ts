import { Controller, Post, Body, Get, Param, Patch, Delete, Req } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiBody } from '@nestjs/swagger';
import { SuiviEnfantService } from './suivi-enfant.service';
import { CreateSuiviEnfantDto } from './dto/create-suivi-enfant.dto';
import { UpdateSuiviEnfantDto } from './dto/update-suivi-enfant.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { UsersService } from '../users/users.service';

@ApiTags('SuiviEnfant')
@ApiBearerAuth('JWT-auth')
@Controller('suivi-enfant')
export class SuiviEnfantController {
  constructor(
    private readonly suiviEnfantService: SuiviEnfantService,
    private readonly usersService: UsersService,
  ) {}

  @Post()
  @ApiBearerAuth('JWT-auth')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Créer un suivi pour un enfant (COACH uniquement)' })
  @ApiResponse({ status: 201, description: 'Suivi créé' })
  @ApiResponse({ status: 403, description: 'Accès refusé : rôle COACH requis' })
  @ApiBody({
    type: CreateSuiviEnfantDto,
    examples: {
      aCoachCreatingSuivi: {
        summary: 'Example for a coach creating a new suivi',
        value: {
          date_suivi: '2023-11-06T14:30:00Z',
          presence: true,
          performance: 85,
          commentaire: 'L\'enfant a montré une excellente participation et a bien compris les concepts.',
          enfantId: '690c6ec9a632ce229c8c1421',
          activityType: 'Entraînement',
          focusAreas: ['Dribble', 'Passe'],
          nextSessionGoals: ['Améliorer le tir'],
          effortLevel: 8,
          emotionalState: 'Motivé',
        },
      },
    },
  })
  create(@Body() dto: CreateSuiviEnfantDto, @Req() req: any) {
    return this.suiviEnfantService.create(dto, req.user);
  }

  @Get()
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Lister tous les suivis (PARENT, COACH, ACADEMIE)' })
  @ApiResponse({ status: 200, description: 'Liste des suivis' })
  findAll(@Req() req: any) {
    return this.suiviEnfantService.findAll(req.user);
  }

  @Get(':id')
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Obtenir un suivi par son id (PARENT, COACH, ACADEMIE)' })
  @ApiResponse({ status: 200, description: 'Détails du suivi' })
  findOne(@Param('id') id: string, @Req() req: any) {
    return this.suiviEnfantService.findOne(id, req.user);
  }

  @Get('enfant/:enfantId')
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Lister tous les suivis d\'un enfant donné (PARENT, COACH, ACADEMIE)' })
  @ApiResponse({ status: 200, description: 'Liste des suivis de l\'enfant' })
  findByEnfant(@Param('enfantId') enfantId: string, @Req() req: any) {
    return this.suiviEnfantService.findByEnfant(enfantId, req.user);
  }

  @Get('coach/children')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Récupérer la liste des enfants associés au coach (COACH uniquement)' })
  @ApiResponse({ status: 200, description: 'Liste des enfants du coach' })
  getCoachChildren(@Req() req: any) {
    return this.usersService.getChildrenOfCoach(req.user.userId);
  }

  @Patch(':id')
  @ApiBearerAuth('JWT-auth')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Mettre à jour un suivi (COACH or PARENT)' })
  @ApiResponse({ status: 200, description: 'Suivi mis à jour' })
  @ApiResponse({ status: 403, description: 'Accès refusé' })
  async update(@Param('id') id: string, @Body() dto: UpdateSuiviEnfantDto, @Req() req: any) {
    // Allow only specific fields to be updated
    const allowed = ['date_suivi','presence','performance','commentaire','focusAreas','nextSessionGoals'];
    const updatePayload: any = {};
    for (const k of allowed) {
      if ((dto as any)[k] !== undefined) updatePayload[k] = (dto as any)[k];
    }

    // never allow enfant/enfantId to be changed via PATCH
    const updated = await this.suiviEnfantService.update(id, updatePayload, req.user);
    return updated;
  }

  @Delete(':id')
  @ApiBearerAuth('JWT-auth')
  @Roles(UserRole.COACH)
  @ApiOperation({ summary: 'Supprimer un suivi (COACH uniquement)' })
  @ApiResponse({ status: 200, description: 'Suivi supprimé' })
  @ApiResponse({ status: 403, description: 'Accès refusé : rôle COACH requis' })
  remove(@Param('id') id: string, @Req() req: any) {
    return this.suiviEnfantService.remove(id, req.user);
  }
}
