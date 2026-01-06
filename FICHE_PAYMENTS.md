# Fiche Technique : Package Payments

Cette fiche contient le code complet du package `payments` ainsi que les configurations nécessaires dans les autres parties de l'application.

## 1. Configuration

### 1.1 Variables d'environnement (.env)
Les variables suivantes doivent être définies dans votre fichier `.env` à la racine du projet :

```env
# Stripe Configuration
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...

# Twilio Configuration (WhatsApp & SMS)
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_WHATSAPP_NUMBER=whatsapp:+...
TWILIO_PHONE_NUMBER=+...

# Autres
DEFAULT_PHONE_NUMBER=whatsapp:+...
```

### 1.2 Registration dans AppModule (src/app.module.ts)
Le module `PaymentsModule` doit être importé dans `AppModule`.

```typescript
import { Module } from '@nestjs/common';
// ... autres imports
import { PaymentsModule } from './payments/payments.module';

@Module({
  imports: [
    // ... autres modules
    PaymentsModule,
  ],
  // ...
})
export class AppModule { }
```

---

## 2. Structure du Package (src/payments)

```
src/payments/
├── dto/
│   ├── complete-payment.dto.ts
│   ├── confirm-payment.dto.ts
│   └── create-payment-intent.dto.ts
├── payments.controller.ts
├── payments.module.ts
├── payments.service.ts
├── twilio.controller.ts
└── twilio.service.ts
```

---

## 3. Code des Fichiers

### 3.1 Module Definition

**Fichier : `src/payments/payments.module.ts`**
```typescript
import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { PaymentsController } from './payments.controller';
import { PaymentsService } from './payments.service';
import { TwilioService } from './twilio.service';
import { OffersModule } from '../offers/offers.module';
import { SubscriptionsModule } from '../subscriptions/subscriptions.module';
import { UsersModule } from '../users/users.module';
import { EmailModule } from '../common/services/email.module';

@Module({
  imports: [
    ConfigModule,
    OffersModule,
    SubscriptionsModule,
    UsersModule,
    EmailModule,
  ],
  controllers: [PaymentsController],
  providers: [PaymentsService, TwilioService],
  exports: [PaymentsService],
})
export class PaymentsModule { }
```

### 3.2 Services

