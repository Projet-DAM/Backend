import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { SubscriptionsController } from './subscriptions.controller';
import { SubscriptionsService } from './subscriptions.service';
import { Subscription, SubscriptionSchema } from './schemas/subscription.schema';
import { UsersModule } from '../users/users.module';
import { OffersModule } from '../offers/offers.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Subscription.name, schema: SubscriptionSchema }]),
    UsersModule,
    OffersModule,
  ],
  controllers: [SubscriptionsController],
  providers: [SubscriptionsService],
})
export class SubscriptionsModule {}






