
import { Controller, Get, Req, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { ForecastService } from '../subscriptions/forecast.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Analytics')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
@Controller('analytics')
export class AnalyticsController {
    constructor(private readonly forecastService: ForecastService) { }

    @Get('revenue-forecast')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    @ApiOperation({ summary: 'Prévisions de revenus (ACADEMIE|ADMIN)' })
    async getRevenueForecast(@Req() req: any) {
        const academyId = req.user.role === UserRole.ACADEMIE ? (req.user.userId || req.user.sub) : undefined;
        return this.forecastService.generateRevenueForecast(academyId);
    }
}
