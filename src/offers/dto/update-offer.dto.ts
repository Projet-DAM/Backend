import { PartialType } from '@nestjs/swagger';
import { CreateOfferDto } from './create-offer.dto';
import { IsEnum, IsMongoId, IsNumber, IsOptional, Max, Min } from 'class-validator';
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

  @IsOptional()
  @IsMongoId({ message: 'academyId doit être un ObjectId MongoDB valide (24 caractères hexadécimaux). Exemple: 690cd9998d614e72c9b1ab55' })
  override academyId?: string;
}



