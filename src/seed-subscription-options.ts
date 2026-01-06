import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import { SubscriptionOptionsService } from './subscription-options/subscription-options.service';

async function seedSubscriptionOptions() {
    console.log('🌱 Affichage des options d\'abonnement...');

    const app = await NestFactory.createApplicationContext(AppModule);
    const subscriptionOptionsService = app.get(SubscriptionOptionsService);

    try {
        console.log('✅ Service d\'options chargé.');

        // Afficher les options créées
        const options = subscriptionOptionsService.getAvailableOptions();
        console.log('\n📋 Options disponibles:');
        options.forEach(option => {
            console.log(`  - ${option.name} (${option.type}): ${option.price} ${option.currency}`);
        });

    } catch (error) {
        console.error('❌ Erreur:', error.message);
    }

    await app.close();
    process.exit(0);
}

seedSubscriptionOptions();
