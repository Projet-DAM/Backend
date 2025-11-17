import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export enum OfferType {
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  ANNUAL = 'ANNUAL',
  CUSTOM = 'CUSTOM',
}

export type OfferDocument = Offer & Document;

@Schema({ timestamps: true })
export class Offer {
  _id: Types.ObjectId;

  @Prop({ required: true })
  name: string;

  @Prop()
  description?: string;

  @Prop({ enum: OfferType, required: true })
  type: OfferType;

  @Prop({ required: true, min: 1 })
  durationDays: number;

  @Prop({ required: true, min: 0.01 })
  price: number;

  @Prop({ default: 0, min: 0, max: 100 })
  discountPct: number;

  @Prop()
  conditions?: string;

  @Prop({ default: true, index: true })
  isActive: boolean;

  @Prop({ type: Types.ObjectId, ref: 'User', index: true, required: true })
  academyId: Types.ObjectId;

  createdAt?: Date;
  updatedAt?: Date;
}

export const OfferSchema = SchemaFactory.createForClass(Offer);






