import { NestFactory, Reflector } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import * as dotenv from 'dotenv';
import { existsSync, mkdirSync } from 'fs';
import { IoAdapter } from '@nestjs/platform-socket.io';
import * as express from 'express';
import { join } from 'path';

dotenv.config();

async function bootstrap() {
  // Ensure all upload directories exist
  const uploadDirs = [
    './uploads',
    './uploads/messages',
    './uploads/messages/images',
    './uploads/messages/audio',
    './uploads/tournois',
  ];
  
  uploadDirs.forEach(dir => {
    if (!existsSync(dir)) {
      mkdirSync(dir, { recursive: true });
      console.log(`Created directory: ${dir}`);
    }
  });

  // Find a free port first to avoid partial app startup logs followed by EADDRINUSE
  const net = await import('net');
  async function findFreePort(start: number, end: number): Promise<number> {
    for (let p = start; p <= end; p++) {
      // attempt to bind a temporary server
      // eslint-disable-next-line no-await-in-loop
      const free = await new Promise<boolean>((resolve) => {
        const tester = net.createServer()
          .once('error', () => {
            resolve(false);
          })
          .once('listening', () => {
            tester.close();
            resolve(true);
          })
          .listen(p, '0.0.0.0');
      });
      if (free) return p;
    }
    throw new Error(`No free port in range ${start}-${end}`);
  }

  const app = await NestFactory.create(AppModule, {
    rawBody: true,
  });

  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  // Request logging middleware (before other middleware)
  app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
  });

  // Servir les fichiers statiques - MUST be before global guards
  // In production (dist folder), go up one more level to reach the project root
  // dist/main.js -> ../uploads would be dist/uploads (wrong)
  // dist/main.js -> ../../uploads would be project-root/uploads (correct)
  const uploadsPath = join(__dirname, '..', '..', 'uploads');
  console.log(`Serving static files from: ${uploadsPath}`);
  
  // Use Express static middleware directly for better control
  app.use('/uploads', express.static(uploadsPath));

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: {
        enableImplicitConversion: true,
      },
    }),
  );

  // CORS - Configuration pour permettre les requêtes depuis Swift
  app.enableCors({
    origin: true, // Permet toutes les origines (à restreindre en production)
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'Accept'],
    credentials: true,
    exposedHeaders: ['Content-Type', 'Authorization'],
  });

  const config = new DocumentBuilder()
    .setTitle('SportyConnect Kids API')
    .setDescription('API REST pour la gestion des utilisateurs et authentification JWT')
    .setVersion('1.0')
    .addBearerAuth(
      {
        type: 'http',
        scheme: 'bearer',
        bearerFormat: 'JWT',
        name: 'JWT',
        description: 'Enter JWT token',
        in: 'header',
      },
      'JWT-auth',
    )
    .addSecurityRequirements('JWT-auth')
    .addTag('SuiviEnfant', 'Suivi des enfants')
    .addTag('App', 'Endpoints généraux de l\'API')
    .addTag('Tournois', 'Gestion des tournois')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  const port = process.env.PORT || 3000;
  await app.listen(3000, '0.0.0.0');
  console.log(`Application is running on: http://localhost:${port}`);
  console.log(`Swagger documentation: http://localhost:${port}/api`);
}

bootstrap();