const axios = require('axios');

async function testNestJSEmailOn3000() {
    console.log('🧪 TEST EMAIL VIA NESTJS (PORT 3000)\n');
    console.log('='.repeat(60));

    try {
        // Test 1: Check if server is running on port 3000
        console.log('\n1️⃣ Vérification du serveur sur le port 3000...');
        try {
            const healthCheck = await axios.get('http://localhost:3000/', { timeout: 3000 });
            console.log('   ✅ Serveur en cours d\'exécution sur le port 3000!\n');
        } catch (error) {
            console.log('   ❌ Serveur NON accessible sur le port 3000');
            console.log('   Erreur:', error.message);
            console.log('\n   💡 Vérifiez que votre serveur NestJS tourne bien sur le port 3000\n');
            return;
        }

        // Test 2: Try to register a test user (this should send a verification email)
        console.log('2️⃣ Création d\'un utilisateur de test (cela déclenche l\'envoi d\'email)...\n');

        const testUser = {
            email: `test.email.${Date.now()}@example.com`,
            motDePasse: 'TestPassword123!',
            nom: 'Test',
            prenom: 'Email',
            role: 'parent'
        };

        console.log('   Email de test:', testUser.email);
        console.log('   Envoi de la requête d\'inscription...\n');

        try {
            const response = await axios.post('http://localhost:3000/auth/register', testUser, {
                timeout: 10000
            });

            console.log('   ✅ Inscription réussie!');
            console.log('   Réponse:', JSON.stringify(response.data, null, 2));

            console.log('\n' + '='.repeat(60));
            console.log('\n📧 VÉRIFIEZ LES LOGS DU SERVEUR!\n');
            console.log('Dans le terminal où tourne votre serveur NestJS, vous devriez voir:');
            console.log('   📤 Tentative d\'envoi de l\'email à ' + testUser.email);
            console.log('   ✅ Code de vérification envoyé à ' + testUser.email);

            console.log('\n💡 Si vous voyez ces messages, l\'email a été envoyé avec succès!');
            console.log('   Vérifiez votre boîte Gmail:', process.env.SMTP_USER || 'eya.boujnayah2020@gmail.com');

        } catch (error) {
            if (error.response) {
                console.log('   ⚠️ Réponse du serveur:', error.response.status);
                console.log('   Message:', JSON.stringify(error.response.data, null, 2));

                if (error.response.status === 400 && error.response.data.message?.includes('existe déjà')) {
                    console.log('\n   💡 L\'utilisateur existe déjà, c\'est normal si vous avez déjà testé.');
                    console.log('      Essayez de vous connecter ou utilisez un autre email.');
                }
            } else {
                console.log('   ❌ Erreur:', error.message);
            }
        }

    } catch (error) {
        console.error('\n❌ Erreur inattendue:', error.message);
    }
}

console.log('📧 Test d\'envoi d\'email via NestJS sur le port 3000\n');
testNestJSEmailOn3000();
