import { ApiProperty } from '@nestjs/swagger';
import { IsOptional, IsEnum, IsNumber, Min, IsMongoId } from 'class-validator';
import { MatchStatut } from '../interfaces/match-statut.enum';

export class UpdateMatchDto {
  @ApiProperty({
    example: 'en_cours',
    description: 'Statut du match',
    enum: MatchStatut,
    enumName: 'MatchStatut',
    required: false,
  })
  @IsOptional()
  @IsEnum(MatchStatut, { message: 'Le statut doit être l\'un des suivants: a_venir, en_cours, termine' })
  statut?: MatchStatut;

  @ApiProperty({
    example: 2,
    description: 'Score de l\'équipe A',
    required: false,
    minimum: 0,
  })
  @IsOptional()
  @IsNumber({}, { message: 'Le score de l\'équipe A doit être un nombre' })
  @Min(0, { message: 'Le score ne peut pas être négatif' })
  scoreEquipeA?: number;

  @ApiProperty({
    example: 1,
    description: 'Score de l\'équipe B',
    required: false,
    minimum: 0,
  })
  @IsOptional()
  @IsNumber({}, { message: 'Le score de l\'équipe B doit être un nombre' })
  @Min(0, { message: 'Le score ne peut pas être négatif' })
  scoreEquipeB?: number;

  @ApiProperty({
    example: '507f1f77bcf86cd799439021',
    description: 'ID du match suivant (pour la progression dans l\'arbre)',
    required: false,
  })
  @IsOptional()
  @IsMongoId({ message: 'matchSuivantId doit être un ID MongoDB valide' })
  matchSuivantId?: string;
}
