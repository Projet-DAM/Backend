
import { Module, forwardRef } from '@nestjs/common';
import { GeminiService } from './gemini.service';
import { GeminiController } from './gemini.controller';
import { MessagesModule } from '../messages/messages.module';
import { UsersModule } from '../users/users.module';
import { SuiviEnfantModule } from '../suivi-enfant/suivi-enfant.module';

@Module({
    imports: [
        forwardRef(() => MessagesModule),
        UsersModule,
        forwardRef(() => SuiviEnfantModule),
    ],
    controllers: [GeminiController],
    providers: [GeminiService],
    exports: [GeminiService],
})
export class GeminiModule { }
