import { ApiProperty } from '@nestjs/swagger';
import { EquipeParticipantDto } from './equipe-participant.dto';

export class EquipeResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439021', description: 'ID unique de l\'équipe' })
  _id: string;

  @ApiProperty({ example: 'Arsenal U12', description: 'Nom de l\'équipe' })
  nom: string;

  @ApiProperty({ example: '#FF0000', description: 'Couleur principale de l\'équipe', required: false })
  couleur?: string;

  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID du tournoi associé' })
  tournoi: string;

  @ApiProperty({ example: 'football', description: 'Sport pratiqué' })
  sport: string;

  @ApiProperty({ example: '5v5', description: 'Format de l\'équipe (ex: 5v5, 7v7)' })
  formatEquipe: string;

  @ApiProperty({
    description: 'Liste des enfants (basée sur les inscriptions au tournoi)',
    type: [EquipeParticipantDto],
    required: false,
  })
  enfants?: EquipeParticipantDto[];

  @ApiProperty({ example: 12, description: 'Nombre de points cumulés', required: false })
  points?: number;

  @ApiProperty({ example: 4, description: 'Nombre de victoires', required: false })
  victoires?: number;

  @ApiProperty({ example: 1, description: 'Nombre de défaites', required: false })
  defaites?: number;

  @ApiProperty({ example: 2, description: 'Nombre de matchs nuls', required: false })
  nuls?: number;

  @ApiProperty({ example: 1, description: 'Position actuelle dans le classement', required: false })
  classement?: number;

  @ApiProperty({ example: '2024-05-01T10:00:00.000Z', description: 'Date de création' })
  createdAt?: Date;

  @ApiProperty({ example: '2024-05-10T10:00:00.000Z', description: 'Date de mise à jour' })
  updatedAt?: Date;
}

