import { Module } from '@nestjs/common';
import { NotificationService } from './notification.service';
import { FirebaseModule } from '../firebase/firebase.module';

@Module({
  imports: [FirebaseModule],
  providers: [NotificationService],
  exports: [NotificationService],
})
export class NotificationModule {}




