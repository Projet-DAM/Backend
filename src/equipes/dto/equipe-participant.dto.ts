import { ApiProperty } from '@nestjs/swagger';

export class EquipeParticipantDto {
  @ApiProperty({ example: '507f1f77bcf86cd799439011', description: 'ID de l\'inscription' })
  _id: string;

  @ApiProperty({ example: 'Lucas', description: 'Prénom de l\'enfant' })
  enfantPrenom: string;

  @ApiProperty({ example: 'Martin', description: 'Nom de l\'enfant' })
  enfantNom: string;

  @ApiProperty({ example: '2012-05-15T00:00:00.000Z', description: 'Date de naissance de l\'enfant' })
  enfantDateNaissance: Date;

  @ApiProperty({ example: 'Sophie', description: 'Prénom du parent' })
  parentPrenom: string;

  @ApiProperty({ example: 'Martin', description: 'Nom du parent' })
  parentNom: string;

  @ApiProperty({ example: '+33612345678', description: 'Téléphone du parent' })
  parentTelephone: string;
}

