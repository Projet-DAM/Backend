import { NestFactory, Reflector } from '@nestjs/core';
import { NestExpressApplication } from '@nestjs/platform-express';
import { ValidationPipe, Logger } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';
import * as dotenv from 'dotenv';
import { existsSync, mkdirSync } from 'fs';
import { join } from 'path';
import * as express from 'express';
import { JwtAuthGuard } from './auth/guards/jwt-auth.guard';

dotenv.config();

async function bootstrap() {
  const logger = new Logger('Bootstrap');

  // Ensure upload directories exist
  const uploadDirs = [
    join(process.cwd(), 'uploads'),
    join(process.cwd(), 'uploads', 'messages'),
    join(process.cwd(), 'uploads', 'messages', 'images'),
    join(process.cwd(), 'uploads', 'messages', 'audio'),
    join(process.cwd(), 'uploads', 'tournois'),
  ];

  uploadDirs.forEach((dir) => {
    if (!existsSync(dir)) {
      mkdirSync(dir, { recursive: true });
      logger.log(`Created directory: ${dir}`);
    }
  });

  const app = await NestFactory.create<NestExpressApplication>(AppModule, {
    rawBody: true,
  });

  // Request logger
  app.use((req, res, next) => {
    console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
    next();
  });

  // Serve static files
  const uploadsPath = join(process.cwd(), 'uploads');
  app.use('/uploads', express.static(uploadsPath));
  app.useStaticAssets(uploadsPath, {
    prefix: '/uploads/',
  });

  // CORS
  app.enableCors({
    origin: true,
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'Accept'],
    credentials: true,
  });

  // Validation
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
      transformOptions: { enableImplicitConversion: true },
    }),
  );

  // Global Guards
  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  // Swagger
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

  const port = Number(process.env.PORT) || 5000;
  await app.listen(port, '0.0.0.0');

  logger.log(`🚀 API running on http://localhost:${port}`);
  logger.log(`📚 Swagger: http://localhost:${port}/api`);
  logger.log(`Serving static files from: ${uploadsPath}`);
}

bootstrap();
