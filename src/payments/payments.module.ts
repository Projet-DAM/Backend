import { Module } from '@nestjs/common';
import { PaymentsController } from './payments.controller';
import { PaymentsService } from './payments.service';
import { TwilioService } from './twilio.service';
import { OffersModule } from '../offers/offers.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { UsersModule } from '../users/users.module';
import { EmailModule } from '../common/services/email.module';

@Module({
  imports: [
    ConfigModule,
    OffersModule,
    SubscriptionsModule,
    UsersModule,
    EmailModule,
  ],
  controllers: [PaymentsController],
  providers: [PaymentsService, TwilioService],
  exports: [PaymentsService],

@Module({
    controllers: [PaymentsController],
    providers: [PaymentsService, TwilioService],
})
export class PaymentsModule { }
