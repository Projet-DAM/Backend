import { Injectable, NotFoundException, BadRequestException, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Inscription, InscriptionDocument } from './inscription.schema';
import { CreateInscriptionDto } from './dto/create-inscription.dto';
import { UpdateInscriptionDto } from './dto/update-inscription.dto';
import { TournoiService } from '../tournoi/tournoi.service';
import { TournoiDocument } from '../tournoi/schemas/tournoi.schema';
import { NotificationService } from '../notifications/notification.service';

@Injectable()
export class InscriptionService {
  private readonly logger = new Logger(InscriptionService.name);

  constructor(
    @InjectModel(Inscription.name) private inscriptionModel: Model<InscriptionDocument>,
    private tournoiService: TournoiService,
    private readonly notificationService: NotificationService,
  ) {}

  async create(createInscriptionDto: CreateInscriptionDto): Promise<InscriptionDocument> {
    // Vérifier que le tournoi existe
    const tournoi = await this.tournoiService.findById(createInscriptionDto.tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    // Convertir la date de naissance
    const enfantDateNaissance = new Date(createInscriptionDto.enfantDateNaissance);

    // Préparer les données
    const inscriptionData = {
      tournoiId: new Types.ObjectId(createInscriptionDto.tournoiId),
      enfantPrenom: createInscriptionDto.enfantPrenom,
      enfantNom: createInscriptionDto.enfantNom,
      enfantDateNaissance,
      parentPrenom: createInscriptionDto.parentPrenom,
      parentNom: createInscriptionDto.parentNom,
      parentTelephone: createInscriptionDto.parentTelephone,
      montantInscription: createInscriptionDto.montantInscription,
      besoinsParticuliers: createInscriptionDto.besoinsParticuliers,
    };

    const inscription = new this.inscriptionModel(inscriptionData);
    const savedInscription = await inscription.save();

    await this.sendCoachNotification(savedInscription, tournoi);

    return savedInscription;
  }

  async findByTournoi(tournoiId: string): Promise<InscriptionDocument[]> {
    // Vérifier que le tournoi existe
    const tournoi = await this.tournoiService.findById(tournoiId);
    if (!tournoi) {
      throw new NotFoundException('Tournoi non trouvé');
    }

    if (!Types.ObjectId.isValid(tournoiId)) {
      throw new BadRequestException('ID du tournoi invalide');
    }

    return this.inscriptionModel
      .find({ tournoiId: new Types.ObjectId(tournoiId) })
      .sort({ createdAt: -1 })
      .exec();
  }

  async findOne(id: string): Promise<InscriptionDocument> {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }

    const inscription = await this.inscriptionModel.findById(id).exec();
    if (!inscription) {
      throw new NotFoundException('Inscription non trouvée');
    }

    return inscription;
  }

  async update(id: string, updateInscriptionDto: UpdateInscriptionDto): Promise<InscriptionDocument> {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }

    const inscription = await this.inscriptionModel.findById(id);
    if (!inscription) {
      throw new NotFoundException('Inscription non trouvée');
    }

    // Préparer les données de mise à jour
    const updateData: any = { ...updateInscriptionDto };

    // Convertir la date de naissance si elle est fournie
    if (updateInscriptionDto.enfantDateNaissance) {
      updateData.enfantDateNaissance = new Date(updateInscriptionDto.enfantDateNaissance);
    }

    Object.assign(inscription, updateData);
    return inscription.save();
  }

  async remove(id: string): Promise<void> {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }

    const result = await this.inscriptionModel.findByIdAndDelete(id);
    if (!result) {
      throw new NotFoundException('Inscription non trouvée');
    }
  }

  private async sendCoachNotification(inscription: InscriptionDocument, tournoi: TournoiDocument): Promise<void> {
    try {
      await this.notificationService.sendToCoaches(inscription, tournoi);
    } catch (error) {
      this.logger.warn(`Impossible d'envoyer la notification Firebase : ${(error as Error).message}`);
    }
  }
}



