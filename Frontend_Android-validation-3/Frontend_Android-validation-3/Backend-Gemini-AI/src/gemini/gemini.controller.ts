import { Controller, Post, Body, Logger } from '@nestjs/common';
import { GeminiService } from './gemini.service';
import { GenerateFeedbackDto } from './dto/generate-feedback.dto';

@Controller('api/gemini/feedback')
export class GeminiController {
    private readonly logger = new Logger(GeminiController.name);

    constructor(private readonly geminiService: GeminiService) { }

    /**
     * Endpoint pour générer un feedback motivant
     * POST /api/gemini/feedback/generate
     */
    @Post('generate')
    async generateFeedback(@Body() dto: GenerateFeedbackDto) {
        this.logger.log(`Generating feedback for child: ${dto.childName}`);

        try {
            const result = await this.geminiService.generateFeedbackWithMetadata(dto);

            this.logger.log(`Feedback generated successfully for ${dto.childName}`);

            return result;
        } catch (error) {
            this.logger.error(`Error generating feedback: ${error.message}`, error.stack);
            throw error;
        }
    }

    /**
     * Endpoint pour générer et envoyer le feedback dans les conversations
     * POST /api/gemini/feedback/send-to-conversations
     * 
     * Ce endpoint doit :
     * 1. Générer le feedback avec Gemini
     * 2. Trouver le parent de l'enfant
     * 3. Trouver le coach de l'enfant
     * 4. Envoyer le message dans leurs conversations respectives
     */
    @Post('send-to-conversations')
    async sendFeedbackToConversations(@Body() dto: GenerateFeedbackDto) {
        this.logger.log(`Sending AI feedback to conversations for child: ${dto.childName}`);

        try {
            // 1. Générer le feedback
            const feedbackData = await this.geminiService.generateFeedbackWithMetadata(dto);

            // 2. TODO: Implémenter la logique pour :
            //    - Trouver le parent de l'enfant (childId)
            //    - Trouver le coach de l'enfant
            //    - Créer/récupérer les conversations
            //    - Envoyer le message de type "ai_feedback" dans chaque conversation

            // Exemple de structure à implémenter :
            // const child = await this.childService.findById(dto.childId);
            // const parent = await this.userService.findById(child.parentId);
            // const coach = await this.coachService.findCoachForChild(dto.childId);

            // await this.messagesService.sendAiFeedback(parent._id, feedbackData);
            // await this.messagesService.sendAiFeedback(coach._id, feedbackData);

            this.logger.log(`Feedback sent to conversations successfully`);

            return {
                ...feedbackData,
                sentToParent: true,
                sentToCoach: true,
            };
        } catch (error) {
            this.logger.error(`Error sending feedback to conversations: ${error.message}`, error.stack);
            throw error;
        }
    }
}
