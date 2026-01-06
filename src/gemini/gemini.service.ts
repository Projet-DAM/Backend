import { Injectable, Logger, Inject, forwardRef } from '@nestjs/common';
import { GenerateFeedbackDto } from './dto/generate-feedback.dto';
import { GoogleGenerativeAI } from '@google/generative-ai';
import { ConfigService } from '@nestjs/config';
import { MessagesService } from '../messages/messages.service';
import { UsersService } from '../users/users.service';
import { SuiviEnfantService } from '../suivi-enfant/suivi-enfant.service';
import { UserRole } from '../users/interfaces/user-role.enum';

@Injectable()
export class GeminiService {
    private readonly logger = new Logger(GeminiService.name);
    private genAI: GoogleGenerativeAI;
    private model: any;

    constructor(
        private configService: ConfigService,
        private messagesService: MessagesService,
        private usersService: UsersService,
        @Inject(forwardRef(() => SuiviEnfantService))
        private suiviEnfantService: SuiviEnfantService,
    ) {
        // Correction : Utilisation de la clé API depuis .env ou la clé de secours
        const apiKey = this.configService.get<string>('GEMINI_API_KEY') || 'AIzaSyBpTeCynQn7Py6cS0Xe_ZbD8DK-Q2CV67U';

        if (apiKey) {
            this.genAI = new GoogleGenerativeAI(apiKey);
            // CORRECTION : On utilise 'gemini-1.5-flash-latest' pour éviter l'erreur 404 sur Render
            this.model = this.genAI.getGenerativeModel({ model: 'gemini-1.5-flash-latest' });
            this.logger.log('Gemini AI initialized with model gemini-1.5-flash-latest');
        } else {
            this.logger.warn('GEMINI_API_KEY not found in environment variables');
        }
    }

    /**
     * Génère un conseil de coaching (Virtual Coach)
     */
    async generateCoachingAdvice(childName: string, topic: string): Promise<string> {
        if (!this.model) {
            return `Allez ${childName} ! Continue de t'entraîner sur le thème ${topic}, tu vas progresser ! 💪⚽`;
        }

        const prompt = `Tu es un coach sportif bienveillant pour enfants. 
        Donne un conseil court (2 phrases maximum) et très motivant à l'enfant nommé ${childName} sur le thème "${topic}". 
        Utilise le tutoiement, sois encourageant et ajoute des emojis sportifs.`;

        try {
            this.logger.log(`Generating coaching advice for ${childName} on topic ${topic}`);
            const result = await this.model.generateContent(prompt);
            const response = await result.response;
            return response.text().trim();
        } catch (error) {
            this.logger.error('Error generating coaching advice', error);
            return `Allez ${childName} ! Le travail sur le thème ${topic} est la clé du succès. Continue tes efforts ! 💪⚽`;
        }
    }

    /**
     * Génère le texte de feedback après un match
     */
    async generateFeedback(dto: GenerateFeedbackDto): Promise<string> {
        if (!this.model) {
            this.logger.error('Gemini AI not initialized (missing API Key)');
            return this.fallbackMessage(dto);
        }

        const prompt = this.buildPrompt(dto);

        try {
            this.logger.log(`Generating feedback for child ${dto.childName} (Match: ${dto.matchId})`);
            const result = await this.model.generateContent(prompt);
            const response = await result.response;
            const text = response.text();
            return text.trim();
        } catch (error) {
            this.logger.error('Error generating feedback with Gemini', error);
            return this.fallbackMessage(dto);
        }
    }

    /**
     * Envoie le feedback généré dans les conversations du parent et du coach
     */
    async sendFeedbackToConversations(dto: GenerateFeedbackDto) {
        const feedbackText = await this.generateFeedback(dto);

        try {
            const child = await this.usersService.findById(dto.childId);
            if (!child) {
                this.logger.warn(`Child user ${dto.childId} not found`);
                return { success: false, error: 'Child not found' };
            }

            const parentId = child.parent ? (child.parent as any)._id?.toString() || child.parent.toString() : null;
            let coachId: string | null = null;

            if (child.coach) {
                coachId = (child.coach as any)._id?.toString() || child.coach.toString();
            }

            // Envoi au parent
            if (parentId) {
                await this.messagesService.createMessage(
                    coachId || parentId, // Expéditeur (par défaut le coach ou le système)
                    {
                        receiver: parentId,
                        type: 'ai_feedback' as any,
                        content: feedbackText,
                        conversationId: MessagesService.generateConversationId(coachId || parentId, parentId),
                    } as any
                );
            }

            return {
                success: true,
                feedback: feedbackText,
                recipients: { parent: parentId, coach: coachId }
            };
        } catch (e) {
            this.logger.error('Error sending feedback to conversations', e);
            return { success: false, error: e.message };
        }
    }

    private fallbackMessage(dto: GenerateFeedbackDto): string {
        const emojiMap = { victoire: '🏆', defaite: '💪', nul: '⚡' };
        const emoji = emojiMap[dto.matchResult.toLowerCase()] || '⚽';
        return `${emoji} Bravo ${dto.childName} pour ton match avec ${dto.teamName} ! Ta détermination est ta plus grande force, continue comme ça ! 🚀`;
    }

    private buildPrompt(dto: GenerateFeedbackDto): string {
        const emojiMap = { victoire: '🏆', defaite: '💪', nul: '⚡' };
        const emoji = emojiMap[dto.matchResult.toLowerCase()] || '⚽';

        return `
            Tu es un coach sportif bienveillant pour enfants.
            CONTEXTE :
            - Enfant : ${dto.childName} ${dto.childAge ? `(${dto.childAge} ans)` : ''}
            - Équipe : ${dto.teamName}
            - Résultat : ${dto.matchResult.toUpperCase()} (Score: ${dto.score})
            - Phase : ${dto.phase}
            ${dto.performance ? `- Performance : ${dto.performance}` : ''}

            MISSION :
            Écris un message court (2 phrases) et TRÈS motivant pour ${dto.childName}.
            Tutoie l'enfant, utilise un langage simple, commence par ${emoji} et termine par un encouragement.
        `;
    }
}