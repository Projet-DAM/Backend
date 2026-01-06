
import { Module } from '@nestjs/common';
import { AnalyticsController } from './analytics.controller';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';

@Module({
    imports: [SubscriptionsModule],
    controllers: [AnalyticsController],
})
export class AnalyticsModule { }
