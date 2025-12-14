import { IsString, IsOptional } from 'class-validator';
import { ApiProperty, ApiPropertyOptional } from '@nestjs/swagger';

export class ChatMessageDto {
    @ApiProperty({ example: 'Quels sont les tarifs ?' })
    @IsString()
    message: string;

    @ApiPropertyOptional({ example: '654321...' })
    @IsOptional()
    @IsString()
    userId?: string;
}
