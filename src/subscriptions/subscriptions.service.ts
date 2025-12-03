import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model } from 'mongoose';
import { Subscription, SubscriptionDocument, SubscriptionStatus, PaymentStatus } from './schemas/subscription.schema';
import { CreateSubscriptionDto } from './dto/create-subscription.dto';
import { UpdateSubscriptionDto } from './dto/update-subscription.dto';
import { UsersService } from '../users/users.service';
import { OffersService } from '../offers/offers.service';
import { OfferDocument } from '../offers/schemas/offer.schema';
import { RecordPaymentDto } from './dto/record-payment.dto';
import { UserRole } from '../users/interfaces/user-role.enum';

@Injectable()
export class SubscriptionsService {
  constructor(
    @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    private usersService: UsersService,
    private offersService: OffersService,
  ) {}

  private computeEndDate(start: Date, offer: OfferDocument): Date {
    const end = new Date(start);
    end.setDate(end.getDate() + offer.durationDays - 1);
    return end;
  }

  private assertFutureOrToday(date: Date) {
    const today = new Date();
    const startOfToday = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
    if (date < startOfToday) {
      throw new BadRequestException('startDate doit être >= aujourd\'hui');
    }
  }

  /**
   * Extract parent ID from child document, handling both populated and non-populated cases
   */
  private extractParentId(child: any): string | null {
    if (!child || !child.parent) {
      return null;
    }
    
    const parent = child.parent;
    
    // If it's an object with _id (populated User)
    if (typeof parent === 'object' && parent !== null) {
      if ('_id' in parent) {
        return String(parent._id);
      }
      // If it's an ObjectId, it has toString method
      if (typeof parent.toString === 'function') {
        return String(parent);
      }
    }
    
    // If it's already a string or can be converted
    return String(parent);
  }

  async create(dto: CreateSubscriptionDto, actor: { userId: string; role: UserRole }): Promise<SubscriptionDocument> {
    if (actor.role !== UserRole.PARENT) {
      throw new ForbiddenException('Seul un parent peut créer un abonnement');
    }
    // Validate parent-child relation
    const child = await this.usersService.findById(dto.childId);
    if (!child) throw new NotFoundException('Enfant non trouvé');
    
    const parentId = this.extractParentId(child);
    const actorId = String(actor.userId).trim();
    
    if (!parentId || parentId.trim() !== actorId) {
      throw new ForbiddenException(
        `Cet enfant n'appartient pas au parent authentifié. ` +
        `Parent ID de l'enfant: ${parentId || 'null'}, ` +
        `ID du parent authentifié: ${actorId}`
      );
    }

    const offer = await this.offersService.findOne(dto.offerId);
    if (!offer.isActive) {
      throw new ConflictException('Offre inactive');
    }

    const startDate = dto.startDate ? new Date(dto.startDate) : new Date();
    this.assertFutureOrToday(startDate);
    const endDate = this.computeEndDate(startDate, offer);

    const created = new this.subscriptionModel({
      childId: dto.childId,
      parentId: actor.userId,
      offerId: dto.offerId,
      startDate,
      endDate,
      autoRenew: !!dto.autoRenew,
      status: SubscriptionStatus.PENDING,
      paymentStatus: PaymentStatus.UNPAID,
      transactions: [],
    });
    return created.save();
  }

  async findAll(filters: { status?: SubscriptionStatus; paymentStatus?: PaymentStatus; parentId?: string; childId?: string; page?: number; limit?: number; sort?: string; }, actor: { userId: string; role: UserRole }) {
    const page = Math.max(1, filters.page || 1);
    const limit = Math.min(100, Math.max(1, filters.limit || 10));
    const sort = filters.sort || '-createdAt';
    const query: FilterQuery<SubscriptionDocument> = {};
    if (filters.status) query.status = filters.status;
    if (filters.paymentStatus) query.paymentStatus = filters.paymentStatus;
    if (filters.parentId) query.parentId = filters.parentId as any;
    if (filters.childId) query.childId = filters.childId as any;

    // Scope: Academy can later be restricted to own offers' subscribers, but for now ADMIN/ACADEMIE full list
    if (![UserRole.ADMIN, UserRole.ACADEMIE].includes(actor.role)) {
      throw new ForbiddenException('Accès refusé');
    }

    const [data, total] = await Promise.all([
      this.subscriptionModel.find(query).sort(sort).skip((page - 1) * limit).limit(limit).exec(),
      this.subscriptionModel.countDocuments(query),
    ]);
    return { data, total, page, limit };
  }

