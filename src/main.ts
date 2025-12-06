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

  const defaultPort = process.env.PORT ? parseInt(process.env.PORT, 10) : 3000;
  const maxProbePort = defaultPort + 10;
  const selectedPort = await findFreePort(defaultPort, maxProbePort);

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
    }),
  );

  // CORS - Allow all origins for development
  app.enableCors({
    origin: '*',
    methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
    credentials: true,
    allowedHeaders: ['Content-Type', 'Authorization', 'Accept'],
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

  // Enable Socket.IO adapter (matches MessagesGateway using socket.io)
  app.useWebSocketAdapter(new IoAdapter(app));

  // Guard global (après configuration Swagger pour ne pas bloquer /api et /api-json)
  // IMPORTANT: This is AFTER static file serving so /uploads/* is publicly accessible
  const reflector = app.get(Reflector);
  app.useGlobalGuards(new JwtAuthGuard(reflector));

  // Listen on ALL network interfaces (0.0.0.0) so external devices can connect
  await app.listen(selectedPort, '0.0.0.0');
  
  // Get and display all network addresses for easy access
  const os = require('os');
  const networkInterfaces = os.networkInterfaces();
  const addresses: string[] = [];
  
  Object.keys(networkInterfaces).forEach(interfaceName => {
    networkInterfaces[interfaceName].forEach((iface: any) => {
      if (iface.family === 'IPv4' && !iface.internal) {
        addresses.push(iface.address);
      }
    });
  });
  
  console.log('\n========================================');
  console.log('🚀 Backend Server Started Successfully!');
  console.log('========================================');
  console.log(`📍 Listening on port: ${selectedPort}`);
  console.log(`📂 Serving uploads from: ${uploadsPath}`);
  console.log('\n🌐 Access the server from:');
  console.log(`   Local:    http://localhost:${selectedPort}`);
  addresses.forEach(addr => {
    console.log(`   Network:  http://${addr}:${selectedPort}`);
  });
  console.log('\n📱 For Android app, use one of the Network URLs above');
  console.log(`📖 Swagger API docs: http://localhost:${selectedPort}/api`);
  console.log(`🔍 Network info: http://localhost:${selectedPort}/diagnostics/network/info`);
  console.log(`🖼️  Test image access: http://localhost:${selectedPort}/test-image`);
  console.log('========================================\n');
  
  console.log('⚠️  IMPORTANT: Ensure Windows Firewall allows port', selectedPort);
  console.log('   Run as Admin: New-NetFirewallRule -DisplayName "Node Dev" -Direction Inbound -Action Allow -Protocol TCP -LocalPort', selectedPort);
  console.log('========================================\n');
}

bootstrap();