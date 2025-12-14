import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument } from './schemas/subscription.schema';
import { UsersService } from '../users/users.service';
import { EmailService } from '../common/services/email.service';
import { OffersService } from '../offers/offers.service';
import { SmsService } from '../common/services/sms.service';

@Injectable()
export class SubscriptionsSchedulerService {
    private readonly logger = new Logger(SubscriptionsSchedulerService.name);

    constructor(
        @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
        private usersService: UsersService,
        private emailService: EmailService,
        private offersService: OffersService,
        private smsService: SmsService,
    ) { }

    // Exécute tous les jours à 9h00
    @Cron(CronExpression.EVERY_DAY_AT_9AM)
    async checkExpiringSubscriptions() {
        this.logger.log('🔍 Checking for expiring subscriptions...');

        const today = new Date();
        today.setHours(0, 0, 0, 0); // Start of today

        const sevenDaysFromNow = new Date(today);
        sevenDaysFromNow.setDate(today.getDate() + 7);
        sevenDaysFromNow.setHours(23, 59, 59, 999); // End of 7th day

        try {
            // Find subscriptions expiring in 7 days
            const expiringSubscriptions = await this.subscriptionModel.find({
                status: 'ACTIVE',
                endDate: {
                    $gte: today,
                    $lte: sevenDaysFromNow,
                },
                expirationWarningSent: { $ne: true }, // Avoid sending multiple warnings
            });

            this.logger.log(`Found ${expiringSubscriptions.length} expiring subscriptions`);

            for (const subscription of expiringSubscriptions) {
                try {
                    const child = await this.usersService.findById(subscription.childId.toString());
                    if (!child || !child.parent) {
                        this.logger.warn(`Child not found or has no parent: ${subscription.childId}`);
                        continue;
                    }

                    const parent = await this.usersService.findById(child.parent.toString());
                    if (!parent) {
                        this.logger.warn(`Parent not found: ${child.parent}`);
                        continue;
                    }

                    const offer = await this.offersService.findOne(subscription.offerId.toString());
                    if (!offer) {
                        this.logger.warn(`Offer not found: ${subscription.offerId}`);
                        continue;
                    }

                    // Send expiration warning email
                    await this.emailService.sendSubscriptionExpirationWarning(
                        parent.email,
                        parent.nom,
                        parent.prenom,
                        offer.name,
                        new Date(subscription.endDate).toLocaleDateString('fr-FR'),
                    );

                    // Send SMS notification
                    if (parent.phoneNumber) {
                        const daysRemaining = Math.max(0, Math.ceil((new Date(subscription.endDate).getTime() - new Date().getTime()) / (1000 * 60 * 60 * 24)));
                        const smsMessage = `Votre abonnement pour "${offer.name}" expire dans ${daysRemaining} jours. Veuillez renouveler pour continuer à profiter des services.`;
                        await this.smsService.sendSms(parent.phoneNumber, smsMessage);
                    }

                    // Mark as sent
                    await this.subscriptionModel.updateOne(
                        { _id: subscription._id },
                        { $set: { expirationWarningSent: true } },
                    );

                    this.logger.log(`✅ Expiration warning sent for subscription: ${subscription._id}`);
                } catch (error: any) {
                    this.logger.error(`Error sending expiration warning for subscription ${subscription._id}: ${error.message}`, error.stack);
                }
            }

            this.logger.log(`✅ Expiration check completed. Sent ${expiringSubscriptions.length} warnings.`);
        } catch (error: any) {
            this.logger.error(`Error in checkExpiringSubscriptions: ${error.message}`, error.stack);
        }
    }
}
