import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Twilio } from 'twilio';

@Injectable()
export class SmsService {
    private readonly logger = new Logger(SmsService.name);
    private twilioClient: Twilio | null = null;
    private twilioPhoneNumber: string;

    constructor(private configService: ConfigService) {
        const accountSid = this.configService.get<string>('TWILIO_ACCOUNT_SID');
        const authToken = this.configService.get<string>('TWILIO_AUTH_TOKEN');
        this.twilioPhoneNumber = this.configService.get<string>('TWILIO_PHONE_NUMBER') || '';

        if (!accountSid || !authToken) {
            this.logger.warn('⚠️ Twilio credentials not configured. SMS features will not work.');
            return;
        }

        this.twilioClient = new Twilio(accountSid, authToken);
        this.logger.log('✅ Twilio client initialized successfully');
    }

    /**
     * Normalise un numéro de téléphone au format E.164 international
     * Exemples:
     * - "92340748" → "+21692340748" (Tunisie)
     * - "21692340748" → "+21692340748"
     * - "+21692340748" → "+21692340748"
     */
    private normalizePhoneNumber(phoneNumber: string): string {
        // Supprimer tous les espaces, tirets, parenthèses
        let cleaned = phoneNumber.replace(/[\s\-\(\)]/g, '');

        // Si le numéro commence déjà par +, le retourner tel quel
        if (cleaned.startsWith('+')) {
            return cleaned;
        }

        // Si le numéro commence par 216 (code pays Tunisie), ajouter +
        if (cleaned.startsWith('216')) {
            return `+${cleaned}`;
        }

        // Si le numéro est un numéro local tunisien (8 chiffres commençant par 2, 4, 5, 7, 9)
        // Ajouter le code pays +216
        if (/^[24579]\d{7}$/.test(cleaned)) {
            return `+216${cleaned}`;
        }

        // Si aucun format reconnu, retourner le numéro nettoyé avec +216 par défaut
        this.logger.warn(`⚠️ Format de numéro non reconnu: ${phoneNumber}. Ajout du code pays +216 par défaut.`);
        return `+216${cleaned}`;
    }

    async sendSms(to: string, message: string): Promise<boolean> {
        if (!this.twilioClient) {
            this.logger.warn('⚠️ Twilio not configured. Cannot send SMS.');
            return false;
        }

        try {
            // Normaliser le numéro au format E.164
            const normalizedTo = this.normalizePhoneNumber(to);
            this.logger.log(`📱 Normalisation du numéro: ${to} → ${normalizedTo}`);

            const result = await this.twilioClient.messages.create({
                body: message,
                from: this.twilioPhoneNumber,
                to: normalizedTo,
            });

            this.logger.log(`✅ SMS sent successfully to ${normalizedTo}. SID: ${result.sid}`);
            return true;
        } catch (error: any) {
            this.logger.error(`❌ Error sending SMS to ${to}: ${error.message}`);
            return false;
        }
    }

    async sendWhatsApp(to: string, message: string): Promise<boolean> {
        if (!this.twilioClient) {
            this.logger.warn('⚠️ Twilio not configured. Cannot send WhatsApp.');
            return false;
        }

        try {
            // Normaliser le numéro au format E.164
            const normalizedTo = this.normalizePhoneNumber(to);
            this.logger.log(`📱 Normalisation du numéro WhatsApp: ${to} → ${normalizedTo}`);

            const formattedTo = normalizedTo.startsWith('whatsapp:') ? normalizedTo : `whatsapp:${normalizedTo}`;
            // Utiliser le numéro sandbox par défaut ou une variable d'env
            const from = this.configService.get<string>('TWILIO_WHATSAPP_NUMBER') || 'whatsapp:+14155238886';

            const result = await this.twilioClient.messages.create({
                body: message,
                from: from,
                to: formattedTo,
            });

            this.logger.log(`✅ WhatsApp message sent successfully to ${normalizedTo}. SID: ${result.sid}`);
            return true;
        } catch (error: any) {
            this.logger.error(`❌ Error sending WhatsApp to ${to}: ${error.message}`);
            return false;
        }
    }
}
