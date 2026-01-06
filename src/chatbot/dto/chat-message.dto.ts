import { ApiProperty } from '@nestjs/swagger';
import { IsString, IsOptional } from 'class-validator';

export class ChatMessageDto {
    @ApiProperty({ example: 'Quel est le prix de l\'abonnement?', description: 'Message de l\'utilisateur' })
    @IsString()
    message: string;

    @ApiProperty({ example: '690cd9998d614e72c9b1ab55', description: 'ID de l\'utilisateur (optionnel)', required: false })
    @IsString()
    @IsOptional()
    userId?: string;
}
