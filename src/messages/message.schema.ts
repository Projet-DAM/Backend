import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export enum MessageType {
  TEXT = 'text',
  IMAGE = 'image',
  AUDIO = 'audio',
}

@Schema({ timestamps: true })
export class Message {
  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  sender: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  receiver: Types.ObjectId;

  // This could be a unique identifier for a conversation between two users
  @Prop({ type: String, required: true, index: true })
  conversationId: string;

  @Prop({ type: String, enum: MessageType, required: true })
  type: MessageType;

  @Prop({ type: String, maxlength: 500, required: function() { return this.type === MessageType.TEXT; } })
  content?: string; // For text messages, max 500 characters

  @Prop({ type: String, required: function() { return this.type === MessageType.IMAGE || this.type === MessageType.AUDIO; } })
  mediaUrl?: string; // URL to the stored image or audio file

  @Prop({ type: Boolean, default: false })
  read: boolean;

  // Deletion logic
  @Prop({ type: Boolean, default: false })
  isDeletedForAll: boolean;

  @Prop({ type: [Types.ObjectId], default: [] })
  deletedFor: Types.ObjectId[];

  // Edit logic
  @Prop({ type: Boolean, default: false })
  edited: boolean;

  @Prop({ type: Date })
  editedAt?: Date;

  // Reactions: one emoji per user
  @Prop({ type: Map, of: String, default: {} })
  reactions: Map<string, string>;

  // Reply to specific message
  @Prop({ type: Types.ObjectId, ref: 'Message' })
  replyToMessageId?: Types.ObjectId;
}

export type MessageDocument = Message & Document;
export const MessageSchema = SchemaFactory.createForClass(Message);
