import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { UsersModule } from './users/users.module';
import { AuthModule } from './auth/auth.module';
import { TournoiModule } from './tournoi/tournoi.module';
import { InscriptionModule } from './inscriptions/inscription.module';
import { EquipeModule } from './equipes/equipe.module';
import { MatchesModule } from './matches/matches.module';
import { FirebaseModule } from './firebase/firebase.module';
import { NotificationModule } from './notifications/notification.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    MongooseModule.forRoot(process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect'),
    UsersModule,
    AuthModule,
    TournoiModule,
    InscriptionModule,
    EquipeModule,
    MatchesModule,
    FirebaseModule,
    NotificationModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
