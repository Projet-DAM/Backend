import { Controller, Get, UseGuards, Req } from '@nestjs/common';
import { ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument } from '../subscriptions/schemas/subscription.schema';

@ApiTags('Test')
@Controller('test')
export class TestController {
    constructor(
        @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    ) { }

    @Get('subscriptions-simple')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    async getSubscriptionsSimple(@Req() req: any) {
        try {
            // Version ultra-simple sans populate
            const subs = await this.subscriptionModel.find({}).limit(5).lean().exec();

            return {
                success: true,
                count: subs.length,
                data: subs.map(s => ({
                    id: s._id.toString(),
                    childId: s.childId.toString(),
                    parentId: s.parentId.toString(),
                    offerId: s.offerId.toString(),
                    status: s.status,
                    paymentStatus: s.paymentStatus
                }))
            };
        } catch (error) {
            return {
                success: false,
                error: error.message,
                stack: error.stack
            };
        }
    }

    @Get('subscriptions-populated')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    async getSubscriptionsPopulated(@Req() req: any) {
        try {
            const subs = await this.subscriptionModel
                .find({})
                .populate('childId', 'nom prenom')
                .populate('parentId', 'nom prenom')
                .populate('offerId', 'name price')
                .limit(5)
                .lean()
                .exec();

            return {
                success: true,
                count: subs.length,
                data: subs
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
