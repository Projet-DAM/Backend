import { Injectable, Logger } from '@nestjs/common';
import * as nodemailer from 'nodemailer';
import * as fs from 'fs';
import * as path from 'path';

@Injectable()
export class EmailTemplateService {
    private readonly logger = new Logger(EmailTemplateService.name);
    private transporter: nodemailer.Transporter | null = null;
    private isUsingEthereal = false;

    private async getTransporter(): Promise<nodemailer.Transporter> {
        if (this.transporter) {
            return this.transporter;
        }

        const smtpPass = process.env.SMTP_PASS?.trim();
        const smtpUser = process.env.SMTP_USER?.trim();
        const smtpHost = process.env.SMTP_HOST?.trim();

        this.logger.log(`📧 SMTP Configuration check: SMTP_PASS=${smtpPass ? '***SET***' : 'NOT SET'}, SMTP_USER=${smtpUser || 'NOT SET'}, SMTP_HOST=${smtpHost || 'NOT SET'}`);

        if (!smtpPass || smtpPass === '' || !smtpUser || smtpUser === '') {
            this.logger.warn('⚠️  SMTP non configuré, utilisation d\'Ethereal Email pour le développement');
            try {
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
                    tls: {
                        rejectUnauthorized: false,
                        ciphers: 'SSLv3',
                    },
                    connectionTimeout: 10000,
                    greetingTimeout: 10000,
                    socketTimeout: 10000,
                });
                this.isUsingEthereal = true;
                this.logger.log(`📧 Transporter Ethereal Email configuré avec succès`);
            } catch (error: any) {
                this.logger.error('❌ Erreur lors de la création du compte Ethereal Email:', error.message || error);
                throw new Error(`Impossible d'envoyer l'email. Erreur: ${error.message || 'Erreur inconnue'}. Veuillez configurer SMTP_PASS dans .env`);
            }
        } else {
            const smtpPort = parseInt(process.env.SMTP_PORT || '587');
            const isSecure = smtpPort === 465;

            this.logger.log(`📧 Configuration SMTP: ${smtpHost || 'smtp.gmail.com'}:${smtpPort} (secure: ${isSecure})`);

            this.transporter = nodemailer.createTransport({
                host: smtpHost || 'smtp.gmail.com',
                port: smtpPort,
                secure: isSecure,
                auth: {
                    user: smtpUser,
                    pass: smtpPass,
                },
                tls: {
                    rejectUnauthorized: process.env.NODE_ENV === 'production' ? true : false,
                },
            });
            this.isUsingEthereal = false;
            this.logger.log('✅ Configuration SMTP chargée avec succès');
        }

        return this.transporter;
    }

    /**
     * Envoie un email de confirmation d'abonnement avec template HTML
     */
    async sendSubscriptionConfirmation(
        email: string,
        prenom: string,
        nom: string,
        type: string,
        price: string,
        dateStart: string,
        dateEnd: string,
    ): Promise<void> {
        const transporter = await this.getTransporter();

        try {
            // Utiliser les nouveaux templates dans src/common/templates/
            const templatePath = path.join(process.cwd(), 'src/common/templates/payment-confirmation.html');
            this.logger.log(`📂 Chemin du template confirmation: ${templatePath}`);
            let htmlTemplate = fs.readFileSync(templatePath, 'utf8');

            // Extraire le montant et la devise du prix (ex: "70 EUR" -> "70" et "EUR")
            const priceParts = price.split(' ');
            const amount = priceParts[0] || '';
            const currency = priceParts[1] || '';

            htmlTemplate = htmlTemplate
                .replace(/{{ parentName }}/g, `${prenom} ${nom}`)
                .replace(/{{parentName}}/g, `${prenom} ${nom}`)
                .replace(/{{ offerName }}/g, type)
                .replace(/{{offerName}}/g, type)
                .replace(/{{ amount }}/g, amount)
                .replace(/{{amount}}/g, amount)
                .replace(/{{ currency }}/g, currency)
                .replace(/{{currency}}/g, currency)
                .replace(/{{ startDate }}/g, dateStart)
                .replace(/{{startDate}}/g, dateStart)
                .replace(/{{ endDate }}/g, dateEnd)
                .replace(/{{endDate}}/g, dateEnd);

            const mailOptions = {
                from: process.env.SMTP_FROM || `"Sporty KIDS" <${process.env.SMTP_USER}>`,
                to: email,
                subject: '✅ Confirmation d\'abonnement - Sporty KIDS',
                html: htmlTemplate,
            };

            this.logger.log(`📤 Envoi de l'email de confirmation à ${email}...`);
            const info = await transporter.sendMail(mailOptions);
            this.logger.log(`✅ Email de confirmation envoyé à ${email}. Message ID: ${info.messageId}`);

            if (this.isUsingEthereal) {
                const previewUrl = nodemailer.getTestMessageUrl(info);
                if (previewUrl) {
                    this.logger.log(`📬 Prévisualisation : ${previewUrl}`);
                }
            }
        } catch (error: any) {
            this.logger.error(`❌ Erreur lors de l'envoi de l'email à ${email}:`, error.message || error);
            throw error;
        }
    }

    /**
     * Envoie un email d'avertissement d'expiration (7 jours avant)
     */
    async sendSubscriptionExpiring(
        email: string,
        prenom: string,
        nom: string,
        childName: string,
        offerName: string,
        endDate: string,
    ): Promise<void> {
        const transporter = await this.getTransporter();

        try {
            const templatePath = path.join(process.cwd(), 'src/common/templates/subscription-expiring.html');
            this.logger.log(`📂 Chemin du template expiration: ${templatePath}`);
            let htmlTemplate = fs.readFileSync(templatePath, 'utf8');

            // URL de renouvellement par défaut
            const renewUrl = process.env.RENEWAL_URL || 'https://sportyconnect.com/renew';

            htmlTemplate = htmlTemplate
                .replace(/{{ parentName }}/g, `${prenom} ${nom}`)
                .replace(/{{parentName}}/g, `${prenom} ${nom}`)
                .replace(/{{ childName }}/g, childName)
                .replace(/{{childName}}/g, childName)
                .replace(/{{ offerName }}/g, offerName)
                .replace(/{{offerName}}/g, offerName)
                .replace(/{{ endDate }}/g, endDate)
                .replace(/{{endDate}}/g, endDate)
                .replace(/{{ daysRemaining }}/g, '7')
                .replace(/{{daysRemaining}}/g, '7')
                .replace(/{{ renewUrl }}/g, renewUrl)
                .replace(/{{renewUrl}}/g, renewUrl);

            const mailOptions = {
                from: process.env.SMTP_FROM || `"Sporty KIDS" <${process.env.SMTP_USER}>`,
                to: email,
                subject: '⏰ Votre abonnement expire bientôt - Sporty KIDS',
                html: htmlTemplate,
            };

            this.logger.log(`📤 Envoi de l'email d'expiration à ${email}...`);
            const info = await transporter.sendMail(mailOptions);
            this.logger.log(`✅ Email d'expiration envoyé à ${email}. Message ID: ${info.messageId}`);

            if (this.isUsingEthereal) {
                const previewUrl = nodemailer.getTestMessageUrl(info);
                if (previewUrl) {
                    this.logger.log(`📬 Prévisualisation : ${previewUrl}`);
                }
            }
        } catch (error: any) {
            this.logger.error(`❌ Erreur lors de l'envoi de l'email à ${email}:`, error.message || error);
            throw error;
        }
    }
}
