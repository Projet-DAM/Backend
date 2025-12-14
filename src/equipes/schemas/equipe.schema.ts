import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type EquipeDocument = Equipe & Document;

@Schema({ timestamps: true })
export class Equipe {
  _id: Types.ObjectId;

  @Prop({ required: true })
  nom: string;

  @Prop()
  couleur?: string;

  @Prop({ type: Types.ObjectId, ref: 'Tournoi', required: true })
  tournoi: Types.ObjectId;

  @Prop({ required: true })
  sport: string;

  @Prop({ required: true })
  formatEquipe: string;

  @Prop({ type: [{ type: Types.ObjectId, ref: 'Inscription' }], default: [] })
  enfants: Types.ObjectId[];

  @Prop({ type: Number, default: 0 })
  points: number;

  @Prop({ type: Number, default: 0 })
  victoires: number;

  @Prop({ type: Number, default: 0 })
  defaites: number;

  @Prop({ type: Number, default: 0 })
  nuls: number;

  @Prop({ type: Number, default: 0 })
  classement?: number;

  createdAt?: Date;
  updatedAt?: Date;
}

export const EquipeSchema = SchemaFactory.createForClass(Equipe);

