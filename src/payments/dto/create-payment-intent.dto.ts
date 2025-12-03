import { ApiProperty } from '@nestjs/swagger';
import { IsNumber, IsString, IsOptional, Min } from 'class-validator';

export class CreatePaymentIntentDto {
  @ApiProperty({ example: 10000, description: 'Montant en centimes (ex: 10000 = 100.00 €)' })
  @IsNumber()
  @Min(1)
  amount: number;

  @ApiProperty({ example: 'eur', description: 'Devise (par défaut: eur)', required: false })
  @IsString()
  @IsOptional()
  currency?: string = 'eur';

  @ApiProperty({ example: 'pm_1234567890', description: 'ID de la méthode de paiement Stripe' })
  @IsString()
  paymentMethodId: string;

  @ApiProperty({ example: 'sub_1234567890', description: 'ID de l\'abonnement (optionnel)', required: false })
  @IsString()
  @IsOptional()
  subscriptionId?: string;
}










