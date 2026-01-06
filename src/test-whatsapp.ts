import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { PaymentsService } from './payments/payments.service';
import { TwilioService } from './payments/twilio.service';
import * as dotenv from 'dotenv';

/**
 * Script de test pour vérifier l'envoi de messages WhatsApp
 * 
 * Usage: npm run test:whatsapp
 */
async function testWhatsApp() {
    console.log('🚀 Démarrage du test WhatsApp...\n');

    const app = await NestFactory.createApplicationContext(AppModule);

    try {
        console.log('📧 Envoi d\'un message de test WhatsApp DIRECT...');
        const phoneNumber = process.env.DEFAULT_PHONE_NUMBER || '+21627863334';

        // Appel direct au service Twilio
        const twilioService = app.get(TwilioService);
        await twilioService.sendPaymentConfirmation(phoneNumber, 5000, 'eur');

        console.log('\n✅ Message envoyé !');
        console.log('Si vous ne le recevez pas, c\'est que vous n\'avez pas rejoint le sandbox.');
        console.log('Envoyez "join <code-sandbox>" au +1 415 523 8886');
    } catch (error) {
        console.error('\n❌ Erreur lors du test:', error.message);
        if (error.code === 21608) {
            console.error('⚠️ Ce numéro n\'est pas vérifié ou n\'a pas rejoint le sandbox !');
        }
    } finally {
        await app.close();
    }
}

testWhatsApp();
