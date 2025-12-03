import { PartialType } from '@nestjs/swagger';
import { CreateSubscriptionDto } from './create-subscription.dto';
import { IsBoolean, IsDateString, IsEnum, IsOptional, IsString, MaxLength } from 'class-validator';
import { PaymentStatus, SubscriptionStatus } from '../schemas/subscription.schema';

export class UpdateSubscriptionDto extends PartialType(CreateSubscriptionDto) {
  // Parent can update autoRenew, notes, and startDate; admin/academy can update status/paymentStatus
  @IsOptional()
  @IsBoolean()
  override autoRenew?: boolean;

  @IsOptional()
  @IsString()
  @MaxLength(1000)
  notes?: string;

  @IsOptional()
  @IsDateString()
  startDate?: string;

  @IsOptional()
  @IsEnum(SubscriptionStatus)
  status?: SubscriptionStatus;

  @IsOptional()
  @IsEnum(PaymentStatus)
  paymentStatus?: PaymentStatus;
}



