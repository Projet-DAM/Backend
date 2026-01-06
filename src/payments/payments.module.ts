import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
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
})
export class PaymentsModule { }
