import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsBoolean, IsNumber, IsOptional, IsString } from 'class-validator';
import { Type } from 'class-transformer';

export class ConfirmPaymentDto {
  @IsString()
  @IsOptional()
  paymentIntentId?: string;

  @IsString()
  @IsOptional()
  payment_intent_id?: string;

  @IsString()
  @IsOptional()
  paymentMethodId?: string;

  @IsString()
  @IsOptional()
  payment_method_id?: string;

  @IsOptional()
  @IsString()
  subscriptionId?: string;

  @IsOptional()
  @IsNumber()
  @Type(() => Number)
  amount?: number;

  @IsOptional()
  @IsString()
  currency?: string;

  @IsOptional()
  @IsArray()
  @IsString({ each: true })
  selectedOptions?: string[];

  @IsOptional()
  @IsString()
  phoneNumber?: string;

  @IsOptional()
  @IsString()
  childId?: string;

  @IsOptional()
  @IsString()
  offerId?: string;

  @IsOptional()
  @IsBoolean()
  @Type(() => Boolean)
  autoRenew?: boolean;
}
