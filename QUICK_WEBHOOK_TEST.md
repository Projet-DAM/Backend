# 🚀 Test Rapide - Webhook Stripe

## Le Problème
Le paiement fonctionne mais l'email n'est PAS envoyé car **le webhook Stripe n'est pas appelé**.

## ✅ Solution: Utiliser Stripe CLI

### Étape 1: Installer Stripe CLI

**Windows (PowerShell en tant qu'administrateur):**
```powershell
scoop install stripe
```

Si vous n'avez pas Scoop:
```powershell
# Installer Scoop d'abord
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Puis installer Stripe CLI
scoop install stripe
```

### Étape 2: Login Stripe
```bash
stripe login
```
Suivez les instructions dans le navigateur.

### Étape 3: Forward Webhooks (IMPORTANT!)

**Dans un nouveau terminal**, exécutez:
```bash
stripe listen --forward-to localhost:3000/payments/webhook
```

**IMPORTANT**: Gardez ce terminal ouvert! Il affichera:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx (^C to quit)
```

### Étape 4: Copier le Webhook Secret

Copiez le secret affiché (`whsec_xxxxx`) et mettez-le dans `.env`:
```env
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx
```

### Étape 5: Redémarrer le Serveur

Arrêtez le serveur (Ctrl+C) et relancez:
```bash
npm run start:dev
```

### Étape 6: Effectuer un Paiement

Maintenant, effectuez un paiement depuis l'application Android.

### Étape 7: Vérifier les Logs

**Dans le terminal Stripe CLI**, vous devriez voir:
```
2025-12-05 15:55:00   --> payment_intent.succeeded [evt_xxxxx]
2025-12-05 15:55:00  <--  [200] POST http://localhost:3000/payments/webhook [evt_xxxxx]
```

**Dans les logs du serveur**, vous devriez voir:
```
🔔 Webhook received!
Webhook called at: 2025-12-05T15:55:00.000Z
📨 Received webhook event: payment_intent.succeeded
💰 Payment succeeded: pi_xxxxx
PaymentIntent metadata: { childId: '...', offerId: '...' }
Processing payment for childId: ..., offerId: ...
✅ Found parent: parent@example.com
✅ Found offer: Abonnement Mensuel
Creating subscription...
✅ Subscription created: ...
Recording payment...
✅ Payment recorded
📧 Sending confirmation email to parent@example.com...
✅ Subscription created and email sent for child: ...
```

## 🐛 Si ça ne fonctionne toujours pas

### Vérification 1: Le webhook est-il appelé?

Si vous ne voyez PAS `🔔 Webhook received!` dans les logs, le webhook n'est pas appelé.

**Solution**: Vérifiez que Stripe CLI est en cours d'exécution:
```bash
stripe listen --forward-to localhost:3000/payments/webhook
```

### Vérification 2: Les métadonnées sont-elles présentes?

Si vous voyez `⚠️ Missing childId or offerId in payment metadata`, les métadonnées ne sont pas envoyées.

**Solution**: Vérifiez dans Stripe Dashboard:
1. Allez sur https://dashboard.stripe.com/test/payments
2. Cliquez sur le dernier paiement
3. Scrollez jusqu'à "Metadata"
4. Vous devriez voir `childId` et `offerId`

Si les métadonnées ne sont pas là, le problème est dans `PaymentsController.createPaymentIntent`.

### Vérification 3: L'email est-il envoyé?

Si vous voyez `📧 Sending confirmation email...` mais pas d'email reçu:

**Solution**: Vérifiez les logs pour voir s'il y a une erreur SMTP. Si vous utilisez Ethereal (dev), cherchez le lien de prévisualisation dans les logs.

## 📋 Checklist Rapide

- [ ] Stripe CLI installé
- [ ] `stripe login` exécuté
- [ ] `stripe listen --forward-to localhost:3000/payments/webhook` en cours d'exécution
- [ ] `STRIPE_WEBHOOK_SECRET` copié dans `.env`
- [ ] Serveur redémarré
- [ ] Paiement effectué
- [ ] Logs vérifiés

## 🎯 Résultat Attendu

Après un paiement, vous devriez voir dans les logs:
```
🔔 Webhook received!
📨 Received webhook event: payment_intent.succeeded
💰 Payment succeeded: pi_xxxxx
✅ Found parent: parent@example.com
✅ Found offer: Abonnement Mensuel
✅ Subscription created
✅ Payment recorded
📧 Sending confirmation email...
✅ Email de confirmation envoyé
```

Et l'email doit être reçu dans la boîte mail du parent!
