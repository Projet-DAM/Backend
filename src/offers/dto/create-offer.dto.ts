import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean, IsEnum, IsMongoId, IsNotEmpty, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';
import { OfferType } from '../schemas/offer.schema';

export class CreateOfferDto {
  @ApiProperty()
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  description?: string;

  @ApiProperty({ enum: OfferType })
  @IsEnum(OfferType)
  type: OfferType;

  @ApiProperty()
  @IsNumber()
  @Min(1)
  durationDays: number;

  @ApiProperty()
  @IsNumber()
  @Min(0.01)
  price: number;

  @ApiProperty({ default: 0 })
  @IsNumber()
  @Min(0)
  @Max(100)
  @IsOptional()
  discountPct?: number = 0;

  @ApiProperty({ required: false })
  @IsString()
  @IsOptional()
  conditions?: string;

  @ApiProperty({ default: true })
  @IsBoolean()
  @IsOptional()
  isActive?: boolean = true;

  // academyId ne doit PAS être dans le DTO
  // Il sera ajouté automatiquement par le controller depuis le token JWT
}



