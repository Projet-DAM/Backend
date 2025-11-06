import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { Message, MessageSchema } from './message.schema';
import { MessagesService } from './messages.service';
import { MessagesController } from './messages.controller';
import { UsersModule } from '../users/users.module'; // Assuming you have a UsersModule

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Message.name, schema: MessageSchema }]),
    UsersModule, // To validate sender/receiver IDs and potentially populate user info
  ],
  providers: [MessagesService],
  controllers: [MessagesController],
  exports: [MessagesService], // Export if other modules need to interact with MessagesService
})
export class MessagesModule {}