**Fichier : `src/payments/payments.service.ts`**
```typescript
import { Injectable, BadRequestException, InternalServerErrorException } from '@nestjs/common';
import Stripe from 'stripe';
import { TwilioService } from './twilio.service';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class PaymentsService {
  private stripe: Stripe;

  constructor(
    private readonly twilioService: TwilioService,
    private readonly configService: ConfigService
  ) {
    // Initialize Stripe with the secret key
    const stripeSecretKey = this.configService.get<string>('STRIPE_SECRET_KEY') || process.env.STRIPE_SECRET_KEY || '';
    this.stripe = new Stripe(stripeSecretKey, {
      apiVersion: '2024-12-18.acacia',
    } as any);
  }

  /**
   * Crée un PaymentIntent avec Stripe
   * @param amount Montant en centimes
   * @param currency Devise (par défaut: eur)
   * @param paymentMethodId ID de la méthode de paiement Stripe
   * @param subscriptionId ID de l'abonnement (optionnel)
   * @param childId ID de l'enfant (optionnel)
   * @param offerId ID de l'offre (optionnel)
   * @param phoneNumber Numéro de téléphone pour notification WhatsApp (optionnel)
   */
  async createPaymentIntent(
    amount: number,
    currency: string = 'eur',
    paymentMethodId?: string,
    subscriptionId?: string,
    childId?: string,
    offerId?: string,
    phoneNumber?: string,
  ): Promise<{ clientSecret: string; paymentIntentId: string }> {
    if (!this.stripe) {
      throw new BadRequestException('Stripe n\'est pas configuré. Veuillez ajouter STRIPE_SECRET_KEY dans le fichier .env');
    }
    try {
      const paymentIntentParams: Stripe.PaymentIntentCreateParams = {
        amount: Math.round(amount),
        currency: currency.toLowerCase(),
        automatic_payment_methods: {
          enabled: true,
        },
      };

      if (paymentMethodId) {
        paymentIntentParams.payment_method = paymentMethodId;
        paymentIntentParams.confirm = true;
        paymentIntentParams.return_url = 'https://sportyconnect.com/payment-return';
      }

      const metadata: Record<string, string> = {};
      if (subscriptionId) metadata.subscriptionId = subscriptionId;
      if (childId) metadata.childId = childId;
      if (offerId) metadata.offerId = offerId;

      const ph = phoneNumber || process.env.DEFAULT_PHONE_NUMBER;
      if (ph) {
        metadata.phoneNumber = ph;
      }

      if (Object.keys(metadata).length > 0) {
        paymentIntentParams.metadata = metadata;
      }

      const paymentIntent = await this.stripe.paymentIntents.create(paymentIntentParams);

      return {
        clientSecret: paymentIntent.client_secret || '',
        paymentIntentId: paymentIntent.id,
      };
    } catch (error: any) {
      console.error('Erreur lors de la création du PaymentIntent:', error);
      if (error instanceof Stripe.errors.StripeError) {
        throw new BadRequestException(`Erreur Stripe: ${error.message}`);
      }
      throw new InternalServerErrorException('Erreur lors de la création du PaymentIntent');
    }
  }

  /**
   * Confirms a Stripe payment intent.
   */
  async confirmPayment(paymentIntentId: string, paymentMethodId?: string) {
    try {
      if (paymentMethodId) {
        return await this.stripe.paymentIntents.confirm(paymentIntentId, {
          payment_method: paymentMethodId
        });
      }
      return await this.stripe.paymentIntents.retrieve(paymentIntentId);
    } catch (error: any) {
      console.error('Erreur lors de la confirmation du paiement:', error);
      throw new BadRequestException(`Erreur confirmation: ${error.message}`);
    }
  }

  async getPaymentIntent(paymentIntentId: string) {
    try {
      return await this.stripe.paymentIntents.retrieve(paymentIntentId);
    } catch (error: any) {
      console.error('Erreur lors de la récupération du PaymentIntent:', error);
      throw new BadRequestException(`Erreur récupération: ${error.message}`);
    }
  }

  /**
   * Vérifie le statut du paiement et envoie le message WhatsApp
   */
  async verifyAndNotifyPayment(paymentIntentId: string) {
    try {
      const paymentIntent = await this.stripe.paymentIntents.retrieve(paymentIntentId);

      if (paymentIntent.status === 'succeeded') {
        console.log(`✅ Paiement ${paymentIntentId} confirmé via API directe`);
        const phoneNumber = paymentIntent.metadata?.phoneNumber || process.env.DEFAULT_PHONE_NUMBER;

        if (phoneNumber) {
          await this.twilioService.sendPaymentConfirmation(
            phoneNumber,
            paymentIntent.amount,
            paymentIntent.currency
          );
          return { success: true, message: 'Message WhatsApp envoyé' };
        } else {
          console.warn('Pas de numéro de téléphone trouvé');
          return { success: false, message: 'Pas de numéro de téléphone' };
        }
      } else {
        console.warn(`⚠️ Paiement ${paymentIntentId} non réussi. Statut: ${paymentIntent.status}`);
        return { success: false, message: `Statut du paiement: ${paymentIntent.status}` };
      }
    } catch (error) {
      console.error('Erreur lors de la confirmation manuelle:', error);
      throw error;
    }
  }

  getPublishableKey(): string {
    return this.configService.get<string>('STRIPE_PUBLISHABLE_KEY') || process.env.STRIPE_PUBLISHABLE_KEY || '';
  }
}
```

