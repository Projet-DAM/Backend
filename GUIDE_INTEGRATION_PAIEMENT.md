# Guide d'Intégration de l'Écran de Paiement (Frontend)

Ce guide détaille les étapes pour implémenter l'interface de paiement dans votre application (Android/Kotlin ou iOS/SwiftUI) en communiquant avec le backend NestJS.

---

## 🚀 Flux de Travail en 3 Étapes

Le processus suit la recommandation officielle de Stripe (PaymentSheet) :

1.  **Backend** : Créer une intention de paiement (`PaymentIntent`).
2.  **Frontend** : Afficher le formulaire Stripe et collecter les fonds.
3.  **Backend** : Confirmer la réussite et activer l'abonnement.

---

## 🛠 Étape 1 : Créer l'Intention de Paiement

Lorsque l'utilisateur clique sur "Payer", appelez l'API du backend. Le serveur calculera automatiquement le montant total si vous ne l'envoyez pas.

**Requête :** `POST /payments/create-intent`  
**Payload :**
```json
{
  "childId": "ID_DE_LENFANT",
  "offerId": "ID_DE_LOFFRE",
  "phoneNumber": "+216XXXXXXXX",
  "selectedOptions": ["SPORTS_OUTFIT", "INSURANCE"],
  "currency": "tnd"
}
```

**Réponse attendue :**
```json
{
  "clientSecret": "pi_3P...secret_...",
  "publishableKey": "pk_test_...",
  "paymentIntentId": "pi_3P..."
}
```

---

## 📱 Étape 2 : Implémentation du Screen (Exemple Kotlin/Android)

Voici comment orchestrer l'affichage du `PaymentSheet` de Stripe.

### A. Initialisation
Dans votre `ViewModel` ou `Fragment`, initialisez le `PaymentSheet`.

```kotlin
// 1. Initialiser Stripe PaymentSheet
paymentSheet = PaymentSheet(this) { result ->
    onPaymentSheetResult(result)
}

// 2. Fonction pour démarrer le paiement
fun startPayment(clientSecret: String, publishableKey: String) {
    PaymentConfiguration.init(context, publishableKey)
    
    val configuration = PaymentSheet.Configuration(
        merchantDisplayName = "Sporty Connect",
        customer = null // Optionnel
    )
    
    // Afficher l'écran Stripe
    paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
}
```

### B. Gestion du Résultat
Une fois que l'utilisateur a saisi ses coordonnées bancaires :

```kotlin
fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
    when (paymentSheetResult) {
        is PaymentSheetResult.Completed -> {
            // ÉTAPE IMPORTANTE : Appeler le backend pour finaliser
            completeSubscriptionOnBackend(paymentIntentId)
        }
        is PaymentSheetResult.Canceled -> {
            showToast("Paiement annulé")
        }
        is PaymentSheetResult.Failed -> {
            showError("Erreur: ${paymentSheetResult.error.localizedMessage}")
        }
    }
}
```

---

## ✅ Étape 3 : Finalisation (Indispensable)

Ne considérez pas l'abonnement comme actif tant que vous n'avez pas appelé cette route. Elle déclenche l'envoi de l'**Email de confirmation**.

**Requête :** `POST /payments/complete`  
**Payload :**
```json
{
  "paymentIntentId": "pi_3P..."
}
```

---

## 📝 Résumé du Code Frontend (Logique Globale)

```javascript
// Pseudo-code du flux
async function handlePaymentClick() {
   // 1. Appel Backend pour le secret (Montant calculé par le serveur)
   const response = await api.post('/payments/create-intent', { 
      childId: '...', 
      offerId: '...',
      currency: 'tnd'
   });

   // 2. Ouvrir Stripe UI
   const result = await StripeSDK.presentPaymentSheet(response.clientSecret);

   if (result.status === 'SUCCEEDED') {
      // 3. Validation Backend (Active l'abonnement + Email)
      const finalResult = await api.post('/payments/complete', { 
         paymentIntentId: response.paymentIntentId 
      });
      if (finalResult.success) {
         navigateToSuccessScreen();
      }
   }
}
```

---

## 💡 Conseils pour l'UX (User Experience)
- **Chargement** : Affichez un indicateur de chargement (`ProgressBar`) pendant l'étape 1 et l'étape 3.
- **Sécurité** : Ne stockez jamais le `clientSecret` localement. Il doit être éphémère.
- **Fallback** : Le backend possède une vérification par Webhook pour rattraper l'activation si l'étape 3 échoue côté client.
