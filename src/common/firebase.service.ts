import { Injectable, Logger } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { ConfigService } from '@nestjs/config';
import * as path from 'path';

type TopicPayload = Omit<admin.messaging.Message, 'token' | 'tokens' | 'topic' | 'condition'>;

@Injectable()
export class FirebaseService {
    private readonly logger = new Logger(FirebaseService.name);
    private app?: admin.app.App;

    constructor(private configService: ConfigService) {
        this.initialize();
    }

    private initialize(): void {
        if (admin.apps.length) {
            this.app = admin.app();
            this.logger.log('[FIREBASE] Firebase Admin SDK already initialized');
            return;
        }

        // Try environment variables first
        const projectId = this.configService.get<string>('FIREBASE_PROJECT_ID');
        const clientEmail = this.configService.get<string>('FIREBASE_CLIENT_EMAIL');
        const privateKey = this.configService.get<string>('FIREBASE_PRIVATE_KEY')?.replace(/\\n/g, '\n');

        if (projectId && clientEmail && privateKey) {
            try {
                this.app = admin.initializeApp({
                    credential: admin.credential.cert({
                        projectId,
                        clientEmail,
                        privateKey,
                    }),
                });
                this.logger.log('[FIREBASE] Firebase initialized successfully via env variables');
                return;
            } catch (error) {
                this.logger.error('[FIREBASE] Failed to initialize via env variables', error.stack);
            }
        }

        // Fallback to service account file
        const serviceAccountPath = this.configService.get<string>('FIREBASE_SERVICE_ACCOUNT_PATH') || './firebase-service-account.json';
        try {
            const absolutePath = path.resolve(process.cwd(), serviceAccountPath);
            if (require('fs').existsSync(absolutePath)) {
                const serviceAccount = require(absolutePath);
                this.app = admin.initializeApp({
                    credential: admin.credential.cert(serviceAccount),
                });
                this.logger.log(`[FIREBASE] Firebase initialized successfully via file: ${absolutePath}`);
            } else {
                this.logger.warn(`[FIREBASE] Service account file not found at ${absolutePath}`);
            }
        } catch (error) {
            this.logger.error('[FIREBASE] Failed to initialize via service account file', error.message);
        }
    }

    isEnabled(): boolean {
        return !!this.app;
    }

    async sendPushNotification(token: string, title: string, body: string, data: any = {}, imageUrl?: string, channelId: string = 'general') {
        if (!this.app || !token) return;

        const stringData = Object.keys(data).reduce((acc, key) => {
            acc[key] = String(data[key]);
            return acc;
        }, {});

        stringData['title'] = title;
        stringData['body'] = body;
        if (imageUrl) stringData['image'] = imageUrl;
        stringData['channel_id'] = channelId;

        try {
            await admin.messaging(this.app).send({
                token,
                notification: { title, body },
                data: stringData,
            });
            this.logger.log(`[FIREBASE] Push notification sent to ${token.substring(0, 10)}...`);
        } catch (error) {
            this.logger.error('[FIREBASE] Error sending push notification', error);
        }
    }

    async sendToTopic(topic: string, message: TopicPayload): Promise<void> {
        if (!this.app) {
            this.logger.warn('[FIREBASE] Firebase not initialized. Cannot send to topic.');
            return;
        }
        await admin.messaging(this.app).send({
            ...message,
            topic,
        });
    }

    async sendMulticastNotification(tokens: string[], title: string, body: string, data: any = {}, imageUrl?: string, channelId: string = 'general'): Promise<string[]> {
        if (!this.app || !tokens || tokens.length === 0) return [];

        const stringData = Object.keys(data).reduce((acc, key) => {
            acc[key] = String(data[key]);
            return acc;
        }, {});

        stringData['title'] = title;
        stringData['body'] = body;
        if (imageUrl) stringData['image'] = imageUrl;
        stringData['channel_id'] = channelId;

        const failedTokens: string[] = [];
        try {
            const response = await admin.messaging(this.app).sendEachForMulticast({
                notification: { title, body },
                data: stringData,
                tokens,
            });
            if (response.failureCount > 0) {
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) failedTokens.push(tokens[idx]);
                });
            }
        } catch (error) {
            this.logger.error('[FIREBASE] Error sending multicast notification', error);
        }
        return failedTokens;
    }
}
