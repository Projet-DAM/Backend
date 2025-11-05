import { Controller, Post, Body, Get, Param, Patch, Delete } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { SuiviEnfantService } from './suivi-enfant.service';
import { CreateSuiviEnfantDto } from './dto/create-suivi-enfant.dto';
import { UpdateSuiviEnfantDto } from './dto/update-suivi-enfant.dto';

@ApiTags('SuiviEnfant')
@Controller('suivi-enfant')
export class SuiviEnfantController {
  constructor(private readonly suiviEnfantService: SuiviEnfantService) {}

  @Post()
  @ApiOperation({ summary: 'Créer un suivi pour un enfant' })
  @ApiResponse({ status: 201, description: 'Suivi créé' })
  create(@Body() dto: CreateSuiviEnfantDto) {
    return this.suiviEnfantService.create(dto);
  }

  @Get()
  @ApiOperation({ summary: 'Lister tous les suivis' })
  findAll() {
    return this.suiviEnfantService.findAll();
  }

  @Get(':id')
  @ApiOperation({ summary: 'Obtenir un suivi par son id' })
  findOne(@Param('id') id: string) {
    return this.suiviEnfantService.findOne(id);
  }

  @Get('enfant/:enfantId')
  @ApiOperation({ summary: 'Lister tous les suivis d’un enfant donné' })
  findByEnfant(@Param('enfantId') enfantId: string) {
    return this.suiviEnfantService.findByEnfant(enfantId);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Mettre à jour un suivi' })
  update(@Param('id') id: string, @Body() dto: UpdateSuiviEnfantDto) {
    return this.suiviEnfantService.update(id, dto);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Supprimer un suivi' })
  remove(@Param('id') id: string) {
    return this.suiviEnfantService.remove(id);
  }
}
