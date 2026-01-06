# 🔍 Diagnostic du Système de Paiement Stripe

## ✅ État de la Configuration

### 1. Variables d'Environnement
- ✅ `STRIPE_SECRET_KEY` : Configurée
- ✅ `STRIPE_WEBHOOK_SECRET` : Configurée
- ⚠️ `STRIPE_PUBLISHABLE_KEY` : À vérifier dans le frontend

### 2. Architecture Backend

#### Fichiers Clés
- ✅ `src/payments/payments.controller.ts` - Endpoints API
- ✅ `src/payments/payments.service.ts` - Logique métier Stripe
- ✅ `src/payments/payments.module.ts` - Module NestJS
- ✅ DTOs de validation

#### Endpoints Disponibles
1. **POST /payments/create-intent** - Créer un PaymentIntent
   - Requiert JWT Auth
   - Paramètres : `amount`, `currency`, `paymentMethodId`, `subscriptionId`
   
2. **POST /payments/confirm** - Confirmer un paiement
   - Requiert JWT Auth
   - Paramètres : `paymentIntentId`, `paymentMethodId`

### 3. Problèmes Potentiels Identifiés

#### ❌ Problème 1 : Absence de Webhook Handler
**Symptôme** : Les paiements ne sont pas enregistrés dans la base de données après confirmation.

**Cause** : Aucun endpoint webhook Stripe n'est configuré pour écouter les événements `payment_intent.succeeded`.

**Impact** : 
- Les paiements réussis ne créent pas d'abonnements
- Pas de confirmation par email
- Pas de mise à jour du statut de paiement

#### ⚠️ Problème 2 : Flow de Paiement Manuel
**Symptôme** : Le frontend doit appeler deux endpoints séparément.

**Cause** : Utilisation de `confirmation_method: 'manual'` dans le PaymentIntent.

**Impact** :
- Complexité accrue côté frontend
- Risque d'erreurs si la confirmation échoue
- Pas de gestion automatique des webhooks

#### ⚠️ Problème 3 : Conversion de Montant Ambiguë
**Code actuel** :
```typescript
const amountInCents = amount < 100 ? Math.round(amount * 100) : amount;
```

**Problème** : Cette logique peut causer des erreurs si le frontend envoie déjà le montant en centimes.

#### ❌ Problème 4 : Pas de Lien avec les Abonnements
**Symptôme** : Le PaymentIntent stocke `subscriptionId` en metadata mais ne crée pas l'abonnement.

**Cause** : Aucune logique pour créer automatiquement l'abonnement après paiement réussi.

## 🔧 Solutions Recommandées

### Solution 1 : Ajouter un Webhook Handler (PRIORITAIRE)

#### Étape 1 : Créer le contrôleur webhook
```typescript
// src/payments/payments.controller.ts

@Post('webhook')
@HttpCode(HttpStatus.OK)
async handleWebhook(
  @Req() request: Request,
  @Headers('stripe-signature') signature: string,
) {
  return this.paymentsService.handleWebhook(request.rawBody, signature);
}
```

#### Étape 2 : Implémenter la logique webhook
```typescript
// src/payments/payments.service.ts

async handleWebhook(rawBody: Buffer, signature: string) {
  const webhookSecret = this.configService.get<string>('STRIPE_WEBHOOK_SECRET');
  
  try {
    const event = this.stripe.webhooks.constructEvent(
      rawBody,
      signature,
      webhookSecret,
    );

    switch (event.type) {
      case 'payment_intent.succeeded':
        await this.handlePaymentSuccess(event.data.object);
        break;
      case 'payment_intent.payment_failed':
        await this.handlePaymentFailure(event.data.object);
        break;
    }

    return { received: true };
  } catch (error) {
    throw new BadRequestException('Webhook signature verification failed');
  }
}

private async handlePaymentSuccess(paymentIntent: Stripe.PaymentIntent) {
  const subscriptionId = paymentIntent.metadata.subscriptionId;
  
  if (subscriptionId) {
    // Créer ou activer l'abonnement
    await this.subscriptionsService.recordPayment(subscriptionId, {
      amount: paymentIntent.amount / 100,
      paymentMethod: 'stripe',
      transactionId: paymentIntent.id,
    });
    
    // Envoyer email de confirmation
    await this.emailService.sendPaymentConfirmation(...);
  }
}
```

