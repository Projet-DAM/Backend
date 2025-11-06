import { Injectable, BadRequestException, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Message, MessageDocument } from './message.schema';
import { CreateMessageDto } from './dto/create-message.dto';
import { UsersService } from '../users/users.service'; // Assuming UsersService for user validation

@Injectable()
export class MessagesService {
  constructor(
    @InjectModel(Message.name) private messageModel: Model<MessageDocument>,
    private usersService: UsersService, // Inject UsersService
  ) {}

  async createMessage(createMessageDto: CreateMessageDto): Promise<Message> {
    const { sender, receiver, conversationId, type, content, mediaUrl } = createMessageDto;

    // Validate sender and receiver
    if (!Types.ObjectId.isValid(sender)) throw new BadRequestException('Invalid sender ID');
    if (!Types.ObjectId.isValid(receiver)) throw new BadRequestException('Invalid receiver ID');

    const senderUser = await this.usersService.findById(sender);
    const receiverUser = await this.usersService.findById(receiver);

    if (!senderUser) throw new NotFoundException('Sender not found');
    if (!receiverUser) throw new NotFoundException('Receiver not found');

    const newMessage = new this.messageModel({
      sender: new Types.ObjectId(sender),
      receiver: new Types.ObjectId(receiver),
      conversationId,
      type,
      content: type === 'text' ? content : undefined, // Only set content for text messages
      mediaUrl: (type === 'image' || type === 'audio') ? mediaUrl : undefined, // Only set mediaUrl for media messages
    });

    return newMessage.save();
  }

  async getConversationMessages(conversationId: string): Promise<Message[]> {
    return this.messageModel
      .find({ conversationId })
      .populate('sender', 'nom prenom photoProfil') // Populate sender info
      .populate('receiver', 'nom prenom photoProfil') // Populate receiver info
      .sort({ createdAt: 1 }) // Sort by oldest first
      .exec();
  }

  async getUserConversations(userId: string): Promise<string[]> {
    if (!Types.ObjectId.isValid(userId)) throw new BadRequestException('Invalid user ID');

    const conversations = await this.messageModel.distinct('conversationId', {
      $or: [{ sender: new Types.ObjectId(userId) }, { receiver: new Types.ObjectId(userId) }],
    }).exec();

    return conversations;
  }

  async markMessageAsRead(messageId: string): Promise<Message> {
    if (!Types.ObjectId.isValid(messageId)) throw new BadRequestException('Invalid message ID');
    const message = await this.messageModel.findByIdAndUpdate(
      messageId,
      { read: true },
      { new: true },
    ).exec();
    if (!message) throw new NotFoundException('Message not found');
    return message;
  }
}
