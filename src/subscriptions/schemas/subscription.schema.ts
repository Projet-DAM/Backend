import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';
import { SubscriptionOption, SubscriptionOptionSchema } from './subscription-option.schema';

export enum SubscriptionStatus {
  PENDING = 'PENDING',
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  CANCELLED = 'CANCELLED',
  EXPIRED = 'EXPIRED',
}

export enum PaymentStatus {
  UNPAID = 'UNPAID',
  PAID = 'PAID',
  REFUNDED = 'REFUNDED',
  PARTIAL = 'PARTIAL',
}

@Schema()
export class Transaction {
  @Prop({ required: true, min: 0.01 })
  amount: number;

  @Prop({ required: true })
  currency: string;

  @Prop({ required: true })
  method: string; // e.g., CASH, CARD, TRANSFER

  @Prop()
  externalRef?: string;

  @Prop({ required: true, default: () => new Date() })
  date: Date;

  @Prop({ required: true })
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
}

export const TransactionSchema = SchemaFactory.createForClass(Transaction);

export type SubscriptionDocument = Subscription & Document;

@Schema({ timestamps: true })
export class Subscription {
  _id: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  childId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  parentId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Offer', required: true })
  offerId: Types.ObjectId;

  @Prop({ required: true })
  startDate: Date;

  @Prop({ required: true })
  endDate: Date;

  @Prop({ default: false })
  autoRenew: boolean;

  @Prop({ enum: SubscriptionStatus, default: SubscriptionStatus.PENDING })
  status: SubscriptionStatus;

  @Prop({ enum: PaymentStatus, default: PaymentStatus.UNPAID })
  paymentStatus: PaymentStatus;

  @Prop({ type: [TransactionSchema], default: [] })
  transactions: Transaction[];

  @Prop()
  notes?: string;

  @Prop({ type: [], default: [] })
  selectedOptions: any[];

  @Prop({ default: false })
  expirationWarningSent: boolean;

  createdAt?: Date;
  updatedAt?: Date;
}

export const SubscriptionSchema = SchemaFactory.createForClass(Subscription);






