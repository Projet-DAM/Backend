import { ApiProperty } from '@nestjs/swagger';
import { IsEmail, IsEnum, IsNotEmpty, IsOptional, IsString, MinLength, IsArray, IsNumber, IsDateString, IsObject, IsMongoId } from 'class-validator';
import { UserRole } from '../interfaces/user-role.enum';
import { IsRegisterRole } from '../../common/validators/register-role.validator';

export class CreateUserDto {
  @ApiProperty({
    example: 'Dupont',
    description: 'Nom de famille',
    required: true
  })
  @IsNotEmpty({ message: 'Le nom est requis' })
  @IsString()
  nom: string;

  @ApiProperty({
    example: 'Jean',
    description: 'Prénom',
    required: true
  })
  @IsNotEmpty({ message: 'Le prénom est requis' })
  @IsString()
  prenom: string;

  @ApiProperty({ example: 'jean.dupont@example.com', description: 'Adresse email (optionnel pour les enfants)' })
  @IsOptional()
  @IsEmail()
  email?: string;

  @ApiProperty({ example: 'password123', description: 'Mot de passe (minimum 6 caractères, optionnel pour les enfants)', minLength: 6 })
  @IsOptional()
  @IsString()
  @MinLength(6)
  motDePasse?: string;


  @ApiProperty({
    example: 'parent',
    description: 'Rôle de l\'utilisateur',
    enum: UserRole,
    enumName: 'UserRole',
    required: true
  })
  @IsNotEmpty({ message: 'Le rôle est requis' })
  @IsEnum(UserRole, { message: 'Le rôle doit être l\'un des suivants: parent, enfant, coach, academie' })
  role: UserRole;

  @ApiProperty({
    example: 'https://example.com/photo.jpg',
    description: 'URL de la photo de profil',
    required: false
  })
  @IsOptional()
  @IsString()
  photoProfil?: string;

  @ApiProperty({ example: '+33612345678', description: 'Numéro de téléphone', required: false })
  @IsOptional()
  @IsString()
  phoneNumber?: string;

  // Attributs spécifiques au Coach
  @ApiProperty({
    example: ['Certification FIFA', 'Diplôme Entraîneur'],
    description: 'Liste des certifications du coach (requis si role=coach)',
    required: false,
    type: [String],
    isArray: true
  })
  @IsOptional()
  @IsArray({ message: 'Les certifications doivent être un tableau' })
  @IsString({ each: true, message: 'Chaque certification doit être une chaîne de caractères' })
  certification?: string[];

  @ApiProperty({
    example: 'Football',
    description: 'Spécialité du coach (requis si role=coach)',
    required: false
  })
  @IsOptional()
  @IsString({ message: 'La spécialité doit être une chaîne de caractères' })
  specialite?: string;

  @ApiProperty({
    example: 5,
    description: 'Années d\'expérience du coach (requis si role=coach)',
    required: false,
    minimum: 0
  })
  @IsOptional()
  @IsNumber({}, { message: 'L\'expérience doit être un nombre' })
  experience?: number;

  // Attributs spécifiques à l'Enfant
  @ApiProperty({
    example: '2010-05-15',
    description: 'Date de naissance de l\'enfant (format ISO: YYYY-MM-DD, requis si role=enfant)',
    required: false,
    format: 'date'
  })
  @IsOptional()
  @IsDateString({}, { message: 'La date de naissance doit être au format YYYY-MM-DD' })
  dateNaissance?: string;

  @ApiProperty({
    example: 'M',
    description: 'Sexe de l\'enfant (M ou F)',
    required: false,
    enum: ['M', 'F']
  })
  @IsOptional()
  @IsEnum(['M', 'F'])
  sexe?: string;

  @ApiProperty({
    example: '507f1f77bcf86cd799439011',
    description: 'ID du parent (automatiquement défini si créé par un parent)',
    required: false
  })
  @IsOptional()
  @IsString()
  parentId?: string;

  // Attributs spécifiques à l'Académie
  @ApiProperty({
    example: 'Académie de Football Excellence',
    description: 'Nom de l\'académie (requis si role=academie)',
    required: false
  })
  @IsOptional()
  @IsString({ message: 'Le nom de l\'académie doit être une chaîne de caractères' })
  nomAcademie?: string;

  @ApiProperty({
    example: '123 Rue de la Sport, 75000 Paris',
    description: 'Adresse/localisation de l\'académie (requis si role=academie)',
    required: false
  })
  @IsOptional()
  @IsString({ message: 'L\'adresse doit être une chaîne de caractères' })
  adresse?: string;

  @ApiProperty({
    example: 'Une académie dédiée au développement des jeunes talents',
    description: 'Description de l\'académie (requis si role=academie)',
    required: false
  })
  @IsOptional()
  @IsString({ message: 'La description doit être une chaîne de caractères' })
  description?: string;

  @ApiProperty({
    example: {
      lundi: { debut: '09:00', fin: '17:00' },
      mardi: { debut: '09:00', fin: '17:00' },
      mercredi: { debut: '09:00', fin: '17:00' }
    },
    description: 'Horaires de l\'académie (jours et heures, requis si role=academie)',
    required: false,
    type: Object,
    additionalProperties: {
      type: 'object',
      properties: {
        debut: { type: 'string', example: '09:00' },
        fin: { type: 'string', example: '17:00' }
      }
    }
  })
  @IsOptional()
  @IsObject({ message: 'Les horaires doivent être un objet' })
  horaires?: {
    [jour: string]: {
      debut: string;
      fin: string;
    };
  };
}

