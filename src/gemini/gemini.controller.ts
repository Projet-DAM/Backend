
import { Controller, Post, Body, UsePipes, ValidationPipe } from '@nestjs/common';
import { GeminiService } from './gemini.service';
import { GenerateFeedbackDto } from './dto/generate-feedback.dto';

@Controller('gemini')
export class GeminiController {
    constructor(private readonly geminiService: GeminiService) { }

    @Post('coaching')
    async getCoachingAdvice(@Body() body: { childName: string; topic: string }) {
        const { childName, topic } = body;
        const advice = await this.geminiService.generateCoachingAdvice(childName, topic);
        return { advice };
    }

    @Post('feedback/generate')
    @UsePipes(new ValidationPipe())
    async generate(@Body() dto: GenerateFeedbackDto) {
        const feedback = await this.geminiService.generateFeedback(dto);
        return { feedback };
    }

    @Post('feedback/send-to-conversations')
    @UsePipes(new ValidationPipe())
    async sendToConversations(@Body() dto: GenerateFeedbackDto) {
        return this.geminiService.sendFeedbackToConversations(dto);
    }
}
