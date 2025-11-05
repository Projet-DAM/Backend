import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsMongoId, IsNumber, IsOptional, IsString, Matches, Min } from 'class-validator';
import { ActivityStatus } from '../schemas/activity.schema';

export class QueryActivityDto {
  @ApiPropertyOptional({ description: 'Filtrer par catégorie' })
  @IsOptional()
  @IsString()
  categorie?: string;

  @ApiPropertyOptional({ description: 'Filtrer par date (YYYY-MM-DD)', example: '2025-11-15' })
  @IsOptional()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  date?: string;

  @ApiPropertyOptional({ description: 'Filtrer par coach (ObjectId)' })
  @IsOptional()
  @IsMongoId()
  coach?: string;

  @ApiPropertyOptional({ description: 'Filtrer par académie (ObjectId)' })
  @IsOptional()
  @IsMongoId()
  academie?: string;

  @ApiPropertyOptional({ description: 'Filtrer par statut', enum: ActivityStatus })
  @IsOptional()
  @IsEnum(ActivityStatus)
  statut?: ActivityStatus;

  @ApiPropertyOptional({ description: 'Page', default: 1 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  page?: number = 1;

  @ApiPropertyOptional({ description: 'Taille de page', default: 10 })
  @IsOptional()
  @IsNumber()
  @Min(1)
  limit?: number = 10;

  @ApiPropertyOptional({ description: 'Champ de tri', example: 'date' })
  @IsOptional()
  @IsString()
  sortBy?: string = 'date';

  @ApiPropertyOptional({ description: 'Ordre de tri', example: 'asc | desc' })
  @IsOptional()
  @IsString()
  order?: 'asc' | 'desc' = 'asc';
}


