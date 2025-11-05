import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

@Schema({ timestamps: true })
export class SuiviEnfant {
  @Prop({ type: Date, required: true })
  date_suivi: Date;

  @Prop({ type: Boolean, required: true })
  presence: boolean;

  @Prop({ type: Number, required: true })
  performance: number;

  @Prop({ type: String })
  commentaire?: string;

  @Prop({ type: Types.ObjectId, ref: 'User', required: true })
  enfant: Types.ObjectId;
}

export type SuiviEnfantDocument = SuiviEnfant & Document;
export const SuiviEnfantSchema = SchemaFactory.createForClass(SuiviEnfant);
