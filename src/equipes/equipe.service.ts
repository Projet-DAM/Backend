import { Injectable, BadRequestException, ConflictException, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Equipe, EquipeDocument } from './schemas/equipe.schema';
import { CreateEquipeDto } from './dto/create-equipe.dto';
import { UpdateEquipeEnfantsDto } from './dto/update-equipe-enfants.dto';
import { SportTeamValidator } from './validators/sport-team.validator';
import { TournoiService } from '../tournoi/tournoi.service';
import { TournoiDocument } from '../tournoi/schemas/tournoi.schema';
import { Inscription, InscriptionDocument } from '../inscriptions/inscription.schema';

@Injectable()
export class EquipeService {
  constructor(
    @InjectModel(Equipe.name) private readonly equipeModel: Model<EquipeDocument>,
    @InjectModel(Inscription.name) private readonly inscriptionModel: Model<InscriptionDocument>,
    private readonly tournoiService: TournoiService,
  ) {}

  async create(createEquipeDto: CreateEquipeDto): Promise<EquipeDocument> {
    const tournoi = await this.getTournoiOrFail(createEquipeDto.tournoiId);
    await this.ensureUniqueName(createEquipeDto.nom, tournoi._id);

    const enfantsObjectIds = this.toObjectIds(createEquipeDto.enfants);
    const inscriptions = await this.findAndValidateInscriptions(enfantsObjectIds, tournoi._id);
    await this.ensureChildrenAvailable(enfantsObjectIds, tournoi._id);

    SportTeamValidator.validateComposition(tournoi.sport, createEquipeDto.formatEquipe, enfantsObjectIds.length);

    const equipe = new this.equipeModel({
      nom: createEquipeDto.nom,
      couleur: createEquipeDto.couleur,
      tournoi: tournoi._id,
      sport: tournoi.sport,
      formatEquipe: createEquipeDto.formatEquipe,
      enfants: inscriptions.map((i) => i._id),
    });

    const saved = await equipe.save();
    return saved.populate('enfants');
  }

  async findByTournoi(tournoiId: string): Promise<EquipeDocument[]> {
    const tournoi = await this.getTournoiOrFail(tournoiId);

    return this.equipeModel
      .find({ tournoi: tournoi._id })
      .populate('enfants')
      .sort({ nom: 1 })
      .exec();
  }

  async updateEnfants(equipeId: string, updateDto: UpdateEquipeEnfantsDto): Promise<EquipeDocument> {
    if ((!updateDto.ajouter || updateDto.ajouter.length === 0) && (!updateDto.retirer || updateDto.retirer.length === 0)) {
      throw new BadRequestException('Aucun enfant à ajouter ou retirer');
    }

    const equipe = await this.equipeModel.findById(equipeId);
    if (!equipe) {
      throw new NotFoundException('Équipe non trouvée');
    }

    const tournoi = await this.getTournoiOrFail(equipe.tournoi.toString());
    let currentIds = equipe.enfants.map((id) => id.toString());

    if (updateDto.retirer?.length) {
      updateDto.retirer.forEach((childId) => {
        if (!currentIds.includes(childId)) {
          throw new BadRequestException('Impossible de retirer un enfant qui n\'est pas dans l\'équipe');
        }
      });
      currentIds = currentIds.filter((id) => !updateDto.retirer?.includes(id));
    }

    if (updateDto.ajouter?.length) {
      const ajouterObjectIds = this.toObjectIds(updateDto.ajouter);
      const inscriptions = await this.findAndValidateInscriptions(ajouterObjectIds, tournoi._id);
      await this.ensureChildrenAvailable(ajouterObjectIds, tournoi._id, equipe._id.toString());

      inscriptions.forEach((inscription) => {
        if (!currentIds.includes(inscription._id.toString())) {
          currentIds.push(inscription._id.toString());
        }
      });
    }

    if (currentIds.length === 0) {
      throw new BadRequestException('Une équipe doit contenir au moins un enfant');
    }

    SportTeamValidator.validateComposition(equipe.sport, equipe.formatEquipe, currentIds.length);

    equipe.enfants = currentIds.map((id) => new Types.ObjectId(id));
    const saved = await equipe.save();
    return saved.populate('enfants');
  }

  async getClassement(tournoiId: string) {
    const tournoi = await this.getTournoiOrFail(tournoiId);
    const equipes = await this.equipeModel
      .find({ tournoi: tournoi._id })
      .populate('enfants')
      .sort({ points: -1, victoires: -1, defaites: 1, nom: 1 })
      .exec();

    const podium = {
      gold: equipes[0] || null,
      silver: equipes[1] || null,
      bronze: equipes[2] || null,
    };

    return {
      tournoiId: tournoi._id.toString(),
      tournoiNom: tournoi.nom,
      sport: tournoi.sport,
      categorieAge: tournoi.categorieAge,
      podium,
      classement: equipes,
    };
  }

  private async getTournoiOrFail(tournoiId: string): Promise<TournoiDocument> {
    if (!Types.ObjectId.isValid(tournoiId)) {
      throw new BadRequestException('ID de tournoi invalide');
    }
    const tournoi = await this.tournoiService.findById(tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }
    return tournoi;
  }

  private async ensureUniqueName(nom: string, tournoiId: Types.ObjectId): Promise<void> {
    const existing = await this.equipeModel.findOne({ tournoi: tournoiId, nom: { $regex: new RegExp(`^${nom}$`, 'i') } });
    if (existing) {
      throw new ConflictException('Une équipe avec ce nom existe déjà pour ce tournoi');
    }
  }

  private async ensureChildrenAvailable(
    enfantsIds: Types.ObjectId[],
    tournoiId: Types.ObjectId,
    ignoreEquipeId?: string,
  ): Promise<void> {
    if (enfantsIds.length === 0) {
      return;
    }

    const query: any = {
      tournoi: tournoiId,
      enfants: { $in: enfantsIds },
    };

    if (ignoreEquipeId) {
      query._id = { $ne: new Types.ObjectId(ignoreEquipeId) };
    }

    const conflit = await this.equipeModel.findOne(query).exec();
    if (conflit) {
      throw new ConflictException('Un ou plusieurs enfants sont déjà assignés à une autre équipe pour ce tournoi');
    }
  }

  private async findAndValidateInscriptions(
    enfantsIds: Types.ObjectId[],
    tournoiId: Types.ObjectId,
  ): Promise<InscriptionDocument[]> {
    if (!enfantsIds.length) {
      return [];
    }

    const inscriptions = await this.inscriptionModel.find({ _id: { $in: enfantsIds } }).exec();
    if (inscriptions.length !== enfantsIds.length) {
      throw new NotFoundException('Un ou plusieurs enfants sélectionnés n\'existent pas');
    }

    inscriptions.forEach((inscription) => {
      if (!inscription.tournoiId || inscription.tournoiId.toString() !== tournoiId.toString()) {
        throw new BadRequestException('Tous les enfants doivent être inscrits au même tournoi');
      }
    });

    return inscriptions;
  }

  private toObjectIds(ids: string[]): Types.ObjectId[] {
    return ids.map((id) => {
      if (!Types.ObjectId.isValid(id)) {
        throw new BadRequestException('ID enfant invalide');
      }
      return new Types.ObjectId(id);
    });
  }
}

