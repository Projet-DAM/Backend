import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsNotEmpty, IsString } from 'class-validator';

export class CreateEnrollmentDto {
    @ApiProperty({
        description: 'ID du programme',
        example: '507f1f77bcf86cd799439011',
    })
    @IsString()
    @IsNotEmpty()
    programId: string;

    @ApiProperty({
        description: 'Liste des IDs des enfants à inscrire',
        example: ['507f1f77bcf86cd799439012', '507f1f77bcf86cd799439013'],
        type: [String],
    })
    @IsArray()
    @IsNotEmpty()
    childIds: string[];
}
