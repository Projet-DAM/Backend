import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { AppModule } from './app.module';

async function bootstrap() {
    const app = await NestFactory.create(AppModule);

    // Enable CORS pour permettre les requêtes depuis Android
    app.enableCors({
        origin: '*', // En production, spécifiez les origines autorisées
        methods: 'GET,HEAD,PUT,PATCH,POST,DELETE',
        credentials: true,
    });

    // Validation globale des DTOs
    app.useGlobalPipes(
        new ValidationPipe({
            whitelist: true,
            transform: true,
        }),
    );

    const port = process.env.PORT || 3001;
    await app.listen(port);

    console.log(`🚀 Backend Gemini AI is running on: http://localhost:${port}`);
    console.log(`📡 API endpoint: http://localhost:${port}/api/gemini/feedback/generate`);
}

bootstrap();
