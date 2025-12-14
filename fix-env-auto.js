const fs = require('fs');
const path = require('path');

console.log('🔧 CORRECTION AUTOMATIQUE DU FICHIER .ENV\n');

const envPath = path.join(__dirname, '.env');
const backupPath = path.join(__dirname, '.env.backup');

try {
    // Read current .env
    let envContent = fs.readFileSync(envPath, 'utf8');

    console.log('📄 Contenu actuel du .env (SMTP uniquement):');
    const currentLines = envContent.split('\n').filter(l => l.startsWith('SMTP_'));
    currentLines.forEach(line => {
        if (line.includes('SMTP_PASS')) {
            console.log('   SMTP_PASS=***HIDDEN***');
        } else {
            console.log('  ', line);
        }
    });
    console.log('');

    // Create backup
    fs.copyFileSync(envPath, backupPath);
    console.log('✅ Backup créé: .env.backup\n');

    // Fix SMTP_PASS - remove all spaces
    const originalContent = envContent;

    // Replace password with spaces to password without spaces
    envContent = envContent.replace(
        /SMTP_PASS=uwoh syqj xcix jgbr/g,
        'SMTP_PASS=uwohsyqjxcixjgbr'
    );

    // Also fix any other format with spaces
    envContent = envContent.replace(
        /SMTP_PASS=([a-z\s]+)/g,
        (match, p1) => {
            const cleaned = p1.replace(/\s/g, '');
            return `SMTP_PASS=${cleaned}`;
        }
    );

    // Fix SMTP_FROM - remove special characters
    envContent = envContent.replace(
        /SMTP_FROM="Acad[éè]mie Sportive"/g,
        'SMTP_FROM="Academie Sportive"'
    );

    if (envContent === originalContent) {
        console.log('⚠️ Aucune modification nécessaire (le fichier semble déjà correct)');
        console.log('   Si le problème persiste, vérifiez manuellement le fichier .env\n');
    } else {
        // Write fixed content
        fs.writeFileSync(envPath, envContent, 'utf8');
        console.log('✅ Fichier .env corrigé !\n');

        console.log('📄 Nouveau contenu (SMTP uniquement):');
        const newLines = envContent.split('\n').filter(l => l.startsWith('SMTP_'));
        newLines.forEach(line => {
            if (line.includes('SMTP_PASS')) {
                const value = line.split('=')[1];
                const hasSpaces = value && value.includes(' ');
                if (hasSpaces) {
                    console.log('   ❌ SMTP_PASS=***CONTIENT ENCORE DES ESPACES***');
                } else {
                    console.log('   ✅ SMTP_PASS=***CORRIGÉ (sans espaces)***');
                }
            } else {
                console.log('  ', line);
            }
        });
        console.log('');
    }

    console.log('📋 PROCHAINES ÉTAPES :');
    console.log('   1. Vérifiez le fichier .env (il devrait être corrigé)');
    console.log('   2. Si le serveur NestJS tourne, redémarrez-le (Ctrl+C puis npm run start:dev)');
    console.log('   3. Testez avec: node test-nestjs-email.js');
    console.log('');
    console.log('💡 Si vous voulez restaurer l\'ancien fichier : copy .env.backup .env');

} catch (error) {
    console.error('❌ Erreur:', error.message);
}
