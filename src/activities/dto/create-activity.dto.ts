import { ApiProperty } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import { IsDateString, IsEnum, IsMongoId, IsNumber, IsOptional, IsPositive, IsString, Length, Matches, Min } from 'class-validator';
import { ActivityStatus } from '../schemas/activity.schema';

export class CreateActivityDto {
  @ApiProperty({ description: 'Nom de l\'activité', example: 'Football U10' })
  @IsString()
  @Length(2, 100)
  nom_activite: string;

  @ApiProperty({ description: 'Description libre', required: false, example: 'Séance d\'entraînement hebdomadaire' })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({ description: 'Catégorie de l\'activité', required: false, example: 'Football' })
  @IsOptional()
  @IsString()
  categorie?: string;

  @ApiProperty({ description: 'Jour de l\'activité (date ISO)', required: false, example: '2025-11-15' })
  @IsOptional()
  @IsDateString()
  date?: string;

  @ApiProperty({ description: 'Heure de début (HH:mm)', required: false, example: '14:30' })
  @IsOptional()
  @IsString()
  @Transform(({ value }) => {
    if (typeof value !== 'string') return value;
    // Normalize potential full-width colon and trim whitespace
    return value.replace(/\uFF1A/g, ':').trim();
  })
  @Matches(/^([01]\d|2[0-3]):[0-5]\d$/, { message: 'heure doit être au format HH:mm' })
  heure?: string;

  @ApiProperty({ description: 'Durée en minutes', required: false, example: 90 })
  @IsOptional()
  @Type(() => Number)
  @Transform(({ value }) => {
    if (value === '' || value === null || value === undefined) return undefined;
    if (typeof value === 'number') return value;
    const num = Number(value);
    return isNaN(num) ? undefined : num;
  })
  @IsNumber()
  @IsPositive()
  duree?: number;

  @ApiProperty({ description: 'Capacité maximale', required: false, example: 20 })
  @IsOptional()
  @Type(() => Number)
  @Transform(({ value }) => {
    if (value === '' || value === null || value === undefined) return undefined;
    if (typeof value === 'number') return value;
    const num = Number(value);
    return isNaN(num) ? undefined : num;
  })
  @IsNumber()
  @Min(1)
  capacite_max?: number;

  @ApiProperty({ description: 'Prix', required: false, example: 15 })
  @IsOptional()
  @Type(() => Number)
  @Transform(({ value }) => {
    if (value === '' || value === null || value === undefined) return undefined;
    if (typeof value === 'number') return value;
    const num = Number(value);
    return isNaN(num) ? undefined : num;
  })
  @IsNumber()
  @Min(0)
  prix?: number;

  @ApiProperty({ description: 'Statut de l\'activité', enum: ActivityStatus, required: false, example: ActivityStatus.ACTIVE })
  @IsOptional()
  @IsEnum(ActivityStatus)
  statut?: ActivityStatus;

  @ApiProperty({ description: 'ID du coach (référencé automatiquement si rôle coach)', required: false, example: '507f1f77bcf86cd799439011' })
  @IsOptional()
  @IsMongoId()
  coach?: string;

  @ApiProperty({ description: 'ID de l\'académie (référencé automatiquement si rôle académie)', required: false, example: '507f1f77bcf86cd799439012' })
  @IsOptional()
  @IsMongoId()
  academie?: string;

  @ApiProperty({ description: 'Chemin de l\'image (généré automatiquement lors de l\'upload)', required: false })
  @IsOptional()
  @IsString()
  image?: string;
}


