import { ApiProperty } from '@nestjs/swagger';
import { EquipeResponseDto } from '../../equipes/dto/equipe-response.dto';

export class ClassementPodiumDto {
  @ApiProperty({ type: () => EquipeResponseDto, nullable: true, description: 'Équipe médaillée d\'or' })
  gold: EquipeResponseDto | null;

  @ApiProperty({ type: () => EquipeResponseDto, nullable: true, description: 'Équipe médaillée d\'argent' })
  silver: EquipeResponseDto | null;

  @ApiProperty({ type: () => EquipeResponseDto, nullable: true, description: 'Équipe médaillée de bronze' })
  bronze: EquipeResponseDto | null;
}

export class ClassementResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID du tournoi' })
  tournoiId: string;

  @ApiProperty({ example: 'Tournoi de Printemps U12', description: 'Nom du tournoi' })
  tournoiNom: string;

  @ApiProperty({ example: 'football', description: 'Sport associé au tournoi' })
  sport: string;

  @ApiProperty({ example: 'U12', description: 'Catégorie d\'âge' })
  categorieAge: string;

  @ApiProperty({ type: () => ClassementPodiumDto, description: 'Podium des meilleures équipes' })
  podium: ClassementPodiumDto;

  @ApiProperty({
    description: 'Classement complet des équipes',
    type: [EquipeResponseDto],
  })
  classement: EquipeResponseDto[];
}

