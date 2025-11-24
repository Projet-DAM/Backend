import { Module, forwardRef } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { TournoiService } from './tournoi.service';
import { TournoiController } from './tournoi.controller';
import { Tournoi, TournoiSchema } from './schemas/tournoi.schema';
import { InscriptionModule } from '../inscriptions/inscription.module';
import { EquipeModule } from '../equipes/equipe.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Tournoi.name, schema: TournoiSchema }]),
    forwardRef(() => InscriptionModule),
    forwardRef(() => EquipeModule),
  ],
  controllers: [TournoiController],
  providers: [TournoiService],
  exports: [TournoiService],
})
export class TournoiModule {}





