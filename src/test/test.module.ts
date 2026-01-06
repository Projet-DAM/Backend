import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TestController } from './test.controller';
import { Subscription, SubscriptionSchema } from '../subscriptions/schemas/subscription.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Subscription.name, schema: SubscriptionSchema }]),
  ],
  controllers: [TestController],
})
export class TestModule { }
