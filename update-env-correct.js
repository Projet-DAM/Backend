const fs = require('fs');
const path = require('path');

console.log('🔧 MISE À JOUR DU .ENV AVEC LA CONFIGURATION CORRECTE\n');

const envPath = path.join(__dirname, '.env');
const backupPath = path.join(__dirname, '.env.backup.correct');

// Configuration correcte fournie par l'utilisateur
const correctConfig = `# Configuration SMTP pour l'envoi d'emails
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=uwoh syqj xcix jgbr
SMTP_FROM="Académie Sportive" <eya.boujnayah2020@gmail.com>
FRONTEND_URL=http://localhost:3000

# Database
DATABASE_URL=mongodb://localhost:27017/academie-sportive

# JWT
JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRES_IN=7d

# Stripe
STRIPE_SECRET_KEY=sk_test_51QTOdyP3bUgGSPBPqOWLJvQjZQMYXqNYfJbVXxKxLpCxvxKxLpCxvxKxLpC
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# Server
PORT=3001
`;

try {
    // Backup current .env
    if (fs.existsSync(envPath)) {
        fs.copyFileSync(envPath, backupPath);
        console.log('✅ Backup créé:', path.basename(backupPath));
    }

    // Write correct configuration
    fs.writeFileSync(envPath, correctConfig, 'utf8');
    console.log('✅ Fichier .env mis à jour avec la configuration correcte\n');

    console.log('📋 Configuration SMTP appliquée:');
    console.log('   SMTP_HOST: smtp.gmail.com');
    console.log('   SMTP_PORT: 587');
    console.log('   SMTP_USER: eya.boujnayah2020@gmail.com');
    console.log('   SMTP_PASS: uwoh syqj xcix jgbr (AVEC espaces - c\'est normal!)');
    console.log('   SMTP_FROM: "Académie Sportive" <eya.boujnayah2020@gmail.com>');

    console.log('\n⚠️  NOTE IMPORTANTE:');
    console.log('   Les espaces dans le mot de passe sont NORMAUX pour Gmail!');
    console.log('   C\'est le format généré par Google.');

} catch (error) {
    console.error('❌ Erreur:', error.message);
}
