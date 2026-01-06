import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument, SubscriptionStatus } from './schemas/subscription.schema';
import { EmailService } from '../common/services/email.service';
import { SmsService } from '../common/services/sms.service';
import { UsersService } from '../users/users.service';
import { OffersService } from '../offers/offers.service';

@Injectable()
export class SubscriptionsSchedulerService {
    private readonly logger = new Logger(SubscriptionsSchedulerService.name);

    constructor(
        @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
        private emailService: EmailService,
        private smsService: SmsService,
        private usersService: UsersService,
        private offersService: OffersService,
    ) { }

    /**
     * Vérifie les abonnements qui expirent dans 7 jours et envoie des rappels (Email + SMS)
     * S'exécute tous les jours à 09h00 (conformément à la documentation)
     */
    @Cron(CronExpression.EVERY_DAY_AT_9AM)
    async checkExpiringSubscriptions() {
        this.logger.log('🔍 [Scheduler] Vérification des abonnements expirant dans 7 jours...');

        try {
            const now = new Date();
            // Date cible : exactement dans 7 jours
            const targetDateStart = new Date();
            targetDateStart.setHours(0, 0, 0, 0);
            targetDateStart.setDate(targetDateStart.getDate() + 7);

            const targetDateEnd = new Date(targetDateStart);
            targetDateEnd.setHours(23, 59, 59, 999);

            // Trouver les abonnements actifs qui expirent dans 7 jours et n'ayant pas encore reçu d'avertissement
            const expiringSubscriptions = await this.subscriptionModel
                .find({
                    status: SubscriptionStatus.ACTIVE,
                    expirationWarningSent: { $ne: true },
                    endDate: {
                        $gte: targetDateStart,
                        $lte: targetDateEnd,
                    },
                })
                .populate('parentId')
                .populate('childId')
                .populate('offerId')
                .exec();

            this.logger.log(`📊 [Scheduler] ${expiringSubscriptions.length} abonnement(s) expirent le ${targetDateStart.toDateString()}`);

            for (const subscription of expiringSubscriptions) {
                try {
                    const parent = subscription.parentId as any;
                    const child = subscription.childId as any;
                    const offer = subscription.offerId as any;

                    if (!parent || !child || !offer) {
                        this.logger.warn(`⚠️ [Scheduler] Données incomplètes pour l'abonnement ${subscription._id}`);
                        continue;
                    }

                    // 1. Envoi Email
                    if (parent.email) {
                        await this.emailService.sendSubscriptionExpiring(
                            parent.email,
                            `${parent.prenom} ${parent.nom}`,
                            `${child.prenom} ${child.nom}`,
                            offer.name,
                            subscription.endDate,
                        );
                    }

                    // 2. Envoi SMS (si téléphone disponible)
                    if (parent.telephone) {
                        const smsMessage = `Bonjour ${parent.prenom}, l'abonnement de ${child.prenom} pour ${offer.name} expire le ${subscription.endDate.toLocaleDateString()}. Pensez à le renouveler ! - Académie S.C. Kids`;
                        await this.smsService.sendSms(parent.telephone, smsMessage);
                    }

                    // 3. Marquer comme envoyé
                    subscription.expirationWarningSent = true;
                    await (subscription as any).save();

                    this.logger.log(`✅ [Scheduler] Rappels envoyés pour l'abonnement ${subscription._id}`);
                } catch (error) {
                    this.logger.error(`❌ [Scheduler] Erreur pour l'abonnement ${subscription._id}:`, error);
                }
            }
        } catch (error) {
            this.logger.error('❌ [Scheduler] Erreur générale lors de la vérification des expirations:', error);
        }
    }

    /**
     * Marque les abonnements expirés
     * S'exécute tous les jours à minuit
     */
    @Cron(CronExpression.EVERY_DAY_AT_MIDNIGHT)
    async markExpiredSubscriptions() {
        this.logger.log('🔍 [Scheduler] Marquage des abonnements expirés...');

        try {
            const now = new Date();

            const result = await this.subscriptionModel.updateMany(
                {
                    status: SubscriptionStatus.ACTIVE,
                    endDate: { $lt: now },
                },
                {
                    $set: { status: SubscriptionStatus.EXPIRED },
                },
            );

            this.logger.log(`✅ [Scheduler] ${result.modifiedCount} abonnement(s) marqué(s) comme expiré(s)`);
        } catch (error) {
            this.logger.error('❌ [Scheduler] Erreur lors du marquage des expirés:', error);
        }
    }
}
