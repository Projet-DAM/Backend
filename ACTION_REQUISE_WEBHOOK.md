# 🎯 ACTION REQUISE - Configuration du Webhook Stripe

## ✅ Modifications Terminées

Le système de paiement Stripe a été **corrigé et amélioré** ! Le code est maintenant prêt à fonctionner.

---

## 🚀 Étapes Suivantes (À FAIRE MAINTENANT)

### Étape 1 : Installer Stripe CLI

#### Option A : Téléchargement Direct
1. Aller sur https://stripe.com/docs/stripe-cli
2. Télécharger la version Windows
3. Extraire et ajouter au PATH

#### Option B : Avec Chocolatey
```powershell
choco install stripe-cli
```

### Étape 2 : Se Connecter à Stripe
```powershell
stripe login
```
Cela ouvrira votre navigateur pour autoriser l'accès.

### Étape 3 : Configurer le Webhook Local
```powershell
# Dans un nouveau terminal PowerShell
stripe listen --forward-to localhost:3000/payments/webhook
```

**Important** : Cette commande va afficher un **webhook signing secret** qui ressemble à :
```
> Ready! Your webhook signing secret is whsec_1234567890abcdef...
```

### Étape 4 : Ajouter le Secret dans .env
1. Copier le secret affiché (commence par `whsec_`)
2. Ouvrir le fichier `.env`
3. Vérifier que cette ligne existe déjà :
   ```
   STRIPE_WEBHOOK_SECRET=whsec_...
   ```
4. Si le secret est différent, le remplacer par le nouveau
5. Sauvegarder le fichier

### Étape 5 : Redémarrer le Serveur
```powershell
# Arrêter le serveur actuel (Ctrl+C)
# Puis redémarrer
npm run start:dev
```

### Étape 6 : Tester le Webhook
Dans un **nouveau terminal** :
```powershell
stripe trigger payment_intent.succeeded
```

---

## 🔍 Vérification

### Logs Attendus dans le Serveur
Après `stripe trigger payment_intent.succeeded`, vous devriez voir :

```
[PaymentsService] 📨 Received webhook event: payment_intent.succeeded
[PaymentsService] ✅ Payment succeeded: pi_xxxxx
[PaymentsService] ⚠️ No subscriptionId in payment metadata
```

C'est **NORMAL** ! Le test Stripe ne contient pas de `subscriptionId`. 

### Test Complet avec un Vrai Paiement

Pour tester avec un vrai abonnement :

1. **Créer un abonnement** via l'API ou l'interface
2. **Noter l'ID de l'abonnement** (ex: `675b1234567890abc`)
3. **Créer un PaymentIntent** avec cet ID :
   ```powershell
   # Via Swagger (http://localhost:3000/api)
   # Ou avec curl/Postman
   POST /payments/create-intent
   {
     "amount": 7000,
     "currency": "tnd",
     "paymentMethodId": "pm_card_visa",
     "subscriptionId": "675b1234567890abc"
   }
   ```
4. **Confirmer le paiement** (via le frontend ou l'API)
5. **Vérifier les logs** - Vous devriez voir :
   ```
   [PaymentsService] ✅ Payment recorded for subscription: 675b1234567890abc
   ```

---

## 📚 Documentation Créée

Trois documents ont été créés pour vous aider :

1. **`DIAGNOSTIC_PAIEMENT_STRIPE.md`**
   - Analyse détaillée des problèmes
   - Solutions techniques
   - Checklist de vérification

2. **`GUIDE_TEST_STRIPE_WEBHOOK.md`**
   - Guide complet de test
   - Options de configuration
   - Troubleshooting

3. **`RESUME_CORRECTION_STRIPE.md`**
   - Résumé des modifications
   - Flow de paiement corrigé
   - TODO pour les améliorations futures

4. **`test-stripe-webhook.ps1`**
   - Script interactif de test
   - Vérification de configuration
   - Test manuel du webhook

---

## 🎯 Résumé Technique

### Fichiers Modifiés
- ✅ `src/payments/payments.service.ts` - Ajout du webhook handler
- ✅ `src/payments/payments.controller.ts` - Ajout de l'endpoint webhook
- ✅ `src/payments/payments.module.ts` - Import de SubscriptionsModule

### Nouveau Flow
```
Paiement Stripe → Webhook → Enregistrement Auto → Abonnement Activé
```

### Endpoints Disponibles
- `POST /payments/create-intent` - Créer un PaymentIntent (JWT requis)
- `POST /payments/confirm` - Confirmer un paiement (JWT requis)
- `POST /payments/webhook` - Webhook Stripe (Public, pas de JWT)

---

## ⚠️ Important

### En Développement
- Le webhook fonctionne avec `stripe listen`
- Le secret change à chaque fois que vous relancez `stripe listen`
- Pensez à mettre à jour `.env` si nécessaire

### En Production
- Configurer le webhook dans le Stripe Dashboard
- URL : `https://votre-domaine.com/payments/webhook`
- Événements : `payment_intent.succeeded`, `payment_intent.payment_failed`
- Le secret est permanent (ne change pas)

---

## 🆘 Besoin d'Aide ?

### Le webhook ne reçoit rien
1. Vérifier que `stripe listen` est en cours d'exécution
2. Vérifier que le serveur backend est démarré
3. Vérifier l'URL : `localhost:3000/payments/webhook`

### Erreur "Webhook signature verification failed"
1. Vérifier que `STRIPE_WEBHOOK_SECRET` est dans `.env`
2. Vérifier qu'il commence par `whsec_`
3. Redémarrer le serveur après modification

### Le paiement n'est pas enregistré
1. Vérifier que le `subscriptionId` est dans les metadata du PaymentIntent
2. Vérifier que l'abonnement existe dans la base de données
3. Consulter les logs du serveur pour plus de détails

---

## ✅ Checklist Finale

- [ ] Stripe CLI installé
- [ ] `stripe login` effectué
- [ ] `stripe listen` en cours d'exécution
- [ ] `STRIPE_WEBHOOK_SECRET` dans `.env`
- [ ] Serveur redémarré
- [ ] Test avec `stripe trigger` réussi
- [ ] Logs vérifiés

---

## 🎉 Félicitations !

Une fois ces étapes complétées, votre système de paiement Stripe sera **100% fonctionnel** ! 🚀

Les paiements seront automatiquement enregistrés et les abonnements activés sans intervention manuelle.
