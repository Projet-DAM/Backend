// src/messages/messages.module.ts
import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { Message, MessageSchema } from './message.schema';
import { MessagesService } from './messages.service';
import { MessagesController } from './messages.controller';
import { UsersModule } from '../users/users.module'; // Adjust path if necessary
import { MessagesGateway } from './messages.gateway'; // Import the new gateway
import { JwtModule } from '@nestjs/jwt';
import { ConfigModule, ConfigService } from '@nestjs/config';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Message.name, schema: MessageSchema }]),
    UsersModule,
    // Ensure JwtModule is registered for use with JwtService
    JwtModule.registerAsync({
      imports: [ConfigModule], // Import ConfigModule to use ConfigService
      useFactory: async (configService: ConfigService) => ({
        secret: configService.get<string>('JWT_SECRET'), // Use environment variable for secret
        signOptions: { expiresIn: '1d' },
      }),
      inject: [ConfigService], // Inject ConfigService
    }),
    ConfigModule, // Also import ConfigModule if not already global
  ],
  providers: [MessagesService, MessagesGateway], // Add MessagesGateway here
  controllers: [MessagesController],
  exports: [MessagesService],
})
export class MessagesModule {}