
import { Injectable, BadRequestException, NotFoundException, Inject, forwardRef } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Message, MessageDocument } from './message.schema';
import { CreateMessageDto } from './dto/create-message.dto';
import { EditMessageDto } from './dto/edit-message.dto';
import { ReactMessageDto } from './dto/react-message.dto';
import { ReplyMessageDto } from './dto/reply-message.dto';
import { UsersService } from '../users/users.service'; // Assuming UsersService for user validation
import { MessagesGateway } from './messages.gateway';

@Injectable()
export class MessagesService {
    /**
     * Génère ou récupère un conversationId unique pour deux utilisateurs (coach/parent)
     */
    async getOrCreateConversationId(userId1: string, userId2: string): Promise<string> {
      // Pour l'instant, conversationId déterministe sans stockage DB
      // Si vous souhaitez stocker les conversations, adaptez ici
      return MessagesService.generateConversationId(userId1, userId2);
    }
  constructor(
    @InjectModel(Message.name) private messageModel: Model<MessageDocument>,
    private usersService: UsersService, // Inject UsersService
    @Inject(forwardRef(() => MessagesGateway)) private gateway: MessagesGateway,
  ) {}

  /**
   * Génère un conversationId unique pour un binôme coach-parent (ordre stable)
   */
  static generateConversationId(userA: string, userB: string): string {
    // Trie les deux ids pour garantir l'unicité quel que soit l'ordre
    return [userA, userB].sort().join('-');
  }

  async createMessage(senderId: string, createMessageDto: CreateMessageDto): Promise<Message> { // senderId is now the first parameter
    const { receiver, type, content, mediaUrl } = createMessageDto;

    // Validate sender and receiver IDs
    if (!Types.ObjectId.isValid(senderId)) throw new BadRequestException('Invalid sender ID');
    if (!Types.ObjectId.isValid(receiver)) throw new BadRequestException('Invalid receiver ID');

    const senderUser = await this.usersService.findById(senderId);
    const receiverUser = await this.usersService.findById(receiver);

    if (!senderUser) throw new NotFoundException('Sender not found');
    if (!receiverUser) throw new NotFoundException('Receiver not found');

    // Générer un conversationId unique pour ce binôme si non fourni
    const conversationId = MessagesService.generateConversationId(senderId, receiver);

    const newMessage = new this.messageModel({
      sender: new Types.ObjectId(senderId),
      receiver: new Types.ObjectId(receiver),
      conversationId,
      type,
      content: type === 'text' ? content : undefined,
      mediaUrl: (type === 'image' || type === 'audio') ? mediaUrl : undefined,
    });
    return newMessage.save();
  }

  async getConversationMessages(conversationId: string, userId: string): Promise<Message[]> {
    // Validation du format du conversationId
    const parts = conversationId.split('-');
    if (parts.length !== 2) {
      throw new BadRequestException('Invalid conversation ID format');
    }
    const [userId1, userId2] = parts;
    // Vérifie que l'utilisateur courant est bien un participant
    if (userId !== userId1 && userId !== userId2) {
      throw new BadRequestException('You are not a participant in this conversation.');
    }
    // Récupère les messages
    return this.messageModel
      .find({ conversationId })
      .populate('sender', '_id nom prenom email photoProfil')
      .populate('receiver', '_id nom prenom email photoProfil')
      .sort({ createdAt: 1 })
      .exec();
  }

  async getUserConversations(userId: string): Promise<string[]> {
    if (!Types.ObjectId.isValid(userId)) throw new BadRequestException('Invalid user ID');
    const conversations = await this.messageModel
      .distinct('conversationId', {
        $or: [{ sender: new Types.ObjectId(userId) }, { receiver: new Types.ObjectId(userId) }],
      })
      .exec();
    return conversations;
  }

  async markMessageAsRead(messageId: string): Promise<Message> {
    if (!Types.ObjectId.isValid(messageId)) throw new BadRequestException('Invalid message ID');
    const message = await this.messageModel
      .findByIdAndUpdate(
        messageId,
        { read: true },
        { new: true },
      )
      .exec();
    if (!message) throw new NotFoundException('Message not found');
    return message;
  }

  async editMessage(messageId: string, userId: string, dto: EditMessageDto): Promise<Message> {
    if (!Types.ObjectId.isValid(messageId)) throw new BadRequestException('Invalid message ID');
    const message = await this.messageModel.findById(messageId).exec();
    if (!message) throw new NotFoundException('Message not found');
    if (message.sender.toString() !== userId) {
      throw new BadRequestException('Only sender can edit the message');
    }
    if (message.isDeletedForAll) throw new BadRequestException('Cannot edit a deleted message');
    message.content = dto.content;
    message.edited = true;
    message.editedAt = new Date();
    const saved = await message.save();
    this.gateway.server.to(saved.conversationId).emit('messageEdited', saved);
    return saved;
  }

  async deleteForMe(messageId: string, userId: string): Promise<Message> {
    if (!Types.ObjectId.isValid(messageId)) throw new BadRequestException('Invalid message ID');
    const message = await this.messageModel.findById(messageId).exec();
    if (!message) throw new NotFoundException('Message not found');
    const uid = new Types.ObjectId(userId);
    const exists = message.deletedFor.some((d) => d.toString() === uid.toString());
    if (!exists) message.deletedFor.push(uid);
    const saved = await message.save();
    this.gateway.server.to(saved.conversationId).emit('messageDeletedForMe', { messageId: saved._id, userId });
    return saved;
  }

  async deleteForEveryone(messageId: string, userId: string): Promise<Message> {
    if (!Types.ObjectId.isValid(messageId)) throw new BadRequestException('Invalid message ID');
    const message = await this.messageModel.findById(messageId).exec();
    if (!message) throw new NotFoundException('Message not found');
    if (message.sender.toString() !== userId) {
      throw new BadRequestException('Only sender can delete for everyone');
    }
    message.isDeletedForAll = true;
    const saved = await message.save();
    this.gateway.server.to(saved.conversationId).emit('messageDeletedForAll', { messageId: saved._id });
    return saved;
  }

  async reactToMessage(messageId: string, userId: string, dto: ReactMessageDto): Promise<Message> {
    if (!Types.ObjectId.isValid(messageId)) throw new BadRequestException('Invalid message ID');
    const message = await this.messageModel.findById(messageId).exec();
    if (!message) throw new NotFoundException('Message not found');
    const reactions = message.reactions || new Map<string, string>();
    reactions.set(userId, dto.emoji);
    message.reactions = reactions;
    const saved = await message.save();
    this.gateway.server.to(saved.conversationId).emit('messageReaction', { messageId: saved._id, userId, emoji: dto.emoji });
    return saved;
  }

  async replyToMessage(senderId: string, conversationId: string, dto: ReplyMessageDto): Promise<Message> {
    if (!Types.ObjectId.isValid(dto.replyToMessageId)) throw new BadRequestException('Invalid replyToMessageId');
    const parent = await this.messageModel.findById(dto.replyToMessageId).exec();
    if (!parent) throw new NotFoundException('Parent message not found');
    const newMessage = new this.messageModel({
      sender: new Types.ObjectId(senderId),
      receiver: parent.sender.toString() === senderId ? parent.receiver : parent.sender,
      conversationId,
      type: dto.mediaUrl ? 'image' : 'text',
      content: dto.content,
      mediaUrl: dto.mediaUrl,
      replyToMessageId: parent._id,
    });
    const saved = await newMessage.save();
    this.gateway.server.to(conversationId).emit('receiveMessage', saved);
    return saved;
  }
}