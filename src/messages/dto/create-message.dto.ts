import { IsString, IsEnum, IsOptional, IsMongoId, MaxLength, IsUrl } from 'class-validator';
import { MessageType } from '../message.schema';

export class CreateMessageDto {
  @IsMongoId()
  receiver: string;

  @IsString()
  conversationId: string;

  @IsEnum(MessageType)
  type: MessageType;

  @IsOptional()
  @IsString()
  @MaxLength(500, { message: 'Message content cannot exceed 500 characters' })
  content?: string;

  @IsOptional()
  @IsString()
  @IsUrl({ require_tld: false, allow_protocol_relative_urls: true }, { message: 'Media URL must be a valid URL' })
  mediaUrl?: string;
}