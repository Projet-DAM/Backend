import {
  Controller,
  Post,
  Body,
  Get,
  Param,
  Patch,
  Req,
  UseGuards,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiParam } from '@nestjs/swagger';
import { MessagesService } from './messages.service';
import { CreateMessageDto } from './dto/create-message.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard'; // Assuming your JWT guard path
import { UserRole } from '../users/interfaces/user-role.enum'; // Assuming your UserRole enum
import { Roles } from '../common/decorators/roles.decorator'; // Assuming your Roles decorator
import { Request } from 'express';
import { Types } from 'mongoose';

interface CustomRequest extends Request {
  user: { userId: string; email: string; role: UserRole; };
}

@ApiTags('Messages')
@ApiBearerAuth('JWT-auth')
@UseGuards(JwtAuthGuard)
@Controller('messages')
export class MessagesController {
  constructor(private readonly messagesService: MessagesService) {}

  @Post()
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Send a new message between coach and parent' })
  @ApiResponse({ status: 201, description: 'Message sent successfully' })
  @ApiResponse({ status: 400, description: 'Bad Request' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  @ApiResponse({ status: 403, description: 'Forbidden: Role mismatch' })
  async createMessage(
    @Body() createMessageDto: CreateMessageDto,
    @Req() req: CustomRequest,
  ): Promise<any> {
    const senderId = req.user.userId;
    console.log('Received message request from user with role:', req.user.role); // Keep this for debugging

    // Basic validation to ensure sender is either coach or parent, and receiver is the other role
    const senderUser = await this.messagesService['usersService'].findById(senderId); // Use senderId here
    const receiverUser = await this.messagesService['usersService'].findById(createMessageDto.receiver);

    if (!senderUser) {
      throw new BadRequestException('Authenticated sender not found.');
    }
    if (!receiverUser) {
      throw new BadRequestException('Receiver not found.');
    }

    if (
      !(
        (senderUser.role === UserRole.COACH && receiverUser.role === UserRole.PARENT) ||
        (senderUser.role === UserRole.PARENT && receiverUser.role === UserRole.COACH)
      )
    ) {
      throw new BadRequestException('Messages can only be sent between a COACH and a PARENT.');
    }

    // Pass the senderId to the service
    return this.messagesService.createMessage(senderId, createMessageDto);
  }

  @Get('conversation/:conversationId')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Get messages for a specific conversation' })
  @ApiParam({ name: 'conversationId', type: 'string', description: 'ID of the conversation' })
  @ApiResponse({ status: 200, description: 'Messages retrieved successfully' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  @ApiResponse({ status: 403, description: 'Forbidden: Access to conversation denied' })
  async getConversationMessages(
    @Param('conversationId') conversationId: string,
    @Req() req: CustomRequest,
  ): Promise<any[]> {
    const messages = await this.messagesService.getConversationMessages(conversationId);

    if (messages.length > 0) {
      const isParticipant = messages.some(
        (message) =>
          message.sender.toString() === req.user.userId ||
          message.receiver.toString() === req.user.userId,
      );
      if (!isParticipant) {
        throw new BadRequestException('You are not a participant in this conversation.');
      }
    } else {
      const [user1Id, user2Id] = conversationId.split('-');
      if (!user1Id || !user2Id || !Types.ObjectId.isValid(user1Id) || !Types.ObjectId.isValid(user2Id)) {
        throw new BadRequestException('Invalid conversation ID format or no messages found.');
      }
      const user1 = await this.messagesService['usersService'].findById(user1Id);
      const user2 = await this.messagesService['usersService'].findById(user2Id);
      if (!user1 || !user2) {
        throw new NotFoundException('One or both users in the conversation not found.');
      }
      if (req.user.userId !== user1Id && req.user.userId !== user2Id) {
        throw new BadRequestException('You are not a participant in this conversation.');
      }
    }
    return messages;
  }

  @Get('conversations/my')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Get all conversation IDs for the logged-in user' })
  @ApiResponse({ status: 200, description: 'Conversation IDs retrieved successfully' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  async getMyConversations(@Req() req: CustomRequest): Promise<string[]> {
    return this.messagesService.getUserConversations(req.user.userId);
  }

  @Patch(':id/read')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Mark a message as read' })
  @ApiParam({ name: 'id', type: 'string', description: 'ID of the message to mark as read' })
  @ApiResponse({ status: 200, description: 'Message marked as read' })
  @ApiResponse({ status: 404, description: 'Message not found' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  @ApiResponse({ status: 403, description: 'Forbidden: Not authorized to mark this message as read' })
  async markMessageAsRead(
    @Param('id') messageId: string,
    @Req() req: CustomRequest,
  ): Promise<any> {
    const message = await this.messagesService.markMessageAsRead(messageId);
    if (message.receiver.toString() !== req.user.userId) {
      throw new BadRequestException('You are not authorized to mark this message as read.');
    }
    return message;
  }
}