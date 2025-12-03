import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, Length } from 'class-validator';

export class VerifyEmailDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID de l\'utilisateur' })
  @IsNotEmpty()
  @IsString()
  userId: string;

  @ApiProperty({ example: '123456', description: 'Code de vérification à 6 chiffres' })
  @IsNotEmpty()
  @IsString()
  @Length(6, 6, { message: 'Le code de vérification doit contenir exactement 6 chiffres' })
  code: string;
}











