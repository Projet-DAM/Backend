import { ApiProperty } from '@nestjs/swagger';
import { TournoiEtat } from '../interfaces/tournoi-etat.enum';
import { TournoiNiveau } from '../interfaces/tournoi-niveau.enum';

export class TournoiResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID unique du tournoi' })
  _id: string;

  @ApiProperty({ example: 'Tournoi de Printemps U12', description: 'Nom du tournoi' })
  nom: string;

  @ApiProperty({
    example: 'Tournoi amical pour les jeunes joueurs de la région',
    description: 'Détails sur le tournoi',
    required: false,
  })
  description?: string;

  @ApiProperty({ example: 'football', description: 'Type de sport' })
  sport: string;

  @ApiProperty({ example: 'U12', description: 'Tranche d\'âge concernée' })
  categorieAge: string;

  @ApiProperty({ example: '2024-05-15T09:00:00.000Z', description: 'Date de début du tournoi' })
  dateDebut: Date;

  @ApiProperty({ example: '2024-05-17T18:00:00.000Z', description: 'Date de fin du tournoi' })
  dateFin: Date;

  @ApiProperty({ example: 'Stade Municipal de la Ville', description: 'Lieu du tournoi' })
  lieu: string;

  @ApiProperty({
    example: 16,
    description: 'Nombre maximal de participants ou d\'équipes',
    required: false,
  })
  nombreParticipantsMax?: number;

  @ApiProperty({
    example: 25.50,
    description: 'Frais d\'inscription',
    required: false,
  })
  fraisParticipation?: number;

  @ApiProperty({
    example: 'https://example.com/images/tournoi-printemps.jpg',
    description: 'URL de l\'affiche du tournoi',
    required: false,
  })
  imageUrl?: string;

  @ApiProperty({
    example: 'ouvert',
    description: 'Statut du tournoi',
    enum: TournoiEtat,
    enumName: 'TournoiEtat',
  })
  etat: TournoiEtat;

  @ApiProperty({
    example: 'intermédiaire',
    description: 'Niveau du tournoi',
    enum: TournoiNiveau,
    enumName: 'TournoiNiveau',
    required: false,
  })
  niveau?: TournoiNiveau;

  @ApiProperty({
    example: 'Trophée et médailles pour les 3 premiers',
    description: 'Description de la récompense',
    required: false,
  })
  recompense?: string;

  @ApiProperty({ example: '2024-01-15T10:00:00.000Z', description: 'Date de création' })
  createdAt?: Date;

  @ApiProperty({ example: '2024-01-20T15:30:00.000Z', description: 'Date de mise à jour' })
  updatedAt?: Date;
}

