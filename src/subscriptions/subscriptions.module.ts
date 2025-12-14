import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ScheduleModule } from '@nestjs/schedule';
import { SubscriptionsController } from './subscriptions.controller';
import { SubscriptionsService } from './subscriptions.service';
import { SubscriptionsSchedulerService } from './subscriptions-scheduler.service';
import { SubscriptionOptionsService } from './subscription-options.service';
import { SubscriptionOptionsController } from './subscription-options.controller';
import { Subscription, SubscriptionSchema } from './schemas/subscription.schema';
import { UsersModule } from '../users/users.module';
import { OffersModule } from '../offers/offers.module';
import { CommonModule } from '../common/common.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Subscription.name, schema: SubscriptionSchema }]),
    ScheduleModule.forRoot(),
    UsersModule,
    OffersModule,
    CommonModule,
  ],
  controllers: [SubscriptionsController, SubscriptionOptionsController],
  providers: [SubscriptionsService, SubscriptionsSchedulerService, SubscriptionOptionsService],
  exports: [SubscriptionsService, SubscriptionOptionsService],
})
export class SubscriptionsModule { }






