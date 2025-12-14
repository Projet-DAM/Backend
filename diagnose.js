const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('🔍 DIAGNOSTIC COMPLET DES EMAILS\n');
console.log('='.repeat(50));

// 1. Check .env file
console.log('\n1️⃣ Vérification du fichier .env...');
const envPath = path.join(__dirname, '.env');

if (!fs.existsSync(envPath)) {
    console.log('   ❌ Fichier .env introuvable !');
} else {
    console.log('   ✅ Fichier .env trouvé');

    const envContent = fs.readFileSync(envPath, 'utf8');
    const lines = envContent.split('\n');

    // Check SMTP variables
    const smtpVars = ['SMTP_HOST', 'SMTP_PORT', 'SMTP_USER', 'SMTP_PASS', 'SMTP_FROM'];
    let allPresent = true;

    smtpVars.forEach(varName => {
        const line = lines.find(l => l.startsWith(varName + '='));
        if (line) {
            if (varName === 'SMTP_PASS') {
                const value = line.split('=')[1];
                if (value && value.trim()) {
                    // Check for spaces in password
                    const hasSpaces = value.includes(' ');
                    if (hasSpaces) {
                        console.log(`   ❌ ${varName}: CONTIENT DES ESPACES ! (doit être sans espaces)`);
                        allPresent = false;
                    } else {
                        console.log(`   ✅ ${varName}: ***SET*** (sans espaces)`);
                    }
                } else {
                    console.log(`   ❌ ${varName}: VIDE`);
                    allPresent = false;
                }
            } else {
                const value = line.split('=')[1];
                console.log(`   ✅ ${varName}: ${value || 'VIDE'}`);
            }
        } else {
            console.log(`   ❌ ${varName}: NON DÉFINI`);
            allPresent = false;
        }
    });

    if (!allPresent) {
        console.log('\n   ⚠️ Configuration .env incomplète ou incorrecte !');
    }
}

// 2. Check if server is running
console.log('\n2️⃣ Vérification du serveur NestJS...');
try {
    const result = execSync('netstat -ano | findstr :3001', { encoding: 'utf8' });
    if (result) {
        console.log('   ✅ Serveur en cours d\'exécution sur le port 3001');
    }
} catch (error) {
    console.log('   ❌ Serveur NestJS NON démarré sur le port 3001');
    console.log('   👉 Démarrez-le avec: npm run start:dev');
}

// 3. Test SMTP connection
console.log('\n3️⃣ Test de connexion SMTP...');
console.log('   Exécution de quick-test.js...\n');

try {
    const testResult = execSync('node quick-test.js', {
        encoding: 'utf8',
        cwd: __dirname,
        timeout: 10000
    });

    if (testResult.includes('✅ Email sent!')) {
        console.log('   ✅ Test SMTP réussi ! La configuration fonctionne.');
    } else if (testResult.includes('✅ Connection OK!')) {
        console.log('   ✅ Connexion SMTP OK');
    } else {
        console.log('   ⚠️ Résultat du test :');
        console.log(testResult);
    }
} catch (error) {
    console.log('   ❌ Test SMTP échoué');
    if (error.stdout) {
        console.log('   Sortie:', error.stdout.toString());
    }
    if (error.stderr) {
        console.log('   Erreur:', error.stderr.toString());
    }
}

// Summary
console.log('\n' + '='.repeat(50));
console.log('\n📋 RÉSUMÉ ET ACTIONS À FAIRE :\n');

const envExists = fs.existsSync(envPath);
let hasSpacesInPass = false;

if (envExists) {
    const envContent = fs.readFileSync(envPath, 'utf8');
    const passLine = envContent.split('\n').find(l => l.startsWith('SMTP_PASS='));
    if (passLine) {
        const value = passLine.split('=')[1];
        hasSpacesInPass = value && value.includes(' ');
    }
}

if (hasSpacesInPass) {
    console.log('❌ PROBLÈME CRITIQUE : Le mot de passe SMTP contient des espaces !');
    console.log('   👉 Modifiez .env et remplacez :');
    console.log('      SMTP_PASS=uwoh syqj xcix jgbr');
    console.log('      par :');
    console.log('      SMTP_PASS=uwohsyqjxcixjgbr');
    console.log('   👉 Puis redémarrez le serveur\n');
}

try {
    execSync('netstat -ano | findstr :3001', { encoding: 'utf8' });
} catch (error) {
    console.log('❌ Le serveur NestJS n\'est pas démarré !');
    console.log('   👉 Démarrez-le avec : npm run start:dev\n');
}

console.log('📖 Pour plus de détails, consultez : START_AND_TEST_GUIDE.md');
console.log('');
