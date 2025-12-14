import { ApiProperty } from '@nestjs/swagger';
import { IsNumber, IsString, IsOptional, Min } from 'class-validator';

export class CreatePaymentIntentDto {
  @ApiProperty({ example: 10000, description: 'Montant en centimes (ex: 10000 = 100.00 €)', required: false })
  @IsNumber()
  @IsOptional()
  @Min(1)
  amount?: number;

  @ApiProperty({ example: 'eur', description: 'Devise (par défaut: eur)', required: false })
  @IsString()
  @IsOptional()
  currency?: string = 'eur';

  @ApiProperty({ example: 'pm_1234567890', description: 'ID de la méthode de paiement Stripe', required: false })
  @IsString()
  @IsOptional()
  paymentMethodId?: string;

  @ApiProperty({ example: 'sub_1234567890', description: 'ID de l\'abonnement (optionnel)', required: false })
  @IsString()
  @IsOptional()
  subscriptionId?: string;

  @ApiProperty({ example: 'child_123', description: 'ID de l\'enfant', required: false })
  @IsString()
  @IsOptional()
  childId?: string;

  @ApiProperty({ example: 'offer_123', description: 'ID de l\'offre', required: false })
  @IsString()
  @IsOptional()
  offerId?: string;

  @ApiProperty({ example: '+21698765432', description: 'Numéro de téléphone du parent', required: false })
  @IsString()
  @IsOptional()
  phoneNumber?: string;
}










