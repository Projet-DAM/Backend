import { ApiProperty } from '@nestjs/swagger';
import { Exclude, Expose, Transform } from 'class-transformer';

export class UserResponseDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011' })
  @Expose()
  @Transform(({ obj }) => obj._id?.toString() || obj.id)
  id: string;

  @ApiProperty({ example: 'jean.dupont@example.com', required: false })
  @Expose()
  email?: string;

  @ApiProperty({ example: 'Dupont' })
  @Expose()
  nom: string;

  @ApiProperty({ example: 'Jean' })
  @Expose()
  prenom: string;

  @ApiProperty({ example: 'parent', enum: ['parent', 'enfant', 'coach', 'academie'] })
  @Expose()
  role: string;

  @ApiProperty({ example: 'https://example.com/photo.jpg', required: false })
  @Expose()
  photoProfil?: string;

  @ApiProperty({ example: '2010-05-15', required: false })
  @Expose()
  @Transform(({ obj }) => obj.dateNaissance ? new Date(obj.dateNaissance).toISOString().split('T')[0] : null)
  dateNaissance?: string;

  @ApiProperty({ example: 'M', enum: ['M', 'F'], required: false })
  @Expose()
  sexe?: string;

  @ApiProperty({ example: '507f1f77bcf86cd799439011', required: false })
  @Expose()
  @Transform(({ obj }) => {
    if (obj.parent) {
      return typeof obj.parent === 'object' && obj.parent._id 
        ? obj.parent._id.toString() 
        : obj.parent.toString();
    }
    return null;
  })
  parentId?: string;

  @Exclude()
  _id?: any;

  @Exclude()
  motDePasse?: string;

  @Exclude()
  parent?: any;

  @Exclude()
  enfants?: any[];

  @Exclude()
  verificationCode?: string;

  @Exclude()
  verificationCodeExpires?: Date;

  @Exclude()
  emailVerified?: boolean;

  @Exclude()
  createdAt?: Date;

  @Exclude()
  updatedAt?: Date;
}





