import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ChatbotController } from './chatbot.controller';
import { ChatbotService } from './chatbot.service';
import { Offer, OfferSchema } from '../offers/schemas/offer.schema';
import { User, UserSchema } from '../users/entity/user.entity';
import { Subscription, SubscriptionSchema } from '../subscriptions/schemas/subscription.schema';

@Module({
    imports: [
        MongooseModule.forFeature([
            { name: Offer.name, schema: OfferSchema },
            { name: User.name, schema: UserSchema },
            { name: Subscription.name, schema: SubscriptionSchema },
        ]),
    ],
    controllers: [ChatbotController],
    providers: [ChatbotService],
})
export class ChatbotModule { }
