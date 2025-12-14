import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsNumber, IsOptional, IsString, Min } from 'class-validator';
import { SubscriptionOptionType } from '../schemas/subscription-option.schema';

export class SubscriptionOptionDto {
    @ApiProperty({
        enum: SubscriptionOptionType,
        example: SubscriptionOptionType.SPORTS_OUTFIT,
        description: 'Type d\'option supplémentaire'
    })
    @IsEnum(SubscriptionOptionType)
    type: SubscriptionOptionType;

    @ApiProperty({
        example: 50,
        description: 'Prix de l\'option'
    })
    @IsNumber()
    @Min(0)
    price: number;

    @ApiProperty({
        example: 'TND',
        default: 'TND',
        required: false
    })
    @IsOptional()
    @IsString()
    currency?: string = 'TND';

    @ApiProperty({
        example: 'Tenue sportive complète',
        required: false
    })
    @IsOptional()
    @IsString()
    description?: string;
}
