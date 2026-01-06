import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { ScheduleModule } from '@nestjs/schedule';
import { ServeStaticModule } from '@nestjs/serve-static';
import { join } from 'path';

import { AppController } from './app.controller';
import { AppService } from './app.service';
import { UsersModule } from './users/users.module';
import { ActivitiesModule } from './activities/activities.module';
import { AuthModule } from './auth/auth.module';
import { ProgramsModule } from './programs/programs.module';
import { EnrollmentsModule } from './enrollments/enrollments.module';
import { PaymentsModule } from './payments/payments.module';
import { CallsModule } from './calls/calls.module';
import { DiagnosticsModule } from './diagnostics/diagnostics.module';
import { EquipeModule } from './equipes/equipe.module';
import { InscriptionModule } from './inscriptions/inscription.module';
import { MatchesModule } from './matches/matches.module';
import { MessagesModule } from './messages/messages.module';
import { NotificationModule } from './notifications/notification.module';
import { SubscriptionOptionsModule } from './subscription-options/subscription-options.module';
import { ChatbotModule } from './chatbot/chatbot.module';
import { AnalyticsModule } from './analytics/analytics.module';
import { OffersModule } from './offers/offers.module';
import { SubscriptionsModule } from './subscriptions/subscriptions.module';
import { SuiviEnfantModule } from './suivi-enfant/suivi-enfant.module';
import { TournoiModule } from './tournoi/tournoi.module';
import { UploadsModule } from './uploads/uploads.module';
import { GeminiModule } from './gemini/gemini.module';
import { FirebaseService } from './common/firebase.service';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    ScheduleModule.forRoot(),
    MongooseModule.forRoot(process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect'),
    ServeStaticModule.forRoot({
      rootPath: join(__dirname, '..', 'uploads'),
      serveRoot: '/uploads',
    }),
    UsersModule,
    ActivitiesModule,
    AuthModule,
    ProgramsModule,
    EnrollmentsModule,
    PaymentsModule,
    CallsModule,
    DiagnosticsModule,
    EquipeModule,
    InscriptionModule,
    MatchesModule,
    MessagesModule,
    NotificationModule,
    SubscriptionOptionsModule,
    ChatbotModule,
    AnalyticsModule,
    OffersModule,
    SubscriptionsModule,
    SuiviEnfantModule,
    TournoiModule,
    UploadsModule,
    GeminiModule,
  ],
  controllers: [AppController],
  providers: [AppService, FirebaseService],
  exports: [FirebaseService],
})
export class AppModule { }
