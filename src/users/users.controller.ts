import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  Query,
  UseInterceptors,
  UploadedFile,
  ParseFilePipe,
  MaxFileSizeValidator,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiConsumes,
  ApiBody,
  ApiParam,
  ApiQuery,
} from '@nestjs/swagger';
import { UsersService } from './users.service';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserDto } from './dto/update-user.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from './interfaces/user-role.enum';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { ImageFileValidator } from './validators/image-file.validator';

@ApiTags('Users')
@Controller('users')
@ApiBearerAuth('JWT-auth')
export class UsersController {
  constructor(private readonly usersService: UsersService) {}

  @Post()
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Créer un nouvel utilisateur (Académie uniquement)' })
  @ApiResponse({ status: 201, description: 'Utilisateur créé avec succès' })
  @ApiResponse({ status: 409, description: 'Email déjà utilisé' })
  create(@Body() createUserDto: CreateUserDto) {
    return this.usersService.create(createUserDto);
  }

  @Get()
  @ApiOperation({ summary: 'Récupérer tous les utilisateurs' })
  @ApiQuery({ name: 'role', required: false, enum: UserRole, description: 'Filtrer par rôle' })
  @ApiResponse({ status: 200, description: 'Liste des utilisateurs' })
  findAll(@Query('role') role?: UserRole) {
    return this.usersService.findAll(role);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Récupérer un utilisateur par ID' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur trouvé' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  findOne(@Param('id') id: string) {
    return this.usersService.findById(id);
  }

  @Patch(':id')
  @ApiOperation({ summary: 'Mettre à jour un utilisateur' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur mis à jour' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  update(@Param('id') id: string, @Body() updateUserDto: UpdateUserDto) {
    return this.usersService.update(id, updateUserDto);
  }

  @Delete(':id')
  @Roles(UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Supprimer un utilisateur (Académie uniquement)' })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Utilisateur supprimé' })
  @ApiResponse({ status: 404, description: 'Utilisateur non trouvé' })
  remove(@Param('id') id: string) {
    return this.usersService.remove(id);
  }

  @Post(':id/link-child')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Lier un enfant à un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        childId: { type: 'string', example: '507f1f77bcf86cd799439011' },
      },
      required: ['childId'],
    },
  })
  @ApiResponse({ status: 200, description: 'Enfant lié avec succès' })
  @ApiResponse({ status: 404, description: 'Parent ou enfant non trouvé' })
  @ApiResponse({ status: 400, description: 'Erreur de validation' })
  linkChild(@Param('id') id: string, @Body('childId') childId: string) {
    return this.usersService.linkChild(id, childId);
  }

  @Get(':id/children')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Récupérer les enfants d\'un parent' })
  @ApiParam({ name: 'id', description: 'ID du parent' })
  @ApiResponse({ status: 200, description: 'Liste des enfants' })
  @ApiResponse({ status: 404, description: 'Parent non trouvé' })
  getChildren(@Param('id') id: string) {
    return this.usersService.getChildren(id);
  }

  @Post(':id/upload-photo')
  @UseInterceptors(
    FileInterceptor('photo', {
      storage: diskStorage({
        destination: './uploads',
        filename: (req, file, cb) => {
          const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
          cb(null, `${uniqueSuffix}${extname(file.originalname)}`);
        },
      }),
    }),
  )
  @ApiOperation({ summary: 'Uploader une photo de profil' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        photo: {
          type: 'string',
          format: 'binary',
        },
      },
    },
  })
  @ApiParam({ name: 'id', description: 'ID de l\'utilisateur' })
  @ApiResponse({ status: 200, description: 'Photo uploadée avec succès' })
  async uploadPhoto(
    @Param('id') id: string,
    @UploadedFile(
      new ParseFilePipe({
        validators: [
          new MaxFileSizeValidator({ maxSize: 20000000 }), // 20MB
          new ImageFileValidator(),
        ],
      }),
    )
    file: Express.Multer.File,
  ) {
    const photoUrl = `/uploads/${file.filename}`;
    return this.usersService.update(id, { photoProfil: photoUrl });
  }
}

