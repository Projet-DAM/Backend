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

      const metadata: Record<string, string> = {
        phoneNumber: phoneNumber || process.env.DEFAULT_PHONE_NUMBER || '',
      };
      if (subscriptionId) metadata.subscriptionId = subscriptionId;
      if (childId) metadata.childId = childId;
      if (offerId) metadata.offerId = offerId;

      paymentIntentParams.metadata = metadata;

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
      // If paymentMethodId is not provided, this might be a simple retrieval or confirmation check
      const paymentIntent = await this.stripe.paymentIntents.retrieve(paymentIntentId);
      if (paymentIntent.status === 'requires_confirmation' && !paymentMethodId) {
        // This matches the simpler confirmPayment logic that might be expected by some parts of the app
        return await this.stripe.paymentIntents.confirm(paymentIntentId);
      }
      return paymentIntent;
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

  /**
   * Traite un webhook Stripe et envoie une confirmation WhatsApp si le paiement est réussi
   */
  async handleWebhook(event: Stripe.Event) {
    console.log('Webhook reçu:', event.type);

    if (event.type === 'payment_intent.succeeded') {
      const paymentIntent = event.data.object as Stripe.PaymentIntent;

      console.log('Paiement réussi:', paymentIntent.id);
      console.log('Montant:', paymentIntent.amount, paymentIntent.currency);

      const phoneNumber = paymentIntent.metadata?.phoneNumber || process.env.DEFAULT_PHONE_NUMBER;

      if (phoneNumber) {
        try {
          await this.twilioService.sendPaymentConfirmation(
            phoneNumber,
            paymentIntent.amount,
            paymentIntent.currency
          );
          console.log('Message de confirmation envoyé à', phoneNumber);
        } catch (error) {
          console.error('Erreur lors de l\'envoi du message WhatsApp:', error);
        }
      } else {
        console.warn('Aucun numéro de téléphone trouvé pour envoyer la confirmation');
      }
    }

    return { received: true };
  }

  getPublishableKey(): string {
    return this.configService.get<string>('STRIPE_PUBLISHABLE_KEY') || process.env.STRIPE_PUBLISHABLE_KEY || '';
  }
}
