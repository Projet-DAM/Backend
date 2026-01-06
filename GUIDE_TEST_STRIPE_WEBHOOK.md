# 🎉 Système de Paiement Stripe - CORRIGÉ

## ✅ Modifications Apportées

### 1. Ajout du Webhook Handler
Le système de paiement a été mis à jour avec un **webhook handler complet** qui :
- ✅ Écoute les événements Stripe `payment_intent.succeeded` et `payment_intent.payment_failed`
- ✅ Vérifie la signature Stripe pour la sécurité
- ✅ Enregistre automatiquement les paiements dans les abonnements
- ✅ Prépare l'envoi d'emails de confirmation

### 2. Fichiers Modifiés

#### `src/payments/payments.service.ts`
- Ajout de `Logger` pour un meilleur suivi
- Injection de `SubscriptionsService` et `EmailService`
- Nouvelle méthode `handleWebhook()` pour traiter les événements Stripe
- Nouvelle méthode `handlePaymentSuccess()` pour enregistrer les paiements réussis
- Nouvelle méthode `handlePaymentFailure()` pour logger les échecs

#### `src/payments/payments.controller.ts`
- Ajout de l'endpoint `POST /payments/webhook`
- Décorateur `@Public()` pour permettre à Stripe d'appeler le webhook sans JWT
- Import du décorateur `Headers` pour récupérer la signature Stripe

#### `src/payments/payments.module.ts`
- Import de `SubscriptionsModule` avec `forwardRef()` pour éviter les dépendances circulaires

## 🧪 Comment Tester

### Option 1 : Utiliser Stripe CLI (Recommandé pour le développement)

#### Étape 1 : Installer Stripe CLI
```powershell
# Télécharger depuis https://stripe.com/docs/stripe-cli
# Ou avec Chocolatey
choco install stripe-cli
```

#### Étape 2 : Se connecter à Stripe
```powershell
stripe login
```

#### Étape 3 : Écouter les webhooks localement
```powershell
stripe listen --forward-to localhost:3000/payments/webhook
```

Cette commande va :
- Afficher un **webhook signing secret** (commençant par `whsec_...`)
- Copier ce secret dans votre `.env` : `STRIPE_WEBHOOK_SECRET=whsec_...`
- Redémarrer le serveur backend

#### Étape 4 : Tester avec un événement simulé
```powershell
# Dans un nouveau terminal
stripe trigger payment_intent.succeeded
```

Vous devriez voir dans les logs du serveur :
```
[PaymentsService] 📨 Received webhook event: payment_intent.succeeded
[PaymentsService] ✅ Payment succeeded: pi_xxxxx
```

### Option 2 : Configurer le Webhook dans Stripe Dashboard (Production)

#### Étape 1 : Accéder au Dashboard Stripe
1. Aller sur https://dashboard.stripe.com/webhooks
2. Cliquer sur "Add endpoint"

#### Étape 2 : Configurer l'endpoint
- **URL** : `https://votre-domaine.com/payments/webhook`
- **Description** : "Webhook pour les paiements"
- **Événements à écouter** :
  - ✅ `payment_intent.succeeded`
  - ✅ `payment_intent.payment_failed`

#### Étape 3 : Récupérer le Signing Secret
1. Après création, cliquer sur le webhook
2. Copier le "Signing secret" (commence par `whsec_...`)
3. L'ajouter dans `.env` : `STRIPE_WEBHOOK_SECRET=whsec_...`
4. Redémarrer le serveur

### Option 3 : Test Manuel avec cURL

```powershell
# Créer un événement de test (nécessite un vrai PaymentIntent)
curl -X POST http://localhost:3000/payments/webhook `
  -H "Content-Type: application/json" `
  -H "stripe-signature: test" `
  -d '{
    "type": "payment_intent.succeeded",
    "data": {
      "object": {
        "id": "pi_test_123",
        "amount": 7000,
        "currency": "tnd",
        "status": "succeeded",
        "metadata": {
          "subscriptionId": "VOTRE_SUBSCRIPTION_ID"
        }
      }
    }
  }'
