import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { getModelToken } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription } from './src/subscriptions/schemas/subscription.schema';
import { User } from './src/users/entity/user.entity';
import { Offer } from './src/offers/schemas/offer.schema';

async function bootstrap() {
    const app = await NestFactory.createApplicationContext(AppModule);
    const subModel = app.get<Model<any>>(getModelToken(Subscription.name));
    const userModel = app.get<Model<any>>(getModelToken(User.name));
    const offerModel = app.get<Model<any>>(getModelToken(Offer.name));

    const subs = await subModel.find({}).exec();
    console.log(`Deep check for ${subs.length} subscriptions...`);

    for (const sub of subs) {
        const child = await userModel.findById(sub.childId).exec();
        const parent = await userModel.findById(sub.parentId).exec();
        const offer = await offerModel.findById(sub.offerId).exec();

        const status = {
            id: sub._id,
            child: child ? '✅' : '❌ NULL',
            parent: parent ? '✅' : '❌ NULL',
            offer: offer ? '✅' : '❌ NULL'
        };

        console.log(`Sub ${status.id}: Child:${status.child}, Parent:${status.parent}, Offer:${status.offer}`);
        if (!child || !parent || !offer) {
            console.log(`  Détails: childId=${sub.childId}, parentId=${sub.parentId}, offerId=${sub.offerId}`);
        }
    }

    await app.close();
}

bootstrap().catch(err => {
    console.error(err);
    process.exit(1);
});
