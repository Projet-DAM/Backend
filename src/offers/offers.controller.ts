import { Body, Controller, Delete, Get, HttpCode, HttpStatus, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
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
  @ApiBody({ schema: { example: { name: 'Mensuel', description: 'Accès mensuel', type: 'MONTHLY', durationDays: 30, price: 50, discountPct: 0, conditions: 'Non remboursable', academyId: '65b1f0...academy' } } })
  create(@Body() dto: CreateOfferDto, @Req() req: any) {
    return this.offersService.create(dto, { userId: req.user.userId, role: req.user.role });
  }

  @Get()
  @Public()
  @ApiOperation({ summary: 'Lister les offres (public)' })
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
  ) {
    const parsed: any = {
      isActive: typeof isActive === 'string' ? isActive === 'true' : undefined,
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
  @Roles(UserRole.ADMIN)
  @HttpCode(HttpStatus.NO_CONTENT)
  @ApiOperation({ summary: 'Supprimer une offre (ADMIN)' })
  async remove(@Param('id') id: string, @Req() req: any) {
    await this.offersService.remove(id, { userId: req.user.userId, role: req.user.role });
    return;
  }
}


