import { Body, Controller, Post, UseGuards, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiBearerAuth, ApiOperation, ApiResponse, ApiTags } from '@nestjs/swagger';
import { PaymentsService } from './payments.service';
import { CreatePaymentIntentDto } from './dto/create-payment-intent.dto';
import { ConfirmPaymentDto } from './dto/confirm-payment.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';

@ApiTags('Payments')
@Controller('payments')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth('JWT-auth')
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

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
      },
    },
  })
  @ApiResponse({ status: 400, description: 'Données invalides' })
  @ApiResponse({ status: 401, description: 'Non autorisé' })
  async createPaymentIntent(@Body() createPaymentIntentDto: CreatePaymentIntentDto) {
    const { amount, currency, paymentMethodId, subscriptionId } = createPaymentIntentDto;

    // Convertir le montant en centimes si nécessaire (si reçu en euros)
    // Le frontend devrait envoyer le montant en centimes, mais on vérifie
    const amountInCents = amount < 100 ? Math.round(amount * 100) : amount;

    return this.paymentsService.createPaymentIntent(
      amountInCents,
      currency || 'eur',
      paymentMethodId,
      subscriptionId,
    );
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
    return this.paymentsService.confirmPayment(paymentIntentId, paymentMethodId);
  }
}





