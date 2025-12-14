# 🚀 Démarrage Rapide - Webhook Stripe

## ⚡ Instructions en 3 Étapes

### 1️⃣ Installer Stripe CLI (Une seule fois)

**PowerShell en tant qu'administrateur:**
```powershell
# Installer Scoop
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Installer Stripe CLI
scoop install stripe

# Vérifier l'installation
stripe --version
```

### 2️⃣ Démarrer Stripe Webhook

**Option A: Script Automatique (Recommandé)**
```powershell
.\start-stripe-webhook.ps1
```

**Option B: Commande Manuelle**
```powershell
# Login (première fois seulement)
stripe login

# Forward webhooks
stripe listen --forward-to localhost:3000/payments/webhook
```

Vous verrez:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx (^C to quit)
```

### 3️⃣ Configurer le Secret

1. **Copiez** le secret `whsec_xxxxx...` affiché
2. **Ouvrez** `.env`
3. **Ajoutez/Modifiez**:
   ```env
   STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx
   ```
4. **Redémarrez** le serveur:
   ```bash
   npm run start:dev
   ```

## ✅ C'est Tout!

Maintenant, chaque paiement déclenchera automatiquement:
- ✅ Création de l'abonnement
- ✅ Enregistrement du paiement
- ✅ Envoi de l'email de confirmation

## 🧪 Test Rapide

Dans un nouveau terminal:
```bash
stripe trigger payment_intent.succeeded
```

Vérifiez les logs du serveur pour voir:
```
🔔 Webhook received!
📨 Received webhook event: payment_intent.succeeded
✅ Subscription created and email sent
```

## 📋 Workflow Quotidien

**Chaque jour de développement:**

1. **Terminal 1**: `.\start-stripe-webhook.ps1` (ou `stripe listen...`)
2. **Terminal 2**: `npm run start:dev`
3. **Développer** normalement
4. **Arrêter**: Ctrl+C dans les deux terminaux

## ⚠️ Important

- **Gardez le terminal Stripe CLI ouvert** pendant le développement
- **Le secret change** à chaque redémarrage de Stripe CLI
- **Mettez à jour `.env`** si vous redémarrez Stripe CLI
- **Redémarrez le serveur** après modification de `.env`

## 🐛 Problèmes Courants

### "stripe: command not found"
```powershell
scoop install stripe
```

### "Webhook signature verification failed"
1. Vérifiez le secret dans le terminal Stripe CLI
2. Copiez-le exactement dans `.env`
3. Redémarrez le serveur

### "Webhook pas reçu"
1. Vérifiez que Stripe CLI est en cours d'exécution
2. Vérifiez que le serveur NestJS est démarré
3. Vérifiez l'URL: `localhost:3000/payments/webhook`

## 📚 Documentation Complète

Voir `STRIPE_WEBHOOK_SETUP.md` pour plus de détails.
