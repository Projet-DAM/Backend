import { ApiProperty } from '@nestjs/swagger';
import {
  IsOptional,
  IsString,
  IsDateString,
  IsNumber,
  Min,
  MinLength,
  Matches,
  MaxLength,
} from 'class-validator';

export class UpdateInscriptionDto {
  @ApiProperty({
    example: 'Lucas',
    description: 'Prénom de l\'enfant',
    required: false,
    minLength: 2,
  })
  @IsOptional()
  @IsString({ message: 'Le prénom de l\'enfant doit être une chaîne de caractères' })
  @MinLength(2, { message: 'Le prénom de l\'enfant doit contenir au moins 2 caractères' })
  enfantPrenom?: string;

  @ApiProperty({
    example: 'Martin',
    description: 'Nom de l\'enfant',
    required: false,
    minLength: 2,
  })
  @IsOptional()
  @IsString({ message: 'Le nom de l\'enfant doit être une chaîne de caractères' })
  @MinLength(2, { message: 'Le nom de l\'enfant doit contenir au moins 2 caractères' })
  enfantNom?: string;

  @ApiProperty({
    example: '2012-05-15',
    description: 'Date de naissance de l\'enfant (format ISO: YYYY-MM-DD)',
    type: String,
    format: 'date',
    required: false,
  })
  @IsOptional()
  @IsDateString({}, { message: 'La date de naissance doit être au format ISO (YYYY-MM-DD)' })
  enfantDateNaissance?: string;

  @ApiProperty({
    example: 'Sophie',
    description: 'Prénom du parent',
    required: false,
  })
  @IsOptional()
  @IsString({ message: 'Le prénom du parent doit être une chaîne de caractères' })
  parentPrenom?: string;

  @ApiProperty({
    example: 'Martin',
    description: 'Nom du parent',
    required: false,
  })
  @IsOptional()
  @IsString({ message: 'Le nom du parent doit être une chaîne de caractères' })
  parentNom?: string;

  @ApiProperty({
    example: '+33612345678',
    description: 'Numéro de téléphone du parent (format international recommandé)',
    required: false,
    pattern: '^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$',
  })
  @IsOptional()
  @IsString({ message: 'Le numéro de téléphone doit être une chaîne de caractères' })
  @Matches(/^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$/, {
    message: 'Le numéro de téléphone doit être au format valide (ex: +33612345678, 0612345678)',
  })
  parentTelephone?: string;

  @ApiProperty({
    example: 25.5,
    description: 'Montant de l\'inscription en euros',
    required: false,
    minimum: 0,
  })
  @IsOptional()
  @IsNumber({}, { message: 'Le montant d\'inscription doit être un nombre' })
  @Min(0, { message: 'Le montant d\'inscription ne peut pas être négatif' })
  montantInscription?: number;

  @ApiProperty({
    example: 'Allergie aux arachides, besoin d\'un accompagnateur',
    description: 'Besoins particuliers ou informations importantes concernant l\'enfant',
    required: false,
    maxLength: 500,
  })
  @IsOptional()
  @IsString({ message: 'Les besoins particuliers doivent être une chaîne de caractères' })
  @MaxLength(500, { message: 'Les besoins particuliers ne peuvent pas dépasser 500 caractères' })
  besoinsParticuliers?: string;
}



