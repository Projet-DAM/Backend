# 🔧 Guide de Test - Emails de Confirmation de Paiement

## ✅ Correctif Appliqué

**Problème**: Les paiements étaient enregistrés mais aucun email n'était envoyé.

**Solution**: Ajout des métadonnées `childId` et `offerId` au PaymentIntent Stripe pour que le webhook puisse créer l'abonnement et envoyer l'email.

### Modifications apportées:

1. **`PaymentsService.createPaymentIntent`**
   - Ajout des paramètres `childId` et `offerId`
   - Stockage de ces valeurs dans `metadata` du PaymentIntent

2. **`PaymentsController.createPaymentIntent`**
   - Passage de `childId` et `offerId` à `PaymentsService`

## 🧪 Comment Tester

### Option 1: Test avec Stripe CLI (Recommandé pour développement)

#### Étape 1: Installer Stripe CLI

**Windows:**
```powershell
scoop install stripe
```

**Mac:**
```bash
brew install stripe/stripe-cli/stripe
```

**Linux:**
```bash
# Télécharger depuis https://github.com/stripe/stripe-cli/releases
```

#### Étape 2: Login Stripe
```bash
stripe login
```

#### Étape 3: Forward Webhooks vers votre serveur local
```bash
stripe listen --forward-to localhost:3000/payments/webhook
```

**Important**: Gardez cette commande en cours d'exécution. Elle affichera le webhook secret:
```
> Ready! Your webhook signing secret is whsec_xxxxx (^C to quit)
```

Copiez ce secret et mettez-le dans `.env`:
```env
STRIPE_WEBHOOK_SECRET=whsec_xxxxx
```

#### Étape 4: Redémarrer le serveur
```bash
npm run start:dev
```

#### Étape 5: Effectuer un paiement de test

Depuis l'application Android ou avec cURL:

```bash
# 1. Obtenir un token JWT
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@test.com","motDePasse":"password123"}'

# 2. Créer un PaymentIntent
curl -X POST http://localhost:3000/payments/create-intent \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "childId": "CHILD_ID_HERE",
    "offerId": "OFFER_ID_HERE"
  }'

# 3. Simuler un paiement réussi (dans un autre terminal)
stripe trigger payment_intent.succeeded
```

#### Étape 6: Vérifier les logs

Vous devriez voir dans les logs du serveur:
```
Received webhook event: payment_intent.succeeded
Payment succeeded: pi_xxxxx
✅ Subscription created and email sent for child: xxxxx
📤 Envoi de l'email de confirmation de paiement à parent@test.com...
✅ Email de confirmation envoyé à parent@test.com. Message ID: xxxxx
```

### Option 2: Test avec un vrai paiement

#### Étape 1: Configurer le webhook dans Stripe Dashboard

1. Allez sur https://dashboard.stripe.com/test/webhooks
2. Cliquez sur "Add endpoint"
3. URL de l'endpoint: `https://votre-domaine.com/payments/webhook`
4. Sélectionnez les événements:
   - `payment_intent.succeeded`
   - `payment_intent.payment_failed`
5. Cliquez sur "Add endpoint"
6. Copiez le "Signing secret" et mettez-le dans `.env`:
   ```env
   STRIPE_WEBHOOK_SECRET=whsec_xxxxx
   ```

#### Étape 2: Effectuer un paiement depuis l'app Android

1. Lancez l'application Android
2. Connectez-vous en tant que parent
3. Sélectionnez un enfant et une offre
4. Effectuez le paiement avec une carte de test:
   - Numéro: `4242 4242 4242 4242`
   - Date: N'importe quelle date future
   - CVC: N'importe quel 3 chiffres

#### Étape 3: Vérifier

1. **Dans les logs du serveur:**
   ```
   Received webhook event: payment_intent.succeeded
   Payment succeeded: pi_xxxxx
   ✅ Subscription created and email sent for child: xxxxx
   ```

2. **Dans la base de données:**
   - Un nouvel abonnement doit être créé
   - Le statut doit être `ACTIVE`
   - Le `paymentStatus` doit être `PAID`

3. **Dans l'email:**
   - Vérifiez la boîte mail du parent
   - Un email de confirmation doit être reçu
   - Si vous utilisez Ethereal (dev), cherchez le lien dans les logs

