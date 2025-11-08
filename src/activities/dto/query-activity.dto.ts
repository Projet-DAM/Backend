import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsIn, IsMongoId, IsNumber, IsOptional, IsString, Matches, Min } from 'class-validator';
import { Transform } from 'class-transformer';
import { ActivityStatus } from '../schemas/activity.schema';

// Helper function to transform empty strings to undefined
const transformEmptyToUndefined = ({ value }: any) => {
  if (value === '' || value === null) {
    return undefined;
  }
  return value;
};

export class QueryActivityDto {
  @ApiPropertyOptional({ description: 'Filtrer par catégorie' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsString()
  categorie?: string;

  @ApiPropertyOptional({ description: 'Filtrer par date (YYYY-MM-DD)', example: '2025-11-15' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @Matches(/^\d{4}-\d{2}-\d{2}$/)
  date?: string;

  @ApiPropertyOptional({ description: 'Filtrer par coach (ObjectId)' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsMongoId()
  coach?: string;

  @ApiPropertyOptional({ description: 'Filtrer par académie (ObjectId)' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsMongoId()
  academie?: string;

  @ApiPropertyOptional({ description: 'Filtrer par statut', enum: ActivityStatus })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsEnum(ActivityStatus)
  statut?: ActivityStatus;

  @ApiPropertyOptional({ description: 'Page', default: 1 })
  @Transform(({ value }) => value === '' || value === null ? undefined : Number(value))
  @IsOptional()
  @IsNumber()
  @Min(1)
  page?: number = 1;

  @ApiPropertyOptional({ description: 'Taille de page', default: 10 })
  @Transform(({ value }) => value === '' || value === null ? undefined : Number(value))
  @IsOptional()
  @IsNumber()
  @Min(1)
  limit?: number = 10;

  @ApiPropertyOptional({ description: 'Champ de tri', example: 'date' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsString()
  sortBy?: string = 'date';

  @ApiPropertyOptional({ description: 'Ordre de tri', example: 'asc | desc' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsIn(['asc', 'desc'])
  order?: 'asc' | 'desc' = 'asc';
}



