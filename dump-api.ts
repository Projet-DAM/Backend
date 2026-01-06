import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { SubscriptionsService } from './src/subscriptions/subscriptions.service';
import { UserRole } from './src/users/interfaces/user-role.enum';

async function bootstrap() {
    const app = await NestFactory.createApplicationContext(AppModule);
    const service = app.get(SubscriptionsService);

    const result = await service.findAll({}, { userId: 'admin', role: UserRole.ADMIN });
    console.log('API_RESULT:' + JSON.stringify(result, null, 2));

    await app.close();
}

bootstrap().catch(err => {
    console.error(err);
    process.exit(1);
});
