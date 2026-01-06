import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { getModelToken } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription } from './src/subscriptions/schemas/subscription.schema';
import { SubscriptionOptionsService } from './src/subscription-options/subscription-options.service';

async function bootstrap() {
    const app = await NestFactory.createApplicationContext(AppModule);
    const subModel = app.get<Model<any>>(getModelToken(Subscription.name));
    const optionsService = app.get(SubscriptionOptionsService);
    const availableOptions = optionsService.getAvailableOptions();

    const subs = await subModel.find({}).exec();
    console.log(`Checking ${subs.length} subscriptions for option format...`);

    let fixedCount = 0;
    for (const sub of subs) {
        if (sub.selectedOptions && sub.selectedOptions.length > 0) {
            let modified = false;
            const newOptions = sub.selectedOptions.map(opt => {
                if (typeof opt === 'string') {
                    const matched = availableOptions.find(o => o.type === opt);
                    if (matched) {
                        modified = true;
                        return {
                            type: matched.type,
                            name: matched.name,
                            price: matched.price,
                            currency: matched.currency
                        };
                    }
                }
                return opt;
            });

            if (modified) {
                sub.selectedOptions = newOptions;
                await sub.save();
                fixedCount++;
                console.log(`  Fixed options for sub ${sub._id}`);
            }
        }
    }

    console.log(`Successfully fixed options for ${fixedCount} subscriptions.`);
    await app.close();
}

bootstrap().catch(err => {
    console.error(err);
    process.exit(1);
});
