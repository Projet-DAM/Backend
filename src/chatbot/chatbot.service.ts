import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Offer, OfferDocument } from '../offers/schemas/offer.schema';
import { User, UserDocument } from '../users/entity/user.entity';
import { Subscription, SubscriptionDocument } from '../subscriptions/schemas/subscription.schema';
import { UserRole } from '../users/interfaces/user-role.enum';
import OpenAI from 'openai';

@Injectable()
export class ChatbotService {
    private readonly logger = new Logger(ChatbotService.name);
    private openai: OpenAI;

    constructor(
        @InjectModel(Offer.name) private offerModel: Model<OfferDocument>,
        @InjectModel(User.name) private userModel: Model<UserDocument>,
        @InjectModel(Subscription.name) private subModel: Model<SubscriptionDocument>,
    ) {
        // Initialize OpenAI only if API key is present, otherwise we'll fail gracefully or use mock
        if (process.env.OPENAI_API_KEY) {
            this.openai = new OpenAI({
                apiKey: process.env.OPENAI_API_KEY,
            });
        } else {
            this.logger.warn('OPENAI_API_KEY not found. Chatbot will use basic fallback replies.');
        }
    }

    async processMessage(userMessage: string, userId?: string): Promise<string> {
        // 1. Gather Context
        const context = await this.buildContext(userId);

        // 2. If OpenAI is available, use it
        if (this.openai) {
            try {
                const completion = await this.openai.chat.completions.create({
                    messages: [
                        { role: 'system', content: context },
                        { role: 'user', content: userMessage },
                    ],
                    model: 'gpt-3.5-turbo', // Or gpt-4 if available/affordable
                    max_tokens: 150, // Keep responses concise for voice
                });

                return completion.choices[0]?.message?.content || "Je n'ai pas compris, pouvez-vous répéter ?";
            } catch (error) {
                this.logger.error('OpenAI Error', error);
                // Fallback to simple logic if AI quota is exceeded (429) or other errors
                return this.fallbackLogic(userMessage);
            }
        }

        // 3. Fallback (Simple Rule-based) if no AI key
        return this.fallbackLogic(userMessage);
    }

    private async buildContext(userId?: string): Promise<string> {
        // A. General Info
        let systemPrompt = `Tu es l'assistant virtuel intelligent d'une académie sportive. 
Ta mission est d'aider les parents en répondant à leurs questions vocalement.
Réponds de manière courte, naturelle et chaleureuse (adapté à la voix).
Evite les listes à puces complexes, privilégie les phrases complètes.
Si tu ne sais pas, dis-le poliment et conseille de contacter le secrétariat.

INFORMATIONS GÉNÉRALES:
- Nom: Académie SportyConnect (Exemple)
- Horaires: Lundi-Vendredi 9h-18h, Samedi 9h-13h
- Lieu: Centre Sportif Municipal
- Contact: contact@sportyconnect.com
`;

        // B. Offers
        const offers = await this.offerModel.find({ isActive: true }).exec();
        if (offers.length > 0) {
            const offersText = offers.map(o =>
                `- Offre "${o.name}": ${o.price} TND, durée ${o.durationDays} jours. ${o.description || ''}`
            ).join('\n');
            systemPrompt += `\nOFFRES DISPONIBLES:\n${offersText}\n`;
        }

        // C. Coaches
        const coaches = await this.userModel.find({ role: UserRole.COACH }).exec();
        if (coaches.length > 0) {
            const coachesText = coaches.map(c =>
                `- Coach ${c.prenom} ${c.nom}: Spécialité ${c.specialite || 'Général'}, Exp: ${c.experience || 0} ans.`
            ).join('\n');
            systemPrompt += `\nCOACHS:\n${coachesText}\n`;
        }

        // D. Specific User Context (Subscriptions)
        if (userId) {
            const subs = await this.subModel.find({ parentId: userId }).populate('offerId').exec();
            if (subs.length > 0) {
                const subDetails = subs.map(s => {
                    const offer = s.offerId as unknown as Offer; // Cast for TS
                    return `n- Abonnement à "${offer?.name || 'Inconnu'}" (Statut: ${s.status}, Fin le: ${s.endDate ? s.endDate.toISOString().split('T')[0] : 'N/A'})`;
                }).join('\n');
                systemPrompt += `\nINFO UTILISATEUR ACTUEL (Le parent qui te parle):\n${subDetails}\n`;
            } else {
                systemPrompt += `\nINFO UTILISATEUR: Ce parent n'a pas encore d'abonnement actif.\n`;
            }
        }

        return systemPrompt;
    }

    private fallbackLogic(message: string): string {
        const msg = message.toLowerCase();
        if (msg.includes('bonjour') || msg.includes('salut') || msg.includes('coucou')) {
            return "Bonjour ! Je suis l'assistant de l'académie. Comment puis-je vous aider ?";
        }
        if (msg.includes('horaire') || msg.includes('heure') || msg.includes('quand') || msg.includes('ferme')) {
            return "Nous sommes ouverts du lundi au vendredi de 9h à 18h et le samedi de 9h à 13h.";
        }
        if (msg.includes('tarif') || msg.includes('prix') || msg.includes('combien') || msg.includes('argent') || msg.includes('coût')) {
            return "Nos tarifs varient selon l'offre : Mensuel (50 TND), Trimestriel (135 TND) ou Annuel (500 TND).";
        }
        if (msg.includes('coach') || msg.includes('entraineur') || msg.includes('prof')) {
            return "Nos coachs sont certifiés et experts dans leur domaine. Vous pouvez voir leur profil sur l'application.";
        }
        if (msg.includes('abonnement') || msg.includes('inscrire')) {
            return "Pour vous inscrire, rendez-vous dans la section 'Abonnements' de l'application et choisissez une formule.";
        }
        if (msg.includes('test')) {
            return "Le test fonctionne ! Je vous reçois 5 sur 5.";
        }
        return "Je n'ai pas la réponse, mais vous pouvez appeler le secrétariat pour plus d'infos.";
    }
}
