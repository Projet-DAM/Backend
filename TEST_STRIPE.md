# 🧪 Test Rapide Stripe Backend

## ✅ Checklist de Vérification

### 1. Vérifier la Configuration
```bash
# Vérifiez que STRIPE_SECRET_KEY est définie dans .env
cat .env | grep STRIPE_SECRET_KEY
```

**Résultat attendu** :
```
STRIPE_SECRET_KEY=sk_test_XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

### 2. Démarrer le Backend
```bash
npm run start:dev
```

**Résultat attendu** :
```
[Nest] INFO [NestFactory] Starting Nest application...
[Nest] INFO [InstanceLoader] AppModule dependencies initialized
[Nest] INFO [RoutesResolver] PaymentsController {/api/payments}
```

### 3. Tester l'Endpoint de Paiement

#### Test avec cURL (Windows PowerShell)
```powershell
# Remplacez YOUR_AUTH_TOKEN par un vrai token JWT
$headers = @{
    "Authorization" = "Bearer YOUR_AUTH_TOKEN"
    "Content-Type" = "application/json"
}

$body = @{
    amount = 50.00
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:3000/api/payments/create-payment-intent" -Method POST -Headers $headers -Body $body
```

#### Test avec cURL (Linux/Mac)
```bash
# Remplacez YOUR_AUTH_TOKEN par un vrai token JWT
curl -X POST http://localhost:3000/api/payments/create-payment-intent \
  -H "Authorization: Bearer YOUR_AUTH_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00}'
```

**Résultat attendu** :
```json
{
  "clientSecret": "pi_XXXXXXXXXXXXXXXXXXXXXXXXXX_secret_XXXXXXXXXXXXXXXXXXXXXXXXXX"
}
```

### 4. Vérifier dans le Dashboard Stripe

1. Connectez-vous à [Stripe Dashboard (Mode Test)](https://dashboard.stripe.com/test/payments)
2. Allez dans **Paiements** → **Tous les paiements**
3. Vous devriez voir le Payment Intent créé avec le statut "Requires payment method"

---

## 🔍 Vérification des Logs

### Logs à surveiller
```
[PaymentsService] Creating payment intent for amount: XX.XX
[PaymentsService] Payment intent created: pi_XXXXXXXXX
```

### En cas d'erreur
```
[PaymentsService] Error creating payment intent: [message d'erreur]
```

**Causes possibles** :
- ❌ `STRIPE_SECRET_KEY` non définie ou invalide
- ❌ Clé de production utilisée au lieu de clé de test
- ❌ Problème de connexion réseau avec Stripe

---

## 🎯 Test Complet du Flux

### Étape 1 : Créer un Payment Intent
```bash
POST /api/payments/create-payment-intent
Body: { "amount": 100.00 }
```

### Étape 2 : Utiliser le clientSecret dans l'App Android
Le `clientSecret` retourné doit être utilisé dans le Payment Sheet Android.

### Étape 3 : Confirmer le Paiement
Utilisez la carte de test : `4242 4242 4242 4242`

### Étape 4 : Vérifier le Résultat
- ✅ Dans l'app : Toast "Inscription réussie"
- ✅ Dans Stripe Dashboard : Paiement avec statut "Succeeded"
- ✅ Dans les logs backend : Inscription créée

---

## 🐛 Dépannage Backend

### Erreur : "No API key provided"
**Solution** :
```bash
# Vérifiez que .env est bien chargé
echo $STRIPE_SECRET_KEY  # Linux/Mac
$env:STRIPE_SECRET_KEY   # Windows PowerShell
```

### Erreur : "Invalid API Key"
**Solution** :
1. Vérifiez que la clé commence par `sk_test_`
2. Récupérez une nouvelle clé depuis [Stripe Dashboard → Developers → API Keys](https://dashboard.stripe.com/test/apikeys)

### Erreur : "Amount must be at least $0.50"
**Solution** :
Stripe requiert un montant minimum de 0.50 dans la devise utilisée. Vérifiez que `amount >= 0.50`.

---

## 📊 Montants de Test Recommandés

| Montant | Devise | Résultat |
|---------|--------|----------|
| 50.00 TND | TND | ✅ Succès |
| 100.00 TND | TND | ✅ Succès |
| 0.30 TND | TND | ❌ Montant trop faible |

---

## 🔗 Endpoints Backend

### POST `/api/payments/create-payment-intent`
**Headers** :
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Body** :
```json
{
  "amount": 50.00
}
```

**Response** :
```json
{
  "clientSecret": "pi_xxx_secret_xxx"
}
```

---

## 📝 Notes

1. **Mode Test** : Toujours utiliser des clés de test (`sk_test_` et `pk_test_`)
2. **Devise** : Par défaut, Stripe utilise USD. Vérifiez la configuration de devise dans `payments.service.ts`
3. **Webhooks** : Pour tester les webhooks localement, utilisez [Stripe CLI](https://stripe.com/docs/stripe-cli)

---

**Dernière mise à jour** : 2025-11-30
