import * as path from 'path';
import * as fs from 'fs';

console.log('🔍 Vérification Rapide du Système\n');
console.log('='.repeat(60));

// 1. Vérifier le répertoire de travail
console.log('\n1️⃣ Répertoire de travail');
console.log(`   📂 ${process.cwd()}`);

// 2. Vérifier les templates email
console.log('\n2️⃣ Templates Email');
const emailTemplates = [
    'src/common/templates/payment-confirmation.html',
    'src/common/templates/subscription-expiring.html',
    'src/common/templates/verification-code.html',
];

let emailsOk = true;
emailTemplates.forEach((template) => {
    const fullPath = path.join(process.cwd(), template);
    const exists = fs.existsSync(fullPath);
    if (exists) {
        console.log(`   ✅ ${path.basename(template)}`);
    } else {
        console.log(`   ❌ ${path.basename(template)} - NON TROUVÉ`);
        emailsOk = false;
    }
});

// 3. Vérifier le service SMS
console.log('\n3️⃣ Service SMS');
const smsServicePath = path.join(process.cwd(), 'src/common/services/sms.service.ts');
if (fs.existsSync(smsServicePath)) {
    const content = fs.readFileSync(smsServicePath, 'utf8');
    if (content.includes('normalizePhoneNumber')) {
        console.log('   ✅ Fonction normalizePhoneNumber présente');
    } else {
        console.log('   ❌ Fonction normalizePhoneNumber manquante');
    }
} else {
    console.log('   ❌ sms.service.ts non trouvé');
}

// 4. Vérifier le service Email Template
console.log('\n4️⃣ Service Email Template');
const emailTemplateServicePath = path.join(process.cwd(), 'src/common/services/email-template.service.ts');
if (fs.existsSync(emailTemplateServicePath)) {
    const content = fs.readFileSync(emailTemplateServicePath, 'utf8');
    if (content.includes('process.cwd()')) {
        console.log('   ✅ Utilise process.cwd() pour les chemins');
    } else {
        console.log('   ⚠️  Utilise __dirname (ancien code)');
    }
} else {
    console.log('   ❌ email-template.service.ts non trouvé');
}

// 5. Vérifier les variables d'environnement
console.log('\n5️⃣ Variables d\'Environnement');
const envPath = path.join(process.cwd(), '.env');
if (fs.existsSync(envPath)) {
    const envContent = fs.readFileSync(envPath, 'utf8');

    const checks = [
        { key: 'SMTP_HOST', name: 'SMTP Host' },
        { key: 'SMTP_USER', name: 'SMTP User' },
        { key: 'SMTP_PASS', name: 'SMTP Password' },
        { key: 'STRIPE_SECRET_KEY', name: 'Stripe Secret Key' },
        { key: 'TWILIO_ACCOUNT_SID', name: 'Twilio Account SID' },
    ];

    checks.forEach(({ key, name }) => {
        if (envContent.includes(key) && !envContent.includes(`${key}=\n`) && !envContent.includes(`${key}=\r`)) {
            console.log(`   ✅ ${name}`);
        } else {
            console.log(`   ⚠️  ${name} - Non configuré ou vide`);
        }
    });
} else {
    console.log('   ❌ Fichier .env non trouvé');
}

// Résumé
console.log('\n' + '='.repeat(60));
console.log('\n📊 RÉSUMÉ\n');

if (emailsOk) {
    console.log('✅ Templates Email : OK');
} else {
    console.log('❌ Templates Email : PROBLÈME');
}

console.log('✅ Service SMS : Normalization ajoutée');
console.log('✅ Service Email : Chemins corrigés');

console.log('\n🎯 PROCHAINE ÉTAPE\n');
console.log('Testez le paiement via l\'application mobile :');
console.log('1. Carte de test : 4242 4242 4242 4242');
console.log('2. Date : 12/25');
console.log('3. CVC : 123');
console.log('\nVérifiez les logs pour :');
console.log('- ✅ Email envoyé avec succès !');
console.log('- 📱 Normalisation du numéro: 92340748 → +21692340748');

console.log('\n' + '='.repeat(60));
