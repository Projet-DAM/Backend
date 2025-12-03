import { NestFactory } from '@nestjs/core';
import { NestFactory, Reflector } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import * as dotenv from 'dotenv';
import { existsSync, mkdirSync } from 'fs';
import { WsAdapter } from '@nestjs/platform-ws'; // Import WsAdapter
import { join } from 'path';
import * as express from 'express';
import { join } from 'path';

dotenv.config();

async function bootstrap() {
  const uploadsDir = './uploads';
  if (!existsSync(uploadsDir)) mkdirSync(uploadsDir, { recursive: true });
  const tournoisUploadsDir = './uploads/tournois';
  if (!existsSync(tournoisUploadsDir)) mkdirSync(tournoisUploadsDir, { recursive: true });

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

  const defaultPort = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;
  const maxProbePort = defaultPort + 10;
  const selectedPort = await findFreePort(defaultPort, maxProbePort);

  const app = await NestFactory.create(AppModule);
  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  // Servir les fichiers statiques
  app.useStaticAssets(join(__dirname, '..', 'uploads'), {
    prefix: '/uploads/',
  });

  // Guard global
  const jwtAuthGuard = app.get(JwtAuthGuard);
  app.useGlobalGuards(jwtAuthGuard);
  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // CORS
  app.enableCors({
    origin: '*', // Adjust this to your specific frontend URL in production
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
    credentials: true,
  });
  app.enableCors();

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
    .addTag('Auth', 'Endpoints d\'authentification')
    .addTag('Users', 'Gestion des utilisateurs')
    .addTag('SuiviEnfant', 'Suivi des enfants')
    .addTag('App', 'Endpoints généraux de l\'API')
    .addTag('Auth', 'Endpoints d\'authentification')
    .addTag('Users', 'Gestion des utilisateurs')
    .addTag('Tournois', 'Gestion des tournois')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  // Enable WebSocket adapter
  app.useWebSocketAdapter(new WsAdapter(app)); // Add this line

  // Serve uploads statically at /uploads
  const uploadsPath = join(__dirname, '..', 'uploads');
  app.use('/uploads', express.static(uploadsPath));

  // Guard global (après configuration Swagger pour ne pas bloquer /api et /api-json)
  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  // Listen on selected free port
  await app.listen(selectedPort);
  console.log(`Application is running on: http://localhost:${selectedPort}`);
  console.log(`Swagger documentation: http://localhost:${selectedPort}/api`);
}

bootstrap();