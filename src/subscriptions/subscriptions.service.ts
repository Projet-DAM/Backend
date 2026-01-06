import { BadRequestException, ConflictException, ForbiddenException, Injectable, NotFoundException, Logger } from '@nestjs/common';
import { EmailService } from '../common/services/email.service';
import { SmsService } from '../common/services/sms.service';
import { InjectModel } from '@nestjs/mongoose';
import { FilterQuery, Model, Types } from 'mongoose';
import { Subscription, SubscriptionDocument, SubscriptionStatus, PaymentStatus } from './schemas/subscription.schema';
import { CreateSubscriptionDto } from './dto/create-subscription.dto';
import { UpdateSubscriptionDto } from './dto/update-subscription.dto';
import { UsersService } from '../users/users.service';
import { OffersService } from '../offers/offers.service';
import { OfferDocument } from '../offers/schemas/offer.schema';
import { RecordPaymentDto } from './dto/record-payment.dto';
import { UserRole } from '../users/interfaces/user-role.enum';
import { SubscriptionOptionsService } from '../subscription-options/subscription-options.service';

@Injectable()
export class SubscriptionsService {
  private readonly logger = new Logger(SubscriptionsService.name);

  constructor(
    @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    private usersService: UsersService,
    private offersService: OffersService,
    private emailService: EmailService,
    private smsService: SmsService,
    private subscriptionOptionsService: SubscriptionOptionsService,
  ) { }

  private async sendConfirmationEmail(sub: SubscriptionDocument, offer: OfferDocument, totalPaid: number) {
    try {
      const parent = await this.usersService.findById(sub.parentId.toString());
      if (!parent) return;

      const dateStart = new Date(sub.startDate).toLocaleDateString('fr-FR');
      const dateEnd = new Date(sub.endDate).toLocaleDateString('fr-FR');
      const currency = sub.transactions[0]?.currency || 'TND';
      const amountFormatted = `${totalPaid.toFixed(2)} ${currency}`;

      await this.emailService.sendPaymentConfirmation(
        parent.email,
        `${parent.prenom || 'Parent'} ${parent.nom || ''}`,
        offer.name,
        totalPaid,
        currency,
        new Date(sub.startDate),
        new Date(sub.endDate)
      );

      // Send SMS
      const phoneNumber = parent.phoneNumber || (parent as any).telephone;
      if (phoneNumber) {
        const smsMessage = `Votre paiement de ${amountFormatted} pour l'offre "${offer.name}" a été effectué avec succès.`;
        await this.smsService.sendSms(phoneNumber, smsMessage);
      }
    } catch (error: any) {
      this.logger.error(`Failed to send confirmation email`, error.message);
    }
  }

  /**
   * Vérifie si un enfant a déjà un abonnement actif pour éviter les doublons
   */
  async validateNewSubscription(childId: string): Promise<void> {
    const now = new Date();
    const existingActiveSub = await this.subscriptionModel.findOne({
      childId: childId,
      status: SubscriptionStatus.ACTIVE,
      endDate: { $gt: now },
    });

    if (existingActiveSub) {
      throw new BadRequestException(
        `Abonnement actif jusqu'au ${existingActiveSub.endDate.toLocaleDateString('fr-FR')}. Inscription possible le mois prochain.`
      );
    }
  }

  /**
   * Vérifie si un parent a déjà un abonnement actif pour la même offre
   * Un parent peut avoir plusieurs abonnements actifs mais pas pour la même offre
   */
  async validateParentSubscriptionForOffer(parentId: string, offerId: string): Promise<void> {
    const now = new Date();
    const existingActiveSub = await this.subscriptionModel.findOne({
      parentId: parentId,
      offerId: offerId,
      status: SubscriptionStatus.ACTIVE,
      endDate: { $gt: now },
    });

    if (existingActiveSub) {
      const offer = await this.offersService.findOne(offerId).catch(() => null);
      const offerName = offer?.name || 'cette offre';
      throw new ConflictException(
        `Vous avez déjà un abonnement actif pour ${offerName} jusqu'au ${existingActiveSub.endDate.toLocaleDateString('fr-FR')}. Vous ne pouvez pas vous inscrire à nouveau à la même offre tant que l'abonnement est actif.`
      );
    }
  }

  private assertFutureOrToday(date: Date) {
    const today = new Date();
    const startOfToday = new Date(Date.UTC(today.getUTCFullYear(), today.getUTCMonth(), today.getUTCDate()));
    if (date < startOfToday) {
      throw new BadRequestException('startDate doit être >= aujourd\'hui');
    }
  }