### Solution 2 : Simplifier le Flow avec Automatic Confirmation

**Modifier** `createPaymentIntent` :
```typescript
const paymentIntentParams: Stripe.PaymentIntentCreateParams = {
  amount: Math.round(amount),
  currency: currency.toLowerCase(),
  payment_method: paymentMethodId,
  confirmation_method: 'automatic', // ✅ Changé de 'manual'
  confirm: true, // ✅ Confirmer immédiatement
  return_url: 'https://your-app.com/payment-success', // Pour 3D Secure
  automatic_payment_methods: {
    enabled: true,
    allow_redirects: 'never',
  },
};
```

### Solution 3 : Standardiser le Montant

**Option A** : Toujours recevoir en centimes
```typescript
// Supprimer la conversion ambiguë
const amountInCents = Math.round(amount); // Assume déjà en centimes
```

**Option B** : Toujours recevoir en unités
```typescript
const amountInCents = Math.round(amount * 100); // Toujours convertir
```

### Solution 4 : Configurer le Webhook Stripe

#### Via Stripe CLI (Développement)
```bash
stripe listen --forward-to localhost:3000/payments/webhook
```

#### Via Dashboard Stripe (Production)
1. Aller sur https://dashboard.stripe.com/webhooks
2. Cliquer "Add endpoint"
3. URL : `https://your-api.com/payments/webhook`
4. Événements : `payment_intent.succeeded`, `payment_intent.payment_failed`
5. Copier le webhook secret dans `.env`

## 🧪 Tests à Effectuer

### Test 1 : Vérifier la Configuration Stripe
```bash
# Vérifier que les clés sont chargées
curl http://localhost:3000/api
# Chercher les endpoints /payments dans Swagger
```

### Test 2 : Créer un PaymentIntent
```bash
# Obtenir un token JWT d'abord
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'

# Créer un PaymentIntent
curl -X POST http://localhost:3000/payments/create-intent \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 7000,
    "currency": "tnd",
    "paymentMethodId": "pm_card_visa",
    "subscriptionId": "SUBSCRIPTION_ID"
  }'
```

### Test 3 : Tester le Webhook (avec Stripe CLI)
```bash
stripe trigger payment_intent.succeeded
```

## 📋 Checklist de Vérification

- [ ] Les clés Stripe sont dans `.env`
- [ ] Le serveur démarre sans erreur
- [ ] Les endpoints `/payments/*` sont accessibles dans Swagger
- [ ] Un webhook handler est implémenté
- [ ] Le webhook est configuré dans Stripe Dashboard
- [ ] Les abonnements sont créés après paiement réussi
- [ ] Les emails de confirmation sont envoyés
- [ ] Les erreurs de paiement sont gérées

## 🚨 Erreurs Courantes

### Erreur : "Stripe n'est pas configuré"
**Solution** : Vérifier que `STRIPE_SECRET_KEY` est dans `.env` et redémarrer le serveur.

### Erreur : "No such payment_method"
**Solution** : Le `paymentMethodId` doit être créé côté frontend avec Stripe.js avant d'appeler l'API.

### Erreur : "Webhook signature verification failed"
**Solution** : Vérifier que `STRIPE_WEBHOOK_SECRET` correspond au secret du webhook dans Stripe Dashboard.

### Paiement réussi mais pas d'abonnement créé
**Solution** : Implémenter le webhook handler pour `payment_intent.succeeded`.

## 📞 Prochaines Étapes

1. **Immédiat** : Implémenter le webhook handler
2. **Court terme** : Tester le flow complet de paiement
3. **Moyen terme** : Ajouter la gestion des remboursements
4. **Long terme** : Implémenter les abonnements récurrents Stripe
