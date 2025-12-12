import { IsMongoId, IsOptional, IsString, MaxLength } from 'class-validator';

export class ReplyMessageDto {
  @IsMongoId()
  replyToMessageId: string;

  @IsOptional()
  @IsString()
  @MaxLength(500)
  content?: string;

  @IsOptional()
  @IsString()
  mediaUrl?: string;
}
