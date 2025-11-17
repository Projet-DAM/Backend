import { BadRequestException, ForbiddenException, Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model, Types } from 'mongoose';
import { Offer, OfferDocument } from './schemas/offer.schema';
import { CreateOfferDto } from './dto/create-offer.dto';
import { UpdateOfferDto } from './dto/update-offer.dto';
import { UserRole } from '../users/interfaces/user-role.enum';
import { Subscription, SubscriptionDocument, SubscriptionStatus } from '../subscriptions/schemas/subscription.schema';

@Injectable()
export class OffersService {
  constructor(
    @InjectModel(Offer.name) private offerModel: Model<OfferDocument>,
    @InjectModel(Subscription.name) private subModel: Model<SubscriptionDocument>,
  ) {}

  async create(dto: CreateOfferDto & { academyId?: string }, actor: { userId: string; role: UserRole }): Promise<OfferDocument> {
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Seules les académies ou un admin peuvent créer une offre');
    }
    // academyId est ajouté par le controller depuis le token JWT
    const academyId = dto.academyId || actor.userId;
    if (actor.role === UserRole.ACADEMIE && academyId !== actor.userId) {
      throw new ForbiddenException('Une académie ne peut créer que ses propres offres');
    }
    if (dto.price <= 0) {
      throw new BadRequestException('Le prix doit être > 0');
    }
    if (dto.discountPct != null && (dto.discountPct < 0 || dto.discountPct > 100)) {
      throw new BadRequestException('discountPct doit être entre 0 et 100');
    }
    // Créer l'offre avec academyId inclus
    const offerData = {
      ...dto,
      academyId: new Types.ObjectId(academyId), // S'assurer que c'est un ObjectId
    };
    console.log('Création d\'offre avec academyId:', academyId, 'ObjectId:', offerData.academyId);
    const created = new this.offerModel(offerData);
    const saved = await created.save();
    console.log('Offre créée avec ID:', saved._id, 'academyId:', saved.academyId);
    return saved;
  }

  async findAll(params: { isActive?: boolean; academyId?: string; page?: number; limit?: number; sort?: string; }): Promise<{ data: OfferDocument[]; total: number; page: number; limit: number; }>{
    const { isActive, academyId } = params;
    const page = Math.max(1, params.page || 1);
    const limit = Math.min(100, Math.max(1, params.limit || 10));
    const sort = params.sort || '-createdAt';

    const filter: FilterQuery<OfferDocument> = {};
    if (typeof isActive === 'boolean') filter.isActive = isActive;
    if (academyId) {
      // Convertir academyId (string) en ObjectId pour la requête MongoDB
      // MongoDB peut stocker academyId comme ObjectId ou string, donc on essaie les deux
      try {
        const academyObjectId = new Types.ObjectId(academyId);
        // Filtrer par ObjectId OU par string (pour compatibilité avec les anciennes offres)
        filter.$or = [
          { academyId: academyObjectId },
          { academyId: academyId }
        ];
        console.log('Filtrage par academyId:', academyId, 'ObjectId:', academyObjectId);
      } catch (error) {
        // Si la conversion échoue, filtrer simplement par string
        filter.academyId = academyId;
        console.log('Filtrage par academyId (string):', academyId);
      }
    }

    console.log('Filtre MongoDB:', JSON.stringify(filter, null, 2));
    const [data, total] = await Promise.all([
      this.offerModel.find(filter).sort(sort).skip((page - 1) * limit).limit(limit).exec(),
      this.offerModel.countDocuments(filter),
    ]);
    console.log(`Offres trouvées: ${data.length} sur ${total} total`);
    if (data.length > 0) {
      console.log('Première offre trouvée:', {
        _id: data[0]._id,
        name: data[0].name,
        academyId: data[0].academyId,
        academyIdType: typeof data[0].academyId
      });
    }
    return { data, total, page, limit };
  }

  async findOne(id: string): Promise<OfferDocument> {
    const offer = await this.offerModel.findById(id).exec();
    if (!offer) throw new NotFoundException('Offre non trouvée');
    return offer;
    
  }

  async update(id: string, dto: UpdateOfferDto, actor: { userId: string; role: UserRole }): Promise<OfferDocument> {
    const offer = await this.findOne(id);
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Seules les académies ou un admin peuvent modifier une offre');
    }
    if (actor.role === UserRole.ACADEMIE && offer.academyId.toString() !== actor.userId) {
      throw new ForbiddenException('Vous ne pouvez modifier que vos offres');
    }
    if (dto.price != null && dto.price <= 0) {
      throw new BadRequestException('Le prix doit être > 0');
    }
    if (dto.discountPct != null && (dto.discountPct < 0 || dto.discountPct > 100)) {
      throw new BadRequestException('discountPct doit être entre 0 et 100');
    }
    Object.assign(offer, dto);
    return offer.save();
  }

  async remove(id: string, actor: { userId: string; role: UserRole }): Promise<void> {
    const offer = await this.findOne(id);
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Seules les académies ou un admin peuvent supprimer une offre');
    }
    // Si c'est une académie, vérifier qu'elle ne supprime que ses propres offres
    if (actor.role === UserRole.ACADEMIE && offer.academyId.toString() !== actor.userId) {
      throw new ForbiddenException('Vous ne pouvez supprimer que vos propres offres');
    }
    const activeCount = await this.subModel.countDocuments({ offerId: offer._id, status: SubscriptionStatus.ACTIVE });
    if (activeCount > 0) {
      throw new ConflictException('Impossible de supprimer: des abonnements actifs existent');
    }
    await this.offerModel.findByIdAndDelete(offer._id).exec();
  }
}