```

**Note** : Sans le bon `STRIPE_WEBHOOK_SECRET`, la vérification de signature échouera.

## 📊 Flow Complet de Paiement

### Avant (❌ Ne fonctionnait pas)
```
1. Frontend crée PaymentIntent
2. Frontend confirme le paiement
3. ❌ Rien ne se passe dans le backend
4. ❌ L'abonnement n'est pas activé
5. ❌ Pas d'email de confirmation
```

### Après (✅ Fonctionne maintenant)
```
1. Frontend crée PaymentIntent avec subscriptionId
2. Frontend confirme le paiement
3. ✅ Stripe envoie un webhook payment_intent.succeeded
4. ✅ Backend enregistre le paiement dans l'abonnement
5. ✅ L'abonnement est activé automatiquement
6. ✅ Email de confirmation envoyé (TODO: à compléter)
```

## 🔍 Vérification des Logs

Après avoir configuré le webhook, vous devriez voir ces logs dans le serveur :

### Au démarrage
```
[PaymentsService] ✅ Stripe client initialized successfully
```

### Lors d'un paiement réussi
```
[PaymentsService] 📨 Received webhook event: payment_intent.succeeded
[PaymentsService] ✅ Payment succeeded: pi_xxxxx
[PaymentsService] ✅ Payment recorded for subscription: sub_xxxxx
[PaymentsService] 📧 TODO: Send payment confirmation email for subscription sub_xxxxx
```

### En cas d'échec
```
[PaymentsService] 📨 Received webhook event: payment_intent.payment_failed
[PaymentsService] ❌ Payment failed: pi_xxxxx
[PaymentsService] Failure reason: Your card was declined
```

## 🚨 Problèmes Potentiels et Solutions

### Problème : "Webhook signature verification failed"
**Cause** : Le `STRIPE_WEBHOOK_SECRET` n'est pas correct ou manquant.

**Solution** :
1. Vérifier que `STRIPE_WEBHOOK_SECRET` est dans `.env`
2. S'assurer qu'il commence par `whsec_`
3. Redémarrer le serveur après modification

### Problème : "No subscriptionId in payment metadata"
**Cause** : Le PaymentIntent n'a pas été créé avec le `subscriptionId`.

**Solution** : S'assurer que le frontend envoie le `subscriptionId` lors de la création du PaymentIntent :
```typescript
{
  "amount": 7000,
  "currency": "tnd",
  "paymentMethodId": "pm_xxx",
  "subscriptionId": "675b123456789abc" // ✅ Important !
}
```

### Problème : "Error recording payment for subscription"
**Cause** : L'abonnement n'existe pas ou a un statut invalide.

**Solution** :
1. Vérifier que l'abonnement existe dans la base de données
2. Vérifier que le statut de l'abonnement permet l'enregistrement de paiement
3. Consulter les logs pour plus de détails

## 📝 TODO - Améliorations Futures

### 1. Compléter l'Envoi d'Email
Actuellement, le code log juste un TODO. Il faut :
- Récupérer les informations de l'utilisateur (email, nom)
- Récupérer les informations de l'offre (nom, prix)
- Appeler `emailService.sendPaymentConfirmation()` avec les bonnes données

### 2. Gérer les Échecs de Paiement
Actuellement, on log juste l'échec. Il faudrait :
- Mettre à jour le statut de l'abonnement
- Envoyer un email au parent
- Notifier l'académie

### 3. Ajouter d'Autres Événements Webhook
- `payment_intent.processing` - Paiement en cours
- `charge.refunded` - Remboursement
- `customer.subscription.deleted` - Annulation d'abonnement

### 4. Améliorer la Sécurité
- En production, forcer la vérification de signature (throw si pas de secret)
- Ajouter un rate limiting sur le webhook
- Logger tous les événements dans une table d'audit

## 🎯 Résumé

Le système de paiement Stripe est maintenant **fonctionnel** avec :
- ✅ Création de PaymentIntent
- ✅ Confirmation de paiement
- ✅ **Webhook handler pour automatiser l'activation des abonnements**
- ✅ Logging détaillé pour le debugging
- ⚠️ Email de confirmation (à compléter)

Le problème principal était l'**absence de webhook handler**. Maintenant que c'est corrigé, les paiements Stripe devraient fonctionner de bout en bout !