  async findMine(actor: { userId: string; role: UserRole }) {
    if (actor.role !== UserRole.PARENT) throw new ForbiddenException('Accès parent requis');
    const data = await this.subscriptionModel
      .find({ parentId: actor.userId })
      .populate('offerId')
      .populate('childId')
      .exec();
    return data;
  }

  async findByChild(childId: string, actor: { userId: string; role: UserRole }) {
    if ([UserRole.PARENT].includes(actor.role)) {
      const child = await this.usersService.findById(childId);
      if (!child) throw new NotFoundException('Enfant non trouvé');
      const parentId = this.extractParentId(child);
      const actorId = String(actor.userId).trim();
      if (!parentId || parentId.trim() !== actorId) {
        throw new ForbiddenException('Accès refusé pour cet enfant');
      }
    } else if (![UserRole.COACH, UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Accès refusé');
    }

    return this.subscriptionModel
      .find({ childId })
      .populate('offerId')
      .populate('childId')
      .exec();
  }

  async findOne(id: string, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) {
      throw new ForbiddenException('Accès refusé');
    }
    return sub;
  }

  private ensureStatusTransition(current: SubscriptionStatus, next: SubscriptionStatus) {
    const allowed: Record<SubscriptionStatus, SubscriptionStatus[]> = {
      [SubscriptionStatus.PENDING]: [SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED],
      [SubscriptionStatus.ACTIVE]: [SubscriptionStatus.SUSPENDED, SubscriptionStatus.CANCELLED, SubscriptionStatus.EXPIRED],
      [SubscriptionStatus.SUSPENDED]: [SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELLED],
      [SubscriptionStatus.CANCELLED]: [],
      [SubscriptionStatus.EXPIRED]: [],
    };
    if (!allowed[current].includes(next)) {
      throw new ConflictException(`Transition de statut invalide: ${current} -> ${next}`);
    }
  }

  async update(id: string, dto: UpdateSubscriptionDto, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).populate('offerId').exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');

    // Check if subscription is cancelled (cannot be modified)
    if (sub.status === SubscriptionStatus.CANCELLED) {
      throw new ConflictException('Un abonnement annulé ne peut pas être modifié');
    }

    if (actor.role === UserRole.PARENT) {
      if (sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');
      
      // Parent may update autoRenew, notes, and startDate
      if (dto.status != null || dto.paymentStatus != null) {
        throw new ForbiddenException('Le parent ne peut pas modifier status/paymentStatus');
      }

      // Update startDate if provided
      if (dto.startDate != null) {
        const newStartDate = new Date(dto.startDate);
        this.assertFutureOrToday(newStartDate);
        
        // Recalculate endDate based on the offer's duration
        let offerId: string;
        if (typeof sub.offerId === 'object' && sub.offerId !== null && '_id' in sub.offerId) {
          offerId = (sub.offerId as any)._id.toString();
        } else {
          offerId = String(sub.offerId);
        }
        const offer = await this.offersService.findOne(offerId);
        if (!offer) throw new NotFoundException('Offre non trouvée');
        
        sub.startDate = newStartDate;
        sub.endDate = this.computeEndDate(newStartDate, offer);
      }

      if (dto.autoRenew != null) sub.autoRenew = dto.autoRenew;
      if (dto.notes != null) sub.notes = dto.notes;
      
      return sub.save();
    }

    if (![UserRole.ADMIN, UserRole.ACADEMIE].includes(actor.role)) {
      throw new ForbiddenException('Accès refusé');
    }

    // Admin/Academy can update all fields
    if (dto.status && dto.status !== sub.status) {
      this.ensureStatusTransition(sub.status, dto.status);
      sub.status = dto.status;
    }

    if (dto.paymentStatus && dto.paymentStatus !== sub.paymentStatus) {
      sub.paymentStatus = dto.paymentStatus;
    }

    // Update startDate if provided (for admin/academy)
    if (dto.startDate != null) {
      const newStartDate = new Date(dto.startDate);
      this.assertFutureOrToday(newStartDate);
      
      // Recalculate endDate based on the offer's duration
      let offerId: string;
      if (typeof sub.offerId === 'object' && sub.offerId !== null && '_id' in sub.offerId) {
        offerId = (sub.offerId as any)._id.toString();
      } else {
        offerId = String(sub.offerId);
      }
      const offer = await this.offersService.findOne(offerId);
      if (!offer) throw new NotFoundException('Offre non trouvée');
      
      sub.startDate = newStartDate;
      sub.endDate = this.computeEndDate(newStartDate, offer);
    }

    if (dto.autoRenew != null) sub.autoRenew = dto.autoRenew;
    if (dto.notes != null) sub.notes = dto.notes;

    return sub.save();
  }

