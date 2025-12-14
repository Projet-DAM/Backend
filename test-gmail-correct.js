const nodemailer = require('nodemailer');
require('dotenv').config();

async function testWithSpaces() {
    console.log('🧪 TEST EMAIL AVEC MOT DE PASSE GMAIL (avec espaces)\n');
    console.log('='.repeat(60));

    // Gmail app passwords have spaces - that's normal!
    const password = process.env.SMTP_PASS;

    console.log('\n📧 Configuration:');
    console.log('   Host:', process.env.SMTP_HOST);
    console.log('   Port:', process.env.SMTP_PORT);
    console.log('   User:', process.env.SMTP_USER);
    console.log('   Pass:', password ? '***SET***' : 'NOT SET');
    console.log('   Pass length:', password ? password.length : 0);
    console.log('   Pass has spaces:', password ? password.includes(' ') : false);

    const transporter = nodemailer.createTransport({
        host: process.env.SMTP_HOST,
        port: parseInt(process.env.SMTP_PORT),
        secure: false,
        auth: {
            user: process.env.SMTP_USER,
            pass: password, // Use password AS IS (with spaces)
        },
        tls: {
            rejectUnauthorized: false,
        },
    });

    try {
        console.log('\n🔌 Test de connexion...');
        await transporter.verify();
        console.log('✅ Connexion réussie!\n');

        console.log('📤 Envoi d\'un email de test...');
        const info = await transporter.sendMail({
            from: process.env.SMTP_FROM || `"Test" <${process.env.SMTP_USER}>`,
            to: process.env.SMTP_USER,
            subject: '✅ Test Email - Configuration Réussie',
            html: `
                <h1 style="color: #4A90E2;">🎉 Ça marche!</h1>
                <p>Votre configuration email fonctionne parfaitement!</p>
                <p><strong>Date:</strong> ${new Date().toLocaleString('fr-FR')}</p>
            `,
        });

        console.log('✅ EMAIL ENVOYÉ!');
        console.log('   Message ID:', info.messageId);
        console.log('   Response:', info.response);

        console.log('\n' + '='.repeat(60));
        console.log('\n🎉 SUCCÈS TOTAL!');
        console.log('\n📋 Prochaines étapes:');
        console.log('   1. Vérifiez votre boîte Gmail:', process.env.SMTP_USER);
        console.log('   2. Redémarrez votre serveur NestJS');
        console.log('   3. Testez l\'inscription depuis votre app');

    } catch (error) {
        console.log('\n❌ ERREUR:', error.message);
        console.log('   Code:', error.code);

        if (error.code === 'EAUTH') {
            console.log('\n⚠️  PROBLÈME D\'AUTHENTIFICATION');
            console.log('\n💡 Solutions possibles:');
            console.log('   1. Vérifiez que la validation en 2 étapes est activée sur Gmail');
            console.log('   2. Générez un NOUVEAU mot de passe d\'application:');
            console.log('      → https://myaccount.google.com/apppasswords');
            console.log('   3. Copiez le nouveau mot de passe (AVEC les espaces)');
            console.log('   4. Mettez à jour SMTP_PASS dans le fichier .env');
            console.log('   5. Relancez ce test');
        } else if (error.code === 'ESOCKET') {
            console.log('\n⚠️  PROBLÈME DE CONNEXION SSL');
            console.log('   La configuration TLS devrait résoudre ce problème...');
        }

        if (error.stack) {
            console.log('\nStack trace:', error.stack);
        }
    }
}

testWithSpaces();
