// src/messages/messages.gateway.ts
import {
  WebSocketGateway,
  SubscribeMessage,
  MessageBody,
  WebSocketServer,
  ConnectedSocket,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { MessagesService } from './messages.service';
import { CreateMessageDto } from './dto/create-message.dto';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { UserRole } from '../users/interfaces/user-role.enum'; // Adjust path if necessary

interface AuthenticatedSocket extends Socket {
  user: { userId: string; email: string; role: UserRole };
}

@WebSocketGateway({
  cors: {
    origin: '*', // Adjust to your specific frontend URL in production (e.g., 'http://localhost:8081')
    methods: ['GET', 'POST'],
    credentials: true,
  },
  // You can specify a path for your WebSocket if needed, e.g., '/ws/messages'
  // If no path is specified, it defaults to the root '/'
})
export class MessagesGateway {
  @WebSocketServer() server: Server;

  constructor(
    private readonly messagesService: MessagesService,
    private readonly jwtService: JwtService,
    private readonly configService: ConfigService,
  ) {}

  async handleConnection(@ConnectedSocket() client: AuthenticatedSocket) {
    const token = client.handshake.headers.authorization?.split(' ')[1];
    if (token) {
      try {
        const decoded = this.jwtService.verify(token, {
          secret: this.configService.get<string>('JWT_SECRET'),
        });
        client.user = decoded;
        console.log(`WebSocket: Client connected: ${client.id} with user ${client.user.userId} (role: ${client.user.role})`);
      } catch (error) {
        console.error('WebSocket authentication failed:', error.message);
        client.disconnect(true);
        return;
      }
    } else {
      console.error('WebSocket connection denied: No token provided');
      client.disconnect(true);
      return;
    }
  }

  handleDisconnect(@ConnectedSocket() client: AuthenticatedSocket) {
    console.log(`WebSocket: Client disconnected: ${client.id} from user ${client.user?.userId}`);
  }

  @SubscribeMessage('sendMessage')
  async handleSendMessage(
    @ConnectedSocket() client: AuthenticatedSocket,
    @MessageBody() payload: CreateMessageDto,
  ) {
    if (!client.user || (client.user.role !== UserRole.PARENT && client.user.role !== UserRole.COACH)) {
      client.emit('error', 'Unauthorized to send messages.');
      return;
    }

    const senderId = client.user.userId;
    console.log(`WebSocket: User ${senderId} (role: ${client.user.role}) sending message to ${payload.receiver} in conversation ${payload.conversationId}`);

    try {
      const message = await this.messagesService.createMessage(senderId, payload);
      this.server.to(payload.conversationId).emit('receiveMessage', message); // Emit to all in conversation
    } catch (error) {
      console.error('Error sending message via WebSocket:', error.message);
      client.emit('error', 'Failed to send message.');
    }
  }

  @SubscribeMessage('joinConversation')
  handleJoinConversation(@ConnectedSocket() client: Socket, @MessageBody() conversationId: string) {
    client.join(conversationId);
    console.log(`WebSocket: Client ${client.id} joined conversation ${conversationId}`);
  }

  @SubscribeMessage('leaveConversation')
  handleLeaveConversation(@ConnectedSocket() client: Socket, @MessageBody() conversationId: string) {
    client.leave(conversationId);
    console.log(`WebSocket: Client ${client.id} left conversation ${conversationId}`);
  }
}