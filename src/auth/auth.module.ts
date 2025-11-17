import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { PassportModule } from '@nestjs/passport';
import { AuthService } from './auth.service';
import { AuthController } from './auth.controller';
import { JwtStrategy } from './jwt.strategy';
import { UsersModule } from '../users/users.module';
import { JwtAuthGuard } from './guards/jwt-auth.guard';
import { EmailModule } from '../common/services/email.module';

@Module({
  imports: [
    UsersModule,
    PassportModule,
    EmailModule,
    JwtModule.register({
      secret: process.env.JWT_SECRET || 'secretKey123',
      signOptions: { 
        expiresIn: (process.env.JWT_EXPIRES_IN || '1d') as any
      },
    }),
  ],
  controllers: [AuthController],
  providers: [AuthService, JwtStrategy, JwtAuthGuard],
  exports: [AuthService, JwtAuthGuard],
})
export class AuthModule {}

