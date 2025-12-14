# 🔐 Login Stripe en Cours

## ✅ Stripe CLI Fonctionne!

Stripe CLI est installé et fonctionne correctement à: `C:\Users\hatem\scoop\apps\stripe\current\stripe.exe`

## 🔑 Login en Cours

Le processus de login a démarré. Vous devriez voir:

```
Your pairing code is: brainy-best-happy-humor
This pairing code verifies your authentication with Stripe.
Press Enter to open the browser or visit https://dashboard.stripe.com/stripecli/confirm_auth?t=...
```

## 📋 Étapes à Suivre:

### 1. Ouvrir le Lien dans le Navigateur

Un navigateur devrait s'ouvrir automatiquement. Si ce n'est pas le cas:
1. Copiez le lien affiché dans le terminal
2. Ouvrez-le dans votre navigateur
3. Connectez-vous à votre compte Stripe
4. Confirmez le code de pairing: **brainy-best-happy-humor**

### 2. Attendre la Confirmation

Dans le terminal, vous verrez:
```
Done! The Stripe CLI is configured for [votre-email] with account id acct_xxxxx
```

### 3. Démarrer le Webhook Forwarding

Une fois le login réussi, exécutez:

```powershell
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe listen --forward-to localhost:3000/payments/webhook
```

**OU** utilisez le script qui fait tout automatiquement:

```powershell
# Créer un alias pour faciliter l'utilisation
function stripe { & "C:\Users\hatem\scoop\apps\stripe\current\stripe.exe" $args }

# Maintenant vous pouvez utiliser simplement:
stripe listen --forward-to localhost:3000/payments/webhook
```

### 4. Copier le Webhook Secret

Vous verrez:
```
> Ready! Your webhook signing secret is whsec_xxxxxxxxxxxxx (^C to quit)
```

**Copiez** le secret `whsec_xxxxx...` et ajoutez-le dans `.env`:

```env
STRIPE_WEBHOOK_SECRET=whsec_xxxxxxxxxxxxx
```

### 5. Redémarrer le Serveur

Dans un **autre terminal**:
```bash
npm run start:dev
```

### 6. Tester

Dans un **troisième terminal**:
```powershell
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe trigger payment_intent.succeeded
```

## 🎯 Script PowerShell Simplifié

Pour éviter de taper le chemin complet à chaque fois, créez un alias dans votre profil PowerShell:

```powershell
# Ajouter cette ligne à votre profil PowerShell
function stripe { & "C:\Users\hatem\scoop\apps\stripe\current\stripe.exe" $args }
```

Ou utilisez directement:

```powershell
# Dans le terminal actuel
$env:Path += ";C:\Users\hatem\scoop\apps\stripe\current"
stripe --version
```

## 📊 Commandes Utiles

```powershell
# Avec le chemin complet
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe --version
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe login
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe listen --forward-to localhost:3000/payments/webhook
C:\Users\hatem\scoop\apps\stripe\current\stripe.exe trigger payment_intent.succeeded

# Ou avec l'alias (après l'avoir créé)
stripe --version
stripe listen --forward-to localhost:3000/payments/webhook
stripe trigger payment_intent.succeeded
```

## ⚡ Prochaine Étape

1. **Confirmez le login** dans le navigateur
2. **Attendez** "Done! The Stripe CLI is configured..."
3. **Démarrez** le webhook forwarding
4. **Copiez** le webhook secret dans `.env`
5. **Redémarrez** le serveur
6. **Testez** avec un paiement!

## 🐛 Si le Navigateur ne S'ouvre Pas

Copiez manuellement le lien affiché et ouvrez-le dans votre navigateur:
```
https://dashboard.stripe.com/stripecli/confirm_auth?t=Pqic4Z7YcSWb7NH3IM5CLw3wU4EY51Kn
```
