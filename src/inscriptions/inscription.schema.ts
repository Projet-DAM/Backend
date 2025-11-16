import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type InscriptionDocument = Inscription & Document;

@Schema({ timestamps: true })
export class Inscription {
  _id: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Tournoi', required: true })
  tournoiId: Types.ObjectId;

  @Prop({ required: true })
  enfantPrenom: string;

  @Prop({ required: true })
  enfantNom: string;

  @Prop({ required: true, type: Date })
  enfantDateNaissance: Date;

  @Prop({ required: true })
  parentPrenom: string;

  @Prop({ required: true })
  parentNom: string;

  @Prop({ required: true })
  parentTelephone: string;

  @Prop({ type: Number })
  montantInscription?: number;

  @Prop()
  besoinsParticuliers?: string;

  createdAt?: Date;
  updatedAt?: Date;
}

export const InscriptionSchema = SchemaFactory.createForClass(Inscription);

