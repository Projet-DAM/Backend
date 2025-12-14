# ✅ Webhook Stripe Démarré!

## 🎉 Succès!

Le webhook Stripe est maintenant en cours d'exécution!

## 📋 Vous devriez voir dans le terminal:

```
> Ready! You are using Stripe API Version [2025-10-29.clover]
> Ready! Your webhook signing secret is whsec_7c12d5d2c2684a362f7d7e4b2f35e7312ae82c7ae866 (^C to quit)
```

## ⚠️ ACTION REQUISE: Copier le Webhook Secret

### 1. Copiez le Secret

Dans le terminal où vous avez exécuté la commande `stripe listen`, vous devriez voir:

```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx
```

**Copiez** le secret complet qui commence par `whsec_`

### 2. Ajoutez-le dans `.env`

Ouvrez le fichier `.env` et ajoutez/modifiez cette ligne:

```env
STRIPE_WEBHOOK_SECRET=whsec_7c12d5d2c2684a362f7d7e4b2f35e7312ae82c7ae866
```

**Remplacez** par votre secret réel affiché dans le terminal.

### 3. Redémarrez le Serveur NestJS

Dans un **autre terminal** (gardez le webhook ouvert!):

```bash
# Arrêter le serveur (Ctrl+C)
# Puis relancer
npm run start:dev
```

## 🧪 Test du Webhook

### Option 1: Déclencher un Événement de Test

Dans un **troisième terminal**:

```powershell
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe trigger payment_intent.succeeded
```

### Option 2: Effectuer un Vrai Paiement

Effectuez un paiement depuis l'application Android.

## 📊 Logs Attendus

### Dans le Terminal Stripe CLI:
```
2025-12-05 16:20:00   --> payment_intent.succeeded [evt_xxxxx]
2025-12-05 16:20:00  <--  [200] POST http://localhost:3000/payments/webhook [evt_xxxxx]
```

### Dans les Logs du Serveur NestJS:
```
🔔 Webhook received!
Webhook called at: 2025-12-05T16:20:00.000Z
📨 Received webhook event: payment_intent.succeeded
💰 Payment succeeded: pi_xxxxx
PaymentIntent metadata: { childId: '67xxxxx', offerId: '67xxxxx' }
Processing payment for childId: 67xxxxx, offerId: 67xxxxx
✅ Found parent: parent@example.com
✅ Found offer: Abonnement Mensuel
Creating subscription...
✅ Subscription created: sub_xxxxx
Recording payment...
✅ Payment recorded
📧 Sending confirmation email to parent@example.com...
✅ Email de confirmation envoyé à parent@example.com
✅ Subscription created and email sent for child: 67xxxxx
```

## ✅ Checklist

- [x] Stripe CLI installé
- [x] Login Stripe réussi
- [x] Webhook forwarding démarré
- [ ] Webhook secret copié dans `.env`
- [ ] Serveur NestJS redémarré
- [ ] Test effectué

## 📝 Commandes Utiles

### Démarrer le Webhook (à faire chaque jour)
```powershell
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe listen --forward-to localhost:3000/payments/webhook
```

### Tester le Webhook
```powershell
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe trigger payment_intent.succeeded
```

### Voir les Événements
```powershell
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe events list
```

## ⚠️ Important

- **Gardez le terminal webhook OUVERT** pendant le développement
- **Le secret change** à chaque redémarrage du webhook
- **Mettez à jour `.env`** si vous redémarrez le webhook
- **Redémarrez le serveur** après modification de `.env`

## 🎯 Workflow Quotidien

**Chaque jour de développement:**

1. **Terminal 1**: Démarrer le webhook
   ```powershell
   C:\Users\hatem\scoop\apps\stripe\current\stripe.exe listen --forward-to localhost:3000/payments/webhook
   ```

2. **Copier** le webhook secret dans `.env`

3. **Terminal 2**: Démarrer le serveur
   ```bash
   npm run start:dev
   ```

4. **Développer** normalement

5. **Arrêter**: Ctrl+C dans les deux terminaux

## 🎉 C'est Tout!

Le webhook est maintenant configuré et opérationnel!

Chaque paiement déclenchera automatiquement:
- ✅ Création de l'abonnement
- ✅ Enregistrement du paiement
- ✅ Envoi de l'email de confirmation

**Sans aucune intervention manuelle!** 🚀
