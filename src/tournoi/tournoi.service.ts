import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Tournoi, TournoiDocument } from './schemas/tournoi.schema';
import { CreateTournoiDto } from './dto/create-tournoi.dto';
import { UpdateTournoiDto } from './dto/update-tournoi.dto';
import { TournoiEtat } from './interfaces/tournoi-etat.enum';

@Injectable()
export class TournoiService {
  constructor(
    @InjectModel(Tournoi.name) private tournoiModel: Model<TournoiDocument>,
  ) {}

  async create(createTournoiDto: CreateTournoiDto & { imageUrl?: string }): Promise<TournoiDocument> {
    // Validation des dates
    const dateDebut = new Date(createTournoiDto.dateDebut);
    const dateFin = new Date(createTournoiDto.dateFin);

    if (dateDebut >= dateFin) {
      throw new BadRequestException('La date de début doit être antérieure à la date de fin');
    }

    // Préparer les données
    const tournoiData: any = {
      nom: createTournoiDto.nom,
      description: createTournoiDto.description,
      sport: createTournoiDto.sport,
      categorieAge: createTournoiDto.categorieAge,
      dateDebut,
      dateFin,
      lieu: createTournoiDto.lieu,
      nombreParticipantsMax: createTournoiDto.nombreParticipantsMax,
      fraisParticipation: createTournoiDto.fraisParticipation,
      etat: createTournoiDto.etat || TournoiEtat.OUVERT,
      niveau: createTournoiDto.niveau,
      recompense: createTournoiDto.recompense,
    };

    if (createTournoiDto.imageUrl) {
      tournoiData.imageUrl = createTournoiDto.imageUrl;
    }

    const tournoi = new this.tournoiModel(tournoiData);
    return tournoi.save();
  }

  async findAll(): Promise<TournoiDocument[]> {
    return this.tournoiModel.find().sort({ dateDebut: 1 }).exec();
  }

  async findById(id: string): Promise<TournoiDocument | null> {
    return this.tournoiModel.findById(id).exec();
  }

  async update(id: string, updateTournoiDto: UpdateTournoiDto): Promise<TournoiDocument> {
    const tournoi = await this.tournoiModel.findById(id);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    // Validation des dates si elles sont modifiées
    let dateDebut = tournoi.dateDebut;
    let dateFin = tournoi.dateFin;

    if (updateTournoiDto.dateDebut) {
      dateDebut = new Date(updateTournoiDto.dateDebut);
    }

    if (updateTournoiDto.dateFin) {
      dateFin = new Date(updateTournoiDto.dateFin);
    }

    // Vérifier que la date de début est antérieure à la date de fin
    if (dateDebut >= dateFin) {
      throw new BadRequestException('La date de début doit être antérieure à la date de fin');
    }

    // Préparer les données de mise à jour
    const updateData: any = { ...updateTournoiDto };
    
    if (updateTournoiDto.dateDebut) {
      updateData.dateDebut = dateDebut;
    }
    
    if (updateTournoiDto.dateFin) {
      updateData.dateFin = dateFin;
    }

    Object.assign(tournoi, updateData);
    return tournoi.save();
  }

  async remove(id: string): Promise<void> {
    const result = await this.tournoiModel.findByIdAndDelete(id);
    if (!result) {
      throw new NotFoundException('Tournoi non trouvé');
    }
  }
}

