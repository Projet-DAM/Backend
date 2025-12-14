import { Controller, Get, Req, UseGuards, Query } from '@nestjs/common';
import { AnalyticsService } from './analytics.service';
import { ApiBearerAuth, ApiOperation, ApiTags, ApiQuery } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Analytics')
@Controller('analytics')
export class AnalyticsController {
    constructor(private readonly analyticsService: AnalyticsService) { }

    @Get('revenue-forecast')
    @ApiBearerAuth('JWT-auth')
    @UseGuards(JwtAuthGuard)
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    @ApiOperation({ summary: 'Prédiction des revenus futurs (IA simple)' })
    @ApiQuery({ name: 'months', required: false, type: Number, description: 'Nombre de mois à prédire (défaut: 6)' })
    async getRevenueForecast(@Req() req: any, @Query('months') months?: number) {
        const currentUserId = req.user?.userId || req.user?.sub;
        const monthsToPredict = months ? Number(months) : 6;

        return this.analyticsService.getRevenueForecast(
            { userId: currentUserId, role: req.user.role },
            monthsToPredict
        );
    }
}
