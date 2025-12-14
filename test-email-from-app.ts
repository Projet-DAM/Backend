import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { EmailService } from './src/common/services/email.service';
import * as dotenv from 'dotenv';

dotenv.config();

async function testEmailFromApp() {
  console.log('\n🔍 === TEST EMAIL DEPUIS L\'APPLICATION NESTJS ===\n');
  
  try {
    console.log('🔄 Création de l\'application NestJS...');
    const app = await NestFactory.createApplicationContext(AppModule);
    
    console.log('🔄 Récupération du EmailService...');
    const emailService = app.get(EmailService);
    
    if (!emailService) {
      console.error('❌ EmailService non trouvé!');
      process.exit(1);
    }
    
    console.log('✅ EmailService trouvé');
    
    // Test avec votre email
    const testEmail = process.env.SMTP_USER || 'votre-email@example.com';
    console.log(`\n📧 Envoi d'un email de test à ${testEmail}...\n`);
    
    await emailService.sendVerificationCode(
      testEmail,
      'Test',
      'User',
      '123456'
    );
    
    console.log('\n✅ Email envoyé avec succès depuis l\'application!\n');
    
    await app.close();
  } catch (error: any) {
    console.error('\n❌ ERREUR:');
    console.error('Message:', error.message);
    console.error('Stack:', error.stack);
    console.error('Code:', error.code);
    process.exit(1);
  }
}

testEmailFromApp();



