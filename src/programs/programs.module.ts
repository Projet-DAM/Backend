import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ProgramsService } from './programs.service';
import { ProgramsController } from './programs.controller';
import { Program, ProgramSchema } from './schemas/program.schema';
import { Activity, ActivitySchema } from '../activities/schemas/activity.schema';
import { Enrollment, EnrollmentSchema } from '../enrollments/entity/enrollment.entity';

import { UsersModule } from '../users/users.module';
import { FirebaseService } from '../common/firebase.service';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Program.name, schema: ProgramSchema },
      { name: Activity.name, schema: ActivitySchema },
      { name: Enrollment.name, schema: EnrollmentSchema },
    ]),
    UsersModule,
  ],
  controllers: [ProgramsController],
  providers: [ProgramsService, FirebaseService],
  exports: [ProgramsService],
})
export class ProgramsModule { }



