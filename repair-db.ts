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

    const firstOffer = await offerModel.findOne({ isActive: true }).exec();
    if (!firstOffer) {
        console.log("No active offers found to use as fallback!");
        await app.close();
        return;
    }

    const subs = await subModel.find({}).exec();
    console.log(`Checking ${subs.length} subscriptions...`);

    let fixedCount = 0;
    for (const sub of subs) {
        let needsFix = false;

        if (!sub.offerId) {
            needsFix = true;
        } else if (!Types.ObjectId.isValid(sub.offerId.toString())) {
            needsFix = true;
        } else {
            const offer = await offerModel.findById(sub.offerId).exec();
            if (!offer) {
                needsFix = true;
            }
        }

        if (needsFix) {
            console.log(`Repairing sub ${sub._id}: pointing to "${firstOffer.name}" (${firstOffer._id})`);
            sub.offerId = firstOffer._id;
            await sub.save();
            fixedCount++;
        }
    }

    console.log(`Successfully repaired ${fixedCount} subscriptions.`);
    await app.close();
}

bootstrap().catch(err => {
    console.error(err);
    process.exit(1);
});
