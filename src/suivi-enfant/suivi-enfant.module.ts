import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { SuiviEnfant, SuiviEnfantSchema } from './suivi-enfant.schema';
import { SuiviEnfantService } from './suivi-enfant.service';
import { SuiviEnfantController } from './suivi-enfant.controller';
import { UsersModule } from '../users/users.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: SuiviEnfant.name, schema: SuiviEnfantSchema }]),
    UsersModule,
  ],
  controllers: [SuiviEnfantController],
  providers: [SuiviEnfantService],
  exports: [SuiviEnfantService],
})
export class SuiviEnfantModule {}
