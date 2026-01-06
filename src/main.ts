import { NestFactory, Reflector } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';
import * as dotenv from 'dotenv';
import { existsSync, mkdirSync } from 'fs';
import { join } from 'path';
import * as express from 'express';

dotenv.config();

async function bootstrap() {
  const uploadsDir = './uploads';
  if (!existsSync(uploadsDir)) mkdirSync(uploadsDir, { recursive: true });

  const tournoisUploadsDir = './uploads/tournois';
  if (!existsSync(tournoisUploadsDir)) mkdirSync(tournoisUploadsDir, { recursive: true });

  const app = await NestFactory.create<NestExpressApplication>(AppModule);

  app.useStaticAssets(join(__dirname, '..', 'uploads'), {
    prefix: '/uploads/',
  });

  app.enableCors({
    origin: '*',
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
    credentials: true,
  });

  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

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
        in: 'header',
      },
      'JWT-auth',
    )
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  app.use('/uploads', express.static(join(__dirname, '..', 'uploads')));

  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  const port = Number(process.env.PORT) || 5000;
  await app.listen(port, '127.0.0.1');


  console.log(`🚀 API running on http://localhost:${port}`);
  console.log(`📚 Swagger: http://localhost:${port}/api`);
}

bootstrap();