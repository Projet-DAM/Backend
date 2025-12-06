import { ApiProperty } from '@nestjs/swagger';
import { IsEnum, IsOptional } from 'class-validator';
import { EnrollmentStatus } from '../entity/enrollment.entity';

export class UpdateEnrollmentDto {
    @ApiProperty({
        description: 'Statut de l\'inscription',
        enum: EnrollmentStatus,
        example: EnrollmentStatus.COMPLETED,
        required: false,
    })
    @IsEnum(EnrollmentStatus)
    @IsOptional()
    status?: EnrollmentStatus;
}
