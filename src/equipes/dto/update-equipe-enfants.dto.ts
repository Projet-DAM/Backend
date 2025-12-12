import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsArray, IsMongoId, IsOptional, ArrayUnique } from 'class-validator';

export class UpdateEquipeEnfantsDto {
  @ApiPropertyOptional({
    description: 'IDs des inscriptions à ajouter à l\'équipe',
    type: [String],
    example: ['65a1c5e9f85d2d7a31c9bf05'],
  })
  @IsOptional()
  @IsArray({ message: 'Les enfants à ajouter doivent être un tableau' })
  @ArrayUnique({ message: 'Chaque enfant à ajouter doit être unique' })
  @IsMongoId({ each: true, message: 'Chaque enfant à ajouter doit être un ID MongoDB valide' })
  ajouter?: string[];

  @ApiPropertyOptional({
    description: 'IDs des inscriptions à retirer de l\'équipe',
    type: [String],
    example: ['65a1c5e9f85d2d7a31c9bf06'],
  })
  @IsOptional()
  @IsArray({ message: 'Les enfants à retirer doivent être un tableau' })
  @ArrayUnique({ message: 'Chaque enfant à retirer doit être unique' })
  @IsMongoId({ each: true, message: 'Chaque enfant à retirer doit être un ID MongoDB valide' })
  retirer?: string[];
}

