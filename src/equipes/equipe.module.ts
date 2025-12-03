import { Module, forwardRef } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { EquipeService } from './equipe.service';
import { EquipeController } from './equipe.controller';
import { Equipe, EquipeSchema } from './schemas/equipe.schema';
import { TournoiModule } from '../tournoi/tournoi.module';
import { Inscription, InscriptionSchema } from '../inscriptions/inscription.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Equipe.name, schema: EquipeSchema },
      { name: Inscription.name, schema: InscriptionSchema },
    ]),
    forwardRef(() => TournoiModule),
  ],
  controllers: [EquipeController],
  providers: [EquipeService],
  exports: [EquipeService],
})
export class EquipeModule {}

