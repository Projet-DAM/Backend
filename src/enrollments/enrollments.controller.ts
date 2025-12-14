import {
    Controller,
    Get,
    Post,
    Body,
    Patch,
    Param,
    Delete,
    UseGuards,
    Req,
    UnauthorizedException,
    ForbiddenException,
    Query,
} from '@nestjs/common';
import {
    ApiTags,
    ApiOperation,
    ApiResponse,
    ApiBearerAuth,
    ApiParam,
    ApiQuery,
} from '@nestjs/swagger';
import { EnrollmentsService } from './enrollments.service';
import { CreateEnrollmentDto } from './dto/create-enrollment.dto';
import { UpdateEnrollmentDto } from './dto/update-enrollment.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Enrollments')
@Controller('enrollments')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
export class EnrollmentsController {
    constructor(private readonly enrollmentsService: EnrollmentsService) { }

    @Post()
    @Roles(UserRole.PARENT)
    @ApiOperation({
        summary: 'Inscrire des enfants à un programme (PARENT uniquement)',
    })
    @ApiResponse({
        status: 201,
        description: 'Inscriptions créées avec succès',
    })
    @ApiResponse({
        status: 409,
        description: 'Un ou plusieurs enfants sont déjà inscrits',
    })
    async create(@Req() req: any, @Body() createEnrollmentDto: CreateEnrollmentDto) {
        const parentId = req?.user?.userId;

        if (!parentId) {
            throw new UnauthorizedException('Token manquant ou invalide');
        }

        return this.enrollmentsService.createEnrollments(
            parentId,
            createEnrollmentDto,
        );
    }

    @Get()
    @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Récupérer les inscriptions (filtrées par rôle)',
    })
    @ApiQuery({
        name: 'programId',
        required: false,
        description: 'Filtrer par programme (pour les coachs)',
    })
    @ApiResponse({
        status: 200,
        description: 'Liste des inscriptions',
    })
    async findAll(@Req() req: any, @Query('programId') programId?: string) {
        const userId = req?.user?.userId;
        const role = req?.user?.role;

        if (!userId) {
            throw new UnauthorizedException('Token manquant ou invalide');
        }

        // Parent: voir uniquement ses inscriptions
        if (role === UserRole.PARENT) {
            return this.enrollmentsService.findByParent(userId);
        }

        // Coach: voir les inscriptions d'un programme spécifique
        if (role === UserRole.COACH) {
            if (programId) {
                return this.enrollmentsService.findByProgram(programId);
            }
            // Si pas de programId, retourner vide ou toutes les inscriptions
            return [];
        }

        // Académie: voir toutes les inscriptions
        if (role === UserRole.ACADEMIE) {
            if (programId) {
                return this.enrollmentsService.findByProgram(programId);
            }
            return this.enrollmentsService.findAll();
        }

        throw new ForbiddenException('Rôle non autorisé');
    }

    @Get('program/:programId')
    @Roles(UserRole.COACH, UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Récupérer les enfants inscrits à un programme (COACH/ACADEMIE)',
    })
    @ApiParam({
        name: 'programId',
        description: 'ID du programme',
    })
    @ApiResponse({
        status: 200,
        description: 'Liste des enfants inscrits',
    })
    async findByProgram(@Param('programId') programId: string) {
        return this.enrollmentsService.findByProgram(programId);
    }

    @Get('child/:childId')
    @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Récupérer les inscriptions d\'un enfant',
    })
    @ApiParam({
        name: 'childId',
        description: 'ID de l\'enfant',
    })
    @ApiResponse({
        status: 200,
        description: 'Liste des inscriptions de l\'enfant',
    })
    async findByChild(@Param('childId') childId: string) {
        return this.enrollmentsService.findByChild(childId);
    }

    @Get('check/:childId/:programId')
    @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Vérifier si un enfant est déjà inscrit à un programme',
    })
    @ApiParam({
        name: 'childId',
        description: 'ID de l\'enfant',
    })
    @ApiParam({
        name: 'programId',
        description: 'ID du programme',
    })
    @ApiResponse({
        status: 200,
        description: 'Retourne true si l\'enfant est inscrit, false sinon',
    })
    async checkEnrollment(
        @Param('childId') childId: string,
        @Param('programId') programId: string,
    ) {
        const isEnrolled = await this.enrollmentsService.isEnrolled(
            childId,
            programId,
        );
        return { isEnrolled };
    }

    @Get(':id')
    @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Récupérer une inscription par ID',
    })
    @ApiParam({
        name: 'id',
        description: 'ID de l\'inscription',
    })
    @ApiResponse({
        status: 200,
        description: 'Inscription trouvée',
    })
    @ApiResponse({
        status: 404,
        description: 'Inscription non trouvée',
    })
    async findOne(@Param('id') id: string) {
        return this.enrollmentsService.findOne(id);
    }

    @Patch(':id')
    @Roles(UserRole.COACH, UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Mettre à jour le statut d\'une inscription (COACH/ACADEMIE)',
    })
    @ApiParam({
        name: 'id',
        description: 'ID de l\'inscription',
    })
    @ApiResponse({
        status: 200,
        description: 'Inscription mise à jour',
    })
    @ApiResponse({
        status: 404,
        description: 'Inscription non trouvée',
    })
    async update(
        @Param('id') id: string,
        @Body() updateEnrollmentDto: UpdateEnrollmentDto,
    ) {
        return this.enrollmentsService.updateStatus(id, updateEnrollmentDto);
    }

    @Delete(':id')
    @Roles(UserRole.ACADEMIE)
    @ApiOperation({
        summary: 'Supprimer une inscription (ACADEMIE uniquement)',
    })
    @ApiParam({
        name: 'id',
        description: 'ID de l\'inscription',
    })
    @ApiResponse({
        status: 200,
        description: 'Inscription supprimée',
    })
    @ApiResponse({
        status: 404,
        description: 'Inscription non trouvée',
    })
    async remove(@Param('id') id: string) {
        return this.enrollmentsService.remove(id);
    }
}
