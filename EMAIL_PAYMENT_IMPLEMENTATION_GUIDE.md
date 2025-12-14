# Guide d'implémentation - Emails de paiement et d'expiration

## ✅ Étape 1: Méthodes d'email ajoutées

J'ai ajouté deux nouvelles méthodes dans `EmailService` (`src/common/services/email.service.ts`):

### 1. `sendPaymentConfirmation`
Envoie un email de confirmation après un paiement réussi.

**Paramètres:**
- `email`: Email du parent
- `nom`: Nom du parent
- `prenom`: Prénom du parent
- `subscriptionType`: Type d'abonnement (ex: "Mensuel", "Annuel")
- `price`: Prix formaté (ex: "29.99 €")
- `dateStart`: Date de début formatée (ex: "05/12/2024")
- `dateEnd`: Date de fin formatée (ex: "05/01/2025")

### 2. `sendSubscriptionExpirationWarning`
Envoie un email d'alerte 7 jours avant l'expiration.

**Paramètres:**
- `email`: Email du parent
- `nom`: Nom du parent
- `prenom`: Prénom du parent
- `subscriptionType`: Type d'abonnement
- `dateEnd`: Date d'expiration formatée

## 🔄 Étape 2: Implémenter le Webhook Stripe

Pour envoyer l'email de confirmation automatiquement après un paiement, vous devez implémenter un webhook Stripe.

### Option A: Webhook Handler (Recommandé)

Créez un nouveau fichier `src/payments/payments-webhook.controller.ts`:

