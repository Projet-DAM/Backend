# ✅ Stripe CLI Installé - Prochaines Étapes

## 🎉 Installation Réussie!

Stripe CLI version 1.33.0 a été installé avec succès via Scoop.

## ⚠️ Action Requise: Redémarrer le Terminal

Le PATH n'est pas encore mis à jour dans ce terminal. Vous devez:

### Option 1: Redémarrer VS Code (Recommandé)
1. Fermez VS Code complètement
2. Rouvrez VS Code
3. Ouvrez un nouveau terminal PowerShell

### Option 2: Redémarrer uniquement le Terminal
1. Fermez tous les terminaux dans VS Code
2. Ouvrez un nouveau terminal PowerShell

### Option 3: Recharger le PATH (Avancé)
```powershell
$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
```

## 🚀 Après Redémarrage

### 1. Vérifier l'Installation
```powershell
stripe --version
```

Vous devriez voir:
```
stripe version 1.33.0
```

### 2. Login Stripe
```powershell
stripe login
```

Cela ouvrira votre navigateur pour vous connecter à votre compte Stripe.

### 3. Démarrer le Webhook Forwarding

**Option A: Script Automatique**
```powershell
.\start-stripe-webhook.ps1
```

**Option B: Commande Manuelle**
```powershell
stripe listen --forward-to localhost:3000/payments/webhook
```

Vous verrez:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx (^C to quit)
```

### 4. Copier le Webhook Secret

1. **Copiez** le secret `whsec_xxxxx...` affiché
2. **Ouvrez** `.env`
3. **Ajoutez/Modifiez**:
   ```env
   STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx
   ```

### 5. Redémarrer le Serveur NestJS

Dans un autre terminal:
```bash
npm run start:dev
```

## ✅ Test

Dans un troisième terminal:
```bash
stripe trigger payment_intent.succeeded
```

Vérifiez les logs du serveur pour voir:
```
🔔 Webhook received!
📨 Received webhook event: payment_intent.succeeded
💰 Payment succeeded
✅ Subscription created and email sent
```

## 📋 Résumé des Commandes

```powershell
# 1. Vérifier installation
stripe --version

# 2. Login (première fois)
stripe login

# 3. Démarrer webhook forwarding (garder ouvert)
stripe listen --forward-to localhost:3000/payments/webhook

# 4. Dans un autre terminal: démarrer le serveur
npm run start:dev

# 5. Dans un troisième terminal: tester
stripe trigger payment_intent.succeeded
```

## 🐛 Si "stripe: command not found"

1. Redémarrez VS Code complètement
2. Ouvrez un nouveau terminal
3. Essayez à nouveau `stripe --version`

Si ça ne fonctionne toujours pas:
```powershell
# Vérifier où Stripe est installé
scoop prefix stripe

# Devrait afficher: C:\Users\hatem\scoop\apps\stripe\current
```

## 📚 Documentation

- `QUICK_START_WEBHOOK.md` - Guide rapide
- `STRIPE_WEBHOOK_SETUP.md` - Guide complet
- `start-stripe-webhook.ps1` - Script automatique

## 🎯 Prochaine Étape

**REDÉMARREZ VOTRE TERMINAL** puis suivez les étapes ci-dessus!
