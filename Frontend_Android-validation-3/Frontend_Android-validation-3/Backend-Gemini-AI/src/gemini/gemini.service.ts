import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { GoogleGenerativeAI } from '@google/generative-ai';
import { GenerateFeedbackDto } from './dto/generate-feedback.dto';

@Injectable()
export class GeminiService {
    private readonly logger = new Logger(GeminiService.name);
    private genAI: GoogleGenerativeAI;
    private model;

    constructor(private configService: ConfigService) {
        const apiKey = this.configService.get<string>('GEMINI_API_KEY');

        if (!apiKey) {
            this.logger.error('GEMINI_API_KEY is not defined in environment variables');
            throw new Error('GEMINI_API_KEY is required');
        }

        this.genAI = new GoogleGenerativeAI(apiKey);
        this.model = this.genAI.getGenerativeModel({ model: 'gemini-pro' });
    }

    /**
     * Génère un feedback motivant et pédagogique pour un enfant après un match
     */
    async generateFeedback(dto: GenerateFeedbackDto): Promise<string> {
        try {
            const prompt = this.buildPrompt(dto);

            this.logger.log(`Generating feedback for child: ${dto.childName}`);

            const result = await this.model.generateContent(prompt);
            const response = await result.response;
            const feedback = response.text();

            this.logger.log(`Feedback generated successfully for ${dto.childName}`);

            return feedback;
        } catch (error) {
            this.logger.error(`Error generating feedback: ${error.message}`, error.stack);
            throw error;
        }
    }

    /**
     * Construit le prompt pour Gemini selon le contexte du match
     */
    private buildPrompt(dto: GenerateFeedbackDto): string {
        const { childName, childAge, matchResult, teamName, score, phase, performance, tournamentName } = dto;

        const ageContext = childAge ? `${childName} a ${childAge} ans.` : '';
        const tournamentContext = tournamentName ? `dans le tournoi "${tournamentName}"` : '';
        const performanceContext = performance ? `L'enfant a montré : ${performance}.` : '';

        let resultContext = '';
        let tone = '';

        switch (matchResult.toLowerCase()) {
            case 'victoire':
                resultContext = `L'équipe ${teamName} a GAGNÉ avec un score de ${score} en ${phase}.`;
                tone = 'Félicite chaleureusement l\'enfant pour sa victoire. Sois enthousiaste et encourageant.';
                break;

            case 'defaite':
                resultContext = `L'équipe ${teamName} a perdu avec un score de ${score} en ${phase}.`;
                tone = 'Encourage l\'enfant malgré la défaite. Sois positif, motivant et mets l\'accent sur l\'effort et le progrès.';
                break;

            case 'nul':
                resultContext = `L'équipe ${teamName} a fait match nul avec un score de ${score} en ${phase}.`;
                tone = 'Félicite l\'enfant pour son effort et sa détermination. Sois positif et encourageant.';
                break;
        }

        return `
Tu es un coach sportif bienveillant et motivant pour enfants. 

CONTEXTE :
- ${childName} joue dans l'équipe ${teamName} ${tournamentContext}
- ${ageContext}
- ${resultContext}
- ${performanceContext}

MISSION :
Écris un message court (2-3 phrases maximum) et TRÈS motivant pour ${childName}.

RÈGLES IMPORTANTES :
1. ${tone}
2. Utilise un langage simple et adapté aux enfants
3. Sois TRÈS positif et encourageant
4. Mentionne l'équipe et le résultat du match
5. Ajoute UN SEUL emoji au début du message (🏆 pour victoire, 💪 pour défaite, ⚡ pour nul)
6. Reste concis : 2-3 phrases maximum
7. Parle directement à l'enfant (utilise "tu")
8. Termine par un message d'encouragement pour la suite

EXEMPLE DE FORMAT :
🏆 Bravo ${childName} ! Ton équipe ${teamName} a gagné ${score} en ${phase} ! Continue comme ça, tu es sur la bonne voie !

Maintenant, génère le message :
`.trim();
    }

    /**
     * Génère un feedback avec métadonnées complètes
     */
    async generateFeedbackWithMetadata(dto: GenerateFeedbackDto) {
        const feedback = await this.generateFeedback(dto);

        const emoji = this.getEmojiForResult(dto.matchResult);

        return {
            feedback,
            generatedAt: new Date().toISOString(),
            childId: dto.childId,
            matchId: dto.matchId,
            metadata: {
                matchResult: dto.matchResult,
                teamName: dto.teamName,
                score: dto.score,
                phase: dto.phase,
                emoji,
            },
        };
    }

    /**
     * Retourne l'emoji approprié selon le résultat
     */
    private getEmojiForResult(result: string): string {
        switch (result.toLowerCase()) {
            case 'victoire':
                return '🏆';
            case 'defaite':
                return '💪';
            case 'nul':
                return '⚡';
            default:
                return '⚽';
        }
    }
}