\`\`\`typescript
import { Controller, Post, Req, Headers, BadRequestException, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Request } from 'express';
import Stripe from 'stripe';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import { UsersService } from '../users/users.service';
import { EmailService } from '../common/services/email.service';
import { OffersService } from '../offers/offers.service';

@Controller('payments/webhook')
export class PaymentsWebhookController {
  private readonly logger = new Logger(PaymentsWebhookController.name);
  private stripe: Stripe;

  constructor(
    private readonly configService: ConfigService,
    private readonly subscriptionsService: SubscriptionsService,
    private readonly usersService: UsersService,
    private readonly emailService: EmailService,
    private readonly offersService: OffersService,
  ) {
    const secretKey = this.configService.get<string>('STRIPE_SECRET_KEY');
    if (secretKey) {
      this.stripe = new Stripe(secretKey, { apiVersion: '2023-10-16' });
    }
  }

  @Post()
  async handleWebhook(
    @Req() req: Request,
    @Headers('stripe-signature') signature: string,
  ) {
    const webhookSecret = this.configService.get<string>('STRIPE_WEBHOOK_SECRET');
    
    if (!webhookSecret) {
      throw new BadRequestException('Webhook secret not configured');
    }

    let event: Stripe.Event;

    try {
      event = this.stripe.webhooks.constructEvent(
        req.body,
        signature,
        webhookSecret,
      );
    } catch (err) {
      this.logger.error(\`Webhook signature verification failed: \${err.message}\`);
      throw new BadRequestException(\`Webhook Error: \${err.message}\`);
    }

    this.logger.log(\`Received webhook event: \${event.type}\`);

    // Handle the event
    switch (event.type) {
      case 'payment_intent.succeeded':
        await this.handlePaymentSuccess(event.data.object as Stripe.PaymentIntent);
        break;
      case 'payment_intent.payment_failed':
        await this.handlePaymentFailed(event.data.object as Stripe.PaymentIntent);
        break;
      default:
        this.logger.log(\`Unhandled event type: \${event.type}\`);
    }

    return { received: true };
  }

  private async handlePaymentSuccess(paymentIntent: Stripe.PaymentIntent) {
    this.logger.log(\`Payment succeeded: \${paymentIntent.id}\`);

    // Extract metadata
    const { childId, offerId } = paymentIntent.metadata || {};

    if (!childId || !offerId) {
      this.logger.warn('Missing childId or offerId in payment metadata');
      return;
    }

    try {
      // Get child and parent info
      const child = await this.usersService.findById(childId);
      if (!child || !child.parent) {
        this.logger.error(\`Child not found or has no parent: \${childId}\`);
        return;
      }

      const parent = await this.usersService.findById(child.parent.toString());
      if (!parent) {
        this.logger.error(\`Parent not found: \${child.parent}\`);
        return;
      }

      // Get offer details
      const offer = await this.offersService.findOne(offerId);
      if (!offer) {
        this.logger.error(\`Offer not found: \${offerId}\`);
        return;
      }

      // Create subscription
      const startDate = new Date();
      const endDate = new Date(startDate);
      endDate.setDate(endDate.getDate() + offer.durationDays);

      const subscription = await this.subscriptionsService.create(
        {
          childId,
          offerId,
          startDate: startDate.toISOString(),
          autoRenew: false,
        },
        { userId: parent._id.toString(), role: parent.role },
      );

      // Record payment
      await this.subscriptionsService.recordPayment(
        subscription._id.toString(),
        {
          amount: paymentIntent.amount / 100, // Convert from cents
          currency: paymentIntent.currency.toUpperCase(),
          method: 'STRIPE',
        },
        { userId: parent._id.toString(), role: parent.role },
      );

      // Send confirmation email
      await this.emailService.sendPaymentConfirmation(
        parent.email,
        parent.nom,
        parent.prenom,
        offer.name,
        \`\${(paymentIntent.amount / 100).toFixed(2)} €\`,
        startDate.toLocaleDateString('fr-FR'),
        endDate.toLocaleDateString('fr-FR'),
      );

      this.logger.log(\`Subscription created and email sent for child: \${childId}\`);
    } catch (error) {
      this.logger.error(\`Error handling payment success: \${error.message}\`, error.stack);
    }
  }

  private async handlePaymentFailed(paymentIntent: Stripe.PaymentIntent) {
    this.logger.error(\`Payment failed: \${paymentIntent.id}\`);
    // TODO: Implement failure handling (notify user, etc.)
  }
}
\`\`\`

### Mise à jour de `PaymentsModule`

Ajoutez le webhook controller et les dépendances nécessaires:

\`\`\`typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { PaymentsController } from './payments.controller';
import { PaymentsWebhookController } from './payments-webhook.controller';
import { PaymentsService } from './payments.service';
import { OffersModule } from '../offers/offers.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { UsersModule } from '../users/users.module';
import { CommonModule } from '../common/common.module';

@Module({
  imports: [
    ConfigModule,
    OffersModule,
    SubscriptionsModule,
    UsersModule,
    CommonModule,
  ],
  controllers: [PaymentsController, PaymentsWebhookController],
  providers: [PaymentsService],
  exports: [PaymentsService],
})
export class PaymentsModule {}
\`\`\`

### Configuration du Webhook dans Stripe

1. Allez sur https://dashboard.stripe.com/test/webhooks
2. Cliquez sur "Add endpoint"
3. URL: `https://votre-domaine.com/payments/webhook`
4. Événements à écouter:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
5. Copiez le "Signing secret" et ajoutez-le dans `.env`:
   \`\`\`
   STRIPE_WEBHOOK_SECRET=whsec_...
   \`\`\`

### Test en local avec Stripe CLI

\`\`\`bash
# Installer Stripe CLI
# Windows: scoop install stripe
# Mac: brew install stripe/stripe-cli/stripe

# Login
stripe login

# Forward webhooks to local
stripe listen --forward-to localhost:3000/payments/webhook

# Test un webhook
stripe trigger payment_intent.succeeded
\`\`\`

## ⏰ Étape 3: Implémenter les alertes d'expiration (Cron Job)

Pour envoyer des emails 7 jours avant l'expiration, utilisez `@nestjs/schedule`.

### Installation

\`\`\`bash
npm install @nestjs/schedule
\`\`\`

### Créer le service de tâches planifiées

Créez `src/subscriptions/subscriptions-scheduler.service.ts`:

\`\`\`typescript
import { Injectable, Logger } from '@nestjs/common';
import { Cron, CronExpression } from '@nestjs/schedule';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Subscription, SubscriptionDocument } from './schemas/subscription.schema';
import { UsersService } from '../users/users.service';
import { EmailService } from '../common/services/email.service';
import { OffersService } from '../offers/offers.service';

@Injectable()
export class SubscriptionsSchedulerService {
  private readonly logger = new Logger(SubscriptionsSchedulerService.name);

  constructor(
    @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
    private readonly usersService: UsersService,
    private readonly emailService: EmailService,
    private readonly offersService: OffersService,
  ) {}

  // Exécute tous les jours à 9h00
  @Cron(CronExpression.EVERY_DAY_AT_9AM)
  async checkExpiringSubscriptions() {
    this.logger.log('Checking for expiring subscriptions...');

    const today = new Date();
    const sevenDaysFromNow = new Date(today);
    sevenDaysFromNow.setDate(today.getDate() + 7);

    // Find subscriptions expiring in 7 days
    const expiringSubscriptions = await this.subscriptionModel.find({
      status: 'ACTIVE',
      endDate: {
        $gte: today,
        $lte: sevenDaysFromNow,
      },
      expirationWarningS

ent: { $ne: true }, // Avoid sending multiple warnings
    }).populate('child').populate('offer');

    this.logger.log(\`Found \${expiringSubscriptions.length} expiring subscriptions\`);

    for (const subscription of expiringSubscriptions) {
      try {
        const child = await this.usersService.findById(subscription.child.toString());
        if (!child || !child.parent) continue;

        const parent = await this.usersService.findById(child.parent.toString());
        if (!parent) continue;

        const offer = await this.offersService.findOne(subscription.offer.toString());
        if (!offer) continue;

        // Send expiration warning email
        await this.emailService.sendSubscriptionExpirationWarning(
          parent.email,
          parent.nom,
          parent.prenom,
          offer.name,
          new Date(subscription.endDate).toLocaleDateString('fr-FR'),
        );

        // Mark as sent
        await this.subscriptionModel.updateOne(
          { _id: subscription._id },
          { expirationWarningSent: true },
        );

        this.logger.log(\`Expiration warning sent for subscription: \${subscription._id}\`);
      } catch (error) {
        this.logger.error(\`Error sending expiration warning: \${error.message}\`, error.stack);
      }
    }
  }
}
\`\`\`

### Mise à jour du schéma Subscription

Ajoutez le champ `expirationWarningSent` dans `src/subscriptions/schemas/subscription.schema.ts`:

\`\`\`typescript
@Prop({ default: false })
expirationWarningSent: boolean;
\`\`\`

### Mise à jour de `SubscriptionsModule`

\`\`\`typescript
import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ScheduleModule } from '@nestjs/schedule';
import { SubscriptionsController } from './subscriptions.controller';
import { SubscriptionsService } from './subscriptions.service';
import { SubscriptionsSchedulerService } from './subscriptions-scheduler.service';
import { Subscription, SubscriptionSchema } from './schemas/subscription.schema';
import { OffersModule } from '../offers/offers.module';
import { UsersModule } from '../users/users.module';
import { CommonModule } from '../common/common.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Subscription.name, schema: SubscriptionSchema }]),
    ScheduleModule.forRoot(),
    OffersModule,
    UsersModule,
    CommonModule,
  ],
  controllers: [SubscriptionsController],
  providers: [SubscriptionsService, SubscriptionsSchedulerService],
  exports: [SubscriptionsService],
})
export class SubscriptionsModule {}
\`\`\`

## 📝 Résumé

### Ce qui a été fait:
- ✅ Ajout de `sendPaymentConfirmation` dans `EmailService`
- ✅ Ajout de `sendSubscriptionExpirationWarning` dans `EmailService`
- ✅ Templates HTML intégrés dans les méthodes

### Ce qu'il reste à faire:
1. ⏳ Créer `PaymentsWebhookController` pour gérer les webhooks Stripe
2. ⏳ Mettre à jour `PaymentsModule` pour inclure les dépendances
3. ⏳ Configurer le webhook dans Stripe Dashboard
4. ⏳ Créer `SubscriptionsSchedulerService` pour les alertes d'expiration
5. ⏳ Ajouter le champ `expirationWarningSent` au schéma Subscription
6. ⏳ Mettre à jour `SubscriptionsModule` avec le scheduler

### Test
Une fois implémenté, vous pouvez tester:
- **Webhook**: Utilisez Stripe CLI ou effectuez un vrai paiement
- **Cron**: Modifiez temporairement le cron à `@Cron('*/30 * * * * *')` pour tester toutes les 30 secondes

## 🔗 Ressources
- [Stripe Webhooks Documentation](https://stripe.com/docs/webhooks)
- [NestJS Schedule Documentation](https://docs.nestjs.com/techniques/task-scheduling)
- [Stripe CLI](https://stripe.com/docs/stripe-cli)
