import { Module } from '@nestjs/common';
import { PaymentsController } from './payments.controller';
import { PaymentsService } from './payments.service';
import { TwilioService } from './twilio.service';

@Module({
    controllers: [PaymentsController],
    providers: [PaymentsService, TwilioService],
})
export class PaymentsModule { }
