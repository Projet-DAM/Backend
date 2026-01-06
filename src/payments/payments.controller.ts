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
