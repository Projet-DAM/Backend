import { Body, Controller, Post, Headers, RawBodyRequest, Req, BadRequestException } from '@nestjs/common';
import { PaymentsService } from './payments.service';
import type { Request } from 'express';
import Stripe from 'stripe';

@Controller('payments')
export class PaymentsController {
    private stripe: Stripe;

    constructor(private readonly paymentsService: PaymentsService) {
        this.stripe = new Stripe(process.env.STRIPE_SECRET_KEY || '', {
            apiVersion: '2024-12-18.acacia',
        } as any);
    }

    @Post('create-payment-intent')
    async createPaymentIntent(@Body() body: {
        amount: number;
        currency?: string;
        phoneNumber?: string;
    }) {
        return this.paymentsService.createPaymentIntent(
            body.amount,
            body.currency,
            body.phoneNumber,
        );
    }

    @Post('confirm-payment')
    async confirmPayment(@Body() body: { paymentIntentId: string }) {
        return this.paymentsService.confirmPayment(body.paymentIntentId);
    }

    /**
     * Webhook Stripe pour gérer les événements de paiement
     * IMPORTANT: Configurez ce webhook dans votre dashboard Stripe
     */
    @Post('webhook')
    async handleWebhook(
        @Headers('stripe-signature') signature: string,
        @Req() request: Request,
    ) {
        const endpointSecret = process.env.STRIPE_WEBHOOK_SECRET;

        if (!endpointSecret) {
            throw new BadRequestException('STRIPE_WEBHOOK_SECRET is not set');
        }

        let event: Stripe.Event;

        try {
            // Vérifier que le webhook provient bien de Stripe
            const rawBody = (request as any).rawBody;

            if (!rawBody) {
                throw new BadRequestException('Request body is missing');
            }

            event = this.stripe.webhooks.constructEvent(
                rawBody,
                signature,
                endpointSecret,
            );
        } catch (err) {
            console.error('⚠️  Webhook signature verification failed.', err.message);
            throw new BadRequestException(`Webhook Error: ${err.message}`);
        }

        // Traiter l'événement
        return this.paymentsService.handleWebhook(event);
    }
}
