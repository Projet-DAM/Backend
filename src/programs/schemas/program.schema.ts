import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type ProgramDocument = Program & Document;

export enum ProgramStatus {
  BROUILLON = 'BROUILLON',
  ACTIF = 'ACTIF',
  ARCHIVE = 'ARCHIVE',
}

@Schema({ timestamps: true })
export class Program {
  _id: Types.ObjectId;

  @Prop({ required: true, trim: true })
  nom_programme: string;

  @Prop()
  description?: string;

  @Prop()
  objectif?: string;

  @Prop()
  niveau?: string;

  @Prop()
  prix?: number;

  @Prop({ enum: ProgramStatus, default: ProgramStatus.BROUILLON })
  statut: ProgramStatus;

  @Prop({ type: [{ type: Types.ObjectId, ref: 'Activity' }], default: [] })
  activites: Types.ObjectId[];

  @Prop({ type: Types.ObjectId, ref: 'User' })
  coach?: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'User' })
  academie?: Types.ObjectId;

  @Prop()
  image?: string;

  createdAt?: Date;
  updatedAt?: Date;
}

export const ProgramSchema = SchemaFactory.createForClass(Program);

ProgramSchema.index({ nom_programme: 1, coach: 1 });
ProgramSchema.index({ nom_programme: 1, academie: 1 });