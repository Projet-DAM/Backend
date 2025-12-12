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
    required: true
  })
  @IsNotEmpty({ message: 'Le nom du tournoi est requis' })
  @IsString({ message: 'Le nom doit être une chaîne de caractères' })
  nom: string;

  @ApiProperty({
    example: 'Tournoi amical pour les jeunes joueurs de la région',
    description: 'Détails sur le tournoi, son objectif, son déroulement',
    required: false,
  })
  @IsOptional()
  @IsString({ message: 'La description doit être une chaîne de caractères' })
  description?: string;

  @ApiProperty({
    example: 'football',
    description: 'Type de sport (football, basketball, natation, etc.)',
    required: true
  })
  @IsNotEmpty({ message: 'Le sport est requis' })
  @IsString({ message: 'Le sport doit être une chaîne de caractères' })
  sport: string;

  @ApiProperty({
    example: 'U12',
    description: 'Tranche d\'âge concernée (ex: U10, U12, U14)',
    required: true
  })
  @IsNotEmpty({ message: 'La catégorie d\'âge est requise' })
  @IsString({ message: 'La catégorie d\'âge doit être une chaîne de caractères' })
  categorieAge: string;

  @ApiProperty({
    example: '2024-05-15T09:00:00.000Z',
    description: 'Date de début du tournoi (format ISO: YYYY-MM-DDTHH:mm:ss.sssZ)',
    type: String,
    format: 'date-time',
    required: true
  })
  @IsNotEmpty({ message: 'La date de début est requise' })
  @IsDateString({}, { message: 'La date de début doit être au format ISO (YYYY-MM-DDTHH:mm:ss.sssZ)' })
  dateDebut: string;

  @ApiProperty({
    example: '2024-05-17T18:00:00.000Z',
    description: 'Date de fin du tournoi (format ISO: YYYY-MM-DDTHH:mm:ss.sssZ). Doit être postérieure à la date de début.',
    type: String,
    format: 'date-time',
    required: true
  })
  @IsNotEmpty({ message: 'La date de fin est requise' })
  @IsDateString({}, { message: 'La date de fin doit être au format ISO (YYYY-MM-DDTHH:mm:ss.sssZ)' })
  dateFin: string;

  @ApiProperty({
    example: 'Stade Municipal de la Ville',
    description: 'Lieu où se déroule le tournoi (stade, gymnase, etc.)',
    required: true
  })
  @IsNotEmpty({ message: 'Le lieu est requis' })
  @IsString({ message: 'Le lieu doit être une chaîne de caractères' })
  lieu: string;

  @ApiProperty({
    example: 16,
    description: 'Nombre maximal de participants ou d\'équipes',
    required: false,
    minimum: 1,
  })
  @IsOptional()
  @IsNumber({}, { message: 'Le nombre de participants maximum doit être un nombre' })
  @Min(1, { message: 'Le nombre de participants maximum doit être au moins 1' })
  nombreParticipantsMax?: number;

  @ApiProperty({
    example: 25.50,
    description: 'Frais d\'inscription en euros (si applicable)',
    required: false,
    minimum: 0,
  })
  @IsOptional()
  @IsNumber({}, { message: 'Les frais de participation doivent être un nombre' })
  @Min(0, { message: 'Les frais de participation ne peuvent pas être négatifs' })
  fraisParticipation?: number;

  @ApiProperty({
    example: 'ouvert',
    description: 'Statut du tournoi',
    enum: TournoiEtat,
    enumName: 'TournoiEtat',
    default: TournoiEtat.OUVERT,
    required: false
  })
  @IsOptional()
  @IsEnum(TournoiEtat, { message: 'L\'état doit être l\'un des suivants: ouvert, fermé, terminé' })
  etat?: TournoiEtat;

  @ApiProperty({
    example: 'intermédiaire',
    description: 'Niveau du tournoi',
    enum: TournoiNiveau,
    enumName: 'TournoiNiveau',
    required: false,
  })
  @IsOptional()
  @IsEnum(TournoiNiveau, { message: 'Le niveau doit être l\'un des suivants: débutant, intermédiaire, avancé' })
  niveau?: TournoiNiveau;

  @ApiProperty({
    example: 'Trophée et médailles pour les 3 premiers',
    description: 'Description de la récompense pour les gagnants',
    required: false,
  })
  @IsOptional()
  @IsString({ message: 'La récompense doit être une chaîne de caractères' })
  recompense?: string;
}

