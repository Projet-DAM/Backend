import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TournoiService } from './tournoi.service';
import { TournoiController } from './tournoi.controller';
import { Tournoi, TournoiSchema } from './schemas/tournoi.schema';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Tournoi.name, schema: TournoiSchema }]),
  ],
  controllers: [TournoiController],
  providers: [TournoiService],
  exports: [TournoiService],
})
export class TournoiModule {}


