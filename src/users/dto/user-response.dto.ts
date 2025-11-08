import { ApiProperty } from '@nestjs/swagger';
import { UserRole } from '../interfaces/user-role.enum';

export class UserResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID unique de l\'utilisateur' })
  _id: string;

  @ApiProperty({ example: 'Dupont', description: 'Nom de famille' })
  nom: string;

  @ApiProperty({ example: 'Jean', description: 'Prénom' })
  prenom: string;

  @ApiProperty({ example: 'jean.dupont@example.com', description: 'Adresse email' })
  email: string;

  @ApiProperty({ 
    example: 'parent', 
    description: 'Rôle de l\'utilisateur',
    enum: UserRole,
    enumName: 'UserRole'
  })
  role: UserRole;

  @ApiProperty({ 
    example: '/uploads/photo.jpg', 
    description: 'URL de la photo de profil',
    nullable: true,
    required: false
  })
  photoProfil?: string | null;

  @ApiProperty({ 
    example: ['507f1f77bcf86cd799439012'], 
    description: 'Liste des IDs des enfants (pour les parents)',
    type: [String],
    required: false
  })
  enfants?: string[];

  @ApiProperty({ 
    example: '507f1f77bcf86cd799439011', 
    description: 'ID du parent (pour les enfants)',
    required: false
  })
  parent?: string;

  @ApiProperty({ 
    example: ['Certification FIFA'], 
    description: 'Liste des certifications (pour les coaches)',
    type: [String],
    required: false
  })
  certification?: string[];

  @ApiProperty({ 
    example: 'Football', 
    description: 'Spécialité (pour les coaches)',
    required: false
  })
  specialite?: string;

  @ApiProperty({ 
    example: 5, 
    description: 'Années d\'expérience (pour les coaches)',
    required: false
  })
  experience?: number;

  @ApiProperty({ 
    example: '2010-05-15T00:00:00.000Z', 
    description: 'Date de naissance (pour les enfants)',
    required: false
  })
  dateNaissance?: Date;

  @ApiProperty({ 
    example: 'Académie de Football Excellence', 
    description: 'Nom de l\'académie (pour les académies)',
    required: false
  })
  nomAcademie?: string;

  @ApiProperty({ 
    example: '123 Rue de la Sport, 75000 Paris', 
    description: 'Adresse (pour les académies)',
    required: false
  })
  adresse?: string;

  @ApiProperty({ 
    example: 'Une académie dédiée au développement des jeunes talents', 
    description: 'Description (pour les académies)',
    required: false
  })
  description?: string;

  @ApiProperty({ 
    example: {
      lundi: { debut: '09:00', fin: '17:00' },
      mardi: { debut: '09:00', fin: '17:00' }
    }, 
    description: 'Horaires (pour les académies)',
    required: false,
    type: Object
  })
  horaires?: {
    [jour: string]: {
      debut: string;
      fin: string;
    };
  };

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z', description: 'Date de création' })
  createdAt?: Date;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z', description: 'Date de mise à jour' })
  updatedAt?: Date;
}


