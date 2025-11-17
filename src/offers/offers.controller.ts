import { Body, Controller, Delete, Get, HttpCode, HttpStatus, Param, Patch, Post, Query, Req, UseGuards, ForbiddenException, UsePipes, ValidationPipe } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiQuery, ApiResponse, ApiTags } from '@nestjs/swagger';
import { OffersService } from './offers.service';
import { CreateOfferDto } from './dto/create-offer.dto';
import { UpdateOfferDto } from './dto/update-offer.dto';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { Public } from '../common/decorators/public.decorator';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@ApiTags('Offers')
@Controller('offers')
export class OffersController {
  constructor(private readonly offersService: OffersService) {}

  @Post()
  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Créer une offre (ACADEMIE|ADMIN)' })
  @ApiBody({ schema: { example: { name: 'Mensuel', description: 'Accès mensuel', type: 'MONTHLY', durationDays: 30, price: 50, discountPct: 0, conditions: 'Non remboursable' } } })
  @UsePipes(new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: false, // Désactiver pour cette route spécifique
    transform: true,
  }))
  create(@Body() dto: CreateOfferDto, @Req() req: any) {
    // Log pour debug
    console.log('Received DTO:', JSON.stringify(dto, null, 2));
    console.log('Request body:', JSON.stringify(req.body, null, 2));
    
    // Extraire l'academyId depuis le token JWT
    const currentUserId = req.user?.userId || req.user?.sub;
    if (!currentUserId) {
      throw new ForbiddenException('Utilisateur non authentifié');
    }
    
    // Ajouter automatiquement l'academyId au DTO
    const dtoWithAcademyId = {
      ...dto,
      academyId: currentUserId.toString(),
    };
    
    return this.offersService.create(dtoWithAcademyId, { userId: currentUserId, role: req.user.role });
  }

  @Get()
  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard)
  @ApiOperation({ summary: 'Lister les offres' })
  @ApiQuery({ name: 'isActive', required: false, type: Boolean })
  @ApiQuery({ name: 'academyId', required: false, type: String })
  @ApiQuery({ name: 'page', required: false, type: Number })
  @ApiQuery({ name: 'limit', required: false, type: Number })
  @ApiQuery({ name: 'sort', required: false, type: String })
  findAll(
    @Query('isActive') isActive?: string,
    @Query('academyId') academyId?: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('sort') sort?: string,
    @Req() req?: any,
  ) {
    const currentUserRole = req?.user?.role;
    const currentUserId = req?.user?.userId || req?.user?.sub;
    
    console.log('findAll - currentUserRole:', currentUserRole);
    console.log('findAll - currentUserId:', currentUserId);
    console.log('findAll - academyId query param:', academyId);
    
    // Si l'utilisateur est une académie, filtrer automatiquement par son academyId
    if (currentUserRole === UserRole.ACADEMIE && currentUserId && !academyId) {
      academyId = currentUserId.toString();
      console.log('findAll - academyId auto-assigné:', academyId);
    }
    
    // Pour les parents, filtrer uniquement les offres actives
    let isActiveFilter: boolean | undefined;
    if (currentUserRole === UserRole.PARENT) {
      isActiveFilter = true; // Les parents voient uniquement les offres actives
    } else {
      isActiveFilter = typeof isActive === 'string' ? isActive === 'true' : undefined;
    }
    
    const parsed: any = {
      isActive: isActiveFilter,
      academyId,
      page: page ? parseInt(page, 10) : undefined,
      limit: limit ? parseInt(limit, 10) : undefined,
      sort,
    };
    return this.offersService.findAll(parsed);
  }

  @Get(':id')
  @Public()
  @ApiOperation({ summary: 'Récupérer une offre (public)' })
  findOne(@Param('id') id: string) {
    return this.offersService.findOne(id);
  }

  @Patch(':id')
  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Mettre à jour une offre (ACADEMIE|ADMIN)' })
  update(@Param('id') id: string, @Body() dto: UpdateOfferDto, @Req() req: any) {
    return this.offersService.update(id, dto, { userId: req.user.userId, role: req.user.role });
  }

  @Delete(':id')
  @ApiBearerAuth('JWT-auth')
  @UseGuards(JwtAuthGuard)
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Supprimer une offre (ACADEMIE|ADMIN)' })
  async remove(@Param('id') id: string, @Req() req: any) {
    const currentUserId = req.user?.userId || req.user?.sub;
    await this.offersService.remove(id, { userId: currentUserId, role: req.user.role });
    return;
  }
}


