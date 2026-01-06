import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type SubscriptionOptionDocument = SubscriptionOption & Document;

export enum SubscriptionOptionType {
    SPORTS_OUTFIT = 'SPORTS_OUTFIT',
    INSURANCE = 'INSURANCE',
    TRANSPORT = 'TRANSPORT',
}

@Schema({
    timestamps: true,
    toJSON: {
        virtuals: true,
        transform: (doc: any, ret: any) => {
            ret.id = ret._id?.toString() || ret.id;
            // Ne pas supprimer _id pour compatibilité
            return ret;
        },
    },
    toObject: { virtuals: true },
})
export class SubscriptionOption {
    @Prop({ required: true, unique: true })
    type: string; // SPORTS_OUTFIT, INSURANCE, TRANSPORT

    @Prop({ required: true })
    name: string; // Changé de displayName à name pour corespondre à la doc utilisateur

    @Prop({ required: true })
    description: string;

    @Prop({ required: true, type: Number })
    price: number;

    @Prop({ default: true })
    isActive: boolean;

    @Prop({ type: Number, default: 0 })
    displayOrder: number;
}

export const SubscriptionOptionSchema = SchemaFactory.createForClass(SubscriptionOption);

SubscriptionOptionSchema.virtual('id').get(function (this: any) {
    return this._id?.toHexString ? this._id.toHexString() : this._id?.toString();
});
