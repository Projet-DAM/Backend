import { BadRequestException, ForbiddenException, Injectable, NotFoundException, ConflictException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model } from 'mongoose';
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

  async create(dto: CreateOfferDto, actor: { userId: string; role: UserRole }): Promise<OfferDocument> {
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Seules les académies ou un admin peuvent créer une offre');
    }
    if (actor.role === UserRole.ACADEMIE && dto.academyId !== actor.userId) {
      throw new ForbiddenException('Une académie ne peut créer que ses propres offres');
    }
    if (dto.price <= 0) {
      throw new BadRequestException('Le prix doit être > 0');
    }
    if (dto.discountPct != null && (dto.discountPct < 0 || dto.discountPct > 100)) {
      throw new BadRequestException('discountPct doit être entre 0 et 100');
    }
    const created = new this.offerModel(dto);
    return created.save();
  }

  async findAll(params: { isActive?: boolean; academyId?: string; page?: number; limit?: number; sort?: string; }): Promise<{ data: OfferDocument[]; total: number; page: number; limit: number; }>{
    const { isActive, academyId } = params;
    const page = Math.max(1, params.page || 1);
    const limit = Math.min(100, Math.max(1, params.limit || 10));
    const sort = params.sort || '-createdAt';

    const filter: FilterQuery<OfferDocument> = {};
    if (typeof isActive === 'boolean') filter.isActive = isActive;
    if (academyId) filter.academyId = academyId as any;

    const [data, total] = await Promise.all([
      this.offerModel.find(filter).sort(sort).skip((page - 1) * limit).limit(limit).exec(),
      this.offerModel.countDocuments(filter),
    ]);
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
    if (actor.role !== UserRole.ADMIN) {
      throw new ForbiddenException('Seul un admin peut supprimer une offre');
    }
    const activeCount = await this.subModel.countDocuments({ offerId: offer._id, status: SubscriptionStatus.ACTIVE });
    if (activeCount > 0) {
      throw new ConflictException('Impossible de supprimer: des abonnements actifs existent');
    }
    await this.offerModel.findByIdAndDelete(offer._id).exec();
  }
}


