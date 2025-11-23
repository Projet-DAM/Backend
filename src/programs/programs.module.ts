import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { ProgramsService } from './programs.service';
import { ProgramsController } from './programs.controller';
import { Program, ProgramSchema } from './schemas/program.schema';
import { Activity, ActivitySchema } from '../activities/schemas/activity.schema';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Program.name, schema: ProgramSchema },
      { name: Activity.name, schema: ActivitySchema },
    ]),
  ],
  controllers: [ProgramsController],
  providers: [ProgramsService],
  exports: [ProgramsService],
})
export class ProgramsModule {}