  async recordPayment(id: string, dto: RecordPaymentDto, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (![UserRole.PARENT, UserRole.ACADEMIE].includes(actor.role)) throw new ForbiddenException('Accès refusé');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');
    if (sub.status === SubscriptionStatus.CANCELLED) throw new ConflictException('Abonnement annulé');

    const tx = {
      amount: dto.amount,
      currency: dto.currency,
      method: dto.method,
      externalRef: dto.externalRef,
      date: dto.date ? new Date(dto.date) : new Date(),
      status: 'SUCCESS' as const,
    };
    sub.transactions.push(tx as any);

    // Compute net price
    const offer = await this.offersService.findOne(sub.offerId.toString());
    const netPrice = offer.price * (1 - (offer.discountPct || 0) / 100);
    const totalPaid = sub.transactions.filter(t => t.status === 'SUCCESS').reduce((sum, t) => sum + t.amount, 0);
    if (totalPaid >= netPrice) sub.paymentStatus = PaymentStatus.PAID;
    else if (totalPaid > 0) sub.paymentStatus = PaymentStatus.PARTIAL;
    else sub.paymentStatus = PaymentStatus.UNPAID;

    // Activate when paid and currently pending
    if (sub.paymentStatus === PaymentStatus.PAID && sub.status === SubscriptionStatus.PENDING) {
      sub.status = SubscriptionStatus.ACTIVE;
    }

    return sub.save();
  }

  async cancel(id: string, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (![UserRole.PARENT, UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) throw new ForbiddenException('Accès refusé');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');
    if (sub.status === SubscriptionStatus.CANCELLED) throw new ConflictException('Déjà annulé');
    sub.status = SubscriptionStatus.CANCELLED;
    return sub.save();
  }

  async suspend(id: string, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) throw new ForbiddenException('Accès refusé');
    if (sub.status !== SubscriptionStatus.ACTIVE) throw new ConflictException('Seul un abonnement actif peut être suspendu');
    sub.status = SubscriptionStatus.SUSPENDED;
    return sub.save();
  }

  async resume(id: string, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (![UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) throw new ForbiddenException('Accès refusé');
    if (sub.status !== SubscriptionStatus.SUSPENDED) throw new ConflictException('Seul un abonnement suspendu peut être repris');
    sub.status = SubscriptionStatus.ACTIVE;
    return sub.save();
  }

  async renew(id: string, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (![UserRole.PARENT, UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) throw new ForbiddenException('Accès refusé');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');
    if (!sub.autoRenew) throw new ConflictException('autoRenew est désactivé');
    if (sub.paymentStatus !== PaymentStatus.PAID) throw new ConflictException('Paiement incomplet');

    const offer = await this.offersService.findOne(sub.offerId.toString());
    const newEnd = this.computeEndDate(new Date(sub.endDate), offer);
    // Extend by durationDays
    sub.endDate = newEnd;
    return sub.save();
  }
}