**Fichier : `src/payments/twilio.service.ts`**
```typescript
import { Injectable } from '@nestjs/common';
import twilio from 'twilio';

@Injectable()
export class TwilioService {
    private twilioClient: twilio.Twilio;
    private twilioWhatsAppNumber: string;

    constructor() {
        const accountSid = process.env.TWILIO_ACCOUNT_SID;
        const authToken = process.env.TWILIO_AUTH_TOKEN;
        this.twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || 'whatsapp:+14155238886';

        if (!accountSid || !authToken) {
            console.warn('⚠️ Twilio credentials missing. WhatsApp notifications will be disabled.');
            return;
        }

        try {
            this.twilioClient = twilio(accountSid, authToken);
        } catch (error) {
            console.error('❌ Error initializing Twilio client:', error.message);
        }
    }

    async sendPaymentConfirmation(to: string, amount: number, currency: string = 'EUR'): Promise<void> {
        const formattedAmount = (amount / 100).toFixed(2);
        const message = `🎉 Confirmation de paiement\n\nVotre paiement de ${formattedAmount} ${currency.toUpperCase()} a été effectué avec succès !\n\nMerci pour votre confiance. 💚\n\n- SportyConnect`;

        console.log('📱 [WHATSAPP DEBUG] Tentative d\'envoi:');
        console.log('   - De:', this.twilioWhatsAppNumber);
        console.log('   - À:', `whatsapp:${to}`);

        if (!this.twilioClient) {
            console.warn('⚠️ Twilio client not initialized.');
            return;
        }

        try {
            await this.twilioClient.messages.create({
                body: message,
                from: this.twilioWhatsAppNumber,
                to: `whatsapp:${to}`,
            });
            console.log(`✅ Message WhatsApp envoyé avec succès.`);
        } catch (error) {
            console.error('❌ Erreur WhatsApp:', error.message);
            // Don't throw to prevent blocking payment flow
        }
    }
}
```

### 3.3 Controllers

