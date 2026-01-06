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
import { Injectable } from '@nestjs/common';
import Stripe from 'stripe';
import { TwilioService } from './twilio.service';

@Injectable()
export class PaymentsService {
    private stripe: Stripe;

    constructor(private readonly twilioService: TwilioService) {
        // Initialize Stripe with the secret key from environment variables
        // Make sure to add STRIPE_SECRET_KEY to your .env file
        this.stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
            apiVersion: '2024-12-18.acacia',
        } as any);
    }


    async createPaymentIntent(amount: number, currency: string = 'eur', phoneNumber?: string) {
        const paymentIntent = await this.stripe.paymentIntents.create({
            amount,
            currency,
            automatic_payment_methods: {
                enabled: true,
            },
            metadata: {
                phoneNumber: phoneNumber || process.env.DEFAULT_PHONE_NUMBER || '',
            },
        });

        return {
            clientSecret: paymentIntent.client_secret,
        };
    }


    /**
     * Vérifie le statut du paiement et envoie le message WhatsApp
     * (Alternative aux webhooks pour le développement)
     */
    async confirmPayment(paymentIntentId: string) {
        try {
            // 1. Récupérer le PaymentIntent depuis Stripe
            const paymentIntent = await this.stripe.paymentIntents.retrieve(paymentIntentId);

            // 2. Vérifier si le paiement est réussi
            if (paymentIntent.status === 'succeeded') {
                console.log(`✅ Paiement ${paymentIntentId} confirmé via API directe`);

                // 3. Récupérer le numéro
                const phoneNumber = paymentIntent.metadata?.phoneNumber || process.env.DEFAULT_PHONE_NUMBER;

                console.log('📞 [DEBUG] Numéro de téléphone récupéré:', phoneNumber);
                console.log('   - Depuis metadata:', paymentIntent.metadata?.phoneNumber);
                console.log('   - DEFAULT_PHONE_NUMBER:', process.env.DEFAULT_PHONE_NUMBER);

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

        // Gérer l'événement de paiement réussi
        if (event.type === 'payment_intent.succeeded') {
            const paymentIntent = event.data.object as Stripe.PaymentIntent;

            console.log('Paiement réussi:', paymentIntent.id);
            console.log('Montant:', paymentIntent.amount, paymentIntent.currency);

            // Récupérer le numéro de téléphone depuis les métadonnées
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

    /**
     * Récupère la clé publique Stripe pour le frontend
     */
    getPublishableKey(): string {
        return process.env.STRIPE_PUBLISHABLE_KEY || '';
    }
}
