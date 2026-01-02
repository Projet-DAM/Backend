import { Module, forwardRef } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { MatchesService } from './matches.service';
import { MatchesController } from './matches.controller';
import { Match, MatchSchema } from './schemas/match.schema';
import { TournoiModule } from '../tournoi/tournoi.module';
import { Equipe, EquipeSchema } from '../equipes/schemas/equipe.schema';
import { UsersModule } from '../users/users.module';
import { GeminiModule } from '../gemini/gemini.module';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Match.name, schema: MatchSchema },
      { name: Equipe.name, schema: EquipeSchema },
    ]),
    forwardRef(() => TournoiModule),
    UsersModule,
    GeminiModule,
  ],
  controllers: [MatchesController],
  providers: [MatchesService],
  exports: [MatchesService],
})
export class MatchesModule { }
