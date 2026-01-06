import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { ScheduleModule } from '@nestjs/schedule';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { UsersModule } from './users/users.module';
import { AuthModule } from './auth/auth.module';
import { SuiviEnfantModule } from './suivi-enfant/suivi-enfant.module';
import { UploadsModule } from './uploads/uploads.module';
import { MessagesModule } from './messages/messages.module';

import { OffersModule } from './offers/offers.module';
import { SubscriptionsModule } from './subscriptions/subscriptions.module';
import { PaymentsModule } from './payments/payments.module';
import { TournoiModule } from './tournoi/tournoi.module';
import { InscriptionModule } from './inscriptions/inscription.module';
import { EquipeModule } from './equipes/equipe.module';
import { MatchesModule } from './matches/matches.module';
import { FirebaseModule } from './firebase/firebase.module';
import { NotificationModule } from './notifications/notification.module';
import { SubscriptionOptionsModule } from './subscription-options/subscription-options.module';
import { ChatbotModule } from './chatbot/chatbot.module';
import { AnalyticsModule } from './analytics/analytics.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    ScheduleModule.forRoot(),
    MongooseModule.forRoot(process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect'),
    UsersModule,
    AuthModule,
    SuiviEnfantModule,
    UploadsModule,
    MessagesModule,
    OffersModule,
    SubscriptionsModule,
    PaymentsModule,
    TournoiModule,
    InscriptionModule,
    EquipeModule,
    MatchesModule,
    FirebaseModule,
    NotificationModule,
    SubscriptionOptionsModule,
    ChatbotModule,
    AnalyticsModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule { }
