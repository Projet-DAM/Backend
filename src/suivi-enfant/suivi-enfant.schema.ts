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

    // optional: derived from child user when omitted
    @Prop({ type: String })
    enfantName?: string;

    @Prop({ type: Types.ObjectId, ref: 'User', required: true })
    coach: Types.ObjectId;

    @Prop({ type: String })
    activityType?: string;

    @Prop({ type: [String] })
    focusAreas?: string[];

    @Prop({ type: [String] })
    nextSessionGoals?: string[];

    @Prop({ type: Number, min: 1, max: 10 })
    effortLevel?: number;

    @Prop({ type: String })
    emotionalState?: string;
  }

  export type SuiviEnfantDocument = SuiviEnfant & Document;
  export const SuiviEnfantSchema = SchemaFactory.createForClass(SuiviEnfant);

  // expose enfantId virtual as string and enable virtuals in JSON
  SuiviEnfantSchema.virtual('enfantId').get(function (this: any) {
    const e = this.enfant;
    if (!e) return undefined;
    // If populated document, prefer its _id
    if (e._id) {
      try {
        return e._id.toString();
      } catch (err) {
        return String(e._id);
      }
    }
    // If it's a plain ObjectId or string, normalize to string if it looks like one
    try {
      const s = typeof e === 'string' ? e : e.toString();
      if (/^[0-9a-fA-F]{24}$/.test(s)) return s;
      return s;
    } catch (err) {
      return undefined;
    }
  });
  SuiviEnfantSchema.set('toJSON', { virtuals: true, versionKey: false });
