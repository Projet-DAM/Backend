import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsMongoId } from 'class-validator';

export class ManageProgramActivitiesDto {
  @ApiProperty({
    description: 'Liste complète des activités qui doivent composer le programme',
    type: [String],
  })
  @IsArray()
  @IsMongoId({ each: true })
  activites: string[];
}



