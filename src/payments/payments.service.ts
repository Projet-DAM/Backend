import { Injectable, BadRequestException, InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import Stripe from 'stripe';

@Injectable()
export class PaymentsService {
  private stripe: Stripe;

  constructor(private configService: ConfigService) {
    const secretKey = this.configService.get<string>('STRIPE_SECRET_KEY');
    if (!secretKey) {
      console.warn('⚠️ STRIPE_SECRET_KEY is not configured. Payments will not work.');
      // Ne pas lancer d'erreur pour permettre au serveur de démarrer
      // L'utilisateur devra configurer la clé dans .env
      this.stripe = null as any; // Type assertion temporaire
      return;
    }
    this.stripe = new Stripe(secretKey, {
      apiVersion: '2025-10-29.clover',
    });
  }

  /**
   * Crée un PaymentIntent avec Stripe
   * @param amount Montant en centimes
   * @param currency Devise (par défaut: eur)
   * @param paymentMethodId ID de la méthode de paiement Stripe
   * @param subscriptionId ID de l'abonnement (optionnel)
   */
  async createPaymentIntent(
    amount: number,
    currency: string = 'eur',
    paymentMethodId: string,
    subscriptionId?: string,
  ): Promise<{ clientSecret: string; paymentIntentId: string }> {
    if (!this.stripe) {
      throw new BadRequestException('Stripe n\'est pas configuré. Veuillez ajouter STRIPE_SECRET_KEY dans le fichier .env');
    }
    try {
      const paymentIntentParams: Stripe.PaymentIntentCreateParams = {
        amount: Math.round(amount), // S'assurer que c'est un entier
        currency: currency.toLowerCase(),
        payment_method: paymentMethodId,
        confirmation_method: 'manual',
        confirm: false,
        automatic_payment_methods: {
          enabled: true,
          allow_redirects: 'never',
        },
      };

      // Ajouter des métadonnées si un abonnement est associé
      if (subscriptionId) {
        paymentIntentParams.metadata = {
          subscriptionId: subscriptionId,
        };
      }

      const paymentIntent = await this.stripe.paymentIntents.create(paymentIntentParams);

      return {
        clientSecret: paymentIntent.client_secret || '',
        paymentIntentId: paymentIntent.id,
      };
    } catch (error) {
      console.error('Erreur lors de la création du PaymentIntent:', error);
      if (error instanceof Stripe.errors.StripeError) {
        throw new BadRequestException(`Erreur Stripe: ${error.message}`);
      }
      throw new InternalServerErrorException('Erreur lors de la création du PaymentIntent');
    }
  }

  /**
   * Confirme un PaymentIntent
   * @param paymentIntentId ID du PaymentIntent
   * @param paymentMethodId ID de la méthode de paiement
   */
  async confirmPayment(
    paymentIntentId: string,
    paymentMethodId: string,
  ): Promise<{ status: string; paymentIntentId: string }> {
    if (!this.stripe) {
      throw new BadRequestException('Stripe n\'est pas configuré. Veuillez ajouter STRIPE_SECRET_KEY dans le fichier .env');
    }
    try {
      const paymentIntent = await this.stripe.paymentIntents.confirm(paymentIntentId, {
        payment_method: paymentMethodId,
      });

      return {
        status: paymentIntent.status,
        paymentIntentId: paymentIntent.id,
      };
    } catch (error) {
      console.error('Erreur lors de la confirmation du paiement:', error);
      if (error instanceof Stripe.errors.StripeError) {
        throw new BadRequestException(`Erreur Stripe: ${error.message}`);
      }
      throw new InternalServerErrorException('Erreur lors de la confirmation du paiement');
    }
  }

  /**
   * Récupère un PaymentIntent par son ID
   * @param paymentIntentId ID du PaymentIntent
   */
  async getPaymentIntent(paymentIntentId: string): Promise<Stripe.PaymentIntent> {
    if (!this.stripe) {
      throw new BadRequestException('Stripe n\'est pas configuré. Veuillez ajouter STRIPE_SECRET_KEY dans le fichier .env');
    }
    try {
      return await this.stripe.paymentIntents.retrieve(paymentIntentId);
    } catch (error) {
      console.error('Erreur lors de la récupération du PaymentIntent:', error);
      if (error instanceof Stripe.errors.StripeError) {
        throw new BadRequestException(`Erreur Stripe: ${error.message}`);
      }
      throw new InternalServerErrorException('Erreur lors de la récupération du PaymentIntent');
    }
  }
}

