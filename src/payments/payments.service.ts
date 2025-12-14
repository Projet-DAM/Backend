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
        amount: Math.round(amount), // S'assurer que c'est un entier
        currency: currency.toLowerCase(),
        automatic_payment_methods: {
          enabled: true,
        },
      };

      if (paymentMethodId) {
        paymentIntentParams.payment_method = paymentMethodId;
        paymentIntentParams.confirm = true;
        paymentIntentParams.return_url = 'https://sportyconnect.com/payment-return'; // Required when confirm: true
      }

      // Ajouter des métadonnées
      const metadata: Record<string, string> = {};
      if (subscriptionId) metadata.subscriptionId = subscriptionId;
      if (childId) metadata.childId = childId;
      if (offerId) metadata.offerId = offerId;

      // Add phone number for WhatsApp notification if available
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
   * Used by the controller to finalize payments or check status.
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

  /**
   * Retrieve a payment intent by ID.
   */
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
   * (Legacy/Development helper)
   */
  async verifyAndNotifyPayment(paymentIntentId: string) {
    try {
      // 1. Récupérer le PaymentIntent depuis Stripe
      const paymentIntent = await this.stripe.paymentIntents.retrieve(paymentIntentId);

      // 2. Vérifier si le paiement est réussi
      if (paymentIntent.status === 'succeeded') {
        console.log(`✅ Paiement ${paymentIntentId} confirmé via API directe`);

        // 3. Récupérer le numéro
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
   * Récupère la clé publique Stripe pour le frontend
   */
  getPublishableKey(): string {
    return this.configService.get<string>('STRIPE_PUBLISHABLE_KEY') || process.env.STRIPE_PUBLISHABLE_KEY || '';
  }
}
