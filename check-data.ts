import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { getModelToken } from '@nestjs/mongoose';
import { Offer } from './src/offers/schemas/offer.schema';
import { Model } from 'mongoose';

async function bootstrap() {
    const app = await NestFactory.createApplicationContext(AppModule);
    const offerModel = app.get<Model<any>>(getModelToken(Offer.name));
    const offers = await offerModel.find().exec();

    console.log('--- OFFRES DANS LA BASE ---');
    offers.forEach(o => {
        console.log(`- ${o.name}: ${o.price} TND (ID: ${o._id}, Active: ${o.isActive})`);
    });

    await app.close();
}
bootstrap();
