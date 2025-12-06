import { Injectable } from '@nestjs/common';
import * as admin from 'firebase-admin';
import { ConfigService } from '@nestjs/config';
import * as path from 'path';

@Injectable()
export class FirebaseService {
    constructor(private configService: ConfigService) {
        // Initialize Firebase Admin SDK
        const serviceAccountPath = this.configService.get<string>('FIREBASE_SERVICE_ACCOUNT_PATH') || './firebase-service-account.json';

        try {
            // Resolve to absolute path
            const absolutePath = path.resolve(process.cwd(), serviceAccountPath);
            console.log(`[FIREBASE] Attempting to load service account from: ${absolutePath}`);

            // eslint-disable-next-line @typescript-eslint/no-var-requires
            const serviceAccount = require(absolutePath);

            if (!admin.apps.length) {
                admin.initializeApp({
                    credential: admin.credential.cert(serviceAccount),
                });
                console.log('[FIREBASE] Firebase Admin SDK initialized successfully');
            } else {
                console.log('[FIREBASE] Firebase Admin SDK already initialized');
            }
        } catch (error) {
            console.error('[FIREBASE] Failed to initialize Firebase Admin SDK:', error.message);
            console.error('[FIREBASE] Push notifications will not work. Please check your firebase-service-account.json file.');
        }
    }

    async sendPushNotification(token: string, title: string, body: string, data: any = {}, imageUrl?: string, channelId: string = 'general') {
        if (!token) return;

        // Ensure all data values are strings
        const stringData = Object.keys(data).reduce((acc, key) => {
            acc[key] = String(data[key]);
            return acc;
        }, {});

        // Add rich notification fields to data payload
        stringData['title'] = title;
        stringData['body'] = body;
        if (imageUrl) stringData['image'] = imageUrl;
        stringData['channel_id'] = channelId;

        try {
            await admin.messaging().send({
                token: token,
                notification: {
                    title: title,
                    body: body,
                },
                data: stringData,
            });
            console.log(`[FIREBASE] Push notification sent to ${token.substring(0, 10)}... (Channel: ${channelId})`);
        } catch (error) {
            console.error('[FIREBASE] Error sending push notification:', error);
        }
    }

    async sendMulticastNotification(tokens: string[], title: string, body: string, data: any = {}, imageUrl?: string, channelId: string = 'general'): Promise<string[]> {
        if (!tokens || tokens.length === 0) return [];

        // Ensure all data values are strings
        const stringData = Object.keys(data).reduce((acc, key) => {
            acc[key] = String(data[key]);
            return acc;
        }, {});

        // Add rich notification fields to data payload
        stringData['title'] = title;
        stringData['body'] = body;
        if (imageUrl) stringData['image'] = imageUrl;
        stringData['channel_id'] = channelId;

        const failedTokens: string[] = [];

        try {
            const message = {
                notification: {
                    title: title,
                    body: body
                },
                data: stringData,
                tokens: tokens
            };

            const response = await admin.messaging().sendEachForMulticast(message);
            console.log(`[FIREBASE] ${response.successCount} messages sent successfully (Channel: ${channelId})`);

            if (response.failureCount > 0) {
                response.responses.forEach((resp, idx) => {
                    if (!resp.success) {
                        failedTokens.push(tokens[idx]);
                        console.error(`[FIREBASE] Failure for token ${tokens[idx]}:`, resp.error);
                    }
                });
                console.log('[FIREBASE] List of tokens that caused failures: ' + failedTokens);
            }
        } catch (error) {
            console.error('[FIREBASE] Error sending multicast notification:', error);
        }

        return failedTokens;
    }
}
