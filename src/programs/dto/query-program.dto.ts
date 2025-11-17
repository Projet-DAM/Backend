import { ApiPropertyOptional } from '@nestjs/swagger';
import { Transform } from 'class-transformer';
import { IsEnum, IsIn, IsMongoId, IsNumber, IsOptional, IsString, Min } from 'class-validator';
import { ProgramStatus } from '../schemas/program.schema';

const transformEmptyToUndefined = ({ value }: any) => (value === '' || value === null ? undefined : value);

export class QueryProgramDto {
  @ApiPropertyOptional({ description: 'Filtrer par nom (contient)' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsString()
  nom?: string;

  @ApiPropertyOptional({ description: 'Filtrer par coach' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsMongoId()
  coach?: string;

  @ApiPropertyOptional({ description: 'Filtrer par académie' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsMongoId()
  academie?: string;

  @ApiPropertyOptional({ description: 'Filtrer par statut', enum: ProgramStatus })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsEnum(ProgramStatus)
  statut?: ProgramStatus;

  @ApiPropertyOptional({ description: 'Page', default: 1 })
  @Transform(({ value }) => (value === '' || value === null ? undefined : Number(value)))
  @IsOptional()
  @IsNumber()
  @Min(1)
  page?: number = 1;

  @ApiPropertyOptional({ description: 'Limite', default: 10 })
  @Transform(({ value }) => (value === '' || value === null ? undefined : Number(value)))
  @IsOptional()
  @IsNumber()
  @Min(1)
  limit?: number = 10;

  @ApiPropertyOptional({ description: 'Champ de tri', default: 'createdAt' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsString()
  sortBy?: string = 'createdAt';

  @ApiPropertyOptional({ description: 'Ordre de tri', default: 'desc' })
  @Transform(transformEmptyToUndefined)
  @IsOptional()
  @IsIn(['asc', 'desc'])
  order?: 'asc' | 'desc' = 'desc';
}

