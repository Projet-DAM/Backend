import { Module } from '@nestjs/common';
import { TestController } from './test.controller';
import { EmailModule } from '../common/services/email.module';

@Module({
  imports: [EmailModule],
  controllers: [TestController],
})
export class TestModule { }
