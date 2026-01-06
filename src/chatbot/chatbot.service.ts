import { Injectable, Logger, Inject, forwardRef } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { OpenAI } from 'openai';
import { Offer, OfferDocument } from '../offers/schemas/offer.schema';
import { Subscription, SubscriptionDocument } from '../subscriptions/schemas/subscription.schema';
import { User } from '../users/entity/user.entity';
import { UserRole } from '../users/interfaces/user-role.enum';

@Injectable()
export class ChatbotService {
    private readonly logger = new Logger(ChatbotService.name);
    private openai: OpenAI | null = null;

    constructor(
        private configService: ConfigService,
        @InjectModel(Offer.name) private offerModel: Model<OfferDocument>,
        @InjectModel(Subscription.name) private subscriptionModel: Model<SubscriptionDocument>,
        @InjectModel(User.name) private userModel: Model<any>,
    ) {
        const openaiKey = this.configService.get<string>('OPENAI_API_KEY');

        if (openaiKey) {
            this.openai = new OpenAI({ apiKey: openaiKey });
            this.logger.log('🚀 Chatbot initialized with OpenAI');
        } else {
            this.logger.warn('⚠️ OPENAI_API_KEY not found in .env. Fallback mode active.');
        }
    }

    async processMessage(userMessage: string, userId?: string): Promise<string> {
        this.logger.log(`🎙️ Voice Chatbot Input: "${userMessage}"`);

        const context = await this.buildContext(userId);

        if (this.openai) {
            try {
                this.logger.log('📡 Requesting OpenAI (gpt-4o-mini)...');
                const completion = await this.openai.chat.completions.create({
                    messages: [
                        { role: 'system', content: context + "\nREGLE CRUCIALE : Réponds SANS AUCUN caractère spécial de formatage (pas de **, pas de #). Ta réponse sera lue à haute voix par une synthèse vocale. Sois très bref." },
                        { role: 'user', content: userMessage },
                    ],
                    model: 'gpt-4o-mini',
                    max_tokens: 150, // Plus court pour la voix
                    temperature: 0.7,
                });

                let reply = completion.choices[0]?.message?.content || "";

                // NETTOYAGE POUR LA VOIX (Supprime les étoiles, hashtags, etc. qui font crasher les TTS mobiles)
                reply = reply.replace(/[\*#_]/g, '').trim();

                this.logger.log(`✅ AI Response: "${reply.substring(0, 50)}..."`);
                return reply;
            } catch (error: any) {
                this.logger.error(`❌ OpenAI Error: ${error.message}`);
            }
        }

        return this.fallbackLogic(userMessage);
    }

    private async buildContext(userId?: string): Promise<string> {
        const offers = await this.offerModel.find({ isActive: true }).exec();
        const coaches = await this.userModel.find({ role: UserRole.COACH }).select('prenom nom specialite').exec();

        let prompt = `Tu es l'assistant intelligent de SportyConnect Academy. 
TA MISSION : Analyser la question de l'utilisateur et y répondre précisément avec les données réelles du club.
IMPORTANT : Pour rendre un lien cliquable, utilise EXCLUSIVEMENT ce format HTML : <a href="sportyconnect://chemin">Libellé</a>

DONNÉES ACADÉMIE :
- Nom : SportyConnect Academy
- Horaires : Lundi au Samedi, 08:00 - 18:00
- Lieu : Tunis
- Contact : +216 71 000 000

OFFRES ET TARIFS RÉELS :
${offers.map(o => `- ${o.name} : ${o.price} TND. ${o.description || ''}`).join('\n')}

COACHS :
${coaches.map(c => `- Coach ${c.prenom} ${c.nom} ${c.specialite ? '(' + c.specialite + ')' : ''}`).join('\n')}

LIENS DE NAVIGATION :
- Offres : <a href="sportyconnect://offers">Voir les offres</a>
- Abonnements : <a href="sportyconnect://subscriptions">Mes abonnements</a>
- Suivi enfant : <a href="sportyconnect://followup">Suivi de progression</a>
- Équipe : <a href="sportyconnect://team">Nos coachs</a>
`;

        if (userId) {
            try {
                const user = await this.userModel.findById(userId);
                if (user) {
                    const activeSub = await this.subscriptionModel.findOne({ parentId: userId, status: 'ACTIVE' }).populate('offerId');
                    prompt += `\nINFO UTILISATEUR : Client : ${user.prenom} ${user.nom}.`;
                    if (activeSub) {
                        prompt += ` Abonnement actif : "${(activeSub.offerId as any)?.name}". Lien : <a href="sportyconnect://subscriptions">Gérer mon abonnement</a>`;
                    }
                }
            } catch (e) { }
        }

        prompt += `\n\nCONSIGNE : 
1. Réponds directement à la question sans répéter tout le contexte.
2. Inclus TOUJOURS le lien HTML <a href="...">...</a> pertinent.
3. Sois très précis sur les tarifs.
4. Réponds en 3-4 phrases maximum.`;

        return prompt;
    }

    private fallbackLogic(message: string): string {
        const lowerMsg = message.toLowerCase();

        if (lowerMsg.includes('horaire') || lowerMsg.includes('ouvert')) {
            return `L'académie est ouverte du lundi au samedi, de 8h à 18h. Voir détails dans <a href="sportyconnect://profile">votre profil</a>.`;
        }

        if (lowerMsg.includes('prix') || lowerMsg.includes('tarif') || lowerMsg.includes('coût')) {
            return `Nos tarifs dépendent de l'offre choisie. Consultez les détails complets ici : <a href="sportyconnect://offers">Voir les offres</a>.`;
        }

        if (lowerMsg.includes('coach') || lowerMsg.includes('entraineur')) {
            return `Notre équipe de coachs est disponible pour encadrer vos enfants. Découvrez-les ici : <a href="sportyconnect://team">Nos coachs</a>.`;
        }

        if (lowerMsg.includes('abonnement') || lowerMsg.includes('souscrire') || lowerMsg.includes('mon compte')) {
            return `Gérez vos abonnements et factures dans votre espace dédié : <a href="sportyconnect://subscriptions">Mes abonnements</a>.`;
        }

        if (lowerMsg.includes('enfant') || lowerMsg.includes('suivi') || lowerMsg.includes('progression')) {
            return `Le suivi de progression de vos enfants est disponible ici : <a href="sportyconnect://followup">Suivi de progression</a>.`;
        }

        return `Je suis désolé, je ne parviens pas à traiter votre demande précisément. Vous pouvez accéder directement aux <a href="sportyconnect://offers">offres</a> ou à vos <a href="sportyconnect://subscriptions">abonnements</a>.`;
    }
}
