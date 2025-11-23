import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { UsersModule } from './users/users.module';
import { ActivitiesModule } from './activities/activities.module';
import { AuthModule } from './auth/auth.module';
import { ProgramsModule } from './programs/programs.module';
import { ServeStaticModule } from '@nestjs/serve-static';
import { join } from 'path';

 @Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true, envFilePath: '.env' }),
    MongooseModule.forRoot(process.env.MONGO_URI || 'mongodb://localhost:27017/sportyconnect'),
    // <‑‑ SERVE STATIC BEGIN
    ServeStaticModule.forRoot({
      // Chemin du répertoire contenant les images.
      // __dirname pointe vers dist (ou src en mode dev) ; on remonte d’un niveau
      // puis on indique le dossier uploads à la racine du projet.
      rootPath: join(__dirname, '..', 'uploads'),

      // URL à laquelle les fichiers seront accessibles depuis le client.
      // Exemple : http://<host>:3000/uploads/mon‑image.jpg
      serveRoot: '/uploads',
    }),
    // <‑‑ SERVE STATIC END
    UsersModule,
    ActivitiesModule,
    AuthModule,
    ProgramsModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}