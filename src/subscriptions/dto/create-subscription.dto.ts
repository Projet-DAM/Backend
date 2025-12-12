import { ApiProperty } from '@nestjs/swagger';
import { IsBoolean, IsDateString, IsMongoId, IsOptional } from 'class-validator';

export class CreateSubscriptionDto {
  @ApiProperty({ 
    example: '690cd9998d614e72c9b1ab55',
    description: 'ID de l\'enfant (ObjectId MongoDB - 24 caractères hexadécimaux)'
  })
  @IsMongoId({ message: 'childId doit être un ObjectId MongoDB valide (24 caractères hexadécimaux)' })
  childId: string;

  @ApiProperty({ 
    example: '690cd9998d614e72c9b1ab55',
    description: 'ID de l\'offre (ObjectId MongoDB - 24 caractères hexadécimaux)'
  })
  @IsMongoId({ message: 'offerId doit être un ObjectId MongoDB valide (24 caractères hexadécimaux)' })
  offerId: string;

  @ApiProperty({ required: false })
  @IsOptional()
  @IsDateString()
  startDate?: string;

  @ApiProperty({ required: false, default: false })
  @IsOptional()
  @IsBoolean()
  autoRenew?: boolean = false;
}



