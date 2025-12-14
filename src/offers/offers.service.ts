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

  /**
   * Enrich offer with calculated fields (subscribers count, remaining quota)
   */
  private async enrichOffer(offer: OfferDocument): Promise<any> {
    const activeSubsCount = await this.subModel.countDocuments({
      offerId: offer._id,
      status: { $in: [SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING] }
    });

    const offerObj = offer.toObject();

    // Calculate remaining places if quota is set
    let remainingPlaces: number | null = null;
    if (offer.maxCapacity !== undefined && offer.maxCapacity !== null) {
      const cap = Number(offer.maxCapacity);
      remainingPlaces = cap - activeSubsCount;
      if (remainingPlaces < 0) {
        remainingPlaces = 0;
      }
    }

    return {
      ...offerObj,
      subscribersCount: activeSubsCount,
      remainingPlaces,
      isFull: remainingPlaces !== null && remainingPlaces === 0
    };
  }

  async findAll(params: { isActive?: boolean; academyId?: string; page?: number; limit?: number; sort?: string; }): Promise<{ data: any[]; total: number; page: number; limit: number; }> {
    const { isActive, academyId } = params;
    const page = Math.max(1, params.page || 1);
    const limit = Math.min(100, Math.max(1, params.limit || 10));
    const sort = params.sort || '-createdAt';

    const filter: FilterQuery<OfferDocument> = {};
    if (typeof isActive === 'boolean') filter.isActive = isActive;
    if (academyId) {
      try {
        const academyObjectId = new Types.ObjectId(academyId);
        filter.$or = [
          { academyId: academyObjectId },
          { academyId: academyId }
        ];
      } catch (error) {
        filter.academyId = academyId;
      }
    }

    const [offers, total] = await Promise.all([
      this.offerModel.find(filter).sort(sort).skip((page - 1) * limit).limit(limit).exec(),
      this.offerModel.countDocuments(filter),
    ]);

    // Enrich all offers with quota info
    const enrichedData = await Promise.all(offers.map(offer => this.enrichOffer(offer)));

    return { data: enrichedData, total, page, limit };
  }

  async findOne(id: string): Promise<any> {
    const offer = await this.offerModel.findById(id).exec();
    if (!offer) throw new NotFoundException('Offre non trouvée');
    return this.enrichOffer(offer);
  }

  async update(id: string, dto: UpdateOfferDto, actor: { userId: string; role: UserRole }): Promise<OfferDocument> {
    const offer = await this.offerModel.findById(id);
    if (!offer) throw new NotFoundException('Offre non trouvée');

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
    const offer = await this.offerModel.findById(id);
    if (!offer) throw new NotFoundException('Offre non trouvée');

    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Seules les académies ou un admin peuvent supprimer une offre');
    }
    if (actor.role === UserRole.ACADEMIE && offer.academyId.toString() !== actor.userId) {
      throw new ForbiddenException('Vous ne pouvez supprimer que vos propres offres');
    }

    // Check for ANY subscription to ensure data integrity
    // Using robust ID check (String or ObjectId)
    const offerIdObj = offer._id;
    const offerIdStr = offer._id.toString();

    const subsCount = await this.subModel.countDocuments({
      $or: [
        { offerId: offerIdObj },
        { offerId: offerIdStr }
      ]
      // Removing status filter -> Protects even if subscription is EXPIRED or CANCELLED
      // This ensures history is preserved. User should archive (isActive=false) instead.
    });

    if (subsCount > 0) {
      throw new ConflictException(
        `Impossible de supprimer cette offre car elle est liée à ${subsCount} abonnement(s) (historique inclus). ` +
        `Veuillez plutôt désactiver l'offre pour la masquer.`
      );
    }

    await this.offerModel.findByIdAndDelete(offer._id).exec();
  }

  async getSubscribers(offerId: string, actor: { userId: string; role: UserRole }) {
    const offer = await this.offerModel.findById(offerId);
    if (!offer) throw new NotFoundException('Offre non trouvée');

    // Security check
    if (actor.role === UserRole.ACADEMIE && offer.academyId.toString() !== actor.userId) {
      throw new ForbiddenException('Accès refusé');
    }
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Accès refusé');
    }

    // Find all subscriptions for this offer, populate child info
    const subs = await this.subModel.find({ offerId: offer._id })
      .populate('childId', 'nom prenom dateNaissance photoProfil')
      .populate('parentId', 'nom prenom email phoneNumber')
      .exec();

    return subs.map(sub => ({
      subscriptionId: sub._id,
      status: sub.status,
      startDate: sub.startDate,
      endDate: sub.endDate,
      child: sub.childId,
      parent: sub.parentId
    }));
  }

  /**
   * Get all subscriptions grouped by offer for an academy
   * Returns all offers with their subscribers
   */
  async getAllSubscribersGroupedByOffer(actor: { userId: string; role: UserRole }) {
    try {
      console.log('getAllSubscribersGroupedByOffer - Start', { userId: actor.userId, role: actor.role });

      // Security check
      if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
        throw new ForbiddenException('Accès refusé');
      }

      // Get all offers for this academy
      const filter: any = {};
      if (actor.role === UserRole.ACADEMIE) {
        try {
          filter.academyId = new Types.ObjectId(actor.userId);
        } catch (error) {
          console.error('Error converting userId to ObjectId:', error);
          // Fallback: try with string
          filter.academyId = actor.userId;
        }
      }

      console.log('Filter:', filter);
      const offers = await this.offerModel.find(filter).exec();
      console.log(`Found ${offers.length} offers`);

      // For each offer, get subscribers
      const result = await Promise.all(
        offers.map(async (offer) => {
          try {
            // Try to match offerId as ObjectId or String to be safe
            const offerIdObj = offer._id;
            const offerIdStr = offer._id.toString();

            console.log(`Searching subs for offer ${offer.name} (${offerIdStr})`);

            const subs = await this.subModel.find({
              $or: [
                { offerId: offerIdObj },
                { offerId: offerIdStr }
              ]
            })
              .populate('childId', 'nom prenom dateNaissance photoProfil')
              .populate('parentId', 'nom prenom email phoneNumber')
              .exec();

            console.log(`Found ${subs.length} sub(s) for offer ${offerIdStr}`);

            const activeSubsCount = await this.subModel.countDocuments({
              $or: [
                { offerId: offerIdObj },
                { offerId: offerIdStr }
              ],
              status: { $in: [SubscriptionStatus.ACTIVE, SubscriptionStatus.PENDING] }
            });

            return {
              offer: {
                _id: offer._id,
                name: offer.name,
                type: offer.type,
                price: offer.price,
                maxCapacity: offer.maxCapacity,
                subscribersCount: activeSubsCount
              },
              subscribers: subs.map(sub => ({
                subscriptionId: sub._id,
                status: sub.status,
                startDate: sub.startDate,
                endDate: sub.endDate,
                child: sub.childId,
                parent: sub.parentId
              }))
            };
          } catch (error) {
            console.error(`Error processing offer ${offer._id}:`, error);
            return {
              offer: {
                _id: offer._id,
                name: offer.name,
                type: offer.type,
                price: offer.price,
                maxCapacity: offer.maxCapacity,
                subscribersCount: 0
              },
              subscribers: []
            };
          }
        })
      );

      // Filter out offers with no subscribers
      const offersWithSubscribers = result.filter(item => item.subscribers.length > 0);

      // Calculate total subscribers
      const totalSubscribers = offersWithSubscribers.reduce(
        (sum, item) => sum + item.subscribers.length,
        0
      );

      console.log(`Returning ${offersWithSubscribers.length} offers with ${totalSubscribers} total subscribers`);

      return {
        totalSubscribers,
        offers: offersWithSubscribers
      };
    } catch (error) {
      console.error('getAllSubscribersGroupedByOffer - Error:', error);
      throw error;
    }
  }
}
