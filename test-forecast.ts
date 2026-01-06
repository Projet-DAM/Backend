
import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { ForecastService } from './src/subscriptions/forecast.service';

async function bootstrap() {
    const app = await NestFactory.createApplicationContext(AppModule);
    const forecastService = app.get(ForecastService);

    const academyId = '690cd9998d614e72c9b1ab55';
    console.log(`--- Testing Revenue Forecast for academyId: ${academyId} ---`);
    try {
        const result = await forecastService.generateRevenueForecast(academyId);
        console.log('Result:', JSON.stringify(result, null, 2));
    } catch (error) {
        console.error('Error:', error);
    } finally {
        await app.close();
    }
}

bootstrap();
