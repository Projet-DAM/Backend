import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { SubscriptionsController } from './subscriptions.controller';
import { SubscriptionsService } from './subscriptions.service';
import { SubscriptionsSchedulerService } from './subscriptions-scheduler.service';
import { Subscription, SubscriptionSchema } from './schemas/subscription.schema';
import { UsersModule } from '../users/users.module';
import { OffersModule } from '../offers/offers.module';
import { EmailModule } from '../common/services/email.module';
import { SubscriptionOptionsModule } from '../subscription-options/subscription-options.module';

import { ForecastService } from './forecast.service';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Subscription.name, schema: SubscriptionSchema }]),
    UsersModule,
    OffersModule,
    EmailModule,
    SubscriptionOptionsModule,
  ],
  controllers: [SubscriptionsController],
  providers: [SubscriptionsService, SubscriptionsSchedulerService, ForecastService],
  exports: [SubscriptionsService, ForecastService],
})
export class SubscriptionsModule { }






