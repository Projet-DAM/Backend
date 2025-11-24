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
  @IsOptional()
  @IsString()
  photoProfil?: string;
}
