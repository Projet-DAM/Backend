import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export type EnrollmentDocument = Enrollment & Document;

export enum EnrollmentStatus {
    PENDING = 'PENDING',
    ACTIVE = 'ACTIVE',
    COMPLETED = 'COMPLETED',
    CANCELLED = 'CANCELLED',
}

@Schema({ timestamps: true })
export class Enrollment {
    _id: Types.ObjectId;

    @Prop({ type: Types.ObjectId, ref: 'User', required: true })
    child: Types.ObjectId;

    @Prop({ type: Types.ObjectId, ref: 'Program', required: true })
    program: Types.ObjectId;

    @Prop({ type: Types.ObjectId, ref: 'User', required: true })
    parent: Types.ObjectId;

    @Prop({ type: Date, default: Date.now })
    enrollmentDate: Date;

    @Prop({ enum: EnrollmentStatus, default: EnrollmentStatus.ACTIVE })
    status: EnrollmentStatus;

    @Prop({ required: true })
    amountPaid: number;

    createdAt?: Date;
    updatedAt?: Date;
}

export const EnrollmentSchema = SchemaFactory.createForClass(Enrollment);

// Index to prevent duplicate enrollments
EnrollmentSchema.index({ child: 1, program: 1 }, { unique: true });
// Index for querying by parent
EnrollmentSchema.index({ parent: 1 });
// Index for querying by program
EnrollmentSchema.index({ program: 1 });