**Fichier : `src/payments/payments.controller.ts`**
```typescript
import { Body, Controller, Post, UseGuards, HttpCode, HttpStatus, BadRequestException, Req } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { ConfigService } from '@nestjs/config';
import { PaymentsService } from './payments.service';
import { CreatePaymentIntentDto } from './dto/create-payment-intent.dto';
import { ConfirmPaymentDto } from './dto/confirm-payment.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { OffersService } from '../offers/offers.service';
import { UsersService } from '../users/users.service';
import { SubscriptionsService } from '../subscriptions/subscriptions.service';
import { EmailService } from '../common/services/email.service';

@Controller('payments')
@ApiTags('Payments')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth('JWT-auth')
export class PaymentsController {
  constructor(
    private readonly paymentsService: PaymentsService,
    private readonly offersService: OffersService,
    private readonly configService: ConfigService,
    private readonly usersService: UsersService,
    private readonly subscriptionsService: SubscriptionsService,
    private readonly emailService: EmailService,
  ) { }

  @Post('create-intent')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Créer un PaymentIntent Stripe' })
  @ApiResponse({
    status: 201,
    description: 'PaymentIntent créé avec succès',
    schema: {
      example: {
        clientSecret: 'pi_1234567890_secret_abc123',
        paymentIntentId: 'pi_1234567890',
        publishableKey: 'pk_test_...',
      },
    },
  })
  @ApiResponse({ status: 400, description: 'Données invalides' })
  @ApiResponse({ status: 401, description: 'Non autorisé' })
  async createPaymentIntent(@Body() createPaymentIntentDto: CreatePaymentIntentDto) {
    let { amount, currency, paymentMethodId, subscriptionId, childId, offerId, phoneNumber } = createPaymentIntentDto;

    // If childId and offerId are provided, fetch the offer to get the amount
    if (childId && offerId) {
      // Récupérer l'enfant pour obtenir le parentId
      const child = await this.usersService.findById(childId);
      if (!child || !child.parent) {
        throw new BadRequestException('Enfant non trouvé ou sans parent');
      }
      const parentId = typeof child.parent === 'object' && child.parent !== null && '_id' in child.parent 
        ? String(child.parent._id) 
        : String(child.parent);

      // Validation que le parent n'a pas déjà un abonnement actif pour cette offre
      // (Un parent peut avoir plusieurs abonnements actifs mais pas pour la même offre)
      await this.subscriptionsService.validateParentSubscriptionForOffer(parentId, offerId);

      const offer = await this.offersService.findOne(offerId);
      if (!offer) {
        throw new BadRequestException('Offre non trouvée');
      }
      // Calculate amount in cents from offer price
      amount = Math.round(offer.price * 100);
      subscriptionId = undefined;

      // Update parent's phone number if provided
      if (phoneNumber) {
        try {
          const child = await this.usersService.findById(childId);
          if (child && child.parent) {
            const parentId = child.parent.toString();
            // Update user with phone number
            await this.usersService.update(parentId, { phoneNumber } as any);
          }
        } catch (error) {
          console.error('Error updating phone number:', error);
        }
      }
    }

    // Validate that we have an amount
    if (!amount || amount < 1) {
      throw new BadRequestException('Montant invalide. Fournissez soit amount, soit childId et offerId.');
    }

    const result = await this.paymentsService.createPaymentIntent(
      amount,
      currency || 'eur',
      paymentMethodId,
      subscriptionId,
      childId,
      offerId,
      phoneNumber
    );

    const publishableKey = this.configService.get<string>('STRIPE_PUBLISHABLE_KEY');

    return {
      ...result,
      publishableKey,
    };
  }

  @Post('confirm')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Confirmer un paiement Stripe' })
  @ApiResponse({
    status: 200,
    description: 'Paiement confirmé avec succès',
    schema: {
      example: {
        status: 'succeeded',
        paymentIntentId: 'pi_1234567890',
      },
    },
  })
  @ApiResponse({ status: 400, description: 'Erreur lors de la confirmation' })
  @ApiResponse({ status: 401, description: 'Non autorisé' })
  async confirmPayment(@Body() confirmPaymentDto: ConfirmPaymentDto) {
    const { paymentIntentId, paymentMethodId } = confirmPaymentDto;
    if (!paymentIntentId) {
      throw new BadRequestException('paymentIntentId est requis');
    }
    return this.paymentsService.confirmPayment(paymentIntentId, paymentMethodId);
  }

  @Post('complete')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Compléter un paiement et créer l\'abonnement' })
  @ApiResponse({
    status: 200,
    description: 'Paiement complété, abonnement créé et email envoyé',
    schema: {
      example: {
        success: true,
        subscription: {
          id: 'sub_123',
          status: 'ACTIVE',
          paymentStatus: 'PAID',
        },
        emailSent: true,
      },
    },
  })
  @ApiResponse({ status: 400, description: 'Erreur lors du traitement' })
  @ApiResponse({ status: 401, description: 'Non autorisé' })
  async completePayment(
    @Body() body: { paymentIntentId: string; childId: string; offerId: string },
    @Req() req: any,
  ) {
    const { paymentIntentId, childId, offerId } = body;
    const currentUserId = req.user?.userId || req.user?.sub;

    try {
      // Verify payment intent succeeded
      const paymentIntent = await this.paymentsService.getPaymentIntent(paymentIntentId);

      if (paymentIntent.status !== 'succeeded') {
        throw new BadRequestException('Le paiement n\'a pas encore réussi');
      }

      // Get child and parent info
      const child = await this.usersService.findById(childId);
      if (!child || !child.parent) {
        throw new BadRequestException('Enfant non trouvé ou sans parent');
      }

      const parent = await this.usersService.findById(child.parent.toString());
      if (!parent) {
        throw new BadRequestException('Parent non trouvé');
      }

      // Verify current user is the parent
      if (parent._id.toString() !== currentUserId) {
        throw new BadRequestException('Non autorisé');
      }

      // Get offer details
      const offer = await this.offersService.findOne(offerId);
      if (!offer) {
        throw new BadRequestException('Offre non trouvée');
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
      let emailSent = false;
      let emailError = null;

      console.log('📧 ========== DÉBUT ENVOI EMAIL ==========');
      console.log('📧 Email destinataire:', parent.email);

      try {
        console.log('📧 Appel de emailService.sendPaymentConfirmation...');
        await this.emailService.sendPaymentConfirmation(
          parent.email,
          `${parent.prenom} ${parent.nom}`,
          offer.name,
          paymentIntent.amount / 100,
          paymentIntent.currency.toUpperCase(),
          startDate,
          endDate,
        );
        emailSent = true;
        console.log('✅ Email envoyé avec succès !');
      } catch (error: any) {
        emailError = error.message;
        console.error('❌ ========== ERREUR ENVOI EMAIL ==========');
        console.error('❌ Message:', error.message);
      }

      console.log('📧 ========== FIN ENVOI EMAIL ==========');

      return {
        success: true,
        subscription: {
          id: subscription._id.toString(),
          status: subscription.status,
          paymentStatus: subscription.paymentStatus,
          startDate: subscription.startDate,
          endDate: subscription.endDate,
        },
        emailSent,
        emailError,
      };
    } catch (error: any) {
      console.error('Erreur lors de la complétion du paiement:', error);
      throw error;
    }
  }
}
```

