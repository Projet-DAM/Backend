import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { SubscriptionOptionsController } from './subscription-options.controller';
import { SubscriptionOptionsService } from './subscription-options.service';
import { SubscriptionOption, SubscriptionOptionSchema } from './schemas/subscription-option.schema';

@Module({
    imports: [
        MongooseModule.forFeature([
            { name: SubscriptionOption.name, schema: SubscriptionOptionSchema },
        ]),
    ],
    controllers: [SubscriptionOptionsController],
    providers: [SubscriptionOptionsService],
    exports: [SubscriptionOptionsService],
})
export class SubscriptionOptionsModule { }
