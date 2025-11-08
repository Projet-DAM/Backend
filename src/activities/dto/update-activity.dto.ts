import { PartialType } from '@nestjs/mapped-types';
import { CreateActivityDto } from './create-activity.dto';
import { ApiPropertyOptional } from '@nestjs/swagger';
import { ActivityStatus } from '../schemas/activity.schema';
import { IsEnum, IsOptional, IsString } from 'class-validator';

export class UpdateActivityDto extends PartialType(CreateActivityDto) {
  @ApiPropertyOptional({ description: 'Statut de l\'activité', enum: ActivityStatus })
  @IsOptional()
  @IsEnum(ActivityStatus)
  statut?: ActivityStatus;

  @ApiPropertyOptional({ description: 'Nom de l\'activité' })
  @IsOptional()
  @IsString()
  nom_activite?: string;
}



