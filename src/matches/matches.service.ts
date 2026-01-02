import { Injectable, BadRequestException, NotFoundException, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Match, MatchDocument } from './schemas/match.schema';
import { CreateMatchDto } from './dto/create-match.dto';
import { UpdateMatchDto } from './dto/update-match.dto';
import { MatchPhase } from './interfaces/match-phase.enum';
import { MatchStatut } from './interfaces/match-statut.enum';
import { TournoiService } from '../tournoi/tournoi.service';
import { Equipe, EquipeDocument } from '../equipes/schemas/equipe.schema';
import { GeminiService } from '../gemini/gemini.service';
import { UsersService } from '../users/users.service';

@Injectable()
export class MatchesService {
  private readonly logger = new Logger(MatchesService.name);

  constructor(
    @InjectModel(Match.name) private matchModel: Model<MatchDocument>,
    @InjectModel(Equipe.name) private equipeModel: Model<EquipeDocument>,
    private tournoiService: TournoiService,
    private geminiService: GeminiService,
    private usersService: UsersService,
  ) { }

  async create(createMatchDto: CreateMatchDto): Promise<MatchDocument> {
    const tournoi = await this.tournoiService.findById(createMatchDto.tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    const matchData: any = {
      tournoiId: new Types.ObjectId(createMatchDto.tournoiId),
      phase: createMatchDto.phase,
      statut: createMatchDto.statut || MatchStatut.A_VENIR,
      ordre: createMatchDto.ordre,
    };

    if (createMatchDto.equipeA && createMatchDto.equipeB) {
      if (createMatchDto.equipeA === createMatchDto.equipeB) {
        throw new BadRequestException('Les équipes A et B doivent être différentes');
      }

      const equipeA = await this.equipeModel.findById(createMatchDto.equipeA);
      const equipeB = await this.equipeModel.findById(createMatchDto.equipeB);

      if (!equipeA || !equipeB) {
        throw new NotFoundException('Une ou plusieurs équipes non trouvées');
      }

      if (equipeA.tournoi.toString() !== createMatchDto.tournoiId || equipeB.tournoi.toString() !== createMatchDto.tournoiId) {
        throw new BadRequestException('Les équipes doivent appartenir au même tournoi');
      }

      matchData.equipeA = new Types.ObjectId(createMatchDto.equipeA);
      matchData.equipeB = new Types.ObjectId(createMatchDto.equipeB);
    }

    const match = new this.matchModel(matchData);
    return match.save();
  }

  async generateBracket(tournoiId: string): Promise<MatchDocument[]> {
    const tournoi = await this.tournoiService.findById(tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    const equipes = await this.equipeModel.find({ tournoi: new Types.ObjectId(tournoiId) }).exec();
    if (equipes.length < 2) {
      throw new BadRequestException('Un tournoi doit avoir au moins 2 équipes pour générer un arbre');
    }

    const existingMatches = await this.matchModel.find({ tournoiId: new Types.ObjectId(tournoiId) }).exec();
    if (existingMatches.length > 0) {
      throw new BadRequestException('Un arbre existe déjà pour ce tournoi. Supprimez les matchs existants avant de régénérer.');
    }

    const matches: MatchDocument[] = [];
    const numEquipes = equipes.length;

    if (numEquipes === 2) {
      const finale = await this.createMatchForPhase(tournoiId, equipes, MatchPhase.FINALE, 1);
      matches.push(finale);
    } else if (numEquipes === 4) {
      // Phase 1 : 2 matchs QUART_FINAL avec les 4 équipes
      const quart1 = await this.createMatchForPhase(tournoiId, [equipes[0], equipes[1]], MatchPhase.QUART_FINAL, 1);
      const quart2 = await this.createMatchForPhase(tournoiId, [equipes[2], equipes[3]], MatchPhase.QUART_FINAL, 2);
      matches.push(quart1, quart2);

      // Phase 2 : 1 match DEMI_FINAL qui attend les vainqueurs des quarts
      const demi = await this.createMatchForPhase(tournoiId, [], MatchPhase.DEMI_FINAL, 1);
      matches.push(demi);

      // Phase 3 : 1 match FINALE qui attend le vainqueur de la demi-finale
      const finale = await this.createMatchForPhase(tournoiId, [], MatchPhase.FINALE, 1);
      matches.push(finale);
    } else if (numEquipes === 6) {
      // Format pour 6 équipes : 2 quarts de finale + 2 demi-finales + 1 finale
      const equipesQuarts = equipes.slice(2, 6); // Équipes 3-6 jouent les quarts
      const equipesBye = equipes.slice(0, 2);    // Équipes 1-2 ont un bye direct en demies

      // Quarts de finale : Équipe 3 vs Équipe 6, Équipe 4 vs Équipe 5
      const quart1 = await this.createMatchForPhase(tournoiId, [equipesQuarts[0], equipesQuarts[3]], MatchPhase.QUART_FINAL, 1);
      const quart2 = await this.createMatchForPhase(tournoiId, [equipesQuarts[1], equipesQuarts[2]], MatchPhase.QUART_FINAL, 2);
      matches.push(quart1, quart2);

      // Demi-finales : Équipe 1 vs Gagnant Q2, Équipe 2 vs Gagnant Q1
      const demi1 = await this.createMatchForPhase(tournoiId, [], MatchPhase.DEMI_FINAL, 1);
      const demi2 = await this.createMatchForPhase(tournoiId, [], MatchPhase.DEMI_FINAL, 2);
      matches.push(demi1, demi2);

      // Finale
      const finale = await this.createMatchForPhase(tournoiId, [], MatchPhase.FINALE, 1);
      matches.push(finale);
    } else if (numEquipes >= 8) {
      const numQuarts = Math.min(8, numEquipes);
      const equipesQuarts = equipes.slice(0, numQuarts);

      for (let i = 0; i < numQuarts; i += 2) {
        const quart = await this.createMatchForPhase(
          tournoiId,
          [equipesQuarts[i], equipesQuarts[i + 1]],
          MatchPhase.QUART_FINAL,
          i / 2 + 1,
        );
        matches.push(quart);
      }

      const numDemis = numQuarts / 2;
      for (let i = 0; i < numDemis; i++) {
        const demi = await this.createMatchForPhase(tournoiId, [], MatchPhase.DEMI_FINAL, i + 1);
        matches.push(demi);
      }

      const finale = await this.createMatchForPhase(tournoiId, [], MatchPhase.FINALE, 1);
      matches.push(finale);
    } else {
      throw new BadRequestException('Le nombre d\'équipes doit être 2, 4, 6, 8, 16 ou 32');
    }

    this.logger.log(`Arbre généré pour le tournoi ${tournoiId} avec ${matches.length} matchs`);
    return matches;
  }

  private async createMatchForPhase(
    tournoiId: string,
    equipes: EquipeDocument[],
    phase: MatchPhase,
    ordre: number,
  ): Promise<MatchDocument> {
    const matchData: any = {
      tournoiId: new Types.ObjectId(tournoiId),
      phase,
      statut: MatchStatut.A_VENIR,
      ordre,
    };

    if (equipes.length === 2) {
      matchData.equipeA = equipes[0]._id;
      matchData.equipeB = equipes[1]._id;
    }

    const match = new this.matchModel(matchData);
    return match.save();
  }

  async update(id: string, updateMatchDto: UpdateMatchDto): Promise<MatchDocument> {
    const match = await this.matchModel.findById(id);
    if (!match) {
      throw new NotFoundException('Match non trouvé');
    }

    if (updateMatchDto.statut === MatchStatut.TERMINE) {
      if (updateMatchDto.scoreEquipeA === undefined || updateMatchDto.scoreEquipeB === undefined) {
        throw new BadRequestException('Les scores sont requis pour terminer un match');
      }

      if (updateMatchDto.scoreEquipeA === updateMatchDto.scoreEquipeB) {
        throw new BadRequestException('Un match ne peut pas se terminer sur un match nul. Utilisez une autre méthode de départage.');
      }

      match.scoreEquipeA = updateMatchDto.scoreEquipeA;
      match.scoreEquipeB = updateMatchDto.scoreEquipeB;
      match.vainqueur = updateMatchDto.scoreEquipeA > updateMatchDto.scoreEquipeB ? match.equipeA : match.equipeB;
      match.statut = MatchStatut.TERMINE;

      await this.progressWinnerToNextMatch(match);
      this.triggerAiFeedback(match);
    } else {
      if (updateMatchDto.statut) {
        match.statut = updateMatchDto.statut;
      }
      if (updateMatchDto.scoreEquipeA !== undefined) {
        match.scoreEquipeA = updateMatchDto.scoreEquipeA;
      }
      if (updateMatchDto.scoreEquipeB !== undefined) {
        match.scoreEquipeB = updateMatchDto.scoreEquipeB;
      }
      if (updateMatchDto.matchSuivantId) {
        match.matchSuivantId = new Types.ObjectId(updateMatchDto.matchSuivantId);
      }
    }

    return match.save();
  }

  private async progressWinnerToNextMatch(match: MatchDocument): Promise<void> {
    if (!match.vainqueur) {
      return;
    }

    const nextPhase = this.getNextPhase(match.phase);
    if (!nextPhase) {
      this.logger.log(`Match ${match._id} est la finale, pas de progression`);
      return;
    }

    const tournoiId = match.tournoiId;
    const nextMatches = await this.matchModel
      .find({
        tournoiId,
        phase: nextPhase,
      })
      .sort({ ordre: 1 })
      .exec();

    if (nextMatches.length === 0) {
      this.logger.warn(`Aucun match ${nextPhase} disponible pour la progression`);
      return;
    }

    const nextMatch = nextMatches.find((m) => !m.equipeA || !m.equipeB);
    if (!nextMatch) {
      this.logger.warn(`Tous les matchs ${nextPhase} sont déjà complets`);
      return;
    }

    if (!nextMatch.equipeA) {
      nextMatch.equipeA = match.vainqueur;
    } else if (!nextMatch.equipeB) {
      nextMatch.equipeB = match.vainqueur;
    }

    match.matchSuivantId = nextMatch._id;
    await match.save();
    await nextMatch.save();

    this.logger.log(`Vainqueur ${match.vainqueur} progressé vers le match ${nextMatch._id} (${nextPhase})`);
  }

  private getNextPhase(phase: MatchPhase): MatchPhase | null {
    switch (phase) {
      case MatchPhase.QUART_FINAL:
        return MatchPhase.DEMI_FINAL;
      case MatchPhase.DEMI_FINAL:
        return MatchPhase.FINALE;
      case MatchPhase.FINALE:
        return null;
      default:
        return null;
    }
  }

  async findByTournoi(tournoiId: string, showFuture: boolean = false): Promise<MatchDocument[]> {
    const tournoi = await this.tournoiService.findById(tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    const query: any = { tournoiId: new Types.ObjectId(tournoiId) };

    if (!showFuture) {
      query.statut = { $ne: MatchStatut.A_VENIR };
    }

    return this.matchModel
      .find(query)
      .populate({ path: 'equipeA', populate: { path: 'enfants' } })
      .populate({ path: 'equipeB', populate: { path: 'enfants' } })
      .populate({ path: 'vainqueur', populate: { path: 'enfants' } })
      .sort({ phase: 1, ordre: 1 })
      .exec();
  }

  async getBracket(tournoiId: string, showFuture: boolean = false): Promise<any> {
    const tournoi = await this.tournoiService.findById(tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    const matches = await this.findByTournoi(tournoiId, showFuture);

    const bracket = {
      quartFinal: matches.filter((m) => m.phase === MatchPhase.QUART_FINAL),
      demiFinal: matches.filter((m) => m.phase === MatchPhase.DEMI_FINAL),
      finale: matches.find((m) => m.phase === MatchPhase.FINALE) || null,
    };

    return {
      tournoiId: tournoi._id.toString(),
      tournoiNom: tournoi.nom,
      phases: bracket,
    };
  }

  async findOne(id: string): Promise<MatchDocument> {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }

    const match = await this.matchModel
      .findById(id)
      .populate({ path: 'equipeA', populate: { path: 'enfants' } })
      .populate({ path: 'equipeB', populate: { path: 'enfants' } })
      .populate({ path: 'vainqueur', populate: { path: 'enfants' } })
      .exec();

    if (!match) {
      throw new NotFoundException('Match non trouvé');
    }

    return match;
  }

  async remove(id: string): Promise<void> {
    const result = await this.matchModel.findByIdAndDelete(id);
    if (!result) {
      throw new NotFoundException('Match non trouvé');
    }
  }

  private async triggerAiFeedback(match: MatchDocument) {
    try {
      // Need to populate participants if not already
      const m: any = await this.matchModel.findById(match._id)
        .populate({ path: 'equipeA', populate: { path: 'enfants' } })
        .populate({ path: 'equipeB', populate: { path: 'enfants' } })
        .populate('tournoiId')
        .exec();

      if (!m || !m.equipeA || !m.equipeB) return;

      const processTeam = async (team: any, result: 'victoire' | 'defaite' | 'nul') => {
        if (!team.enfants || !Array.isArray(team.enfants)) return;

        for (const inscription of team.enfants) {
          // Find User
          // Note: Inscription does not guarantee a User exists. We try to find by name.
          const childUser = await this.usersService.findChildByName(inscription.enfantPrenom, inscription.enfantNom);

          if (childUser) {
            const dto = {
              childId: childUser._id.toString(),
              childName: inscription.enfantPrenom,
              matchId: match._id.toString(),
              matchResult: result,
              teamName: team.nom,
              score: `${m.scoreEquipeA}-${m.scoreEquipeB}`,
              phase: m.phase,
              tournamentName: m.tournoiId ? (m.tournoiId as any).nom : undefined
            };
            // Fire and forget, or await? iterating, so await is safer for rate limits but slower.
            // Given it's a background process triggered by update, we can await.
            await this.geminiService.sendFeedbackToConversations(dto);
          }
        }
      };

      const scoreA = m.scoreEquipeA;
      const scoreB = m.scoreEquipeB;

      let resA: 'victoire' | 'defaite' | 'nul' = 'nul';
      let resB: 'victoire' | 'defaite' | 'nul' = 'nul';

      if (scoreA > scoreB) { resA = 'victoire'; resB = 'defaite'; }
      else if (scoreB > scoreA) { resA = 'defaite'; resB = 'victoire'; }

      await processTeam(m.equipeA, resA);
      await processTeam(m.equipeB, resB);

    } catch (e) {
      this.logger.error(`Error triggering AI feedback: ${e.message}`, e.stack);
    }
  }
}
