import { ApiProperty } from '@nestjs/swagger';
import { IsDateString, IsNotEmpty, IsNumber, IsOptional, IsString, Min } from 'class-validator';

export class RecordPaymentDto {
  @ApiProperty()
  @IsNumber()
  @Min(0.01)
  amount: number;

  @ApiProperty({ example: 'EUR' })
  @IsString()
  @IsNotEmpty()
  currency: string;

  @ApiProperty({ example: 'CASH' })
  @IsString()
  @IsNotEmpty()
  method: string;

  @ApiProperty({ required: false })
  @IsOptional()
  @IsString()
  externalRef?: string;

  @ApiProperty({ required: false })
  @IsOptional()
  @IsDateString()
  date?: string;
}



