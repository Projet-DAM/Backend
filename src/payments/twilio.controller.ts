import { Controller, Post, Body, UseGuards, Logger, HttpCode, HttpStatus } from '@nestjs/common';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { ConfigService } from '@nestjs/config';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { Roles } from '../common/decorators/roles.decorator';
import { UserRole } from '../users/interfaces/user-role.enum';
import { Twilio } from 'twilio';

interface SMSResult {
    phoneNumber: string;
    success: boolean;
    messageSid?: string;
    error?: string;
}

@ApiTags('Twilio')
@Controller('twilio')
export class TwilioController {
    private readonly logger = new Logger(TwilioController.name);
    private twilioClient: Twilio | null = null;
    private twilioPhoneNumber: string;

    constructor(private configService: ConfigService) {
        const accountSid = this.configService.get<string>('TWILIO_ACCOUNT_SID');
        const authToken = this.configService.get<string>('TWILIO_AUTH_TOKEN');
        this.twilioPhoneNumber = this.configService.get<string>('TWILIO_PHONE_NUMBER') || '';

        if (!accountSid || !authToken) {
            this.logger.warn('⚠️ Twilio credentials not configured. SMS/Voice features will not work.');
            return;
        }

        this.twilioClient = new Twilio(accountSid, authToken);
        this.logger.log('✅ Twilio client initialized successfully');
    }

    @Post('send-sms')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Envoyer un SMS (ACADEMIE|ADMIN)' })
    async sendSMS(@Body() body: { to: string; message: string }) {
        if (!this.twilioClient) {
            return { success: false, error: 'Twilio not configured' };
        }

        try {
            const message = await this.twilioClient.messages.create({
                body: body.message,
                from: this.twilioPhoneNumber,
                to: body.to,
            });

            this.logger.log(`✅ SMS sent successfully. SID: ${message.sid}`);
            return {
                success: true,
                messageSid: message.sid,
                status: message.status,
            };
        } catch (error: any) {
            this.logger.error(`❌ Error sending SMS: ${error.message}`);
            return {
                success: false,
                error: error.message,
            };
        }
    }

    @Post('make-call')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN)
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Passer un appel vocal (ACADEMIE|ADMIN)' })
    async makeCall(@Body() body: { to: string; message: string }) {
        if (!this.twilioClient) {
            return { success: false, error: 'Twilio not configured' };
        }

        try {
            // Créer un TwiML pour lire le message
            const twiml = `<?xml version="1.0" encoding="UTF-8"?>
        <Response>
          <Say language="fr-FR">${body.message}</Say>
        </Response>`;

            const call = await this.twilioClient.calls.create({
                twiml: twiml,
                from: this.twilioPhoneNumber,
                to: body.to,
            });

            this.logger.log(`✅ Call initiated successfully. SID: ${call.sid}`);
            return {
                success: true,
                callSid: call.sid,
                status: call.status,
            };
        } catch (error: any) {
            this.logger.error(`❌ Error making call: ${error.message}`);
            return {
                success: false,
                error: error.message,
            };
        }
    }

    @Post('send-notification')
    @UseGuards(JwtAuthGuard)
    @ApiBearerAuth('JWT-auth')
    @Roles(UserRole.ACADEMIE, UserRole.ADMIN, UserRole.PARENT)
    @HttpCode(HttpStatus.OK)
    @ApiOperation({ summary: 'Envoyer une notification SMS aux parents' })
    async sendNotification(@Body() body: { phoneNumbers: string[]; message: string }) {
        if (!this.twilioClient) {
            return { success: false, error: 'Twilio not configured' };
        }

        const results: SMSResult[] = [];

        for (const phoneNumber of body.phoneNumbers) {
            try {
                const message = await this.twilioClient.messages.create({
                    body: body.message,
                    from: this.twilioPhoneNumber,
                    to: phoneNumber,
                });

                results.push({
                    phoneNumber,
                    success: true,
                    messageSid: message.sid,
                });

                this.logger.log(`✅ SMS sent to ${phoneNumber}. SID: ${message.sid}`);
            } catch (error: any) {
                results.push({
                    phoneNumber,
                    success: false,
                    error: error.message,
                });

                this.logger.error(`❌ Error sending SMS to ${phoneNumber}: ${error.message}`);
            }
        }

        return {
            totalSent: results.filter(r => r.success).length,
            totalFailed: results.filter(r => !r.success).length,
            results,
        };
    }
}
