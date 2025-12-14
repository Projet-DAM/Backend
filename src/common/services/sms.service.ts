import { Injectable, Logger } from '@nestjs/common';
import * as Twilio from 'twilio';

@Injectable()
export class SmsService {
    private client: Twilio.Twilio;
    private readonly logger = new Logger(SmsService.name);
    private readonly enabled: boolean;

    constructor() {
        const accountSid = process.env.TWILIO_ACCOUNT_SID;
        const authToken = process.env.TWILIO_AUTH_TOKEN;

        if (accountSid && authToken) {
            this.client = new Twilio.Twilio(accountSid, authToken);
            this.enabled = true;
        } else {
            this.logger.warn('Twilio credentials not found. SMS service disabled.');
            this.enabled = false;
        }
    }

    async sendSms(to: string, message: string): Promise<void> {
        if (!this.enabled) {
            this.logger.warn(`SMS service disabled (missing credentials). Would have sent to ${to}: ${message}`);
            return;
        }

        try {
            if (!to) {
                this.logger.warn('No phone number provided for SMS');
                return;
            }

            // Simple formatting: Ensure E.164 format
            // If the number doesn't start with '+', and is 8 digits (common in Tunisia), add +216
            let formattedTo = to.replace(/\s+/g, '');
            if (!formattedTo.startsWith('+')) {
                if (formattedTo.length === 8) {
                    formattedTo = `+216${formattedTo}`;
                } else {
                    // Fallback or let Twilio handle/reject it, but usually + is needed
                    formattedTo = `+${formattedTo}`;
                }
            }

            const from = process.env.TWILIO_PHONE_NUMBER;
            if (!from) {
                this.logger.error('TWILIO_PHONE_NUMBER not configured.');
                return;
            }

            this.logger.log(`Attempting to send SMS to ${formattedTo} (original: ${to})...`);

            await this.client.messages.create({
                body: message,
                from: from,
                to: formattedTo,
            });
            this.logger.log(`SMS sent successfully to ${formattedTo}`);
        } catch (error) {
            this.logger.error(`Failed to send SMS to ${to}: ${error.message}`);
            // Don't throw to prevent blocking main flow
        }
    }
}
