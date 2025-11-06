import { IsDateString, IsBoolean, IsNumber, IsString, IsOptional, IsMongoId } from 'class-validator';
import { Type } from 'class-transformer';

export class CreateSuiviEnfantDto {
  @IsDateString()
  date_suivi: Date;

  @IsBoolean()
  presence: boolean;

  @IsNumber()
  @Type(() => Number)
  performance: number;

  @IsOptional()
  @IsString()
  commentaire?: string;

  @IsMongoId()
  @IsString()
  enfantId: string; // Correspond à l'ID du User avec rôle ENFANT
}
