import { ApiProperty } from '@nestjs/swagger';
import { MatchResponseDto } from './match-response.dto';

export class BracketPhaseDto {
  @ApiProperty({
    description: 'Matchs de la phase quart de finale',
    type: [MatchResponseDto],
    required: false,
  })
  quartFinal?: MatchResponseDto[];

  @ApiProperty({
    description: 'Matchs de la phase demi-finale',
    type: [MatchResponseDto],
    required: false,
  })
  demiFinal?: MatchResponseDto[];

  @ApiProperty({
    description: 'Match de la finale',
    type: MatchResponseDto,
    nullable: true,
    required: false,
  })
  finale?: MatchResponseDto | null;
}

export class BracketResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID du tournoi' })
  tournoiId: string;

  @ApiProperty({ example: 'Tournoi de Printemps U12', description: 'Nom du tournoi' })
  tournoiNom: string;

  @ApiProperty({
    description: 'Arbre de tournoi organisé par phases',
    type: BracketPhaseDto,
  })
  phases: BracketPhaseDto;
}


