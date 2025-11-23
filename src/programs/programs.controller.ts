import { Body, Controller, Delete, Get, Param, Patch, Post, Query, Req, UseInterceptors, UploadedFile, ParseFilePipe, MaxFileSizeValidator, ValidationPipe } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiParam, ApiQuery, ApiResponse, ApiTags, ApiConsumes } from '@nestjs/swagger';
import { ProgramsService } from './programs.service';
import { CreateProgramDto } from './dto/create-program.dto';
import { UpdateProgramDto } from './dto/update-program.dto';
import { QueryProgramDto } from './dto/query-program.dto';
import { ManageProgramActivitiesDto } from './dto/manage-program-activities.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { ImageFileValidator } from '../users/validators/image-file.validator';

@ApiTags('Programs')
@ApiBearerAuth('JWT-auth')
@Controller('programs')
export class ProgramsController {
  constructor(private readonly programsService: ProgramsService) {}

  @Post()
  @Roles(UserRole.ACADEMIE, UserRole.COACH)
  @UseInterceptors(
    FileInterceptor('image', {
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
          cb(null, `program-${uniqueSuffix}${extname(file.originalname)}`);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Créer un programme' })
  @ApiConsumes('multipart/form-data')
  @ApiResponse({ status: 201, description: 'Programme créé' })
  @ApiBody({
    description: 'Corps de la requête pour créer un programme',
    schema: {
      type: 'object',
      properties: {
        nom_programme: { type: 'string', example: 'Pré-saison U13' },
        description: { type: 'string', example: 'Programme de préparation' },
        objectif: { type: 'string', example: 'Améliorer la condition physique' },
        niveau: { type: 'string', example: 'Intermédiaire' },
        prix: { type: 'number', example: 99 },
        statut: { type: 'string', enum: ['BROUILLON', 'ACTIF', 'ARCHIVE'], example: 'BROUILLON' },
        activites: {
          type: 'array',
          items: { type: 'string' },
          example: ['507f1f77bcf86cd799439011', '507f1f77bcf86cd799439012'],
        },
        image: {
          type: 'string',
          format: 'binary',
          description: 'Image du programme',
        },
      },
      required: ['nom_programme'],
    },
  })
  create(
    @Body(new ValidationPipe({ 
      transform: true, 
      transformOptions: { enableImplicitConversion: true },
      skipMissingProperties: false,
      skipNullProperties: false,
      skipUndefinedProperties: false,
      whitelist: true,
      forbidNonWhitelisted: false, // Plus permissif pour multipart/form-data
    })) dto: CreateProgramDto,
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
    // Gérer l'image si un fichier est uploadé
    if (file) {
      dto.image = `/uploads/${file.filename}`;
    }
    
    // Convertir les strings en nombres pour multipart/form-data
    if (dto.prix !== undefined && dto.prix !== null) {
      if (typeof dto.prix === 'string') {
        dto.prix = dto.prix === '' ? undefined : Number(dto.prix);
        // Si la conversion échoue, définir à undefined
        if (isNaN(dto.prix as number)) {
          dto.prix = undefined;
        }
      }
    }
    
    // Convertir activites si c'est une string séparée par des virgules ou un array
    if (dto.activites !== undefined && dto.activites !== null) {
      const activitesValue = dto.activites as any;
      if (typeof activitesValue === 'string') {
        // Si c'est une string vide, définir à undefined
        if (activitesValue.trim() === '') {
          dto.activites = undefined;
        } else {
          // Sinon, convertir en array
          dto.activites = activitesValue.split(',').map((item: string) => item.trim()).filter((item: string) => item.length > 0);
        }
      } else if (!Array.isArray(activitesValue)) {
        // Si ce n'est ni une string ni un array, définir à undefined
        dto.activites = undefined;
      }
    }
    
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



