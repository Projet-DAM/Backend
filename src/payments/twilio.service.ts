import { Injectable } from '@nestjs/common';
import twilio from 'twilio';

@Injectable()
export class TwilioService {
    private twilioClient: twilio.Twilio;
    private twilioWhatsAppNumber: string;

    constructor() {
        const accountSid = process.env.TWILIO_ACCOUNT_SID;
        const authToken = process.env.TWILIO_AUTH_TOKEN;
        this.twilioWhatsAppNumber = process.env.TWILIO_WHATSAPP_NUMBER || 'whatsapp:+14155238886';

        if (!accountSid || !authToken) {
            console.warn('⚠️ Twilio credentials missing (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN). WhatsApp notifications will be disabled.');
            return;
        }

        try {
            this.twilioClient = twilio(accountSid, authToken);
        } catch (error) {
            console.error('❌ Error initializing Twilio client:', error.message);
        }
    }

    /**
     * Envoie un message WhatsApp de confirmation de paiement
     * @param to Numéro de téléphone du destinataire (format: +21627863334)
     * @param amount Montant payé
     * @param currency Devise (par défaut EUR)
     */
    async sendPaymentConfirmation(to: string, amount: number, currency: string = 'EUR'): Promise<void> {
        const formattedAmount = (amount / 100).toFixed(2); // Stripe utilise les centimes
        const message = `🎉 Confirmation de paiement\n\nVotre paiement de ${formattedAmount} ${currency.toUpperCase()} a été effectué avec succès !\n\nMerci pour votre confiance. 💚\n\n- SportyConnect`;

        console.log('📱 [WHATSAPP DEBUG] Tentative d\'envoi:');
        console.log('   - De (From):', this.twilioWhatsAppNumber);
        console.log('   - À (To):', `whatsapp:${to}`);
        console.log('   - Message:', message);

        if (!this.twilioClient) {
            console.warn('⚠️ Twilio client not initialized. Skipping WhatsApp message.');
            return;
        }

        try {
            const messageResponse = await this.twilioClient.messages.create({
                body: message,
                from: this.twilioWhatsAppNumber,
                to: `whatsapp:${to}`,
            });

            console.log(`✅ Message WhatsApp envoyé avec succès: ${messageResponse.sid}`);
            console.log('   - Statut:', messageResponse.status);
            console.log('   - Direction:', messageResponse.direction);
        } catch (error) {
            console.error('❌ Erreur lors de l\'envoi du message WhatsApp:', error);
            if (error.code) console.error('   - Code erreur:', error.code);
            if (error.message) console.error('   - Message erreur:', error.message);
            throw error;
        }
    }
}
