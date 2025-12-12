# Configuration du Webhook Stripe pour WhatsApp

## Option 1 : Test Local avec Stripe CLI

### Étape 1 : Installer Stripe CLI

Téléchargez et installez depuis : https://stripe.com/docs/stripe-cli

### Étape 2 : Se connecter à Stripe

```bash
stripe login
```

Cela ouvrira votre navigateur pour vous authentifier.

### Étape 3 : Démarrer le forwarding des webhooks

```bash
stripe listen --forward-to http://localhost:3000/payments/webhook
```

La commande affichera quelque chose comme :
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxxx
```

Copiez ce secret et ajoutez-le dans votre `.env` :
```env
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxxx
```

### Étape 4 : Tester

Dans un autre terminal, démarrez votre serveur :
```bash
npm run start:dev
```

Puis testez le webhook :
```bash
stripe trigger payment_intent.succeeded
```

Vous devriez voir dans les logs :
- "Webhook reçu: payment_intent.succeeded"
- "Message de confirmation envoyé à +21627863334"

---

## Option 2 : Dashboard Stripe (pour production)

### Étape 1 : Accéder au Dashboard

Allez sur : https://dashboard.stripe.com/webhooks

### Étape 2 : Ajouter un endpoint

1. Cliquez sur "Add endpoint"
2. Entrez votre URL : `https://votre-domaine.com/payments/webhook`
3. Sélectionnez les événements à écouter :
   - ✅ `payment_intent.succeeded`
4. Cliquez sur "Add endpoint"

### Étape 3 : Récupérer le signing secret

Une fois l'endpoint créé :
1. Cliquez sur l'endpoint
2. Cliquez sur "Reveal" à côté de "Signing secret"
3. Copiez le secret (commence par `whsec_`)
4. Ajoutez-le dans votre `.env`

---

## ⚠️ Important pour WhatsApp Sandbox

Avant de recevoir des messages WhatsApp, vous devez rejoindre le sandbox Twilio :

1. Ouvrez WhatsApp sur votre téléphone
2. Envoyez un message à : **+1 415 523 8886**
3. Message à envoyer : `join <votre-code-sandbox>`

Pour trouver votre code sandbox :
- Allez sur : https://console.twilio.com/us1/develop/sms/try-it-out/whatsapp-learn
- Vous verrez un message type : "join <code>"

Une fois connecté, vous recevrez : "Twilio Sandbox: You are all set!"

Maintenant votre numéro `+21627863334` pourra recevoir des messages WhatsApp automatiques ! 📱✅