  private calculateTotalPrice(offer: OfferDocument, selectedOptions: any[]): number {
    const basePrice = offer.price * (1 - (offer.discountPct || 0) / 100);
    const optionsTotal = (selectedOptions || []).reduce((sum, option) => sum + (option.price || 0), 0);
    return basePrice + optionsTotal;
  }

  private extractParentId(child: any): string | null {
    if (!child || !child.parent) return null;
    const parent = child.parent;
    if (typeof parent === 'object' && parent !== null && '_id' in parent) return String(parent._id);
    return String(parent);
  }

  async create(dto: CreateSubscriptionDto, actor: { userId: string; role: UserRole }): Promise<SubscriptionDocument> {
    if (actor.role !== UserRole.PARENT) {
      throw new ForbiddenException('Seul un parent peut créer un abonnement');
    }
    const child = await this.usersService.findById(dto.childId);
    if (!child) throw new NotFoundException('Enfant non trouvé');

    const parentId = this.extractParentId(child);
    if (!parentId || parentId.trim() !== String(actor.userId).trim()) {
      throw new ForbiddenException('Enfant non lié à ce parent');
    }

    const offer = await this.offersService.findOne(dto.offerId);
    if (!offer.isActive) throw new ConflictException('Offre inactive');
    // if (offer.isFull) throw new ConflictException('Offre complète'); // isFull non présent dans le schéma actuel

    // Vérifier que le parent n'a pas déjà un abonnement actif pour cette offre
    await this.validateParentSubscriptionForOffer(actor.userId, dto.offerId);

    const startDate = dto.startDate ? new Date(dto.startDate) : new Date();
    this.assertFutureOrToday(startDate);
    const endDate = this.computeEndDate(startDate, offer);

    // Resolve options
    const resolvedOptions: any[] = [];
    if (dto.selectedOptions && dto.selectedOptions.length > 0) {
      const allOptions = this.subscriptionOptionsService.getAvailableOptions();
      for (const optType of dto.selectedOptions) {
        const option = allOptions.find(o => o.type === optType as any);
        if (option) {
          resolvedOptions.push({
            type: option.type,
            name: option.name,
            price: option.price,
            currency: option.currency
          });
        }
      }
    }

    const created = new this.subscriptionModel({
      childId: dto.childId,
      parentId: actor.userId,
      offerId: dto.offerId,
      startDate,
      endDate,
      autoRenew: !!dto.autoRenew,
      status: SubscriptionStatus.PENDING,
      paymentStatus: PaymentStatus.UNPAID,
      selectedOptions: resolvedOptions,
    });
    return created.save();
  }

  async findAll(filters: any, actor: { userId: string; role: UserRole }) {
    if (![UserRole.ADMIN, UserRole.ACADEMIE].includes(actor.role)) {
      throw new ForbiddenException('Accès refusé');
    }

    const page = Math.max(1, filters.page || 1);
    const limit = Math.min(100, Math.max(1, filters.limit || 10));
    const sort = filters.sort || '-createdAt';

    const query: FilterQuery<SubscriptionDocument> = {};
    if (filters.status) query.status = filters.status;
    if (filters.paymentStatus) query.paymentStatus = filters.paymentStatus;
    if (filters.parentId) query.parentId = filters.parentId;
    if (filters.childId) query.childId = filters.childId;

    const [rawData, total] = await Promise.all([
      this.subscriptionModel
        .find(query)
        .populate('childId', 'nom prenom photoProfil')
        .populate('parentId', 'nom prenom email phoneNumber')
        .populate('offerId', 'name price durationDays')
        .sort(sort)
        .skip((page - 1) * limit)
        .limit(limit)
        .lean()
        .exec(),
      this.subscriptionModel.countDocuments(query),
    ]);

    // Filtrage de sécurité : si un parent, un enfant ou une offre a été supprimé de la base
    // mais que l'abonnement existe encore, on ne l'envoie pas au mobile pour éviter un crash.
    const data = rawData.filter(sub => sub.childId && sub.parentId && sub.offerId);
    const displayedTotal = data.length !== rawData.length ? data.length : total;

    return {
      data,
      total: displayedTotal,
      page,
      limit,
      totalPages: Math.ceil(displayedTotal / limit),
    };
  }

