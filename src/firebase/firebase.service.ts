import { Injectable, Logger } from '@nestjs/common';
import * as admin from 'firebase-admin';

type TopicPayload = Omit<admin.messaging.Message, 'token' | 'tokens' | 'topic' | 'condition'>;

@Injectable()
export class FirebaseService {
  private readonly logger = new Logger(FirebaseService.name);
  private app?: admin.app.App;

  constructor() {
    this.initialize();
  }

  isEnabled(): boolean {
    return !!this.app;
  }

  async sendToTopic(topic: string, message: TopicPayload): Promise<void> {
    await this.sendMessage({
      ...message,
      topic,
    });
  }

  private async sendMessage(message: admin.messaging.Message): Promise<void> {
    if (!this.app) {
      throw new Error('Firebase n\'est pas configuré. Impossible d\'envoyer la notification.');
    }

    await admin.messaging(this.app).send(message);
  }

  private initialize(): void {
    const projectId = process.env.FIREBASE_PROJECT_ID;
    const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;
    const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n');

    if (!projectId || !clientEmail || !privateKey) {
      this.logger.warn('Variables d\'environnement Firebase manquantes : notifications désactivées.');
      return;
    }

    try {
      if (admin.apps.length) {
        this.app = admin.app();
      } else {
        this.app = admin.initializeApp({
          credential: admin.credential.cert({
            projectId,
            clientEmail,
            privateKey,
          }),
        });
      }
      this.logger.log('Firebase initialisé avec succès');
    } catch (error) {
      this.logger.error('Échec de l\'initialisation Firebase', (error as Error).stack);
      this.app = undefined;
    }
  }
}


