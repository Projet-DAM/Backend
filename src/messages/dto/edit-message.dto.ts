import { IsString, MaxLength } from 'class-validator';

export class EditMessageDto {
  @IsString()
  @MaxLength(500)
  content: string;
}