## 🐛 Dépannage

### Problème: Webhook non reçu

**Vérifications:**
1. Le serveur est-il en cours d'exécution?
2. Stripe CLI est-il en cours d'exécution (`stripe listen`)?
3. Le `STRIPE_WEBHOOK_SECRET` est-il correct dans `.env`?

**Solution:**
```bash
# Vérifier les logs de Stripe CLI
# Il devrait afficher les événements reçus

# Vérifier les logs du serveur
# Il devrait afficher "Received webhook event: ..."
```

### Problème: Email non envoyé

**Vérifications:**
1. Les métadonnées sont-elles présentes?
   ```
   # Dans les logs, vous devriez voir:
   Payment succeeded: pi_xxxxx
   # Pas de "Missing childId or offerId in payment metadata"
   ```

2. Le parent existe-t-il?
   ```
   # Vérifiez dans les logs:
   # Pas de "Parent not found"
   ```

3. L'offre existe-t-elle?
   ```
   # Vérifiez dans les logs:
   # Pas de "Offer not found"
   ```

**Solution:**
```bash
# Vérifier les métadonnées du PaymentIntent dans Stripe Dashboard
# Aller sur: https://dashboard.stripe.com/test/payments
# Cliquer sur le paiement
# Vérifier la section "Metadata"
# Doit contenir: childId et offerId
```

### Problème: Erreur SMTP

**Vérifications:**
1. Les variables SMTP sont-elles correctes dans `.env`?
2. Le mot de passe d'application Gmail est-il valide?

**Solution:**
```env
# Vérifier .env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=uwoh syqj xcix jgbr  # Mot de passe d'application, pas le mot de passe Gmail
```

## 📊 Vérification des Métadonnées

Pour vérifier que les métadonnées sont bien envoyées:

### Dans Stripe Dashboard:
1. Allez sur https://dashboard.stripe.com/test/payments
2. Cliquez sur le dernier paiement
3. Scrollez jusqu'à "Metadata"
4. Vous devriez voir:
   ```
   childId: 67xxxxx
   offerId: 67xxxxx
   ```

### Dans les logs du webhook:
```typescript
// Dans payments-webhook.controller.ts
console.log('PaymentIntent metadata:', paymentIntent.metadata);
// Devrait afficher: { childId: '...', offerId: '...' }
```

## ✅ Checklist de Test

- [ ] Stripe CLI installé et connecté
- [ ] `stripe listen` en cours d'exécution
- [ ] `STRIPE_WEBHOOK_SECRET` configuré dans `.env`
- [ ] Serveur redémarré après modification `.env`
- [ ] PaymentIntent créé avec `childId` et `offerId`
- [ ] Métadonnées visibles dans Stripe Dashboard
- [ ] Webhook reçu (logs: "Received webhook event")
- [ ] Abonnement créé dans la base de données
- [ ] Email envoyé (logs: "Email de confirmation envoyé")
- [ ] Email reçu dans la boîte mail (ou lien Ethereal dans les logs)

## 🎯 Résultat Attendu

Après un paiement réussi, vous devriez avoir:

1. **Dans les logs:**
   ```
   Received webhook event: payment_intent.succeeded
   Payment succeeded: pi_1234567890
   ✅ Subscription created and email sent for child: 67xxxxx
   📤 Envoi de l'email de confirmation de paiement à parent@example.com...
   ✅ Email de confirmation envoyé à parent@example.com. Message ID: <xxxxx>
   ```

2. **Dans la base de données:**
   - Nouvel abonnement avec `status: ACTIVE` et `paymentStatus: PAID`

3. **Dans l'email:**
   - Email de confirmation avec tous les détails de l'abonnement

## 📝 Notes Importantes

- **Environnement de test**: Utilisez toujours les clés de test Stripe (`sk_test_...`)
- **Cartes de test**: Utilisez `4242 4242 4242 4242` pour les tests
- **Webhook secret**: Différent entre Stripe CLI et Stripe Dashboard
- **Logs**: Surveillez toujours les logs pour détecter les erreurs

## 🔗 Ressources

- [Stripe CLI Documentation](https://stripe.com/docs/stripe-cli)
- [Stripe Test Cards](https://stripe.com/docs/testing#cards)
- [Stripe Webhooks](https://stripe.com/docs/webhooks)
