import { IsMongoId, IsString } from 'class-validator';

export class TypingDto {
  @IsMongoId()
  conversationUserId: string; // the other participant

  @IsString()
  status: 'start' | 'stop';
}