  async recordPayment(id: string, dto: RecordPaymentDto, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (![UserRole.PARENT, UserRole.ACADEMIE].includes(actor.role)) throw new ForbiddenException('Accès refusé');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');

    // Logique standard d'ajout de transaction
    sub.transactions.push({
      amount: dto.amount,
      currency: dto.currency || 'TND',
      method: dto.method,
      externalRef: dto.externalRef,
      date: dto.date ? new Date(dto.date) : new Date(),
      status: 'SUCCESS',
    });

    const offer = await this.offersService.findOne(sub.offerId.toString()).catch(() => null);
    if (!offer) {
      throw new NotFoundException('Impossible d\'enregistrer le paiement : l\'offre associée à cet abonnement n\'existe plus.');
    }
    const totalPrice = this.calculateTotalPrice(offer, sub.selectedOptions);
    const totalPaid = sub.transactions.filter(t => t.status === 'SUCCESS').reduce((sum, t) => sum + t.amount, 0);

    if (totalPaid >= totalPrice) sub.paymentStatus = PaymentStatus.PAID;
    else if (totalPaid > 0) sub.paymentStatus = PaymentStatus.PARTIAL;

    if (sub.paymentStatus === PaymentStatus.PAID && sub.status === SubscriptionStatus.PENDING) {
      sub.status = SubscriptionStatus.ACTIVE;
    }

    const savedSub = await sub.save();

    if (sub.paymentStatus === PaymentStatus.PAID) {
      this.sendConfirmationEmail(savedSub, offer, totalPaid);
    }

    return savedSub;
  }

  private computeEndDate(start: Date, offer: OfferDocument): Date {
    const end = new Date(start);
    end.setDate(end.getDate() + offer.durationDays - 1);
    return end;
  }

  async findMine(actor: { userId: string; role: UserRole }) {
    if (actor.role !== UserRole.PARENT) throw new ForbiddenException('Accès parent requis');
    const data = await this.subscriptionModel
      .find({ parentId: actor.userId })
      .populate('offerId')
      .populate('childId')
      .lean()
      .exec();

    const filtered = data.filter(sub => sub.childId && sub.offerId);
    return filtered;
  }

  async findByChild(childId: string, actor: { userId: string; role: UserRole }) {
    if (actor.role === UserRole.PARENT) {
      const child = await this.usersService.findById(childId);
      if (!child) throw new NotFoundException('Enfant non trouvé');
      const parentId = this.extractParentId(child);
      if (!parentId || parentId.trim() !== String(actor.userId).trim()) {
        throw new ForbiddenException('Accès refusé pour cet enfant');
      }
    } else if (![UserRole.COACH, UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      throw new ForbiddenException('Accès refusé');
    }

    const data = await this.subscriptionModel
      .find({ childId })
      .populate('offerId')
      .populate('childId')
      .lean()
      .exec();


    const filtered = data.filter(sub => sub.childId && sub.offerId);
    return filtered;
  }

  async findOne(id: string, actor: { userId: string; role: UserRole }) {
    if (!Types.ObjectId.isValid(id)) {
      throw new NotFoundException('Abonnement introuvable (ID invalide)');
    }
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) {
      throw new ForbiddenException('Accès refusé');
    }
    return sub;
  }

  async update(id: string, dto: UpdateSubscriptionDto, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');

    if (actor.role === UserRole.PARENT) {
      if (sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');
      if (dto.autoRenew !== undefined) sub.autoRenew = dto.autoRenew;
      if (dto.notes !== undefined) sub.notes = dto.notes;
      if (dto.startDate !== undefined) {
        sub.startDate = new Date(dto.startDate);
        const offer = await this.offersService.findOne(sub.offerId.toString()).catch(() => null);
        if (offer) {
          sub.endDate = this.computeEndDate(sub.startDate, offer);
        }
      }
    } else if ([UserRole.ACADEMIE, UserRole.ADMIN].includes(actor.role)) {
      if (dto.status) sub.status = dto.status;
      if (dto.paymentStatus) sub.paymentStatus = dto.paymentStatus;
      if (dto.autoRenew !== undefined) sub.autoRenew = dto.autoRenew;
      if (dto.notes !== undefined) sub.notes = dto.notes;
      if (dto.startDate !== undefined) {
        sub.startDate = new Date(dto.startDate);
        const offer = await this.offersService.findOne(sub.offerId.toString()).catch(() => null);
        if (offer) {
          sub.endDate = this.computeEndDate(sub.startDate, offer);
        }
      }
    }

    return sub.save();
  }

  async cancel(id: string, actor: { userId: string; role: UserRole }) {
    const sub = await this.subscriptionModel.findById(id).exec();
    if (!sub) throw new NotFoundException('Abonnement introuvable');
    if (actor.role === UserRole.PARENT && sub.parentId.toString() !== actor.userId) throw new ForbiddenException('Accès refusé');

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

    const offer = await this.offersService.findOne(sub.offerId.toString()).catch(() => null);
    if (!offer) throw new NotFoundException('Impossible de renouveler : l\'offre n\'existe plus.');

    sub.endDate = this.computeEndDate(sub.endDate, offer);
    return sub.save();
  }
}



