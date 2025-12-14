import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';

export enum SubscriptionOptionType {
    SPORTS_OUTFIT = 'SPORTS_OUTFIT',
    INSURANCE = 'INSURANCE',
    TRANSPORT = 'TRANSPORT',
}

@Schema()
export class SubscriptionOption {
    @Prop({
        enum: SubscriptionOptionType,
        required: true
    })
    type: SubscriptionOptionType;

    @Prop({ required: true, min: 0 })
    price: number;

    @Prop({ default: 'TND' })
    currency: string;

    @Prop()
    description?: string;
}

export const SubscriptionOptionSchema = SchemaFactory.createForClass(SubscriptionOption);
