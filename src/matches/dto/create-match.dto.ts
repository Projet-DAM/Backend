import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, IsEnum, IsMongoId, IsOptional, IsNumber, Min } from 'class-validator';
import { MatchPhase } from '../interfaces/match-phase.enum';
import { MatchStatut } from '../interfaces/match-statut.enum';

export class CreateMatchDto {
  @ApiProperty({
    example: '507f1f77bcf86cd799439011',
    description: 'ID du tournoi (MongoDB ObjectId)',
    required: true,
  })
  @IsNotEmpty({ message: 'Le tournoi est requis' })
  @IsMongoId({ message: 'tournoiId doit être un ID MongoDB valide' })
  tournoiId: string;

  @ApiProperty({
    example: '507f1f77bcf86cd799439021',
    description: 'ID de l\'équipe A (MongoDB ObjectId) - optionnel si généré automatiquement',
    required: false,
  })
  @IsOptional()
  @IsMongoId({ message: 'equipeA doit être un ID MongoDB valide' })
  equipeA?: string;

  @ApiProperty({
    example: '507f1f77bcf86cd799439022',
    description: 'ID de l\'équipe B (MongoDB ObjectId) - optionnel si généré automatiquement',
    required: false,
  })
  @IsOptional()
  @IsMongoId({ message: 'equipeB doit être un ID MongoDB valide' })
  equipeB?: string;

  @ApiProperty({
    example: 'quart_final',
    description: 'Phase du match',
    enum: MatchPhase,
    enumName: 'MatchPhase',
    required: true,
  })
  @IsNotEmpty({ message: 'La phase est requise' })
  @IsEnum(MatchPhase, { message: 'La phase doit être l\'un des suivants: quart_final, demi_final, finale' })
  phase: MatchPhase;

  @ApiProperty({
    example: 'a_venir',
    description: 'Statut du match',
    enum: MatchStatut,
    enumName: 'MatchStatut',
    default: MatchStatut.A_VENIR,
    required: false,
  })
  @IsOptional()
  @IsEnum(MatchStatut, { message: 'Le statut doit être l\'un des suivants: a_venir, en_cours, termine' })
  statut?: MatchStatut;

  @ApiProperty({
    example: 1,
    description: 'Ordre du match dans la phase (pour l\'affichage)',
    required: false,
  })
  @IsOptional()
  @IsNumber({}, { message: 'L\'ordre doit être un nombre' })
  @Min(1, { message: 'L\'ordre doit être au moins 1' })
  ordre?: number;
}
