import { IsOptional, IsBoolean, IsNumber, IsString, IsArray } from 'class-validator';

export class UpdateSuiviEnfantDto {
  @IsOptional()
  @IsString()
  date_suivi?: string;

  @IsOptional()
  @IsBoolean()
  presence?: boolean;

  @IsOptional()
  @IsNumber()
  performance?: number;

  @IsOptional()
  @IsString()
  commentaire?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  focusAreas?: string[];

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  nextSessionGoals?: string[];
}
