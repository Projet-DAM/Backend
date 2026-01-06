import { Body, Controller, Post, UseGuards, HttpCode, HttpStatus, BadRequestException, Req, Headers } from '@nestjs/common';
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
import type { Request } from 'express';
import Stripe from 'stripe';

@Controller('payments')
@ApiTags('Payments')
export class PaymentsController {
  private stripe: Stripe;

  constructor(
    private readonly paymentsService: PaymentsService,
    private readonly offersService: OffersService,
    private readonly configService: ConfigService,
    private readonly usersService: UsersService,
    private readonly subscriptionsService: SubscriptionsService,
    private readonly emailService: EmailService,
  ) {
    this.stripe = new Stripe(this.configService.get<string>('STRIPE_SECRET_KEY') || process.env.STRIPE_SECRET_KEY || '', {
      apiVersion: '2024-12-18.acacia',
    } as any);
  }

  @Post('create-intent')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('JWT-auth')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Créer un PaymentIntent Stripe' })
  @ApiResponse({ status: 201, description: 'PaymentIntent créé avec succès' })
  async createPaymentIntent(@Body() createPaymentIntentDto: CreatePaymentIntentDto) {
    let { amount, currency, paymentMethodId, subscriptionId, childId, offerId, phoneNumber } = createPaymentIntentDto;

    if (childId && offerId) {
      const child = await this.usersService.findById(childId);
      if (!child || !child.parent) {
        throw new BadRequestException('Enfant non trouvé ou sans parent');
      }
      const parentId = typeof child.parent === 'object' && child.parent !== null && '_id' in child.parent
        ? String(child.parent._id)
        : String(child.parent);

      await this.subscriptionsService.validateParentSubscriptionForOffer(parentId, offerId);

      const offer = await this.offersService.findOne(offerId);
      if (!offer) {
        throw new BadRequestException('Offre non trouvée');
      }
      amount = Math.round(offer.price * 100);
      subscriptionId = undefined;

      if (phoneNumber) {
        try {
          await this.usersService.update(parentId, { phoneNumber } as any);
        } catch (error) {
          console.error('Error updating phone number:', error);
        }
      }
    }

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
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('JWT-auth')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Confirmer un paiement Stripe' })
  async confirmPayment(@Body() confirmPaymentDto: ConfirmPaymentDto) {
    const { paymentIntentId, paymentMethodId } = confirmPaymentDto;
    if (!paymentIntentId) {
      throw new BadRequestException('paymentIntentId est requis');
    }
    return this.paymentsService.confirmPayment(paymentIntentId, paymentMethodId);
  }

  @Post('complete')
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth('JWT-auth')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Compléter un paiement et créer l\'abonnement' })
  async completePayment(
    @Body() body: { paymentIntentId: string; childId: string; offerId: string },
    @Req() req: any,
  ) {
    const { paymentIntentId, childId, offerId } = body;
    const currentUserId = req.user?.userId || req.user?.sub;

    try {
      const paymentIntent = await this.paymentsService.getPaymentIntent(paymentIntentId);

      if (paymentIntent.status !== 'succeeded') {
        throw new BadRequestException('Le paiement n\'a pas encore réussi');
      }

      const child = await this.usersService.findById(childId);
      if (!child || !child.parent) {
        throw new BadRequestException('Enfant non trouvé ou sans parent');
      }

      const parent = await this.usersService.findById(child.parent.toString());
      if (!parent) {
        throw new BadRequestException('Parent non trouvé');
      }

      if (parent._id.toString() !== currentUserId) {
        throw new BadRequestException('Non autorisé');
      }

      const offer = await this.offersService.findOne(offerId);
      if (!offer) {
        throw new BadRequestException('Offre non trouvée');
      }

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

      await this.subscriptionsService.recordPayment(
        subscription._id.toString(),
        {
          amount: paymentIntent.amount / 100,
          currency: paymentIntent.currency.toUpperCase(),
          method: 'STRIPE',
        },
        { userId: parent._id.toString(), role: parent.role },
      );

      let emailSent = false;
      let emailError = null;

      try {
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
      } catch (error: any) {
        emailError = error.message;
        console.error('Error sending email:', error.message);
      }

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

  @Post('webhook')
  @ApiOperation({ summary: 'Webhook Stripe pour les événements de paiement' })
  async handleWebhook(
    @Headers('stripe-signature') signature: string,
    @Req() request: Request,
  ) {
    const endpointSecret = this.configService.get<string>('STRIPE_WEBHOOK_SECRET') || process.env.STRIPE_WEBHOOK_SECRET;

    if (!endpointSecret) {
      throw new BadRequestException('STRIPE_WEBHOOK_SECRET missing');
    }

    let event: Stripe.Event;

    try {
      const rawBody = (request as any).rawBody;
      if (!rawBody) {
        throw new BadRequestException('Request body missing');
      }

      event = this.stripe.webhooks.constructEvent(
        rawBody,
        signature,
        endpointSecret,
      );
    } catch (err) {
      console.error('Webhook signature verification failed.', err.message);
      throw new BadRequestException(`Webhook Error: ${err.message}`);
    }

    return this.paymentsService.handleWebhook(event);
  }
}
