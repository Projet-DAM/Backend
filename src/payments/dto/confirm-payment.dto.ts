import { ApiProperty } from '@nestjs/swagger';
import { IsString } from 'class-validator';

export class ConfirmPaymentDto {
  @ApiProperty({ example: 'pi_1234567890', description: 'ID du PaymentIntent' })
  @IsString()
  paymentIntentId: string;

  @ApiProperty({ example: 'pm_1234567890', description: 'ID de la méthode de paiement Stripe' })
  @IsString()
  paymentMethodId: string;
}





