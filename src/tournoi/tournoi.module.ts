import { Module, forwardRef } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TournoiService } from './tournoi.service';
import { TournoiController } from './tournoi.controller';
import { Tournoi, TournoiSchema } from './schemas/tournoi.schema';
import { InscriptionModule } from '../inscriptions/inscription.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Tournoi.name, schema: TournoiSchema }]),
    forwardRef(() => InscriptionModule),
  ],
  controllers: [TournoiController],
  providers: [TournoiService],
  exports: [TournoiService],
})
export class TournoiModule {}





