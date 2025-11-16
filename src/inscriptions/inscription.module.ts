import { Module, forwardRef } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { InscriptionService } from './inscription.service';
import { InscriptionController } from './inscription.controller';
import { Inscription, InscriptionSchema } from './inscription.schema';
import { TournoiModule } from '../tournoi/tournoi.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Inscription.name, schema: InscriptionSchema }]),
    forwardRef(() => TournoiModule),
  ],
  controllers: [InscriptionController],
  providers: [InscriptionService],
  exports: [InscriptionService],
})
export class InscriptionModule {}

