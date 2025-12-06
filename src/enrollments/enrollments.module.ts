import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { EnrollmentsService } from './enrollments.service';
import { EnrollmentsController } from './enrollments.controller';
import { Enrollment, EnrollmentSchema } from './entity/enrollment.entity';
import { Program, ProgramSchema } from '../programs/schemas/program.schema';

@Module({
    imports: [
        MongooseModule.forFeature([
            { name: Enrollment.name, schema: EnrollmentSchema },
            { name: Program.name, schema: ProgramSchema },
        ]),
    ],
    controllers: [EnrollmentsController],
    providers: [EnrollmentsService],
    exports: [EnrollmentsService],
})
export class EnrollmentsModule { }
