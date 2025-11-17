import { Body, Controller, Delete, Get, Param, Patch, Post, Query, Req } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiParam, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { ProgramsService } from './programs.service';
import { CreateProgramDto } from './dto/create-program.dto';
import { UpdateProgramDto } from './dto/update-program.dto';
import { QueryProgramDto } from './dto/query-program.dto';
import { ManageProgramActivitiesDto } from './dto/manage-program-activities.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Programs')
@ApiBearerAuth('JWT-auth')
@Controller('programs')
export class ProgramsController {
  constructor(private readonly programsService: ProgramsService) {}

  @Post()
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Créer un programme' })
  @ApiResponse({ status: 201, description: 'Programme créé' })
  @ApiBody({ type: CreateProgramDto })
  create(@Body() dto: CreateProgramDto, @Req() req: any) {
    return this.programsService.create(dto, req.user);
  }

  @Get()
  @ApiOperation({ summary: 'Lister les programmes' })
  @ApiQuery({ name: 'nom', required: false, description: 'Recherche par nom (contient)' })
  @ApiQuery({ name: 'coach', required: false })
  @ApiQuery({ name: 'academie', required: false })
  @ApiQuery({ name: 'statut', required: false })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'sortBy', required: false })
  @ApiQuery({ name: 'order', required: false })
  findAll(@Query() query: QueryProgramDto) {
    return this.programsService.findAll(query);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Récupérer un programme' })
  @ApiParam({ name: 'id', description: 'ID du programme' })
  findOne(@Param('id') id: string) {
    return this.programsService.findOne(id);
  }

  @Patch(':id')
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Mettre à jour un programme' })
  @ApiParam({ name: 'id', description: 'ID du programme' })
  update(@Param('id') id: string, @Body() dto: UpdateProgramDto, @Req() req: any) {
    return this.programsService.update(id, dto, req.user);
  }

  @Patch(':id/activities')
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Mettre à jour la composition du programme' })
  @ApiParam({ name: 'id', description: 'ID du programme' })
  updateActivities(
    @Param('id') id: string,
    @Body() dto: ManageProgramActivitiesDto,
    @Req() req: any,
  ) {
    return this.programsService.updateActivities(id, dto, req.user);
  }

  @Delete(':id')
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @ApiOperation({ summary: 'Supprimer un programme' })
  @ApiParam({ name: 'id', description: 'ID du programme' })
  remove(@Param('id') id: string, @Req() req: any) {
    return this.programsService.remove(id, req.user);
  }
}

