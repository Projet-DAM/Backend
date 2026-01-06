import { Injectable, Logger, Inject, forwardRef } from '@nestjs/common';
import * as nodemailer from 'nodemailer';
import { EmailTemplateService } from './email-template.service';

@Injectable()
export class EmailService {
  private readonly logger = new Logger(EmailService.name);
  private transporter: nodemailer.Transporter | null = null;
  private isUsingEthereal = false;

  constructor(
    @Inject(forwardRef(() => EmailTemplateService))
    private templateService: EmailTemplateService
  ) { }

  private async getTransporter(): Promise<nodemailer.Transporter> {
    // Si le transporter existe déjà, le retourner
    if (this.transporter) {
      return this.transporter;
    }

    // Vérifier si SMTP est configuré (vérifier que SMTP_PASS existe et n'est pas vide)
    const smtpPass = process.env.SMTP_PASS?.trim();
    const smtpUser = process.env.SMTP_USER?.trim();
    const smtpHost = process.env.SMTP_HOST?.trim();

    // Log pour déboguer (utiliser log au lieu de debug pour être sûr de voir les messages)
    this.logger.log(`📧 SMTP Configuration check: SMTP_PASS=${smtpPass ? '***SET***' : 'NOT SET'}, SMTP_USER=${smtpUser || 'NOT SET'}, SMTP_HOST=${smtpHost || 'NOT SET'}`);

    if (!smtpPass || smtpPass === '' || !smtpUser || smtpUser === '') {
      // Mode développement : Si SMTP n'est pas configuré, utiliser Ethereal Email
      this.logger.warn('⚠️  SMTP non configuré, utilisation d\'Ethereal Email pour le développement');
      try {
        // Créer un compte Ethereal Email temporaire pour le développement
        this.logger.log('🔄 Création d\'un compte Ethereal Email...');
        const testAccount = await nodemailer.createTestAccount();
        this.logger.log(`✅ Compte Ethereal créé: ${testAccount.user}`);

        this.transporter = nodemailer.createTransport({
          host: 'smtp.ethereal.email',
          port: 587,
          secure: false,
          auth: {
            user: testAccount.user,
            pass: testAccount.pass,
          },
          // Ignorer les erreurs de certificat SSL pour Ethereal Email
          tls: {
            rejectUnauthorized: false,
            ciphers: 'SSLv3',
          },
          // Options supplémentaires pour éviter les erreurs de certificat
          connectionTimeout: 10000,
          greetingTimeout: 10000,
          socketTimeout: 10000,
        });
        this.isUsingEthereal = true;
        this.logger.log(`📧 Transporter Ethereal Email configuré avec succès`);
      } catch (error: any) {
        this.logger.error('❌ Erreur lors de la création du compte Ethereal Email:', error.message || error);
        this.logger.error('Stack trace:', error.stack);
        throw new Error(`Impossible d'envoyer l'email de vérification. Erreur: ${error.message || 'Erreur inconnue'}. Veuillez configurer SMTP_PASS dans .env`);
      }
    } else {
      // Configuration SMTP normale
      const smtpPort = parseInt(process.env.SMTP_PORT || '587');
      const isSecure = smtpPort === 465;

      this.logger.log(`📧 Configuration SMTP: ${smtpHost || 'smtp.gmail.com'}:${smtpPort} (secure: ${isSecure})`);

      this.transporter = nodemailer.createTransport({
        host: smtpHost || 'smtp.gmail.com',
        port: smtpPort,
        secure: isSecure, // true pour 465, false pour les autres ports
        auth: {
          user: smtpUser,
          pass: smtpPass,
        },
        // Ignorer les erreurs de certificat SSL en mode développement
        // En production, utilisez des certificats valides
        tls: {
          rejectUnauthorized: process.env.NODE_ENV === 'production' ? true : false,
        },
      });
      this.isUsingEthereal = false;
      this.logger.log('✅ Configuration SMTP chargée avec succès');
    }

    return this.transporter;
  }

