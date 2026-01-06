import { Controller, Get, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import { UserRole } from '../users/interfaces/user-role.enum';

@ApiTags('Debug')
@Controller('debug')
export class DebugController {
    constructor(private readonly subscriptionsService: SubscriptionsService) { }

    @Get('subscriptions-raw')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    async getSubscriptionsRaw() {
        try {
            const result = await this.subscriptionsService.findAll(
                {},
                { userId: 'debug', role: UserRole.ADMIN }
            );

            return {
                success: true,
                count: result.data.length,
                total: result.total,
                sample: result.data[0] ? {
                    keys: Object.keys(result.data[0]),
                    hasId: 'id' in result.data[0],
                    hasChildId: 'childId' in result.data[0],
                    childIdType: typeof result.data[0].childId,
                    data: result.data[0]
                } : null
            };
        } catch (error) {
            return {
                success: false,
                error: error.message,
                stack: error.stack
            };
        }
    }
}
