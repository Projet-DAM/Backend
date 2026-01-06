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
  ) { }

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

  async findAll(params: { isActive?: boolean; academyId?: string; page?: number; limit?: number; sort?: string; }): Promise<{ data: OfferDocument[]; total: number; page: number; limit: number; }> {
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
    // Si l'ID est invalide ou l'offre introuvable, on renvoie une offre "fantôme" 
    // pour éviter de faire planter le chargement des listes dans l'application mobile.
    if (!id || !Types.ObjectId.isValid(id)) {
      return {
        _id: id || new Types.ObjectId(),
        name: 'Offre (ID invalide)',
        price: 0,
        durationDays: 30,
        maxCapacity: 0,
        isActive: false
      } as any;
    }

    const offer = await this.offerModel.findById(id).exec();
    if (!offer) {
      return {
        _id: new Types.ObjectId(id),
        name: 'Offre Supprimée',
        price: 0,
        durationDays: 30,
        maxCapacity: 0,
        isActive: false
      } as any;
    }
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

  async getAllSubscribers(actor: { userId: string; role: UserRole }) {
    const { userId, role } = actor;

    // Filtre par académie si l'utilisateur est une académie
    const offerFilter: any = { isActive: true };
    if (role === UserRole.ACADEMIE && userId) {
      offerFilter.academyId = new Types.ObjectId(userId);
    }

    const offers = await this.offerModel.find(offerFilter).lean().exec();

    const offersWithSubscribers = await Promise.all(
      offers.map(async (offer) => {
        const subscriptions: any[] = await this.subModel
          .find({ offerId: offer._id })
          .populate('childId', 'nom prenom photoProfil')
          .populate('parentId', 'nom prenom email phoneNumber')
          .lean()
          .exec();

        const subscribers = subscriptions
          .filter(sub => sub.childId && typeof sub.childId === 'object' &&
            sub.parentId && typeof sub.parentId === 'object')
          .map(sub => ({
            child: {
              id: sub.childId._id?.toString() || sub.childId.id || '',
              nom: sub.childId.nom || '',
              prenom: sub.childId.prenom || '',
              photoProfil: sub.childId.photoProfil || null
            },
            parent: {
              id: sub.parentId._id?.toString() || sub.parentId.id || '',
              nom: sub.parentId.nom || '',
              prenom: sub.parentId.prenom || '',
              email: sub.parentId.email || '',
              phoneNumber: sub.parentId.phoneNumber || null
            },
            status: sub.status || 'PENDING',
            paymentStatus: sub.paymentStatus || 'UNPAID',
            startDate: sub.startDate ? (typeof sub.startDate === 'string' ? sub.startDate : sub.startDate.toISOString()) : new Date().toISOString(),
            endDate: sub.endDate ? (typeof sub.endDate === 'string' ? sub.endDate : sub.endDate.toISOString()) : new Date().toISOString()
          }));

        return {
          offer: {
            id: offer._id.toString(),
            name: offer.name || '',
            description: offer.description || '',
            type: offer.type || 'CUSTOM',
            durationDays: offer.durationDays || 30,
            price: offer.price || 0,
            discountPct: offer.discountPct || 0,
            conditions: offer.conditions || '',
            isActive: offer.isActive !== false,
            academyId: offer.academyId?.toString() || '',
            maxCapacity: offer.maxCapacity || 0,
            subscribersCount: subscribers.length,
            remainingPlaces: Math.max(0, (offer.maxCapacity || 0) - subscribers.length),
            isFull: (offer.maxCapacity || 0) > 0 && subscribers.length >= (offer.maxCapacity || 0),
            createdAt: offer.createdAt ? (typeof offer.createdAt === 'string' ? offer.createdAt : offer.createdAt.toISOString()) : new Date().toISOString(),
            updatedAt: offer.updatedAt ? (typeof offer.updatedAt === 'string' ? offer.updatedAt : offer.updatedAt.toISOString()) : new Date().toISOString()
          },
          subscribers
        };
      })
    );

    return {
      totalSubscribers: offersWithSubscribers.reduce((sum, o) => sum + o.subscribers.length, 0),
      offers: offersWithSubscribers
    };
  }
}


