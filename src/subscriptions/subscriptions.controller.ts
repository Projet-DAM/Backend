import { Body, Controller, Get, HttpCode, HttpStatus, Param, Patch, Post, Query, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiBody, ApiOperation, ApiQuery, ApiTags } from '@nestjs/swagger';
import { SubscriptionsService } from './subscriptions.service';
import { ForecastService } from './forecast.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { CreateSubscriptionDto } from './dto/create-subscription.dto';
import { UpdateSubscriptionDto } from './dto/update-subscription.dto';
import { RecordPaymentDto } from './dto/record-payment.dto';
import { SubscriptionStatus, PaymentStatus } from './schemas/subscription.schema';

@ApiTags('Subscriptions')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
@Controller('subscriptions')
export class SubscriptionsController {
  constructor(
    private readonly service: SubscriptionsService,
    private readonly forecastService: ForecastService,
  ) { }

  @Post()
  @Roles(UserRole.PARENT)
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Créer un abonnement (PARENT)' })
  @ApiBody({ schema: { example: { childId: '65b1f0...child', offerId: '65b1f0...offer', autoRenew: true } } })
  create(@Body() dto: CreateSubscriptionDto, @Req() req: any) {
    return this.service.create(dto, { userId: req.user.userId, role: req.user.role });
  }

  @Get('my')
  @Roles(UserRole.PARENT)
  @ApiOperation({ summary: 'Lister mes abonnements (PARENT)' })
  mine(@Req() req: any) {
    return this.service.findMine({ userId: req.user.userId, role: req.user.role });
  }

  @Get('by-child/:childId')
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Lister les abonnements par enfant (portée contrôlée)' })
  byChild(@Param('childId') childId: string, @Req() req: any) {
    return this.service.findByChild(childId, { userId: req.user.userId, role: req.user.role });
  }

  @Get()
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Lister les abonnements (ADMIN/ACADEMIE)' })
  @ApiQuery({ name: 'status', required: false, enum: SubscriptionStatus })
  @ApiQuery({ name: 'paymentStatus', required: false, enum: PaymentStatus })
  @ApiQuery({ name: 'parentId', required: false })
  @ApiQuery({ name: 'childId', required: false })
  @ApiQuery({ name: 'page', required: false })
  @ApiQuery({ name: 'limit', required: false })
  @ApiQuery({ name: 'sort', required: false })
  list(
    @Query('status') status: SubscriptionStatus,
    @Query('paymentStatus') paymentStatus: PaymentStatus,
    @Query('parentId') parentId: string,
    @Query('childId') childId: string,
    @Query('page') page?: string,
    @Query('limit') limit?: string,
    @Query('sort') sort?: string,
    @Req() req?: any,
  ) {
    const userId = req?.user?.userId || req?.user?.sub;
    const role = req?.user?.role;

    if (!userId || !role) {
      throw new Error('User authentication data is missing');
    }

    return this.service.findAll(
      {
        status,
        paymentStatus,
        parentId,
        childId,
        page: page ? parseInt(page, 10) : 1,
        limit: limit ? parseInt(limit, 10) : 10,
        sort
      },
      { userId, role }
    );
  }

  @Get('forecast/revenue')
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Prévisions de revenus (ACADEMIE|ADMIN)' })
  async getRevenueForecast(@Req() req: any) {
    const academyId = req.user.role === UserRole.ACADEMIE ? req.user.userId : undefined;
    return this.forecastService.generateRevenueForecast(academyId);
  }

  @Get(':id')
  @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Récupérer un abonnement (portée contrôlée)' })
  get(@Param('id') id: string, @Req() req: any) {
    return this.service.findOne(id, { userId: req.user.userId, role: req.user.role });
  }

  @Patch(':id')
  @Roles(UserRole.PARENT, UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({
    summary: 'Mettre à jour un abonnement',
    description: 'Parent: peut modifier autoRenew, notes, et startDate (recalcule automatiquement endDate). Admin/Academy: peut modifier tous les champs.'
  })
  @ApiBody({
    schema: {
      example: {
        startDate: '2025-12-01T00:00:00.000Z',
        autoRenew: true,
        notes: 'Reporté au mois prochain'
      }
    }
  })
  update(@Param('id') id: string, @Body() dto: UpdateSubscriptionDto, @Req() req: any) {
    return this.service.update(id, dto, { userId: req.user.userId, role: req.user.role });
  }

  @Post(':id/pay')
  @Roles(UserRole.PARENT, UserRole.ACADEMIE)
  @ApiOperation({ summary: 'Enregistrer un paiement' })
  @ApiBody({ schema: { example: { amount: 100, currency: 'EUR', method: 'CASH', externalRef: 'PAY-123' } } })
  pay(@Param('id') id: string, @Body() dto: RecordPaymentDto, @Req() req: any) {
    return this.service.recordPayment(id, dto, { userId: req.user.userId, role: req.user.role });
  }

  @Post(':id/cancel')
  @Roles(UserRole.PARENT, UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Annuler un abonnement' })
  cancel(@Param('id') id: string, @Req() req: any) {
    return this.service.cancel(id, { userId: req.user.userId, role: req.user.role });
  }

  @Post(':id/suspend')
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Suspendre un abonnement' })
  suspend(@Param('id') id: string, @Req() req: any) {
    return this.service.suspend(id, { userId: req.user.userId, role: req.user.role });
  }

  @Post(':id/resume')
  @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Reprendre un abonnement suspendu' })
  resume(@Param('id') id: string, @Req() req: any) {
    return this.service.resume(id, { userId: req.user.userId, role: req.user.role });
  }

  @Post(':id/renew')
  @Roles(UserRole.PARENT, UserRole.ACADEMIE, UserRole.ADMIN)
  @ApiOperation({ summary: 'Renouveler un abonnement' })
  renew(@Param('id') id: string, @Req() req: any) {
    return this.service.renew(id, { userId: req.user.userId, role: req.user.role });
  }
}

