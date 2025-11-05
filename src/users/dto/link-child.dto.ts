import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, IsMongoId } from 'class-validator';

export class LinkChildDto {
  @ApiProperty({ 
    example: '507f1f77bcf86cd799439011', 
    description: 'ID de l\'enfant à lier'
  })
  @IsNotEmpty()
  @IsString()
  @IsMongoId()
  childId: string;
}

