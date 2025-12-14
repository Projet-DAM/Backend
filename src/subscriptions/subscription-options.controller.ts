import { Controller, Get, UseGuards } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiTags } from '@nestjs/swagger';
import { SubscriptionOptionsService } from './subscription-options.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Subscription Options')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
@Controller('subscription-options')
export class SubscriptionOptionsController {
    constructor(private readonly optionsService: SubscriptionOptionsService) { }

    @Get()
    @Roles(UserRole.PARENT, UserRole.COACH, UserRole.ACADEMIE, UserRole.ADMIN)
    @ApiOperation({
        summary: 'Obtenir les options supplémentaires disponibles',
        description: 'Retourne la liste des options disponibles (tenue sportive, assurance, transport) avec leurs prix en TND'
    })
    getAvailableOptions() {
        return this.optionsService.getAvailableOptions();
    }
}
