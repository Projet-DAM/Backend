import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ChatbotController } from './chatbot.controller';
import { ChatbotService } from './chatbot.service';
import { Offer, OfferSchema } from '../offers/schemas/offer.schema';
import { Subscription, SubscriptionSchema } from '../subscriptions/schemas/subscription.schema';
import { User, UserSchema } from '../users/entity/user.entity';

@Module({
    imports: [
        MongooseModule.forFeature([
            { name: Offer.name, schema: OfferSchema },
            { name: Subscription.name, schema: SubscriptionSchema },
            { name: User.name, schema: UserSchema },
        ]),
    ],
    controllers: [ChatbotController],
    providers: [ChatbotService],
    exports: [ChatbotService],
})
export class ChatbotModule { }
