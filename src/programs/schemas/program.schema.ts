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

// Virtual field for capaciteMaximale
ProgramSchema.virtual('capaciteMaximale').get(function () {
  if (!this.activites || this.activites.length === 0) {
    return null;
  }

  // If activites are populated (Activity objects)
  if (typeof this.activites[0] === 'object' && (this.activites[0] as any).capacite_max !== undefined) {
    const capacities = this.activites
      .map((act: any) => act.capacite_max)
      .filter((cap: number) => cap != null && cap > 0);

    if (capacities.length === 0) return null;
    return Math.min(...capacities);
  }

  return null;
});

// Ensure virtuals are included in JSON
ProgramSchema.set('toJSON', { virtuals: true });
ProgramSchema.set('toObject', { virtuals: true });

ProgramSchema.index({ nom_programme: 1, coach: 1 });
ProgramSchema.index({ nom_programme: 1, academie: 1 });