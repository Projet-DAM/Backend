import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
import { Transform, Type } from 'class-transformer';
import { IsArray, IsEnum, IsMongoId, IsNumber, IsOptional, IsString, Length, MaxLength, Min } from 'class-validator';
import { ProgramStatus } from '../schemas/program.schema';

export class CreateProgramDto {
  @ApiProperty({ description: 'Nom du programme', example: 'Pré-saison U13' })
  @IsString()
  @Length(2, 120)
  nom_programme: string;

  @ApiPropertyOptional({ description: 'Description du programme' })
  @IsOptional()
  @IsString()
  @MaxLength(1000)
  description?: string;

  @ApiPropertyOptional({ description: 'Objectif principal du programme' })
  @IsOptional()
  @IsString()
  @MaxLength(500)
  objectif?: string;

  @ApiPropertyOptional({ description: 'Niveau visé (débutant, intermédiaire...)' })
  @IsOptional()
  @IsString()
  @MaxLength(100)
  niveau?: string;

  @ApiPropertyOptional({ description: 'Prix du programme', example: 99 })
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

  @ApiPropertyOptional({ description: 'Statut du programme', enum: ProgramStatus, default: ProgramStatus.BROUILLON })
  @IsOptional()
  @IsEnum(ProgramStatus)
  statut?: ProgramStatus;

  @ApiPropertyOptional({
    description: 'Activités à inclure dans le programme',
    type: [String],
    example: ['507f1f77bcf86cd799439011', '507f1f77bcf86cd799439012'],
  })
  @IsOptional()
  @Transform(({ value }) => {
    if (!value) return undefined;
    if (Array.isArray(value)) return value;
    if (typeof value === 'string') {
      // Si c'est une string séparée par des virgules, la convertir en tableau
      return value.split(',').map(item => item.trim()).filter(item => item.length > 0);
    }
    return value;
  })
  @IsArray()
  @IsMongoId({ each: true })
  activites?: string[];

  @ApiPropertyOptional({ description: 'Chemin de l\'image (généré automatiquement lors de l\'upload)' })
  @IsOptional()
  @IsString()
  image?: string;
}

