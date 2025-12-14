const fs = require('fs');
const path = require('path');

console.log('🔧 CORRECTION FORCÉE DU FICHIER .ENV\n');
console.log('='.repeat(60));

const envPath = path.join(__dirname, '.env');
const backupPath = path.join(__dirname, '.env.backup.' + Date.now());

try {
    // Read current .env
    const envContent = fs.readFileSync(envPath, 'utf8');

    console.log('\n📄 Contenu actuel (lignes SMTP):');
    const lines = envContent.split('\n');
    lines.forEach((line, index) => {
        if (line.startsWith('SMTP_')) {
            if (line.startsWith('SMTP_PASS=')) {
                const value = line.substring('SMTP_PASS='.length);
                const hasSpaces = value.includes(' ');
                console.log(`Ligne ${index + 1}: SMTP_PASS=${hasSpaces ? '***AVEC ESPACES***' : '***OK***'}`);
                if (hasSpaces) {
                    console.log(`   Valeur actuelle (masquée): ${value.replace(/./g, '*')}`);
                    console.log(`   Longueur: ${value.length} caractères`);
                }
            } else {
                console.log(`Ligne ${index + 1}: ${line}`);
            }
        }
    });

    // Create backup
    fs.copyFileSync(envPath, backupPath);
    console.log(`\n✅ Backup créé: ${path.basename(backupPath)}`);

    // Build new .env content with correct values
    const newLines = lines.map(line => {
        // Fix SMTP_PASS - remove ALL spaces
        if (line.startsWith('SMTP_PASS=')) {
            const value = line.substring('SMTP_PASS='.length);
            const cleaned = value.replace(/\s/g, ''); // Remove ALL whitespace
            console.log(`\n🔧 Correction de SMTP_PASS:`);
            console.log(`   Avant: ${value.length} caractères (avec espaces: ${value.includes(' ')})`);
            console.log(`   Après: ${cleaned.length} caractères (sans espaces)`);
            return `SMTP_PASS=${cleaned}`;
        }

        // Fix SMTP_FROM - remove special characters
        if (line.startsWith('SMTP_FROM=')) {
            // Replace any accented characters
            const fixed = line
                .replace(/Académie/g, 'Academie')
                .replace(/Acad�mie/g, 'Academie')
                .replace(/Acad[éè]mie/g, 'Academie');
            if (fixed !== line) {
                console.log(`\n🔧 Correction de SMTP_FROM:`);
                console.log(`   Avant: ${line}`);
                console.log(`   Après: ${fixed}`);
            }
            return fixed;
        }

        return line;
    });

    const newContent = newLines.join('\n');

    // Write new content
    fs.writeFileSync(envPath, newContent, 'utf8');

    console.log('\n✅ Fichier .env mis à jour!');

    // Verify the fix
    console.log('\n📄 Nouveau contenu (lignes SMTP):');
    const verifyContent = fs.readFileSync(envPath, 'utf8');
    const verifyLines = verifyContent.split('\n');

    let allGood = true;
    verifyLines.forEach((line, index) => {
        if (line.startsWith('SMTP_')) {
            if (line.startsWith('SMTP_PASS=')) {
                const value = line.substring('SMTP_PASS='.length);
                const hasSpaces = value.includes(' ');
                if (hasSpaces) {
                    console.log(`Ligne ${index + 1}: ❌ SMTP_PASS=***CONTIENT ENCORE DES ESPACES***`);
                    allGood = false;
                } else {
                    console.log(`Ligne ${index + 1}: ✅ SMTP_PASS=***OK (${value.length} caractères, sans espaces)***`);
                }
            } else {
                console.log(`Ligne ${index + 1}: ${line}`);
            }
        }
    });

    console.log('\n' + '='.repeat(60));

    if (allGood) {
        console.log('\n✅ SUCCÈS! Le fichier .env est maintenant correct!');
        console.log('\n📋 Prochaines étapes:');
        console.log('   1. Testez avec: node test-email-complete.js');
        console.log('   2. Si le serveur NestJS tourne, redémarrez-le');
        console.log('   3. Testez l\'inscription depuis votre application');
    } else {
        console.log('\n❌ PROBLÈME: Le mot de passe contient encore des espaces!');
        console.log('\n🔧 Solution manuelle:');
        console.log('   1. Ouvrez le fichier .env');
        console.log('   2. Trouvez la ligne SMTP_PASS=...');
        console.log('   3. Remplacez par: SMTP_PASS=uwohsyqjxcixjgbr');
        console.log('   4. Sauvegardez le fichier');
    }

} catch (error) {
    console.error('\n❌ Erreur:', error.message);
    console.error(error.stack);
}
