
import { Injectable, Logger, InternalServerErrorException, Inject, forwardRef } from '@nestjs/common';
import { GenerateFeedbackDto } from './dto/generate-feedback.dto';
import { GoogleGenerativeAI } from '@google/generative-ai';
import { ConfigService } from '@nestjs/config';
import { MessagesService } from '../messages/messages.service';
import { UsersService } from '../users/users.service';
import { SuiviEnfantService } from '../suivi-enfant/suivi-enfant.service';
import { MessageType } from '../messages/message.schema';
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
        const apiKey = this.configService.get<string>('GEMINI_API_KEY') || 'AIzaSyBpTeCynQn7Py6cS0Xe_ZbD8DK-Q2CV67U';
        if (apiKey) {
            this.genAI = new GoogleGenerativeAI(apiKey);
            this.model = this.genAI.getGenerativeModel({ model: 'gemini-1.5-flash' });
        } else {
            this.logger.warn('GEMINI_API_KEY not found in environment variables');
        }
    }

    async generateCoachingAdvice(childName: string, topic: string): Promise<string> {
        if (!this.model) {
            return "Désolé, je ne peux pas donner de conseils pour le moment. ⚽";
        }

        const prompt = `Tu es un coach sportif pour enfants. Donne un conseil court et motivant à l'enfant nommé ${childName} sur le thème ${topic}. Utilise le tutoiement et des emojis.`;

        try {
            this.logger.log(`Generating coaching advice for ${childName} on topic ${topic}`);
            const result = await this.model.generateContent(prompt);
            const response = await result.response;
            return response.text().trim();
        } catch (error) {
            this.logger.error('Error generating coaching advice', error);
            return `Allez ${childName} ! Continue de t'entraîner sur le thème ${topic}, tu vas progresser ! 💪⚽`;
        }
    }

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

    private fallbackMessage(dto: GenerateFeedbackDto): string {
        const emojiMap = {
            victoire: '🏆',
            defaite: '💪',
            nul: '⚡'
        };
        const emoji = emojiMap[dto.matchResult] || '⚽';
        return `${emoji} Bravo ${dto.childName} pour ton match avec ${dto.teamName} ! Continue tes efforts, c'est super ! 🚀`;
    }

    private buildPrompt(dto: GenerateFeedbackDto): string {
        const emojiMap = {
            victoire: '🏆',
            defaite: '💪',
            nul: '⚡'
        };
        const emoji = emojiMap[dto.matchResult] || '⚽';

        return `
Tu es un coach sportif bienveillant pour enfants.

CONTEXTE :
- Enfant : ${dto.childName} ${dto.childAge ? `(${dto.childAge} ans)` : ''}
- Équipe : ${dto.teamName}
- Résultat : ${dto.matchResult.toUpperCase()} (Score: ${dto.score})
- Phase : ${dto.phase}
${dto.performance ? `- Performance observée : ${dto.performance}` : ''}
${dto.tournamentName ? `- Tournoi : ${dto.tournamentName}` : ''}

MISSION :
Écris un message court (2-3 phrases) et TRÈS motivant pour ${dto.childName}.

RÈGLES :
1. Félicite chaleureusement ou encourage selon le résultat (${dto.matchResult})
2. Langage simple et énergique adapté à un enfant
3. Sois TRÈS positif, utilise des emojis
4. Mentionne l'équipe "${dto.teamName}" et le résultat
5. Commence par ${emoji}
6. 2-3 phrases maximum
7. Tutoie l'enfant
8. Termine par un encouragement futur

Génère UNIQUEMENT le message.
    `;
    }

    async sendFeedbackToConversations(dto: GenerateFeedbackDto) {
        // 1. Generate text
        const feedbackText = await this.generateFeedback(dto);

        try {
            // 2. Find Child User to get Parent
            const child = await this.usersService.findById(dto.childId);
            if (!child) {
                this.logger.warn(`Child user ${dto.childId} not found, cannot send feedback`);
                return { success: false, error: 'Child not found' };
            }

            const parentId = child.parent ? child.parent._id.toString() : null;

            // 3. Find Coach (via SuiviEnfant or generic coach of the child)
            // Assuming we take the first coach found for this child in SuiviEnfant logic 
            let coachId: string | null = null;
            if (child.coach) {
                coachId = (child.coach as any)._id ? (child.coach as any)._id.toString() : child.coach.toString();
            } else {
                // Try to find from latest SuiviEnfant
                const suivis = await this.suiviEnfantService.findByEnfant(dto.childId, { userId: 'system-ai', role: UserRole.ACADEMIE });
                if (suivis && suivis.length > 0) {
                    const c = suivis[0].coach;
                    coachId = (c as any)._id ? (c as any)._id.toString() : c.toString();
                }
            }

            if (!coachId) {
                // If still no coach, maybe we skip sending to coach or use parent as sender placeholder?
                // We'll proceed with parent only if coach is missing.
            }

            const metadata = {
                matchId: dto.matchId,
                matchResult: dto.matchResult,
                teamName: dto.teamName,
                score: dto.score,
                phase: dto.phase,
                isAiFeedback: true
            };

            // 4. Send to Parent
            if (parentId) {
                await this.messagesService.createMessage(
                    coachId || parentId,
                    {
                        receiver: parentId,
                        type: 'ai_feedback' as any,
                        content: feedbackText,
                        conversationId: MessagesService.generateConversationId(coachId || parentId, parentId),
                    } as any
                );
            }

            // 5. Send to Coach
            if (coachId) {
                // Send from System or Parent?
                // If we want the coach to see the message they "sent" (AI generated), we might create it 
                // but we already did above if coachId was sender.
                // If we want to NOTIFY the coach:
                // We can send a message to the coach from the system or just rely on the fact that if they are the sender,
                // it shows up in their chat history.
                // The prompt says "envoyer ce message dans la conversation... du coach".
                // If the coach is the sender, it IS in their conversation.
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
}
