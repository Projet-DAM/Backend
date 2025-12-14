import { Controller, Get } from '@nestjs/common';
import { EmailService } from '../common/services/email.service';
import { Public } from '../common/decorators/public.decorator';

@Controller('test')
export class TestController {
  constructor(private readonly emailService: EmailService) { }

  @Get('email')
  @Public()
  async testEmail() {
    try {
      await this.emailService.sendPaymentConfirmation(
        'eya.boujnayah2020@gmail.com', // Email de test
        'Test',
        'User',
        'Abonnement Mensuel',
        '50.00 €',
        new Date().toLocaleDateString('fr-FR'),
        new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toLocaleDateString('fr-FR'),
      );
      return { success: true, message: 'Email de test envoyé avec succès' };
    } catch (error: any) {
      return {
        success: false,
        error: error.message,
        stack: error.stack
      };
    }
  }
}
