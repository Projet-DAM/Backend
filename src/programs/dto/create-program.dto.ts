import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';
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
  @IsArray()
  @IsMongoId({ each: true })
  activites?: string[];
}

