
import { IsString, IsOptional, IsNumber, IsEnum, IsNotEmpty } from 'class-validator';

export class GenerateFeedbackDto {
    @IsString()
    @IsNotEmpty()
    childId: string;

    @IsString()
    @IsNotEmpty()
    childName: string;

    @IsNumber()
    @IsOptional()
    childAge?: number;

    @IsString()
    @IsNotEmpty()
    matchId: string;

    @IsString()
    @IsEnum(['victoire', 'defaite', 'nul'])
    matchResult: 'victoire' | 'defaite' | 'nul';

    @IsString()
    @IsNotEmpty()
    teamName: string;

    @IsString()
    @IsNotEmpty()
    score: string;

    @IsString()
    @IsNotEmpty()
    phase: string;

    @IsString()
    @IsOptional()
    performance?: string;

    @IsString()
    @IsOptional()
    tournamentName?: string;
}
