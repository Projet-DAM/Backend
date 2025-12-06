# Configuration WhatsApp pour Confirmation de Paiement Stripe

Ce document explique comment configurer l'envoi automatique de messages WhatsApp via Twilio lorsqu'un paiement Stripe est effectué avec succès.

## 📋 Prérequis

- Compte Stripe (mode test ou production)
- Compte Twilio avec WhatsApp activé
- Backend NestJS configuré

## 🔧 Configuration

### 1. Variables d'environnement

Assurez-vous que votre fichier `.env` contient les variables suivantes :

```env
# Stripe
STRIPE_SECRET_KEY=sk_test_votre_cle_secrete
STRIPE_PUBLISHABLE_KEY=pk_test_votre_cle_publique
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_webhook

# Twilio
TWILIO_ACCOUNT_SID=ACd6c9ee845678e8400d7b8e7bdfbecc21
TWILIO_AUTH_TOKEN=9383c714866a6eb3faee86543e6cb262
TWILIO_WHATSAPP_NUMBER=whatsapp:+14155238886

# Numéro par défaut pour les confirmations
DEFAULT_PHONE_NUMBER=+21627863334
```

### 2. Configuration du Webhook Stripe

#### Étape 1 : Obtenir l'URL du webhook

Pour tester localement avec Stripe CLI :

```bash
# Installer Stripe CLI
# Téléchargez depuis : https://stripe.com/docs/stripe-cli

# Se connecter
stripe login

# Rediriger les webhooks vers votre serveur local
stripe listen --forward-to http://localhost:3000/payments/webhook
```

Cette commande vous donnera un **webhook signing secret** (commence par `whsec_`). Copiez-le dans votre fichier `.env` comme `STRIPE_WEBHOOK_SECRET`.

#### Étape 2 : Configuration en production

1. Allez sur votre [Dashboard Stripe](https://dashboard.stripe.com/webhooks)
2. Cliquez sur "Add endpoint"
3. Entrez l'URL : `https://votre-domaine.com/payments/webhook`
4. Sélectionnez l'événement : `payment_intent.succeeded`
5. Copiez le **signing secret** dans votre `.env`

### 3. Configuration de Twilio WhatsApp

Votre compte Twilio est déjà configuré avec :
- **Account SID** : `ACd6c9ee845678e8400d7b8e7bdfbecc21`
- **Auth Token** : `9383c714866a6eb3faee86543e6cb262`
- **Numéro WhatsApp** : `+14155238886` (Sandbox Twilio)

⚠️ **Important** : En mode sandbox, vous devez d'abord envoyer un message à votre sandbox WhatsApp :
1. Envoyez "join <your-sandbox-code>" au numéro `+14155238886` depuis WhatsApp
2. Une fois connecté, vous pourrez recevoir des messages automatiques

## 🚀 Utilisation

### Côté Frontend (Android/Kotlin)

Lors de la création d'un payment intent, incluez le numéro de téléphone :

```kotlin
// Dans votre ViewModel ou Repository
suspend fun createPaymentSheet(amount: Int, phoneNumber: String): PaymentSheetResult {
    val response = apiService.createPaymentIntent(
        CreatePaymentIntentRequest(
            amount = amount,
            currency = "eur",
            phoneNumber = phoneNumber // Ajouter le numéro de téléphone
        )
    )
    
    return PaymentSheetResult(
        clientSecret = response.clientSecret
    )
}

// Données à envoyer
data class CreatePaymentIntentRequest(
    val amount: Int,
    val currency: String = "eur",
    val phoneNumber: String? = null
)
```

### Flow complet

1. **L'utilisateur initie un paiement** sur l'application Android
2. **Le frontend appelle** `POST /payments/create-payment-intent` avec :
   ```json
   {
     "amount": 5000,
     "currency": "eur",
     "phoneNumber": "+21627863334"
   }
   ```
3. **Le backend crée** un PaymentIntent Stripe avec le numéro de téléphone dans les métadonnées
4. **L'utilisateur complète le paiement** via Stripe Payment Sheet
5. **Stripe envoie** un webhook `payment_intent.succeeded` au backend
6. **Le backend traite le webhook** et envoie automatiquement un message WhatsApp au numéro fourni

## 📱 Message de confirmation

Le message WhatsApp envoyé sera :

```
🎉 Confirmation de paiement

Votre paiement de 50.00 EUR a été effectué avec succès !

Merci pour votre confiance. 💚

- SportyConnect
```

## 🧪 Test de l'intégration

### Test avec Stripe CLI

```bash
# 1. Démarrer votre backend
npm run start:dev

# 2. Dans un autre terminal, démarrer le forwarding de webhooks
stripe listen --forward-to http://localhost:3000/payments/webhook

# 3. Créer un test de paiement
stripe trigger payment_intent.succeeded
```

### Cartes de test Stripe

- **Succès** : `4242 4242 4242 4242`
- **Décliné** : `4000 0000 0000 0002`
- **Authentification requise** : `4000 0025 0000 3155`

Date d'expiration : N'importe quelle date future  
CVC : N'importe quel 3 chiffres

## 🔍 Debugging

### Vérifier que le webhook est bien reçu

Consultez les logs de votre backend :

```bash
npm run start:dev
```

Vous devriez voir :
```
Webhook reçu: payment_intent.succeeded
Paiement réussi: pi_xxxxxxxxxxxxx
Montant: 5000 eur
Message de confirmation envoyé à +21627863334
```

### Vérifier les webhooks Stripe

1. Allez sur [Dashboard Stripe > Developers > Webhooks](https://dashboard.stripe.com/webhooks)
2. Cliquez sur votre endpoint
3. Consultez les "Recent events" pour voir l'historique

### Vérifier les messages Twilio

1. Allez sur [Twilio Console > Messaging](https://console.twilio.com/us1/monitor/logs/messaging)
2. Consultez l'historique des messages envoyés

## ⚠️ Notes importantes

1. **Sandbox WhatsApp** : En mode sandbox, seuls les numéros qui ont rejoint le sandbox peuvent recevoir des messages
2. **Numéros de production** : Pour envoyer à n'importe quel numéro, vous devez configurer un numéro WhatsApp Business approuvé
3. **Format des numéros** : Toujours utiliser le format E.164 (`+21627863334`)
4. **Coûts** : Vérifiez les tarifs Twilio pour l'envoi de messages WhatsApp

## 🔐 Sécurité

- ✅ Ne jamais commiter le fichier `.env`
- ✅ Le webhook vérifie la signature Stripe pour garantir l'authenticité
- ✅ Les credentials Twilio sont stockés dans les variables d'environnement
- ✅ Utilisez HTTPS en production

## 📚 Documentation

- [Stripe Webhooks](https://stripe.com/docs/webhooks)
- [Twilio WhatsApp API](https://www.twilio.com/docs/whatsapp)
- [Stripe CLI](https://stripe.com/docs/stripe-cli)

## 🆘 Support

En cas de problème :
1. Vérifiez les logs du backend
2. Consultez les webhooks dans le Dashboard Stripe
3. Vérifiez les messages dans la console Twilio
4. Assurez-vous que toutes les variables d'environnement sont correctement configurées
