import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsMongoId, IsNotEmpty, IsOptional, IsString, ArrayMinSize, ArrayUnique } from 'class-validator';

export class CreateEquipeDto {
  @ApiProperty({
    example: 'ARSENAL',
    description: 'Nom de l\'équipe (unique par tournoi)',
    required: true,
  })
  @IsNotEmpty({ message: 'Le nom de l\'équipe est requis' })
  @IsString({ message: 'Le nom de l\'équipe doit être une chaîne de caractères' })
  nom: string;

  @ApiProperty({
    example: '#FF0000',
    description: 'Couleur principale de l\'équipe (code HEX ou nom de couleur)',
    required: false,
  })
  @IsOptional()
  @IsString({ message: 'La couleur doit être une chaîne de caractères' })
  couleur?: string;

  @ApiProperty({
    example: '507f1f77bcf86cd799439011',
    description: 'ID du tournoi (MongoDB ObjectId)',
  })
  @IsNotEmpty({ message: 'Le tournoi est requis' })
  @IsMongoId({ message: 'tournoiId doit être un ID MongoDB valide' })
  tournoiId: string;

  @ApiProperty({
    example: ['65a1c5e9f85d2d7a31c9bf01', '65a1c5e9f85d2d7a31c9bf02'],
    description: 'IDs des inscriptions (enfants) sélectionnés pour l\'équipe',
    type: [String],
  })
  @IsArray({ message: 'La liste des enfants doit être un tableau' })
  @ArrayMinSize(1, { message: 'Une équipe doit contenir au moins un enfant' })
  @ArrayUnique({ message: 'Chaque enfant doit être unique dans l\'équipe' })
  @IsMongoId({ each: true, message: 'Chaque enfant doit être un ID MongoDB valide' })
  enfants: string[];

  @ApiProperty({
    example: '5v5',
    description: 'Format choisi pour l\'équipe (ex: 5v5, 7v7, simple)',
  })
  @IsNotEmpty({ message: 'Le format de l\'équipe est requis' })
  @IsString({ message: 'Le format de l\'équipe doit être une chaîne de caractères' })
  formatEquipe: string;
}

