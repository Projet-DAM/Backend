# Guide de test - Abonnements et Offres

## 🧪 Tests des nouvelles fonctionnalités

### Prérequis
```bash
# 1. Installer les dépendances
npm install --legacy-peer-deps

# 2. Démarrer MongoDB (si local)
# mongod

# 3. Démarrer le serveur
npm run start:dev

# 4. (Optionnel) Démarrer Stripe CLI pour les webhooks
stripe listen --forward-to localhost:3000/webhooks/stripe
```

### 1. Test du Chatbot (Parents)

```bash
# Obtenir un token JWT (remplacer avec vos credentials)
$token = "votre_token_jwt_parent"

# Tester le chatbot
curl -X POST http://localhost:3000/subscriptions/chatbot `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{"message": "Quelles sont les offres disponibles ?"}'

# Exemples de questions à tester :
# - "Comment puis-je renouveler mon abonnement ?"
# - "Quels sont les coachs disponibles ?"
# - "Combien coûte l'abonnement mensuel ?"
```

### 2. Test des Prévisions de revenus (Académies)

```bash
# Obtenir un token JWT académie
$token = "votre_token_jwt_academie"

# Récupérer les prévisions
curl -X GET http://localhost:3000/subscriptions/forecast/revenue `
  -H "Authorization: Bearer $token"

# Résultat attendu :
# {
#   "currentMonth": { "month": "2025-12", "revenue": 5000, "subscriptionCount": 50 },
#   "nextMonth": { "estimatedRevenue": 5200, "estimatedSubscriptions": 52, "confidence": 85 },
#   "historicalData": [...],
#   "trend": "increasing"
# }
```

### 3. Test du Webhook Stripe

```bash
# En développement, utiliser Stripe CLI
stripe listen --forward-to localhost:3000/webhooks/stripe

# Dans un autre terminal, créer un paiement test
stripe trigger payment_intent.succeeded

# Vérifier les logs du serveur pour :
# - "📨 Received Stripe webhook event: payment_intent.succeeded"
# - "✅ Payment succeeded: pi_xxx"
# - "✅ Subscription xxx updated successfully"
# - "✅ Payment confirmation email sent to xxx"
```

### 4. Test des emails

#### Email de confirmation de paiement
```bash
# Créer un abonnement et effectuer un paiement via Stripe
# L'email sera envoyé automatiquement après le webhook

# Vérifier les logs pour :
# - "📤 Envoi de l'email de confirmation de paiement à xxx"
# - "✅ Email de confirmation envoyé à xxx"
```

#### Email d'expiration (Test manuel)
```bash
# Créer un abonnement qui expire dans 7 jours
# Attendre le cron job (9h00) ou le déclencher manuellement

# Pour tester immédiatement, modifier temporairement le cron :
# @Cron('*/5 * * * *') // Toutes les 5 minutes
```

### 5. Test complet du flux de paiement

```bash
# 1. Créer une offre (Académie)
$token = "votre_token_jwt_academie"

curl -X POST http://localhost:3000/offers `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d '{
    "name": "Abonnement Mensuel Test",
    "description": "Accès mensuel à toutes les activités",
    "type": "MONTHLY",
    "durationDays": 30,
    "price": 50,
    "discountPct": 0,
    "conditions": "Non remboursable"
  }'

# Récupérer l'ID de l'offre créée
$offerId = "xxx"

# 2. Créer un abonnement (Parent)
$token = "votre_token_jwt_parent"
$childId = "votre_child_id"

curl -X POST http://localhost:3000/subscriptions `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{
    \"childId\": \"$childId\",
    \"offerId\": \"$offerId\",
    \"autoRenew\": true
  }"

# Récupérer l'ID de l'abonnement créé
$subscriptionId = "xxx"

# 3. Créer un PaymentIntent
curl -X POST http://localhost:3000/payments/create-intent `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -d "{
    \"amount\": 5000,
    \"currency\": \"eur\",
    \"paymentMethodId\": \"pm_card_visa\",
    \"subscriptionId\": \"$subscriptionId\"
  }"

# 4. Le webhook Stripe mettra à jour l'abonnement et enverra l'email
```

### 6. Test des tâches Cron

```bash
# Les tâches cron s'exécutent automatiquement :
# - Vérification des abonnements expirants : 9h00
# - Marquage des abonnements expirés : 00h00

# Pour tester immédiatement, modifier temporairement les crons dans :
# src/subscriptions/subscriptions-cron.service.ts

# Exemple :
# @Cron('*/2 * * * *') // Toutes les 2 minutes
# async checkExpiringSubscriptions() { ... }
```

## 📊 Vérification des résultats

### Logs à surveiller

1. **Démarrage du serveur** :
   ```
   ✅ Configuration SMTP chargée avec succès
   📧 Transporter configuré
   ```

2. **Chatbot** :
   ```
   ✅ Chatbot response generated for user xxx
   ```

3. **Prévisions** :
   ```
   📊 Generating revenue forecast...
   ```

4. **Webhook Stripe** :
   ```
   📨 Received Stripe webhook event: payment_intent.succeeded
   ✅ Payment succeeded: pi_xxx
   ✅ Subscription xxx updated successfully
   ✅ Payment confirmation email sent to xxx
   ```

5. **Cron jobs** :
   ```
   🔍 Vérification des abonnements qui expirent bientôt...
   📊 X abonnement(s) expire(nt) dans les 7 prochains jours
   ✅ Email d'expiration envoyé pour l'abonnement xxx
   ```

## 🐛 Dépannage

### Problème : Erreurs TypeScript
**Solution** : Attendre la fin de `npm install`

### Problème : Emails non envoyés
**Solution** : Vérifier la configuration SMTP dans `.env`

### Problème : Chatbot ne répond pas
**Solution** : Vérifier que `OPENAI_API_KEY` est configurée dans `.env`

### Problème : Webhook Stripe échoue
**Solution** : 
- Vérifier que Stripe CLI est en cours d'exécution
- Vérifier `STRIPE_WEBHOOK_SECRET` dans `.env`

### Problème : Cron jobs ne s'exécutent pas
**Solution** : Vérifier que `ScheduleModule.forRoot()` est dans `app.module.ts`

## ✅ Checklist de validation

- [ ] Le serveur démarre sans erreur
- [ ] Les offres peuvent être créées
- [ ] Les abonnements peuvent être créés
- [ ] Le chatbot répond aux questions
- [ ] Les prévisions de revenus sont générées
- [ ] Les paiements Stripe sont traités
- [ ] Les emails de confirmation sont envoyés
- [ ] Les emails d'expiration sont envoyés (cron)
- [ ] Les abonnements expirés sont marqués (cron)

## 📞 Support

En cas de problème, vérifier :
1. Les logs du serveur
2. La configuration `.env`
3. Les dépendances installées (`node_modules`)
4. La connexion MongoDB
5. La connexion Internet (pour OpenAI et Stripe)
