import {
  Controller,
  Post,
  Body,
  Get,
  Param,
  Patch,
  Delete,
  Req,
  UseGuards,
  BadRequestException,
  NotFoundException,
} from '@nestjs/common';
import { ApiTags, ApiOperation, ApiResponse, ApiBearerAuth, ApiParam } from '@nestjs/swagger';
import { MessagesService } from './messages.service';
import { CreateMessageDto } from './dto/create-message.dto';
import { EditMessageDto } from './dto/edit-message.dto';
import { ReactMessageDto } from './dto/react-message.dto';
import { ReplyMessageDto } from './dto/reply-message.dto';
import { TypingDto } from './dto/typing.dto';
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

  @Post('conversation/init')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Initier une conversation entre deux utilisateurs (coach/parent)' })
  @ApiResponse({ status: 200, description: 'conversationId généré ou récupéré' })
  async initiateConversation(
    @Req() req: CustomRequest,
    @Body() body: { otherUserId: string }
  ) {
    const currentUserId = req.user.userId;
    const { otherUserId } = body;
    if (!otherUserId) throw new BadRequestException('otherUserId requis');
    const conversationId = await this.messagesService.getOrCreateConversationId(
      currentUserId,
      otherUserId
    );
    return { conversationId };
  }

  @Get('conversation-id/:userId')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Obtenir le conversationId unique pour discuter avec un utilisateur (coach/parent)' })
  @ApiParam({ name: 'userId', type: 'string', description: "ID de l'autre utilisateur (coach ou parent)" })
  @ApiResponse({ status: 200, description: 'conversationId généré' })
  getConversationId(@Param('userId') userId: string, @Req() req: CustomRequest) {
    const myId = req.user.userId;
    return { conversationId: MessagesService.generateConversationId(myId, userId) };
  }

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
    return this.messagesService.getConversationMessages(conversationId, req.user.userId);
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

  @Patch(':id/edit')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Edit a message content (sender only)' })
  async editMessage(
    @Param('id') messageId: string,
    @Body() dto: EditMessageDto,
    @Req() req: CustomRequest,
  ) {
    return this.messagesService.editMessage(messageId, req.user.userId, dto);
  }

  @Patch(':id/for-me')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Delete a message for current user (hide locally)' })
  async deleteForMe(
    @Param('id') messageId: string,
    @Req() req: CustomRequest,
  ) {
    return this.messagesService.deleteForMe(messageId, req.user.userId);
  }

  @Delete(':id/for-everyone')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Delete a message for everyone (sender only)' })
  async deleteForEveryone(
    @Param('id') messageId: string,
    @Req() req: CustomRequest,
  ) {
    return this.messagesService.deleteForEveryone(messageId, req.user.userId);
  }

  @Post(':id/react')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'React to a message with an emoji (one per user)' })
  async reactToMessage(
    @Param('id') messageId: string,
    @Body() dto: ReactMessageDto,
    @Req() req: CustomRequest,
  ) {
    return this.messagesService.reactToMessage(messageId, req.user.userId, dto);
  }

  @Post(':conversationId/reply')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Reply to a specific message in a conversation' })
  async replyToMessage(
    @Param('conversationId') conversationId: string,
    @Body() dto: ReplyMessageDto,
    @Req() req: CustomRequest,
  ) {
    return this.messagesService.replyToMessage(req.user.userId, conversationId, dto);
  }

  @Post('typing')
  @Roles(UserRole.COACH, UserRole.PARENT)
  @ApiOperation({ summary: 'Emit typing event via WebSocket channel (HTTP helper)' })
  async typing(@Body() dto: TypingDto, @Req() req: CustomRequest) {
    const otherId = dto.conversationUserId;
    const conversationId = MessagesService.generateConversationId(req.user.userId, otherId);
    // Lightweight publish via gateway if accessible; otherwise return payload for client to emit.
    // In this controller context, we return the target conversation info.
    return { conversationId, status: dto.status };
  }
}