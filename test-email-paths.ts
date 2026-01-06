import * as path from 'path';
import * as fs from 'fs';

console.log('🔍 Vérification des chemins des templates email...\n');

// Vérifier le chemin de travail actuel
console.log(`📂 Répertoire de travail: ${process.cwd()}\n`);

// Chemins à vérifier
const pathsToCheck = [
    'src/common/templates/emails/abonnement-confirmation.html',
    'src/common/templates/emails/abonnement-expire-7j.html',
    'src/common/templates/emails/logo.png',
];

let allExist = true;

pathsToCheck.forEach((relativePath) => {
    const fullPath = path.join(process.cwd(), relativePath);
    const exists = fs.existsSync(fullPath);

    if (exists) {
        const stats = fs.statSync(fullPath);
        console.log(`✅ ${relativePath}`);
        console.log(`   Chemin complet: ${fullPath}`);
        console.log(`   Taille: ${(stats.size / 1024).toFixed(2)} KB\n`);
    } else {
        console.log(`❌ ${relativePath}`);
        console.log(`   Chemin complet: ${fullPath}`);
        console.log(`   FICHIER NON TROUVÉ!\n`);
        allExist = false;
    }
});

if (allExist) {
    console.log('✅ Tous les templates sont accessibles!');
    console.log('✅ L\'envoi d\'email devrait fonctionner correctement.');
} else {
    console.log('❌ Certains templates sont manquants!');
    console.log('❌ L\'envoi d\'email échouera.');
    process.exit(1);
}
