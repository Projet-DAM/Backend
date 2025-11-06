import { IsDateString, IsBoolean, IsNumber, IsString, IsOptional } from 'class-validator';
import { Type } from 'class-transformer';

export class UpdateSuiviEnfantDto {
  @IsOptional()
  @IsDateString()
  date_suivi?: Date;

  @IsOptional()
  @IsBoolean()
  presence?: boolean;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  performance?: number;

  @IsOptional()
  @IsString()
  commentaire?: string;
}
