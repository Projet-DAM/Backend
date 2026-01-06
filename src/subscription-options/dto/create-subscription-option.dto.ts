import { ApiProperty } from '@nestjs/swagger';
import { IsString, IsNumber, IsBoolean, IsOptional, Min } from 'class-validator';

export class CreateSubscriptionOptionDto {
    @ApiProperty({ example: 'SPORTS_OUTFIT', description: 'Type d\'option (SPORTS_OUTFIT, INSURANCE, TRANSPORT)' })
    @IsString()
    type: string;

    @ApiProperty({ example: 'Tenue de sport', description: 'Nom affiché' })
    @IsString()
    name: string; // Changé de displayName à name

    @ApiProperty({ example: 'Tenue complète pour la pratique sportive', description: 'Description de l\'option' })
    @IsString()
    description: string;

    @ApiProperty({ example: 50, description: 'Prix de l\'option en TND' })
    @IsNumber()
    @Min(0)
    price: number;

    @ApiProperty({ example: true, required: false, description: 'Option active ou non' })
    @IsBoolean()
    @IsOptional()
    isActive?: boolean;

    @ApiProperty({ example: 0, required: false, description: 'Ordre d\'affichage' })
    @IsNumber()
    @IsOptional()
    displayOrder?: number;
}
