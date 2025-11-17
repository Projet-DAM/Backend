import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ActivityDocument = Activity & Document;

export enum ActivityStatus {
  ACTIVE = 'ACTIVE',
  ANNULEE = 'ANNULEE',
  TERMINEE = 'TERMINEE',
  BROUILLON = 'BROUILLON',
}

@Schema({ timestamps: true })
export class Activity {
  _id: Types.ObjectId;

  @Prop({ required: true })
  nom_activite: string;

  @Prop()
  description?: string;

  @Prop()
  categorie?: string;

  @Prop({ type: Date })
  date?: Date;

  @Prop()
  heure?: string; // HH:mm

  @Prop()
  duree?: number; // minutes

  @Prop()
  capacite_max?: number;

  @Prop()
  prix?: number;

  @Prop({ enum: ActivityStatus, default: ActivityStatus.BROUILLON })
  statut: ActivityStatus;

  @Prop({ type: Types.ObjectId, ref: 'User' })
  coach?: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'User' })
  academie?: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Program' })
  programme?: Types.ObjectId;

  createdAt?: Date;
  updatedAt?: Date;
}

export const ActivitySchema = SchemaFactory.createForClass(Activity);

ActivitySchema.index({ date: 1 });
ActivitySchema.index({ categorie: 1 });
ActivitySchema.index({ programme: 1 });





