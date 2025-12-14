import { Controller, Post, Body, HttpException, HttpStatus } from '@nestjs/common';
import { ApiBearerAuth, ApiTags } from '@nestjs/swagger';
import { Public } from '../common/decorators/public.decorator';
import { ChatbotService } from './chatbot.service';
import { ChatMessageDto } from './dto/chat-message.dto';

@ApiTags('Chatbot')
@Controller('chatbot')
export class ChatbotController {
    constructor(private readonly chatbotService: ChatbotService) { }

    @Public()
    @Post('message')
    async handleMessage(@Body() chatMessageDto: ChatMessageDto) {
        console.log('🔹 Chatbot Request Received:', JSON.stringify(chatMessageDto));
        try {
            const response = await this.chatbotService.processMessage(
                chatMessageDto.message,
                chatMessageDto.userId,
            );
            console.log('✅ Chatbot Response:', response);
            return { response };
        } catch (error) {
            console.error('❌ Chatbot Error:', error);
            throw new HttpException(
                'Erreur lors du traitement du message',
                HttpStatus.INTERNAL_SERVER_ERROR,
            );
        }
    }
}
