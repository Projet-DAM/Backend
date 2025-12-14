const nodemailer = require('nodemailer');
require('dotenv').config();

async function testCompleteEmailSetup() {
    console.log('🔍 TEST COMPLET DE LA CONFIGURATION EMAIL\n');
    console.log('='.repeat(60));

    // Step 1: Verify environment variables
    console.log('\n📋 ÉTAPE 1: Vérification des variables d\'environnement\n');

    const config = {
        host: process.env.SMTP_HOST,
        port: process.env.SMTP_PORT,
        user: process.env.SMTP_USER,
        pass: process.env.SMTP_PASS,
        from: process.env.SMTP_FROM,
    };

    console.log('SMTP_HOST:', config.host || '❌ NON DÉFINI');
    console.log('SMTP_PORT:', config.port || '❌ NON DÉFINI');
    console.log('SMTP_USER:', config.user || '❌ NON DÉFINI');
    console.log('SMTP_PASS:', config.pass ? '✅ DÉFINI' : '❌ NON DÉFINI');
    console.log('SMTP_FROM:', config.from || '❌ NON DÉFINI');

    if (config.pass) {
        const hasSpaces = config.pass.includes(' ');
        const length = config.pass.length;
        console.log('\nAnalyse du mot de passe:');
        console.log('  Longueur:', length);
        console.log('  Contient des espaces:', hasSpaces ? '❌ OUI (PROBLÈME!)' : '✅ NON');

        if (hasSpaces) {
            console.log('\n⚠️  ERREUR CRITIQUE: Le mot de passe contient des espaces!');
            console.log('   Mot de passe actuel (masqué):', config.pass.replace(/./g, '*'));
            console.log('   Supprimez tous les espaces du mot de passe dans le fichier .env');
            return;
        }
    }

    if (!config.host || !config.port || !config.user || !config.pass) {
        console.log('\n❌ Configuration incomplète! Vérifiez votre fichier .env');
        return;
    }

    console.log('\n✅ Configuration complète');

    // Step 2: Create transporter
    console.log('\n' + '='.repeat(60));
    console.log('\n📧 ÉTAPE 2: Création du transporter SMTP\n');

    const transporterConfig = {
        host: config.host,
        port: parseInt(config.port),
        secure: false, // true for 465, false for other ports
        auth: {
            user: config.user,
            pass: config.pass,
        },
        tls: {
            rejectUnauthorized: false, // Ignore certificate errors
        },
        debug: true, // Show debug output
        logger: true, // Log information
    };

    console.log('Configuration du transporter:');
    console.log('  Host:', transporterConfig.host);
    console.log('  Port:', transporterConfig.port);
    console.log('  Secure:', transporterConfig.secure);
    console.log('  User:', transporterConfig.auth.user);
    console.log('  TLS rejectUnauthorized:', transporterConfig.tls.rejectUnauthorized);

    const transporter = nodemailer.createTransport(transporterConfig);
    console.log('\n✅ Transporter créé');

    // Step 3: Verify connection
    console.log('\n' + '='.repeat(60));
    console.log('\n🔌 ÉTAPE 3: Vérification de la connexion SMTP\n');

    try {
        console.log('Connexion à', config.host + ':' + config.port, '...');
        await transporter.verify();
        console.log('✅ Connexion SMTP réussie!');
    } catch (error) {
        console.log('❌ Échec de la connexion SMTP');
        console.log('Erreur:', error.message);
        console.log('Code:', error.code);

        if (error.code === 'ESOCKET') {
            console.log('\n💡 Problème de certificat SSL détecté');
            console.log('   Solution: La configuration TLS est déjà définie pour ignorer ce problème');
        } else if (error.code === 'EAUTH') {
            console.log('\n💡 Problème d\'authentification');
            console.log('   Vérifiez:');
            console.log('   1. Le mot de passe d\'application est correct');
            console.log('   2. La validation en 2 étapes est activée sur Gmail');
            console.log('   3. Le mot de passe n\'a pas été révoqué');
        }

        return;
    }

    // Step 4: Send test email
    console.log('\n' + '='.repeat(60));
    console.log('\n📤 ÉTAPE 4: Envoi d\'un email de test\n');

    const mailOptions = {
        from: config.from || `"Test" <${config.user}>`,
        to: config.user, // Send to yourself
        subject: '✅ Test Email - Configuration Réussie',
        html: `
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <h1 style="color: #4A90E2;">🎉 Configuration Email Réussie!</h1>
                <p>Si vous recevez cet email, votre configuration SMTP fonctionne parfaitement!</p>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <h3>Détails de la configuration:</h3>
                    <ul>
                        <li><strong>Host:</strong> ${config.host}</li>
                        <li><strong>Port:</strong> ${config.port}</li>
                        <li><strong>User:</strong> ${config.user}</li>
                        <li><strong>Date:</strong> ${new Date().toLocaleString('fr-FR')}</li>
                    </ul>
                </div>
                <p style="color: #666; font-size: 14px;">
                    Vous pouvez maintenant utiliser cette configuration dans votre application NestJS.
                </p>
            </div>
        `,
        text: `
Configuration Email Réussie!

Si vous recevez cet email, votre configuration SMTP fonctionne parfaitement!

Détails:
- Host: ${config.host}
- Port: ${config.port}
- User: ${config.user}
- Date: ${new Date().toLocaleString('fr-FR')}
        `,
    };

    console.log('Envoi de l\'email à:', mailOptions.to);
    console.log('De:', mailOptions.from);
    console.log('Sujet:', mailOptions.subject);

    try {
        const info = await transporter.sendMail(mailOptions);
        console.log('\n✅ EMAIL ENVOYÉ AVEC SUCCÈS!');
        console.log('Message ID:', info.messageId);
        console.log('Response:', info.response);

        console.log('\n' + '='.repeat(60));
        console.log('\n🎉 SUCCÈS TOTAL!\n');
        console.log('Vérifiez votre boîte de réception:', config.user);
        console.log('L\'email devrait arriver dans quelques secondes.');
        console.log('\n📋 Prochaines étapes:');
        console.log('   1. Vérifiez votre boîte Gmail');
        console.log('   2. Redémarrez votre serveur NestJS (si en cours d\'exécution)');
        console.log('   3. Testez l\'inscription depuis votre application');

    } catch (error) {
        console.log('\n❌ ÉCHEC DE L\'ENVOI');
        console.log('Erreur:', error.message);
        console.log('Code:', error.code);

        if (error.stack) {
            console.log('\nStack trace:');
            console.log(error.stack);
        }
    }
}

testCompleteEmailSetup().catch(console.error);