  async sendVerificationCode(email: string, nom: string, prenom: string, code: string): Promise<void> {
    const transporter = await this.getTransporter();

    try {
      const mailOptions = {
        from: process.env.SMTP_FROM || `"Académie Sportive" <${process.env.SMTP_USER}>`,
        to: email,
        subject: 'Code de vérification - Académie Sportive',
        html: `
          <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
            <h2 style="color: #1976d2;">Bienvenue ${prenom} ${nom} !</h2>
            <p>Merci de vous être inscrit sur la plateforme Académie Sportive.</p>
            <p>Pour finaliser votre inscription, veuillez utiliser le code de vérification suivant :</p>
            <div style="text-align: center; margin: 30px 0;">
              <div style="background-color: #f5f5f5; border: 2px dashed #1976d2; border-radius: 8px; padding: 20px; display: inline-block;">
                <p style="font-size: 32px; font-weight: bold; color: #1976d2; letter-spacing: 8px; margin: 0;">${code}</p>
              </div>
            </div>
            <p style="color: #666; font-size: 14px;">
              Ce code est valide pendant 15 minutes. Ne partagez jamais ce code avec personne.
            </p>
            <p style="margin-top: 30px; color: #666; font-size: 12px;">
              Si vous n'avez pas créé de compte, ignorez cet email.
            </p>
          </div>
        `,
        text: `
          Bienvenue ${prenom} ${nom} !
          
          Merci de vous être inscrit sur la plateforme Académie Sportive.
          
          Pour finaliser votre inscription, veuillez utiliser le code de vérification suivant :
          
          ${code}
          
          Ce code est valide pendant 15 minutes. Ne partagez jamais ce code avec personne.
          
          Si vous n'avez pas créé de compte, ignorez cet email.
        `,
      };

      this.logger.log(`📤 Tentative d'envoi de l'email à ${email}...`);
      const info = await transporter.sendMail(mailOptions);
      this.logger.log(`✅ Code de vérification envoyé à ${email}. Message ID: ${info.messageId}`);

      // Si on utilise Ethereal Email, afficher le lien de prévisualisation
      if (this.isUsingEthereal) {
        const previewUrl = nodemailer.getTestMessageUrl(info);
        if (previewUrl) {
          this.logger.log(`📬 ⚠️  IMPORTANT: Prévisualisation de l'email (Ethereal Email): ${previewUrl}`);
          this.logger.log(`📬 Ouvrez ce lien dans votre navigateur pour voir l'email et récupérer le code de vérification`);
        } else {
          this.logger.warn(`⚠️  Impossible d'obtenir le lien de prévisualisation pour Ethereal Email`);
        }
      }
    } catch (error: any) {
      this.logger.error(`❌ Erreur lors de l'envoi de l'email à ${email}:`, error.message || error);
      this.logger.error(`❌ Code d'erreur: ${error.code || 'N/A'}, Commande: ${error.command || 'N/A'}`);
      if (error.stack) {
        this.logger.error(`❌ Stack trace:`, error.stack);
      }
      // Ne pas afficher le code dans les logs, même en cas d'erreur
      throw error; // Relancer l'erreur pour que le service puisse la gérer
    }
  }
  async sendSubscriptionExpiring(email: string, parentName: string, childName: string, offerName: string, endDate: Date): Promise<void> {
    const names = parentName.split(' ');
    const prenom = names[0];
    const nom = names.slice(1).join(' ') || '';

    return this.templateService.sendSubscriptionExpiring(
      email,
      prenom,
      nom,
      childName,
      offerName,
      endDate.toLocaleDateString('fr-FR')
    );
  }

  async sendPaymentConfirmation(email: string, parentName: string, offerName: string, amount: number, currency: string, startDate: Date, endDate: Date): Promise<void> {
    const names = parentName.split(' ');
    const prenom = names[0];
    const nom = names.slice(1).join(' ') || '';

    // On force l'affichage en TND comme demandé par l'utilisateur
    // On garde le montant tel quel (Stripe donne le montant en unités principales ici car divisé par 100 dans le contrôleur)
    return this.templateService.sendSubscriptionConfirmation(
      email,
      prenom,
      nom,
      offerName,
      `${amount} TND`,
      startDate.toLocaleDateString('fr-FR'),
      endDate.toLocaleDateString('fr-FR')
    );
  }
}

