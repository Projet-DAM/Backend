import { ApiProperty } from '@nestjs/swagger';
import { IsArray, IsNumber, IsString, IsOptional, Min, IsBoolean } from 'class-validator';
import { Type } from 'class-transformer';

export class CreatePaymentIntentDto {
  @ApiProperty({ example: 70000, required: false })
  @IsNumber()
  @IsOptional()
  @Type(() => Number)
  amount?: number;

  @ApiProperty({ example: 'eur', required: false })
  @IsString()
  @IsOptional()
  currency?: string;

  // CamelCase
  @IsString()
  @IsOptional()
  paymentMethodId?: string;

  @IsString()
  @IsOptional()
  subscriptionId?: string;

  @IsString()
  @IsOptional()
  phoneNumber?: string;

  @IsString()
  @IsOptional()
  childId?: string;

  @IsString()
  @IsOptional()
  offerId?: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  selectedOptions?: string[];

  @IsBoolean()
  @IsOptional()
  @Type(() => Boolean)
  autoRenew?: boolean;

  // SnakeCase (Aliases)
  @IsString()
  @IsOptional()
  payment_method_id?: string;

  @IsString()
  @IsOptional()
  phone_number?: string;

  @IsString()
  @IsOptional()
  child_id?: string;

  @IsString()
  @IsOptional()
  offer_id?: string;

  @IsArray()
  @IsString({ each: true })
  @IsOptional()
  selected_options?: string[];

  @IsBoolean()
  @IsOptional()
  @Type(() => Boolean)
  auto_renew?: boolean;
}
