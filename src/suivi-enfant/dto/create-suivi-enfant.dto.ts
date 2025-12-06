import { IsDateString, IsBoolean, IsNumber, IsString, IsOptional, IsMongoId, IsArray, Min, Max } from 'class-validator';
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

  @IsOptional()
  @IsString()
  enfantName?: string; // Nom complet de l'enfant (coach selects a child; name derived if omitted)
  @IsOptional()
  @IsString()
  activityType?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  focusAreas?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  nextSessionGoals?: string[];

  @IsOptional()
  @IsNumber()
  @Min(1)
  @Max(10)
  @Type(() => Number)
  effortLevel?: number;

  @IsOptional()
  @IsString()
  emotionalState?: string;
}
