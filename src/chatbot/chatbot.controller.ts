import { Body, Controller, Post, UseGuards, HttpCode, HttpStatus, Logger } from '@nestjs/common';
import { ApiOperation, ApiResponse, ApiTags, ApiBearerAuth } from '@nestjs/swagger';
import { ChatbotService } from './chatbot.service';
import { ChatMessageDto } from './dto/chat-message.dto';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Public } from '../common/decorators/public.decorator';

@ApiTags('Chatbot')
@Controller('chatbot')
export class ChatbotController {
    private readonly logger = new Logger(ChatbotController.name);

    constructor(private readonly chatbotService: ChatbotService) { }

    @Public() // Accessible pour permettre le test vocal même hors connexion si besoin
    @Post('message')
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Envoyer un message au chatbot intelligent' })
    @ApiResponse({ status: 200, description: 'Réponse générée par le chatbot' })
    async handleMessage(@Body() chatMessageDto: ChatMessageDto) {
        try {
            const { message, userId } = chatMessageDto;
            this.logger.log(`Incoming message: "${message}" from ${userId || 'anonymous'}`);

            const reply = await this.chatbotService.processMessage(message, userId);

            // On renvoie 'reply' ET 'response' pour la compatibilité Android
            return {
                reply,
                response: reply,
                status: 'success'
            };
        } catch (error) {
            this.logger.error(`Chatbot error: ${error.message}`);
            return {
                reply: "Une erreur est survenue, veuillez réessayer.",
                response: "Erreur technique",
                status: 'error'
            };
        }
    }
}
