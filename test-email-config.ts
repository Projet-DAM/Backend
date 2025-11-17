import * as nodemailer from 'nodemailer';
import * as dotenv from 'dotenv';

dotenv.config();

async function testEmailConfig() {
  console.log('🔍 Vérification de la configuration email...\n');

  // Vérifier les variables d'environnement
  const smtpHost = process.env.SMTP_HOST || 'smtp.gmail.com';
  const smtpPort = parseInt(process.env.SMTP_PORT || '587');
  const smtpUser = process.env.SMTP_USER;
  const smtpPass = process.env.SMTP_PASS;

  console.log('Configuration SMTP:');
  console.log(`  Host: ${smtpHost}`);
  console.log(`  Port: ${smtpPort}`);
  console.log(`  User: ${smtpUser || '❌ NON CONFIGURÉ'}`);
  console.log(`  Pass: ${smtpPass ? '✅ Configuré' : '❌ NON CONFIGURÉ'}\n`);

  if (!smtpUser || !smtpPass) {
    console.log('❌ ERREUR: SMTP_USER et SMTP_PASS doivent être configurés dans le fichier .env');
    console.log('\n📝 Instructions:');
    console.log('1. Ouvrez le fichier .env');
    console.log('2. Remplissez SMTP_USER avec votre adresse email (ex: votre-email@gmail.com)');
    console.log('3. Remplissez SMTP_PASS avec votre mot de passe d\'application Gmail');
    console.log('   (Pour Gmail: https://myaccount.google.com/apppasswords)');
    process.exit(1);
  }

  // Créer le transporteur
  const transporter = nodemailer.createTransport({
    host: smtpHost,
    port: smtpPort,
    secure: false, // true pour 465, false pour les autres ports
    auth: {
      user: smtpUser,
      pass: smtpPass,
    },
  });

  // Tester la connexion
  console.log('🔌 Test de connexion au serveur SMTP...\n');
  
  try {
    await transporter.verify();
    console.log('✅ Connexion SMTP réussie !');
    console.log('✅ Le serveur est prêt à envoyer des emails\n');
    
    // Optionnel: envoyer un email de test
    console.log('📧 Envoi d\'un email de test...\n');
    const info = await transporter.sendMail({
      from: process.env.SMTP_FROM || `"Académie Sportive" <${smtpUser}>`,
      to: smtpUser, // Envoyer à soi-même pour tester
      subject: 'Test de configuration - Académie Sportive',
      text: 'Ceci est un email de test. Si vous recevez ce message, votre configuration email fonctionne correctement !',
      html: `
        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
          <h2 style="color: #1976d2;">Test de configuration</h2>
          <p>Ceci est un email de test.</p>
          <p>Si vous recevez ce message, votre configuration email fonctionne correctement ! ✅</p>
        </div>
      `,
    });

    console.log('✅ Email de test envoyé avec succès !');
    console.log(`   Message ID: ${info.messageId}`);
    console.log(`   Vérifiez votre boîte de réception: ${smtpUser}\n`);
  } catch (error: any) {
    console.error('❌ ERREUR lors du test de connexion:\n');
    console.error(error.message);
    
    if (error.code === 'EAUTH') {
      console.log('\n💡 Solution:');
      console.log('   - Vérifiez que SMTP_USER et SMTP_PASS sont corrects');
      console.log('   - Pour Gmail, utilisez un mot de passe d\'application, pas votre mot de passe normal');
      console.log('   - Générez un mot de passe d\'application: https://myaccount.google.com/apppasswords');
    } else if (error.code === 'ETIMEDOUT' || error.code === 'ECONNREFUSED') {
      console.log('\n💡 Solution:');
      console.log('   - Vérifiez votre connexion internet');
      console.log('   - Vérifiez que le port 587 n\'est pas bloqué par un firewall');
      console.log('   - Vérifiez que SMTP_HOST est correct');
    }
    
    process.exit(1);
  }
}

testEmailConfig().catch(console.error);






