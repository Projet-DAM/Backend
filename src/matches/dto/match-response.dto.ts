import { ApiProperty } from '@nestjs/swagger';
import { MatchPhase } from '../interfaces/match-phase.enum';
import { MatchStatut } from '../interfaces/match-statut.enum';
import { EquipeResponseDto } from '../../equipes/dto/equipe-response.dto';

export class MatchResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439031', description: 'ID unique du match' })
  _id: string;

  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID du tournoi' })
  tournoiId: string;

  @ApiProperty({
    type: () => EquipeResponseDto,
    description: 'Équipe A avec ses détails complets',
    nullable: true,
    required: false,
  })
  equipeA?: EquipeResponseDto | null;

  @ApiProperty({
    type: () => EquipeResponseDto,
    description: 'Équipe B avec ses détails complets',
    nullable: true,
    required: false,
  })
  equipeB?: EquipeResponseDto | null;

  @ApiProperty({
    example: 'quart_final',
    description: 'Phase du match',
    enum: MatchPhase,
    enumName: 'MatchPhase',
  })
  phase: MatchPhase;

  @ApiProperty({
    example: 'en_cours',
    description: 'Statut du match',
    enum: MatchStatut,
    enumName: 'MatchStatut',
  })
  statut: MatchStatut;

  @ApiProperty({
    example: 2,
    description: 'Score de l\'équipe A',
    nullable: true,
    required: false,
  })
  scoreEquipeA?: number | null;

  @ApiProperty({
    example: 1,
    description: 'Score de l\'équipe B',
    nullable: true,
    required: false,
  })
  scoreEquipeB?: number | null;

  @ApiProperty({
    type: () => EquipeResponseDto,
    description: 'Équipe vainqueur avec ses détails complets',
    nullable: true,
    required: false,
  })
  vainqueur?: EquipeResponseDto | null;

  @ApiProperty({
    example: '507f1f77bcf86cd799439032',
    description: 'ID du match suivant dans l\'arbre',
    nullable: true,
    required: false,
  })
  matchSuivantId?: string | null;

  @ApiProperty({
    example: 1,
    description: 'Ordre du match dans la phase',
    required: false,
  })
  ordre?: number;

  @ApiProperty({ example: '2024-05-15T10:00:00.000Z', description: 'Date de création' })
  createdAt?: Date;

  @ApiProperty({ example: '2024-05-15T12:00:00.000Z', description: 'Date de mise à jour' })
  updatedAt?: Date;
}
