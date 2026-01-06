import { Module } from '@nestjs/common';
import { EmailService } from './email.service';
import { EmailTemplateService } from './email-template.service';
import { SmsService } from './sms.service';

@Module({
  providers: [EmailService, EmailTemplateService, SmsService],
  exports: [EmailService, EmailTemplateService, SmsService],
})
export class EmailModule { }











