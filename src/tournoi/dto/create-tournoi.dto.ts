import { ApiProperty } from '@nestjs/swagger';
import {
  IsNotEmpty,
  IsString,
  IsOptional,
  IsDateString,
  IsEnum,
  IsNumber,
  Min,
} from 'class-validator';
import { TournoiEtat } from '../interfaces/tournoi-etat.enum';
import { TournoiNiveau } from '../interfaces/tournoi-niveau.enum';

export class CreateTournoiDto {
  @ApiProperty({
    example: 'Tournoi de Printemps U12',
    description: 'Nom du tournoi',
  })
  @IsNotEmpty()
  @IsString()
  nom: string;

  @ApiProperty({
    example: 'Tournoi amical pour les jeunes joueurs de la région',
    description: 'Détails sur le tournoi, son objectif, son déroulement',
    required: false,
  })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({
    example: 'football',
    description: 'Type de sport (football, basketball, natation, etc.)',
  })
  @IsNotEmpty()
  @IsString()
  sport: string;

  @ApiProperty({
    example: 'U12',
    description: 'Tranche d\'âge concernée (ex: U10, U12, U14)',
  })
  @IsNotEmpty()
  @IsString()
  categorieAge: string;

  @ApiProperty({
    example: '2024-05-15T09:00:00.000Z',
    description: 'Date de début du tournoi',
    type: String,
    format: 'date-time',
  })
  @IsNotEmpty()
  @IsDateString()
  dateDebut: string;

  @ApiProperty({
    example: '2024-05-17T18:00:00.000Z',
    description: 'Date de fin du tournoi',
    type: String,
    format: 'date-time',
  })
  @IsNotEmpty()
  @IsDateString()
  dateFin: string;

  @ApiProperty({
    example: 'Stade Municipal de la Ville',
    description: 'Lieu où se déroule le tournoi (stade, gymnase, etc.)',
  })
  @IsNotEmpty()
  @IsString()
  lieu: string;

  @ApiProperty({
    example: 16,
    description: 'Nombre maximal de participants ou d\'équipes',
    required: false,
    minimum: 1,
  })
  @IsOptional()
  @IsNumber()
  @Min(1)
  nombreParticipantsMax?: number;

  @ApiProperty({
    example: 25.50,
    description: 'Frais d\'inscription (si applicable)',
    required: false,
    minimum: 0,
  })
  @IsOptional()
  @IsNumber()
  @Min(0)
  fraisParticipation?: number;


  @ApiProperty({
    example: 'ouvert',
    description: 'Statut du tournoi',
    enum: TournoiEtat,
    enumName: 'TournoiEtat',
    default: TournoiEtat.OUVERT,
  })
  @IsOptional()
  @IsEnum(TournoiEtat)
  etat?: TournoiEtat;

  @ApiProperty({
    example: 'intermédiaire',
    description: 'Niveau du tournoi',
    enum: TournoiNiveau,
    enumName: 'TournoiNiveau',
    required: false,
  })
  @IsOptional()
  @IsEnum(TournoiNiveau)
  niveau?: TournoiNiveau;

  @ApiProperty({
    example: 'Trophée et médailles pour les 3 premiers',
    description: 'Description de la récompense pour les gagnants',
    required: false,
  })
  @IsOptional()
  @IsString()
  recompense?: string;
}

