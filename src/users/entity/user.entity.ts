import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';
import { UserRole } from '../interfaces/user-role.enum';

export type UserDocument = User & Document;

@Schema({ timestamps: true })
export class User {
  _id: Types.ObjectId;

  @Prop({ required: true })
  nom: string;

  @Prop({ required: true })
  prenom: string;

  @Prop({ required: true, unique: true })
  email: string;

  @Prop({ required: true })
  motDePasse: string;

  @Prop({ enum: UserRole, required: true })
  role: UserRole;

  @Prop()
  photoProfil?: string;

  @Prop({ type: [{ type: Types.ObjectId, ref: 'User' }], default: [] })
  enfants?: Types.ObjectId[];

  @Prop({ type: Types.ObjectId, ref: 'User' })
  parent?: Types.ObjectId;

  @Prop({ type: [{ type: Types.ObjectId, ref: 'User' }], default: [] })
  coach?: Types.ObjectId[];

  // Attributs spécifiques au Coach
  @Prop({ type: [String] })
  certification?: string[];

  @Prop()
  specialite?: string;

  @Prop()
  experience?: number; // Années d'expérience

  // Attributs spécifiques à l'Enfant
  @Prop()
  dateNaissance?: Date;

  // Attributs spécifiques à l'Académie
  @Prop()
  nomAcademie?: string;

  @Prop()
  adresse?: string; // Localisation

  @Prop()
  description?: string;

  @Prop({ type: Object })
  horaires?: {
    [jour: string]: {
      debut: string;
      fin: string;
    };
  };

  createdAt?: Date;
  updatedAt?: Date;

  @Prop()
  fcmToken?: string;
}

export const UserSchema = SchemaFactory.createForClass(User);

