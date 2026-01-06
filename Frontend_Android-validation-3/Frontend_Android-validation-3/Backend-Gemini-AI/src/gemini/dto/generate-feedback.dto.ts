import { IsString, IsInt, IsOptional } from 'class-validator';

export class GenerateFeedbackDto {
    @IsString()
    childId: string;

    @IsString()
    childName: string;

    @IsOptional()
    @IsInt()
    childAge?: number;

    @IsString()
    matchId: string;

    @IsString()
    matchResult: string; // "victoire", "defaite", "nul"

    @IsString()
    teamName: string;

    @IsString()
    score: string; // ex: "3-2"

    @IsString()
    phase: string; // ex: "finale", "demi-finale"

    @IsOptional()
    @IsString()
    performance?: string;

    @IsOptional()
    @IsString()
    tournamentName?: string;
}
