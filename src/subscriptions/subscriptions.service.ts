import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException, Logger } from '@nestjs/common';
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
import { EmailService } from '../common/services/email.service';
import { SmsService } from '../common/services/sms.service';

@Injectable()
export class SubscriptionsService {
  private readonly logger = new Logger(SubscriptionsService.name);

  constructor(
    @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    private usersService: UsersService,
    private offersService: OffersService,
    private emailService: EmailService,
    private smsService: SmsService,
  ) { }

  // ... (existing code)

  private async sendConfirmationEmail(sub: SubscriptionDocument, offer: OfferDocument, totalPaid: number) {
    try {
      const parent = await this.usersService.findById(sub.parentId.toString());
      if (!parent) {
        this.logger.warn(`Parent not found for subscription ${sub._id}`);
        return;
      }

      const dateStart = new Date(sub.startDate).toLocaleDateString('fr-FR');
      const dateEnd = new Date(sub.endDate).toLocaleDateString('fr-FR');
      const currency = sub.transactions[0]?.currency || 'EUR';
      const amountFormatted = `${totalPaid.toFixed(2)} ${currency}`;

      this.logger.log(`📧 Sending payment confirmation email to ${parent.email}`);

      await this.emailService.sendPaymentConfirmation(
        parent.email,
        parent.nom || '',
        parent.prenom || 'Parent',
        offer.name,
        amountFormatted,
        dateStart,
        dateEnd
      );

      // Send SMS
      if (parent.phoneNumber) {
        const smsMessage = `Votre paiement de ${amountFormatted} pour l'offre "${offer.name}" a été effectué avec succès.`;
        await this.smsService.sendSms(parent.phoneNumber, smsMessage);
      }

      this.logger.log(`✅ Payment confirmation email sent to ${parent.email}`);
    } catch (error: any) {
      this.logger.error(`Failed to send confirmation email for subscription ${sub._id}:`, error.message);
      throw error;
    }
  }


  private assertFutureOrToday(date: Date) {
    const today = new Date();
    const startOfToday = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
    if (date < startOfToday) {
      throw new BadRequestException('startDate doit être >= aujourd\'hui');
    }
  }

  /**
   * Calculate total subscription price including base offer price and selected options
   */
  private calculateTotalPrice(offer: OfferDocument, selectedOptions: any[]): number {
    // Base price with discount
    const basePrice = offer.price * (1 - (offer.discountPct || 0) / 100);

    // Add options prices
    const optionsTotal = (selectedOptions || []).reduce((sum, option) => sum + (option.price || 0), 0);

    return basePrice + optionsTotal;
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

    // Check capacity
    // Note: checking isFull from enriched offer, but double check count to be safe/atomic?
    // Since findOne is enriched, it has isFull property.
    // However, findOne returns "any" due to enrichment, so we can access isFull.
    if (offer.isFull) {
      throw new ConflictException('Désolé, cette offre a atteint sa capacité maximale (Complet)');
    }
    // Double check specific number if needed, but isFull relies on countDocuments which is reasonably accurate.
    // Race conditions are possible in high concurrency but acceptable for this use case.

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
      selectedOptions: dto.selectedOptions || [],
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
      if (dto.selectedOptions != null) sub.selectedOptions = dto.selectedOptions as any;

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
    if (dto.selectedOptions != null) sub.selectedOptions = dto.selectedOptions as any;

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

    // Compute total price including options
    const offer = await this.offersService.findOne(sub.offerId.toString());
    const totalPrice = this.calculateTotalPrice(offer, sub.selectedOptions);
    const totalPaid = sub.transactions.filter(t => t.status === 'SUCCESS').reduce((sum, t) => sum + t.amount, 0);

    if (totalPaid >= totalPrice) sub.paymentStatus = PaymentStatus.PAID;
    else if (totalPaid > 0) sub.paymentStatus = PaymentStatus.PARTIAL;
    else sub.paymentStatus = PaymentStatus.UNPAID;

    // Activate when paid and currently pending
    if (sub.paymentStatus === PaymentStatus.PAID && sub.status === SubscriptionStatus.PENDING) {
      sub.status = SubscriptionStatus.ACTIVE;
    }

    const savedSub = await sub.save();

    // Send confirmation email if paid
    if (sub.paymentStatus === PaymentStatus.PAID) {
      this.sendConfirmationEmail(savedSub, offer, totalPaid).catch(err => {
        this.logger.error('Erreur lors de l\'envoi de l\'email de confirmation', err);
      });
    }

    return savedSub;
  }

  private computeEndDate(start: Date, offer: OfferDocument): Date {
    const end = new Date(start);
    end.setDate(end.getDate() + offer.durationDays - 1);
    return end;
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



