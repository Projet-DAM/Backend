import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';
import { TournoiEtat } from '../interfaces/tournoi-etat.enum';
import { TournoiNiveau } from '../interfaces/tournoi-niveau.enum';

export type TournoiDocument = Tournoi & Document;

@Schema({ timestamps: true })
export class Tournoi {
  _id: Types.ObjectId;

  @Prop({ required: true })
  nom: string;

  @Prop()
  description?: string;

  @Prop({ required: true })
  sport: string;

  @Prop({ required: true })
  categorieAge: string;

  @Prop({ required: true, type: Date })
  dateDebut: Date;

  @Prop({ required: true, type: Date })
  dateFin: Date;

  @Prop({ required: true })
  lieu: string;

  @Prop({ type: Number })
  nombreParticipantsMax?: number;

  @Prop({ type: Number })
  fraisParticipation?: number;

  @Prop()
  image?: string;

  @Prop({ enum: TournoiEtat, required: true, default: TournoiEtat.OUVERT })
  etat: TournoiEtat;

  @Prop({ enum: TournoiNiveau })
  niveau?: TournoiNiveau;

  @Prop()
  recompense?: string;

  createdAt?: Date;
  updatedAt?: Date;
}

export const TournoiSchema = SchemaFactory.createForClass(Tournoi);

