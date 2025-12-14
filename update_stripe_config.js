const fs = require('fs');
const path = require('path');

const envPath = path.join(__dirname, '.env');

console.log('🔧 Mise à jour de la configuration Stripe...\n');

try {
    let envContent = fs.readFileSync(envPath, 'utf8');

    // Nouvelles clés
    const newSecretKey = 'sk_test_51SV9DGHQ95HR9DAI2oVkJgxhwzJGTIaspfxrziZUsglPtwGuBmOXdBEhQj7X3nZSoJhLNRI0B9hrX23IiOiBBUel00KvsbelPY';
    const newPublishableKey = 'pk_test_51SV9DGHQ95HR9DAIM06ZwtZcdrI4VWkwDvZ5SToJa1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5ImqCVBk6W00VGzt9lMG';
    const newWebhookSecret = 'whsec_test_placeholder';

    // Remplacer STRIPE_SECRET_KEY
    if (envContent.includes('STRIPE_SECRET_KEY=')) {
        envContent = envContent.replace(/STRIPE_SECRET_KEY=.*/g, `STRIPE_SECRET_KEY=${newSecretKey}`);
    } else {
        envContent += `\nSTRIPE_SECRET_KEY=${newSecretKey}`;
    }

    // Remplacer ou Ajouter STRIPE_PUBLISHABLE_KEY
    if (envContent.includes('STRIPE_PUBLISHABLE_KEY=')) {
        envContent = envContent.replace(/STRIPE_PUBLISHABLE_KEY=.*/g, `STRIPE_PUBLISHABLE_KEY=${newPublishableKey}`);
    } else {
        // On l'ajoute après la secret key pour grouper
        envContent = envContent.replace(`STRIPE_SECRET_KEY=${newSecretKey}`, `STRIPE_SECRET_KEY=${newSecretKey}\nSTRIPE_PUBLISHABLE_KEY=${newPublishableKey}`);
    }

    // Remplacer STRIPE_WEBHOOK_SECRET
    if (envContent.includes('STRIPE_WEBHOOK_SECRET=')) {
        envContent = envContent.replace(/STRIPE_WEBHOOK_SECRET=.*/g, `STRIPE_WEBHOOK_SECRET=${newWebhookSecret}`);
    } else {
        envContent += `\nSTRIPE_WEBHOOK_SECRET=${newWebhookSecret}`;
    }

    fs.writeFileSync(envPath, envContent, 'utf8');
    console.log('✅ Fichier .env mis à jour avec succès !');
    console.log('   STRIPE_SECRET_KEY mis à jour');
    console.log('   STRIPE_PUBLISHABLE_KEY ajouté/mis à jour');
    console.log('   STRIPE_WEBHOOK_SECRET mis à jour');

    console.log('\n⚠️ IMPORTANT : Vous devez redémarrer votre serveur NestJS pour prendre en compte ces changements.');

} catch (error) {
    console.error('❌ Erreur lors de la mise à jour du .env:', error.message);
}
