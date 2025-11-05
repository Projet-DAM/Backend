import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsEnum, IsNotEmpty, IsOptional, IsString, MinLength, IsArray, IsNumber, IsDateString, IsObject } from 'class-validator';
import { UserRole } from '../interfaces/user-role.enum';

export class CreateUserDto {
  @ApiProperty({ example: 'Dupont', description: 'Nom de famille' })
  @IsNotEmpty()
  @IsString()
  nom: string;

  @ApiProperty({ example: 'Jean', description: 'Prénom' })
  @IsNotEmpty()
  @IsString()
  prenom: string;

  @ApiProperty({ example: 'jean.dupont@example.com', description: 'Adresse email' })
  @IsNotEmpty()
  @IsEmail()
  email: string;

  @ApiProperty({ example: 'password123', description: 'Mot de passe (minimum 6 caractères)', minLength: 6 })
  @IsNotEmpty()
  @IsString()
  @MinLength(6)
  motDePasse: string;

  @ApiProperty({ 
    example: 'parent', 
    description: 'Rôle de l\'utilisateur',
    enum: UserRole,
    enumName: 'UserRole'
  })
  @IsNotEmpty()
  @IsEnum(UserRole)
  role: UserRole;

  @ApiProperty({ example: 'https://example.com/photo.jpg', description: 'URL de la photo de profil', required: false })
  @IsOptional()
  @IsString()
  photoProfil?: string;

  // Attributs spécifiques au Coach
  @ApiProperty({ 
    example: ['Certification FIFA', 'Diplôme Entraîneur'], 
    description: 'Liste des certifications du coach',
    required: false,
    type: [String]
  })
  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  certification?: string[];

  @ApiProperty({ 
    example: 'Football', 
    description: 'Spécialité du coach',
    required: false
  })
  @IsOptional()
  @IsString()
  specialite?: string;

  @ApiProperty({ 
    example: 5, 
    description: 'Années d\'expérience du coach',
    required: false
  })
  @IsOptional()
  @IsNumber()
  experience?: number;

  // Attributs spécifiques à l'Enfant
  @ApiProperty({ 
    example: '2010-05-15', 
    description: 'Date de naissance de l\'enfant (format ISO: YYYY-MM-DD)',
    required: false
  })
  @IsOptional()
  @IsDateString()
  dateNaissance?: string;

  // Attributs spécifiques à l'Académie
  @ApiProperty({ 
    example: 'Académie de Football Excellence', 
    description: 'Nom de l\'académie',
    required: false
  })
  @IsOptional()
  @IsString()
  nomAcademie?: string;

  @ApiProperty({ 
    example: '123 Rue de la Sport, 75000 Paris', 
    description: 'Adresse/localisation de l\'académie',
    required: false
  })
  @IsOptional()
  @IsString()
  adresse?: string;

  @ApiProperty({ 
    example: 'Une académie dédiée au développement des jeunes talents', 
    description: 'Description de l\'académie',
    required: false
  })
  @IsOptional()
  @IsString()
  description?: string;

  @ApiProperty({ 
    example: {
      lundi: { debut: '09:00', fin: '17:00' },
      mardi: { debut: '09:00', fin: '17:00' },
      mercredi: { debut: '09:00', fin: '17:00' }
    }, 
    description: 'Horaires de l\'académie (jours et heures)',
    required: false,
    type: Object
  })
  @IsOptional()
  @IsObject()
  horaires?: {
    [jour: string]: {
      debut: string;
      fin: string;
    };
  };
}

