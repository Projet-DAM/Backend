import { PartialType } from '@nestjs/swagger';
import { CreateOfferDto } from './create-offer.dto';
import { IsEnum, IsNumber, IsOptional, Max, Min } from 'class-validator';
import { OfferType } from '../schemas/offer.schema';

export class UpdateOfferDto extends PartialType(CreateOfferDto) {
  @IsOptional()
  @IsEnum(OfferType)
  override type?: OfferType;

  @IsOptional()
  @IsNumber()
  @Min(1)
  override durationDays?: number;

  @IsOptional()
  @IsNumber()
  @Min(0.01)
  override price?: number;

  @IsOptional()
  @IsNumber()
  @Min(0)
  @Max(100)
  override discountPct?: number;

  // academyId ne doit PAS être dans le DTO
  // Il ne peut pas être modifié via l'endpoint de mise à jour
}



