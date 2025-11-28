import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsEnum, IsOptional, IsString, MinLength, IsArray, IsNumber, IsDateString, IsObject, IsMongoId } from 'class-validator';
import { UserRole } from '../interfaces/user-role.enum';

export class UpdateUserDto {
  @ApiProperty({ example: 'Dupont', description: 'Nom de famille', required: false })
  @IsOptional()
  @IsString()
  nom?: string;

  @ApiProperty({ example: 'Jean', description: 'Prénom', required: false })
  @IsOptional()
  @IsString()
  prenom?: string;

  @ApiProperty({ example: 'jean.dupont@example.com', description: 'Adresse email', required: false })
  @IsOptional()
  @IsEmail()
  email?: string;

  @ApiProperty({ example: 'newpassword123', description: 'Nouveau mot de passe (minimum 6 caractères)', required: false, minLength: 6 })
  @IsOptional()
  @IsString()
  @MinLength(6)
  motDePasse?: string;

  @ApiProperty({ 
    example: 'parent', 
    description: 'Rôle de l\'utilisateur',
    enum: UserRole,
    enumName: 'UserRole',
    required: false
  })
  @IsOptional()
  @IsEnum(UserRole)
  role?: UserRole;

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

  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID du coach à assigner (utilisé pour les enfants)', required: false })
  @IsOptional()
  @IsMongoId()
  @IsString()
  coach?: string;
}

