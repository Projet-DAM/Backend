# 🎯 Guide Rapide : Configuration Webhook Stripe en 4 Étapes

## Méthode la Plus Simple (Test Local)

### ✅ Étape 1 : Installer Stripe CLI

**Ouvrez PowerShell en tant qu'administrateur** et exécutez :

```powershell
winget install stripe
```

Si `winget` ne fonctionne pas, téléchargez manuellement :
- https://github.com/stripe/stripe-cli/releases/latest
- Téléchargez : `stripe_X.X.X_windows_x86_64.zip`
- Décompressez et exécutez `stripe.exe`

---

### ✅ Étape 2 : Se connecter à Stripe

```powershell
stripe login
```

➡️ Votre navigateur s'ouvrira automatiquement
➡️ Cliquez sur "Allow access"
➡️ Revenez au terminal

---

### ✅ Étape 3 : Démarrer votre Backend

**Dans un premier terminal PowerShell** :

```powershell
cd C:\Users\dell\Documents\GitHub\Frontend_Android\Backend
npm run start:dev
```

➡️ **Gardez ce terminal OUVERT**

---

### ✅ Étape 4 : Démarrer le Webhook Forwarding

**Dans un DEUXIÈME terminal PowerShell** :

```powershell
cd C:\Users\dell\Documents\GitHub\Frontend_Android\Backend
stripe listen --forward-to http://localhost:3000/payments/webhook
```

Vous verrez :

```
> Ready! You can now accept webhook events
> Your webhook signing secret is whsec_aBcDeFgHiJkLmNoPqRsTuVwXyZ123456 (^C to quit)
```

**📋 COPIEZ LE SECRET** (commence par `whsec_`)

---

### ✅ Étape 5 : Ajouter le Secret dans .env

Ouvrez votre fichier `.env` et ajoutez :

```env
STRIPE_WEBHOOK_SECRET=whsec_aBcDeFgHiJkLmNoPqRsTuVwXyZ123456
```

➡️ Remplacez par VOTRE secret affiché dans le terminal

---

### ✅ Étape 6 : Redémarrer le Backend

**Dans le premier terminal**, arrêtez le serveur (Ctrl+C) et relancez :

```powershell
npm run start:dev
```

---

## 🎉 C'EST FAIT !

Maintenant, quand vous faites un paiement dans l'application Android :

1. ✅ Le paiement est traité par Stripe
2. ✅ Stripe envoie le webhook à `stripe listen`
3. ✅ `stripe listen` transmet le webhook à votre backend local
4. ✅ Votre backend envoie le WhatsApp de confirmation

---

## 🧪 Tester sans l'app Android

### Test 1 : Message WhatsApp direct

```powershell
npm run test:whatsapp
```

### Test 2 : Simuler un paiement Stripe

**Dans un TROISIÈME terminal** :

```powershell
stripe trigger payment_intent.succeeded
```

➡️ Vous devriez recevoir un WhatsApp !

---

## 📱 Configuration Terminale

Vous devez avoir **3 terminaux ouverts** :

```
Terminal 1 : npm run start:dev
            (Backend qui tourne)
            
Terminal 2 : stripe listen --forward-to http://localhost:3000/payments/webhook
            (Webhook forwarding)
            
Terminal 3 : Libre pour tester avec "stripe trigger"
```

---

## ⚠️ Erreurs Communes

### Erreur : "stripe: command not found"
➡️ Stripe CLI n'est pas installé ou pas dans le PATH
➡️ Solution : `winget install stripe` ou téléchargez manuellement

### Erreur : "Error while authenticating"
➡️ Problème de connexion Stripe
➡️ Solution : `stripe login` et suivez les instructions

### Erreur : "Connection refused"
➡️ Le backend ne tourne pas sur le port 3000
➡️ Solution : Vérifiez que `npm run start:dev` est lancé

### Pas de WhatsApp reçu
➡️ Vous n'avez pas rejoint le sandbox Twilio
➡️ Solution : Envoyez "join <code>" à +14155238886 sur WhatsApp

---

## 🔍 Vérifier que tout fonctionne

### Dans le Terminal 1 (Backend), vous devriez voir :

```
[Nest] 12345  - Application is running on: http://localhost:3000
```

### Dans le Terminal 2 (Webhook), vous devriez voir :

```
> Ready! You can now accept webhook events
```

### Quand un paiement arrive, Terminal 1 affichera :

```
Webhook reçu: payment_intent.succeeded
Paiement réussi: pi_xxxxxxxxxxxxx
Montant: 5000 eur
Message de confirmation envoyé à +21627863334
Message WhatsApp envoyé avec succès: SMxxxxxxxxxxxx
```

---

## 💡 Astuce Pro

Créez un fichier `.env.local` pour vos tests :

```env
# .env.local (pour les tests)
STRIPE_WEBHOOK_SECRET=whsec_votre_secret_de_test
```

Puis lancez avec :
```powershell
npm run start:dev
```

---

## 📞 Support

Si ça ne fonctionne toujours pas :

1. Vérifiez les 3 terminaux sont ouverts
2. Vérifiez le `.env` contient le bon `STRIPE_WEBHOOK_SECRET`
3. Testez avec `npm run test:whatsapp` pour isoler le problème
4. Regardez les logs dans Terminal 1 et Terminal 2

**Le message WhatsApp devrait arriver en 2-5 secondes !** ⚡
