import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { getModelToken } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Subscription } from './src/subscriptions/schemas/subscription.schema';
import { Offer } from './src/offers/schemas/offer.schema';

async function bootstrap() {
    const app = await NestFactory.createApplicationContext(AppModule);
    const subModel = app.get<Model<any>>(getModelToken(Subscription.name));
    const offerModel = app.get<Model<any>>(getModelToken(Offer.name));

    const subs = await subModel.find({}).exec();
    console.log(`Checking ${subs.length} subscriptions for offer existence...`);

    for (const sub of subs) {
        if (!sub.offerId) {
            console.log(`❌ Sub ${sub._id}: Missing offerId`);
            continue;
        }

        if (!Types.ObjectId.isValid(sub.offerId.toString())) {
            console.log(`❌ Sub ${sub._id}: Invalid offerId format "${sub.offerId}"`);
            continue;
        }

        const offer = await offerModel.findById(sub.offerId).exec();
        if (!offer) {
            console.log(`⚠️ Sub ${sub._id}: Points to NON-EXISTENT offer ID "${sub.offerId}"`);
        } else {
            console.log(`✅ Sub ${sub._id}: Offer found ("${offer.name}")`);
        }
    }

    await app.close();
}

bootstrap().catch(err => {
    console.error(err);
    process.exit(1);
});
