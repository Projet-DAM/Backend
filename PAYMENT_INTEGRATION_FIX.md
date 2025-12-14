# Payment Integration Fix - Summary

## Problem
The Android app was sending `childId` and `offerId` to the `/payments/create-intent` endpoint, but the backend was expecting `amount` and `paymentMethodId`, causing a 400 Bad Request error.

## Solution
Updated the backend to support the Android app's payment flow:

### 1. Updated `CreatePaymentIntentDto` (`src/payments/dto/create-payment-intent.dto.ts`)
- Made `amount` and `paymentMethodId` optional
- Added `childId` and `offerId` as optional fields
- This allows the endpoint to accept either:
  - Direct payment: `{ amount, paymentMethodId }`
  - Subscription payment: `{ childId, offerId }`

### 2. Updated `PaymentsService` (`src/payments/payments.service.ts`)
- Made `paymentMethodId` optional in `createPaymentIntent` method
- When `paymentMethodId` is not provided, Stripe creates a PaymentIntent that can be completed later via PaymentSheet
- Removed `confirmation_method: 'manual'` and `allow_redirects: 'never'` to support automatic payment methods

### 3. Updated `PaymentsModule` (`src/payments/payments.module.ts`)
- Added `OffersModule` to imports to make `OffersService` available

### 4. Updated `PaymentsController` (`src/payments/payments.controller.ts`)
- Injected `OffersService` and `ConfigService`
- Added logic to handle `childId` and `offerId`:
  - Fetches the offer using `OffersService.findOne(offerId)`
  - Calculates amount from offer price (converts to cents)
  - Validates that offer exists
- Returns `publishableKey` in the response (from `STRIPE_PUBLISHABLE_KEY` env variable)
- Response now includes: `{ clientSecret, paymentIntentId, publishableKey }`

## Environment Variables
Make sure `.env` contains:
```
STRIPE_SECRET_KEY=sk_test_51SV9DGHQ95HR9DAI2oVkJgxhwzJGTIaspfxrziZUsglPtwGuBmOXdBEhQj7X3nZSoJhLNRI0B9hrX23IiOiBBUel00KvsbelPY
STRIPE_PUBLISHABLE_KEY=pk_test_51SV9DGHQ95HR9DAIM06ZwtZcdrI4VWkwDvZ5SToJa1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5ImqCVBk6W00VGzt9lMG
STRIPE_WEBHOOK_SECRET=whsec_test_placeholder
```

## Testing the Payment Flow

### 1. Test with Android App
The Android app should now be able to:
1. Call `POST /payments/create-intent` with `{ childId, offerId }`
2. Receive `{ clientSecret, paymentIntentId, publishableKey }`
3. Use `clientSecret` to present the Stripe PaymentSheet
4. Complete the payment

### 2. Test with cURL/Postman
```bash
# Get auth token first
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@test.com","motDePasse":"password123"}'

# Create payment intent with childId and offerId
curl -X POST http://localhost:3000/payments/create-intent \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"childId":"CHILD_ID","offerId":"OFFER_ID"}'

# Expected response:
{
  "clientSecret": "pi_xxx_secret_yyy",
  "paymentIntentId": "pi_xxx",
  "publishableKey": "pk_test_..."
}
```

### 3. Test with Direct Amount
The endpoint still supports direct amount specification:
```bash
curl -X POST http://localhost:3000/payments/create-intent \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{"amount":5000,"currency":"eur"}'
```

## Payment Flow Diagram

```
Android App                    Backend                      Stripe
    |                             |                            |
    |--POST /payments/create-intent (childId, offerId)-------->|
    |                             |                            |
    |                             |--Fetch Offer (OffersService)
    |                             |                            |
    |                             |--Create PaymentIntent----->|
    |                             |                            |
    |                             |<--clientSecret-------------|
    |                             |                            |
    |<--{clientSecret, publishableKey}-------------------------|
    |                             |                            |
    |--Present PaymentSheet------>|                            |
    |                             |                            |
    |--User enters card details-->|                            |
    |                             |                            |
    |--Confirm Payment----------->|                            |
    |                             |--Confirm PaymentIntent---->|
    |                             |                            |
    |                             |<--Payment Success----------|
    |                             |                            |
    |<--Payment Success-----------|                            |
```

## Next Steps
1. ✅ Backend updated to accept `childId` and `offerId`
2. ✅ Backend returns `publishableKey`
3. ✅ Build successful
4. 🔄 Test with Android app
5. 🔄 Implement webhook handler to update subscription status after successful payment
6. 🔄 Send payment confirmation email

## Notes
- The PaymentIntent is created without a payment method initially
- The payment method is collected via Stripe's PaymentSheet UI in the Android app
- After successful payment, you should implement a webhook handler to:
  - Create the subscription record
  - Update payment status
  - Send confirmation email
