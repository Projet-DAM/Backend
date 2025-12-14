# 🔧 Configuration Webhook Stripe - Guide Complet

## ⚠️ Problème Actuel

Le webhook ne fonctionne pas car Stripe ne peut pas envoyer de webhooks à `localhost` directement.

## ✅ Solution: Stripe CLI

### Étape 1: Installer Stripe CLI

#### Windows (PowerShell en tant qu'administrateur):
```powershell
# Installer Scoop si pas déjà fait
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Installer Stripe CLI
scoop install stripe
```

#### Alternative Windows (Téléchargement direct):
1. Allez sur https://github.com/stripe/stripe-cli/releases
2. Téléchargez `stripe_X.X.X_windows_x86_64.zip`
3. Extrayez et ajoutez au PATH

#### Mac:
```bash
brew install stripe/stripe-cli/stripe
```

#### Linux:
```bash
# Télécharger depuis https://github.com/stripe/stripe-cli/releases
wget https://github.com/stripe/stripe-cli/releases/download/vX.X.X/stripe_X.X.X_linux_x86_64.tar.gz
tar -xvf stripe_X.X.X_linux_x86_64.tar.gz
sudo mv stripe /usr/local/bin/
```

### Étape 2: Login Stripe

```bash
stripe login
```

Cela ouvrira votre navigateur pour vous connecter à votre compte Stripe.

### Étape 3: Forward Webhooks (IMPORTANT!)

**Ouvrez un nouveau terminal** et exécutez:

```bash
stripe listen --forward-to localhost:3000/payments/webhook
```

**IMPORTANT**: 
- Gardez ce terminal **OUVERT** en permanence pendant le développement
- Chaque fois que vous redémarrez ce terminal, le webhook secret change

Vous verrez:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxxxxxxxxxx (^C to quit)
```

### Étape 4: Copier le Webhook Secret

Copiez le secret affiché (`whsec_xxxxx...`) et mettez-le dans `.env`:

```env
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxxxxxxxxxx
```

### Étape 5: Redémarrer le Serveur NestJS

```bash
# Arrêter le serveur (Ctrl+C)
# Puis relancer
npm run start:dev
```

### Étape 6: Vérifier la Configuration

Le serveur devrait afficher:
```
[Nest] LOG [NestApplication] Nest application successfully started
Application is running on: http://localhost:3000
```

Le terminal Stripe CLI devrait afficher:
```
> Ready! Your webhook signing secret is whsec_xxxxx (^C to quit)
```

## 🧪 Test du Webhook

### Option 1: Déclencher un Événement de Test

Dans un **troisième terminal**:

```bash
stripe trigger payment_intent.succeeded
```

Vous devriez voir:
- **Dans Stripe CLI**: `--> payment_intent.succeeded [evt_xxxxx]`
- **Dans les logs du serveur**: 
  ```
  🔔 Webhook received!
  📨 Received webhook event: payment_intent.succeeded
  💰 Payment succeeded: pi_xxxxx
  ```

### Option 2: Effectuer un Vrai Paiement

1. Effectuez un paiement depuis l'app Android
2. Vérifiez les logs

## 📊 Logs Attendus

### Dans Stripe CLI:
```
2025-12-05 16:00:00   --> payment_intent.succeeded [evt_1234567890]
2025-12-05 16:00:00  <--  [200] POST http://localhost:3000/payments/webhook [evt_1234567890]
```

### Dans les logs du serveur:
```
🔔 Webhook received!
Webhook called at: 2025-12-05T16:00:00.000Z
📨 Received webhook event: payment_intent.succeeded
💰 Payment succeeded: pi_1234567890
PaymentIntent metadata: { childId: '67xxxxx', offerId: '67xxxxx' }
Processing payment for childId: 67xxxxx, offerId: 67xxxxx
✅ Found parent: parent@example.com
✅ Found offer: Abonnement Mensuel
Creating subscription...
✅ Subscription created: sub_xxxxx
Recording payment...
✅ Payment recorded
📧 Sending confirmation email to parent@example.com...
📤 Envoi de l'email de confirmation de paiement à parent@example.com...
✅ Email de confirmation envoyé à parent@example.com. Message ID: <xxxxx>
✅ Subscription created and email sent for child: 67xxxxx
```

## 🐛 Dépannage

### Problème: "stripe: command not found"

**Solution**: Stripe CLI n'est pas installé ou pas dans le PATH.
```bash
# Vérifier l'installation
stripe --version

# Si erreur, réinstaller
scoop install stripe
```

### Problème: "Webhook signature verification failed"

**Solution**: Le webhook secret dans `.env` ne correspond pas.
1. Vérifiez le secret dans le terminal Stripe CLI
2. Copiez-le exactement dans `.env`
3. Redémarrez le serveur

### Problème: "Missing childId or offerId in payment metadata"

**Solution**: Les métadonnées ne sont pas envoyées lors de la création du PaymentIntent.

Vérifiez dans Stripe Dashboard:
1. https://dashboard.stripe.com/test/payments
2. Cliquez sur le paiement
3. Section "Metadata" doit contenir `childId` et `offerId`

Si absent, le problème est dans `PaymentsController.createPaymentIntent`.

### Problème: Webhook pas reçu

**Solution**: Stripe CLI n'est pas en cours d'exécution.
```bash
# Vérifier que Stripe CLI est actif
# Vous devriez voir: "Ready! Your webhook signing secret is..."
stripe listen --forward-to localhost:3000/payments/webhook
```

## 📝 Workflow de Développement

### Démarrage Quotidien:

1. **Terminal 1** - Stripe CLI:
   ```bash
   stripe listen --forward-to localhost:3000/payments/webhook
   ```

2. **Terminal 2** - Serveur NestJS:
   ```bash
   npm run start:dev
   ```

3. **Terminal 3** - Tests (optionnel):
   ```bash
   stripe trigger payment_intent.succeeded
   ```

### Arrêt:
- Ctrl+C dans chaque terminal

## 🚀 Déploiement en Production

En production, vous n'utilisez PAS Stripe CLI. À la place:

1. Allez sur https://dashboard.stripe.com/webhooks
2. Cliquez sur "Add endpoint"
3. URL: `https://votre-domaine.com/payments/webhook`
4. Événements: `payment_intent.succeeded`, `payment_intent.payment_failed`
5. Copiez le "Signing secret"
6. Mettez-le dans `.env` de production:
   ```env
   STRIPE_WEBHOOK_SECRET=whsec_production_secret_here
   ```

## ✅ Checklist

- [ ] Stripe CLI installé (`stripe --version` fonctionne)
- [ ] `stripe login` exécuté
- [ ] `stripe listen --forward-to localhost:3000/payments/webhook` en cours d'exécution
- [ ] Webhook secret copié dans `.env`
- [ ] Serveur NestJS redémarré
- [ ] Test avec `stripe trigger payment_intent.succeeded` réussi
- [ ] Logs du webhook visibles dans le serveur
- [ ] Email de test reçu

## 🎯 Résultat Final

Après configuration, chaque paiement déclenchera automatiquement:
1. ✅ Création de l'abonnement
2. ✅ Enregistrement du paiement
3. ✅ Envoi de l'email de confirmation

**Sans aucune intervention manuelle!** 🎉
