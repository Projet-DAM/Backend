import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';
import { MatchPhase } from '../interfaces/match-phase.enum';
import { MatchStatut } from '../interfaces/match-statut.enum';

export type MatchDocument = Match & Document;

@Schema({ timestamps: true })
export class Match {
  _id: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Tournoi', required: true })
  tournoiId: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Equipe', default: null })
  equipeA?: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Equipe', default: null })
  equipeB?: Types.ObjectId;

  @Prop({ enum: MatchPhase, required: true })
  phase: MatchPhase;

  @Prop({ enum: MatchStatut, required: true, default: MatchStatut.A_VENIR })
  statut: MatchStatut;

  @Prop({ type: Number, default: null })
  scoreEquipeA?: number;

  @Prop({ type: Number, default: null })
  scoreEquipeB?: number;

  @Prop({ type: Types.ObjectId, ref: 'Equipe', default: null })
  vainqueur?: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Match', default: null })
  matchSuivantId?: Types.ObjectId;

  @Prop({ type: Number })
  ordre?: number;

  createdAt?: Date;
  updatedAt?: Date;
}

export const MatchSchema = SchemaFactory.createForClass(Match);
