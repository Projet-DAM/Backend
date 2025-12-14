import * as dotenv from 'dotenv';
import * as nodemailer from 'nodemailer';

// Charger les variables d'environnement
dotenv.config();

async function testEmailDirect() {
  console.log('\n🔍 === TEST DIRECT D\'ENVOI D\'EMAIL ===\n');
  
  // Vérifier les variables d'environnement
  const smtpPass = process.env.SMTP_PASS;
  const smtpUser = process.env.SMTP_USER;
  const smtpHost = process.env.SMTP_HOST || 'smtp.gmail.com';
  const smtpPort = parseInt(process.env.SMTP_PORT || '587');
  
  console.log('📋 Configuration détectée:');
  console.log(`   SMTP_HOST: ${smtpHost}`);
  console.log(`   SMTP_PORT: ${smtpPort}`);
  console.log(`   SMTP_USER: ${smtpUser || '❌ NON DÉFINI'}`);
  console.log(`   SMTP_PASS: ${smtpPass ? '✅ DÉFINI (longueur: ' + smtpPass.length + ')' : '❌ NON DÉFINI'}`);
  console.log(`   SMTP_FROM: ${process.env.SMTP_FROM || 'Non défini'}`);
  console.log('');
  
  if (!smtpPass || !smtpUser) {
    console.error('❌ ERREUR: SMTP_PASS ou SMTP_USER n\'est pas défini dans .env');
    console.error('   Vérifiez que le fichier .env existe et contient ces variables');
    process.exit(1);
  }
  
  // Créer le transporter
  console.log('🔄 Création du transporter SMTP...');
  const transporter = nodemailer.createTransport({
    host: smtpHost,
    port: smtpPort,
    secure: smtpPort === 465,
    auth: {
      user: smtpUser.trim(),
      pass: smtpPass.trim(),
    },
    tls: {
      rejectUnauthorized: false,
    },
  });
  
  // Tester la connexion
  console.log('🔄 Test de connexion SMTP...');
  try {
    await transporter.verify();
    console.log('✅ Connexion SMTP réussie!\n');
  } catch (error: any) {
    console.error('❌ ERREUR de connexion SMTP:');
    console.error(`   Message: ${error.message}`);
    console.error(`   Code: ${error.code}`);
    if (error.code === 'EAUTH') {
      console.error('\n💡 Solution:');
      console.error('   - Vérifiez que SMTP_USER et SMTP_PASS sont corrects');
      console.error('   - Pour Gmail, utilisez un mot de passe d\'application');
    }
    process.exit(1);
  }
  
  // Envoyer un email de test
  const testEmail = smtpUser; // Envoyer à soi-même
  console.log(`📧 Envoi d'un email de test à ${testEmail}...`);
  
  try {
    const info = await transporter.sendMail({
      from: process.env.SMTP_FROM || `"Test" <${smtpUser}>`,
      to: testEmail,
      subject: 'Test Email - Académie Sportive',
      html: `
        <div style="font-family: Arial, sans-serif; padding: 20px;">
          <h2>Test d'envoi d'email</h2>
          <p>Si vous recevez ce message, l'envoi d'email fonctionne correctement! ✅</p>
          <p>Code de test: <strong>123456</strong></p>
        </div>
      `,
      text: 'Test d\'envoi d\'email - Si vous recevez ce message, l\'envoi d\'email fonctionne correctement! Code: 123456',
    });
    
    console.log('✅ Email envoyé avec succès!');
    console.log(`   Message ID: ${info.messageId}`);
    console.log(`   Accepted: ${info.accepted}`);
    console.log(`   Rejected: ${info.rejected}`);
    console.log(`\n📬 Vérifiez votre boîte de réception: ${testEmail}`);
    console.log('   (Vérifiez aussi les spams si nécessaire)\n');
    
  } catch (error: any) {
    console.error('❌ ERREUR lors de l\'envoi de l\'email:');
    console.error(`   Message: ${error.message}`);
    console.error(`   Code: ${error.code}`);
    console.error(`   Command: ${error.command}`);
    if (error.response) {
      console.error(`   Response: ${error.response}`);
    }
    process.exit(1);
  }
}

testEmailDirect().catch(console.error);



