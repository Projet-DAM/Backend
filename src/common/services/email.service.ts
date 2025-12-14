import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as nodemailer from 'nodemailer';
import { readFileSync, existsSync } from 'fs';
import { join } from 'path';

type EmailTemplateName = 'abonnement-confirmation' | 'abonnement-expire-7j';

interface TemplateEmailOptions {
  to: string;
  subject: string;
  template: EmailTemplateName;
  variables: Record<string, string | number>;
  textFallback?: string;
}

@Injectable()
export class EmailService {
  private readonly logger = new Logger(EmailService.name);
  private transporter: nodemailer.Transporter | null = null;
  private isUsingEthereal = false;
  private templateCache = new Map<EmailTemplateName, string>();

  constructor(private configService: ConfigService) {
    this.logger.log('📧 EmailService initialisé');
  }

  private getSmtpFrom(): string {
    const smtpFrom = this.configService.get<string>('SMTP_FROM') || process.env.SMTP_FROM;
    const smtpUser = this.configService.get<string>('SMTP_USER') || process.env.SMTP_USER;
    return smtpFrom || `"Académie Sportive" <${smtpUser}>`;
  }

  private async getTransporter(): Promise<nodemailer.Transporter> {
    // Si le transporter existe déjà, le retourner
    if (this.transporter) {
      this.logger.log(`📧 Transporter déjà initialisé, réutilisation`);
      return this.transporter;
    }

    this.logger.log(`📧 === INITIALISATION DU TRANSPORTER ===`);

    // Utiliser ConfigService pour charger les variables d'environnement
    const smtpPass = this.configService.get<string>('SMTP_PASS')?.trim() || process.env.SMTP_PASS?.trim();
    const smtpUser = this.configService.get<string>('SMTP_USER')?.trim() || process.env.SMTP_USER?.trim();
    const smtpHost = this.configService.get<string>('SMTP_HOST')?.trim() || process.env.SMTP_HOST?.trim();

    this.logger.log(`📧 process.env.SMTP_PASS existe: ${process.env.SMTP_PASS ? 'OUI' : 'NON'}`);
    this.logger.log(`📧 process.env.SMTP_USER existe: ${process.env.SMTP_USER ? 'OUI' : 'NON'}`);
    this.logger.log(`📧 configService SMTP_PASS existe: ${this.configService.get<string>('SMTP_PASS') ? 'OUI' : 'NON'}`);
    this.logger.log(`📧 configService SMTP_USER existe: ${this.configService.get<string>('SMTP_USER') ? 'OUI' : 'NON'}`);

    // Log pour déboguer (utiliser log au lieu de debug pour être sûr de voir les messages)
    this.logger.log(`📧 SMTP Configuration check: SMTP_PASS=${smtpPass ? '***SET***' : 'NOT SET'}, SMTP_USER=${smtpUser || 'NOT SET'}, SMTP_HOST=${smtpHost || 'NOT SET'}`);
    this.logger.log(`📧 SMTP_PASS longueur: ${smtpPass?.length || 0}, SMTP_USER longueur: ${smtpUser?.length || 0}`);

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
      const smtpPort = parseInt(this.configService.get<string>('SMTP_PORT') || process.env.SMTP_PORT || '587');
      const isSecure = smtpPort === 465;

      this.logger.log(`📧 Configuration SMTP: ${smtpHost || 'smtp.gmail.com'}:${smtpPort} (secure: ${isSecure})`);
      this.logger.log(`📧 Utilisation de SMTP_USER: ${smtpUser}`);
      this.logger.log(`📧 Utilisation de SMTP_PASS: ${smtpPass ? '***' + smtpPass.length + ' caractères***' : 'VIDE'}`);

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
          rejectUnauthorized: (this.configService.get<string>('NODE_ENV') || process.env.NODE_ENV) === 'production' ? true : false,
        },
      });
      this.isUsingEthereal = false;
      this.logger.log('✅ Configuration SMTP chargée avec succès');
    }

    return this.transporter;
  }

  async sendVerificationCode(email: string, nom: string, prenom: string, code: string): Promise<void> {
    this.logger.log(`📧 sendVerificationCode appelé avec email=${email}, nom=${nom}, prenom=${prenom}`);

    let transporter: nodemailer.Transporter;
    try {
      this.logger.log(`📧 Récupération du transporter...`);
      transporter = await this.getTransporter();
      this.logger.log(`✅ Transporter obtenu: ${transporter ? 'OUI' : 'NON'}`);
    } catch (error: any) {
      this.logger.error(`❌ Erreur lors de la récupération du transporter:`, error.message || error);
      throw error;
    }

    try {
      const mailOptions = {
        from: this.getSmtpFrom(),
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
      this.logger.log(`📤 From: ${mailOptions.from}, To: ${mailOptions.to}, Subject: ${mailOptions.subject}`);

      const info = await transporter.sendMail(mailOptions);

      this.logger.log(`✅ Code de vérification envoyé à ${email}. Message ID: ${info.messageId}`);
      this.logger.log(`✅ Accepted: ${JSON.stringify(info.accepted)}`);
      this.logger.log(`✅ Rejected: ${JSON.stringify(info.rejected)}`);
      this.logger.log(`✅ Response: ${info.response || 'N/A'}`);

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

  async sendPaymentConfirmation(
    email: string,
    nom: string,
    prenom: string,
    subscriptionType: string,
    price: string,
    dateStart: string,
    dateEnd: string,
  ): Promise<void> {
    try {
      this.logger.log(`📤 Envoi de l'email de confirmation de paiement (Template) à ${email}...`);
      await this.sendTemplateEmail({
        to: email,
        subject: 'Confirmation d\'abonnement - Sporty KIDS',
        template: 'abonnement-confirmation',
        variables: {
          prenom,
          nom,
          type: subscriptionType,
          price,
          dateStart,
          dateEnd,
        },
        textFallback: `Bonjour ${prenom} ${nom},\n\nVotre abonnement ${subscriptionType} a été confirmé.\nMontant: ${price}\nDébut: ${dateStart}\nFin: ${dateEnd}\n\nMerci de votre confiance,\nL'équipe Sporty KIDS`,
      });
      this.logger.log(`✅ Email de confirmation envoyé à ${email}.`);
    } catch (error: any) {
      this.logger.error(`❌ Erreur lors de l'envoi de l'email de confirmation à ${email}:`, error.message || error);
      throw error;
    }
  }

  async sendSubscriptionExpirationWarning(
    email: string,
    nom: string,
    prenom: string,
    subscriptionType: string,
    dateEnd: string,
  ): Promise<void> {
    try {
      this.logger.log(`📤 Envoi de l'alerte d'expiration (Template) à ${email}...`);
      await this.sendTemplateEmail({
        to: email,
        subject: '⚠️ Votre abonnement expire bientôt - Sporty KIDS',
        template: 'abonnement-expire-7j',
        variables: {
          prenom,
          nom,
          type: subscriptionType,
          dateEnd,
        },
        textFallback: `Bonjour ${prenom} ${nom},\n\nVotre abonnement ${subscriptionType} expire bientôt (le ${dateEnd}).\n\nPensez à le renouveler !\n\nL'équipe Sporty KIDS`,
      });
      this.logger.log(`✅ Alerte d'expiration envoyée à ${email}.`);
    } catch (error: any) {
      this.logger.error(`❌ Erreur lors de l'envoi de l'alerte d'expiration à ${email}:`, error.message || error);
      throw error;
    }
  }

  private resolveTemplatePath(template: EmailTemplateName): string {
    return join(process.cwd(), 'templates', 'emails', `${template}.html`);
  }

  private loadTemplate(template: EmailTemplateName): string {
    if (this.templateCache.has(template)) {
      return this.templateCache.get(template)!;
    }
    const path = this.resolveTemplatePath(template);
    if (!existsSync(path)) {
      throw new Error(`Template email introuvable: ${path}`);
    }
    const content = readFileSync(path, 'utf8');
    this.templateCache.set(template, content);
    return content;
  }

  private renderTemplate(template: EmailTemplateName, variables: Record<string, string | number>): string {
    const raw = this.loadTemplate(template);
    return raw.replace(/{{\s*(\w+)\s*}}/g, (_, key) => {
      const value = variables[key];
      return value !== undefined && value !== null ? String(value) : '';
    });
  }

  async sendTemplateEmail(options: TemplateEmailOptions): Promise<void> {
    const transporter = await this.getTransporter();
    const html = this.renderTemplate(options.template, options.variables);

    const mailOptions: nodemailer.SendMailOptions = {
      from: this.getSmtpFrom(),
      to: options.to,
      subject: options.subject,
      html,
      text: options.textFallback,
    };

    const info = await transporter.sendMail(mailOptions);
    this.logger.log(`✅ Email template "${options.template}" envoyé à ${options.to}. Message ID: ${info.messageId}`);

    if (this.isUsingEthereal) {
      const previewUrl = nodemailer.getTestMessageUrl(info);
      if (previewUrl) {
        this.logger.log(`📬 Prévisualisation (Ethereal): ${previewUrl}`);
      }
    }
  }
}