**Fichier : `src/payments/twilio.controller.ts`**
```typescript
import { Controller, Post, Body, UseGuards, Logger, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { ConfigService } from '@nestjs/config';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { Twilio } from 'twilio';

interface SMSResult {
    phoneNumber: string;
    success: boolean;
    messageSid?: string;
    error?: string;
}

@ApiTags('Twilio')
@Controller('twilio')
export class TwilioController {
    private readonly logger = new Logger(TwilioController.name);
    private twilioClient: Twilio | null = null;
    private twilioPhoneNumber: string;

    constructor(private configService: ConfigService) {
        const accountSid = this.configService.get<string>('TWILIO_ACCOUNT_SID');
        const authToken = this.configService.get<string>('TWILIO_AUTH_TOKEN');
        this.twilioPhoneNumber = this.configService.get<string>('TWILIO_PHONE_NUMBER') || '';

        if (!accountSid || !authToken) {
            this.logger.warn('⚠️ Twilio credentials not configured. SMS/Voice features will not work.');
            return;
        }

        this.twilioClient = new Twilio(accountSid, authToken);
        this.logger.log('✅ Twilio client initialized successfully');
    }

    @Post('send-sms')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Envoyer un SMS (ACADEMIE|ADMIN)' })
    async sendSMS(@Body() body: { to: string; message: string }) {
        if (!this.twilioClient) {
            return { success: false, error: 'Twilio not configured' };
        }

        try {
            const message = await this.twilioClient.messages.create({
                body: body.message,
                from: this.twilioPhoneNumber,
                to: body.to,
            });

            this.logger.log(`✅ SMS sent successfully. SID: ${message.sid}`);
            return {
                success: true,
                messageSid: message.sid,
                status: message.status,
            };
        } catch (error: any) {
            this.logger.error(`❌ Error sending SMS: ${error.message}`);
            return {
                success: false,
                error: error.message,
            };
        }
    }

    @Post('make-call')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Passer un appel vocal (ACADEMIE|ADMIN)' })
    async makeCall(@Body() body: { to: string; message: string }) {
        if (!this.twilioClient) {
            return { success: false, error: 'Twilio not configured' };
        }

        try {
            // Créer un TwiML pour lire le message
            const twiml = `<?xml version="1.0" encoding="UTF-8"?>
        <Response>
          <Say language="fr-FR">${body.message}</Say>
        </Response>`;

            const call = await this.twilioClient.calls.create({
                twiml: twiml,
                from: this.twilioPhoneNumber,
                to: body.to,
            });

            this.logger.log(`✅ Call initiated successfully. SID: ${call.sid}`);
            return {
                success: true,
                callSid: call.sid,
                status: call.status,
            };
        } catch (error: any) {
            this.logger.error(`❌ Error making call: ${error.message}`);
            return {
                success: false,
                error: error.message,
            };
        }
    }

    @Post('send-notification')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN, UserRole.PARENT)
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Envoyer une notification SMS aux parents' })
    async sendNotification(@Body() body: { phoneNumbers: string[]; message: string }) {
        if (!this.twilioClient) {
            return { success: false, error: 'Twilio not configured' };
        }

        const results: SMSResult[] = [];

        for (const phoneNumber of body.phoneNumbers) {
            try {
                const message = await this.twilioClient.messages.create({
                    body: body.message,
                    from: this.twilioPhoneNumber,
                    to: phoneNumber,
                });

                results.push({
                    phoneNumber,
                    success: true,
                    messageSid: message.sid,
                });

                this.logger.log(`✅ SMS sent to ${phoneNumber}. SID: ${message.sid}`);
            } catch (error: any) {
                results.push({
                    phoneNumber,
                    success: false,
                    error: error.message,
                });

                this.logger.error(`❌ Error sending SMS to ${phoneNumber}: ${error.message}`);
            }
        }

        return {
            totalSent: results.filter(r => r.success).length,
            totalFailed: results.filter(r => !r.success).length,
            results,
        };
    }
}
```

