import { Controller, Get, Param, NotFoundException } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse } from '@nestjs/swagger';
import { SubscriptionOptionsService } from './subscription-options.service';
import { Public } from '../common/decorators/public.decorator';

@ApiTags('Subscription Options')
@Controller('subscription-options')
export class SubscriptionOptionsController {
    constructor(private readonly subscriptionOptionsService: SubscriptionOptionsService) { }

    @Get()
    @Public()
    @ApiOperation({ summary: 'Récupérer toutes les options d\'abonnement' })
    @ApiResponse({ status: 200, description: 'Liste de toutes les options' })
    findAll() {
        return this.subscriptionOptionsService.getAvailableOptions();
    }

    @Get('active')
    @Public()
    @ApiOperation({ summary: 'Récupérer les options d\'abonnement actives' })
    @ApiResponse({ status: 200, description: 'Liste des options actives' })
    findActive() {
        // Dans cette version statique, toutes les options retournées sont considérées comme actives
        return this.subscriptionOptionsService.getAvailableOptions();
    }

    @Get(':type')
    @Public()
    @ApiOperation({ summary: 'Récupérer une option d\'abonnement par Type' })
    @ApiResponse({ status: 200, description: 'Option trouvée' })
    @ApiResponse({ status: 404, description: 'Option non trouvée' })
    findOne(@Param('type') type: string) {
        const option = this.subscriptionOptionsService.getOptionByType(type as any);
        if (!option) {
            throw new NotFoundException('Option non trouvée');
        }
        return option;
    }
}
