import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsOptional, IsString, IsDateString } from 'class-validator';

export class CreateChildDto {
  @ApiProperty({ example: 'Petit', description: 'Nom de famille de l\'enfant' })
  @IsNotEmpty()
  @IsString()
  nom: string;

  @ApiProperty({ example: 'Paul', description: 'Prénom de l\'enfant' })
  @IsNotEmpty()
  @IsString()
  prenom: string;

  @ApiProperty({ example: '2014-05-10', description: 'Date de naissance (optionnelle)', required: false })
  @IsOptional()
  @IsDateString()
  dateNaissance?: string;

  @ApiProperty({ example: 'https://example.com/photo.jpg', description: 'URL photo (optionnelle)', required: false })
import { IsNotEmpty, IsOptional, IsString, IsDateString, IsEnum } from 'class-validator';
import { SportType } from '../interfaces/sport-type.enum';

export class CreateChildDto {
  @ApiProperty({ 
    example: 'Marie', 
    description: 'Prénom de l\'enfant',
    required: true
  })
  @IsNotEmpty({ message: 'Le prénom est requis' })
  @IsString()
  prenom: string;

  @ApiProperty({ 
    example: 'Dupont', 
    description: 'Nom de famille de l\'enfant',
    required: true
  })
  @IsNotEmpty({ message: 'Le nom est requis' })
  @IsString()
  nom: string;

  @ApiProperty({ 
    example: '2010-05-15', 
    description: 'Date de naissance de l\'enfant (format ISO: YYYY-MM-DD)',
    required: true,
    format: 'date'
  })
  @IsNotEmpty({ message: 'La date de naissance est requise' })
  @IsDateString({}, { message: 'La date de naissance doit être au format YYYY-MM-DD' })
  dateNaissance: string;

  @ApiProperty({ 
    example: 'football', 
    description: 'Sport pratiqué par l\'enfant',
    enum: SportType,
    enumName: 'SportType',
    required: true
  })
  @IsNotEmpty({ message: 'Le sport pratiqué est requis' })
  @IsEnum(SportType, { message: 'Le sport doit être l\'un des suivants: football, basketball, tennis, natation, volleyball, handball, rugby, judo, karate, athletisme, gymnastique, escalade, cyclisme' })
  sportPratique: SportType;

  @ApiProperty({ 
    example: 'https://example.com/photo.jpg', 
    description: 'URL de la photo de profil', 
    required: false 
  })
  @IsOptional()
  @IsString()
  photoProfil?: string;
}