### 3.4 DTOs (Data Transfer Objects)

**Fichier : `src/payments/dto/create-payment-intent.dto.ts`**
```typescript
import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsNumber, IsString, IsOptional, Min, IsBoolean } from 'class-validator';
import { Type } from 'class-transformer';

export class CreatePaymentIntentDto {
  @ApiProperty({ example: 70000, required: false })
  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  amount?: number;

  @ApiProperty({ example: 'eur', required: false })
  @IsString()
  @IsOptional()
  currency?: string;

  // CamelCase
  @IsString()
  @IsOptional()
  paymentMethodId?: string;

  @IsString()
  @IsOptional()
  subscriptionId?: string;

  @IsString()
  @IsOptional()
  phoneNumber?: string;

  @IsString()
  @IsOptional()
  childId?: string;

  @IsString()
  @IsOptional()
  offerId?: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  selectedOptions?: string[];

  @IsBoolean()
  @IsOptional()
  @Type(() => Boolean)
  autoRenew?: boolean;

  // SnakeCase (Aliases)
  @IsString()
  @IsOptional()
  payment_method_id?: string;

  @IsString()
  @IsOptional()
  phone_number?: string;

  @IsString()
  @IsOptional()
  child_id?: string;

  @IsString()
  @IsOptional()
  offer_id?: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  selected_options?: string[];

  @IsBoolean()
  @IsOptional()
  @Type(() => Boolean)
  auto_renew?: boolean;
}
```

**Fichier : `src/payments/dto/confirm-payment.dto.ts`**
```typescript
import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsBoolean, IsNumber, IsOptional, IsString } from 'class-validator';
import { Type } from 'class-transformer';

export class ConfirmPaymentDto {
  @IsString()
  @IsOptional()
  paymentIntentId?: string;

  @IsString()
  @IsOptional()
  payment_intent_id?: string;

  @IsString()
  @IsOptional()
  paymentMethodId?: string;

  @IsString()
  @IsOptional()
  payment_method_id?: string;

  @IsOptional()
  @IsString()
  subscriptionId?: string;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  amount?: number;

  @IsOptional()
  @IsString()
  currency?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  selectedOptions?: string[];

  @IsOptional()
  @IsString()
  phoneNumber?: string;

  @IsOptional()
  @IsString()
  childId?: string;

  @IsOptional()
  @IsString()
  offerId?: string;

  @IsOptional()
  @IsBoolean()
  @Type(() => Boolean)
  autoRenew?: boolean;
}
```

**Fichier : `src/payments/dto/complete-payment.dto.ts`**
```typescript
import { ApiProperty } from '@nestjs/swagger';
import { IsString, IsOptional, IsNumber, IsArray, IsBoolean } from 'class-validator';
import { Type } from 'class-transformer';

export class CompletePaymentDto {
    @IsString()
    @IsOptional()
    paymentIntentId?: string;

    @IsString()
    @IsOptional()
    payment_intent_id?: string;

    @IsOptional()
    @IsString()
    subscriptionId?: string;

    @IsOptional()
    @IsNumber()
    @Type(() => Number)
    amount?: number;

    @IsOptional()
    @IsString()
    currency?: string;

    @IsOptional()
    @IsArray()
    @IsString({ each: true })
    selectedOptions?: string[];

    @IsOptional()
    @IsString()
    phoneNumber?: string;

    @IsOptional()
    @IsString()
    childId?: string;

    @IsOptional()
    @IsString()
    offerId?: string;

    @IsOptional()
    @IsBoolean()
    @Type(() => Boolean)
    autoRenew?: boolean;
}
```
