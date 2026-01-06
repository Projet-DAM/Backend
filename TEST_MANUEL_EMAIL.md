# 🧪 Test Manuel de l'Envoi d'Email après Paiement

## Option 1 : Test via l'Application Mobile (Recommandé)

C'est la méthode la plus simple et la plus réaliste.

### Étapes :
1. Ouvrez l'application mobile
2. Connectez-vous en tant que parent
3. Sélectionnez un enfant
4. Choisissez une offre d'abonnement
5. Cliquez sur "Payer"
6. Entrez les informations de carte de test Stripe :
   - **Carte** : `4242 4242 4242 4242`
   - **Date** : `12/25`
   - **CVC** : `123`
7. Validez le paiement
8. Vérifiez les logs du serveur et votre boîte mail

---

## Option 2 : Test via Postman/Thunder Client

Si vous voulez tester l'API directement sans l'application mobile.

### Étape 1 : Créer un PaymentIntent

**Endpoint :** `POST http://localhost:3000/payments/create-intent`

**Headers :**
```
Authorization: Bearer <votre_token_jwt>
Content-Type: application/json
```

**Body :**
```json
{
  "childId": "67719...",
  "offerId": "67719...",
  "phoneNumber": "+21612345678"
}
```

**Réponse attendue :**
```json
{
  "clientSecret": "pi_..._secret_...",
  "paymentIntentId": "pi_...",
  "publishableKey": "pk_test_..."
}
```

### Étape 2 : Simuler le Paiement Stripe

Vous avez deux options :

#### Option A : Via le Dashboard Stripe (Plus Simple)
1. Allez sur https://dashboard.stripe.com/test/payments
2. Trouvez le PaymentIntent créé (utilisez le `paymentIntentId`)
3. Cliquez sur "Simulate payment" ou utilisez les webhooks de test

#### Option B : Via l'API Stripe CLI
```bash
stripe payment_intents confirm <paymentIntentId> --payment-method=pm_card_visa
```

### Étape 3 : Compléter le Paiement

**Endpoint :** `POST http://localhost:3000/payments/complete`

**Headers :**
```
Authorization: Bearer <votre_token_jwt>
Content-Type: application/json
```

**Body :**
```json
{
  "paymentIntentId": "pi_...",
  "childId": "67719...",
  "offerId": "67719..."
}
```

**Réponse attendue :**
```json
{
  "success": true,
  "subscription": {
    "id": "67719...",
    "status": "ACTIVE",
    "paymentStatus": "PAID",
    "startDate": "2025-12-29T12:00:00.000Z",
    "endDate": "2026-01-29T12:00:00.000Z"
  },
  "emailSent": true,
  "emailError": null
}
```

**✅ Si `emailSent: true`** → L'email a été envoyé avec succès !

**❌ Si `emailSent: false`** → Vérifiez `emailError` pour voir le message d'erreur

---

## Option 3 : Test Direct de l'Envoi d'Email

Si vous voulez tester uniquement la fonction d'envoi d'email sans passer par le paiement.

### Créer un Script de Test

Créez un fichier `test-send-email.ts` :

```typescript
import { NestFactory } from '@nestjs/core';
import { AppModule } from './src/app.module';
import { EmailService } from './src/common/services/email.service';

async function testEmail() {
  const app = await NestFactory.createApplicationContext(AppModule);
  const emailService = app.get(EmailService);

  try {
    console.log('📧 Test d\'envoi d\'email...');
    
    await emailService.sendPaymentConfirmation(
      'votre.email@example.com',  // Remplacez par votre email
      'Jean Dupont',               // Nom du parent
      'Abonnement Mensuel',        // Nom de l'offre
      70,                          // Montant
      'TND',                       // Devise
      new Date(),                  // Date de début
      new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // Date de fin (+30 jours)
    );
    
    console.log('✅ Email envoyé avec succès !');
  } catch (error) {
    console.error('❌ Erreur lors de l\'envoi:', error.message);
  } finally {
    await app.close();
  }
}

testEmail();
```

### Exécuter le Script

```bash
npx ts-node test-send-email.ts
```

---

## 📊 Vérification des Logs

Après avoir effectué un test, vérifiez les logs du serveur :

### Logs de Succès ✅

```
📧 ========== DÉBUT ENVOI EMAIL ==========
📧 Email destinataire: parent@example.com
📧 Appel de emailService.sendPaymentConfirmation...
📂 Chemin du template: C:\Users\hatem\DamBack\src\common\templates\emails\abonnement-confirmation.html
📂 Chemin du logo: C:\Users\hatem\DamBack\src\common\templates\emails\logo.png
📤 Envoi de l'email de confirmation à parent@example.com...
✅ Email de confirmation envoyé à parent@example.com. Message ID: <...>
✅ Email envoyé avec succès !
📧 ========== FIN ENVOI EMAIL ==========
```

### Logs d'Erreur ❌

#### Erreur : Template non trouvé
```
❌ ========== ERREUR ENVOI EMAIL ==========
❌ Message: ENOENT: no such file or directory, open 'C:\Users\hatem\DamBack\src\common\templates\emails\abonnement-confirmation.html'
```

**Solution :** Vérifiez que le fichier existe avec `ls src/common/templates/emails/`

#### Erreur : SMTP invalide
```
❌ Erreur lors de l'envoi de l'email: Invalid login: 535-5.7.8 Username and Password not accepted
```

**Solution :** Vérifiez votre configuration SMTP dans `.env`

---

## 🔍 Vérification de l'Email Reçu

### Si vous utilisez Gmail (SMTP configuré)

1. Ouvrez votre boîte mail
2. Cherchez un email de **"Sporty KIDS"**
3. Sujet : **"✅ Confirmation d'abonnement - Sporty KIDS"**

### Si vous utilisez Ethereal Email (mode développement)

1. Cherchez dans les logs :
   ```
   📬 Prévisualisation : https://ethereal.email/message/...
   ```
2. Ouvrez le lien dans votre navigateur

### Contenu de l'Email

L'email devrait contenir :
- ✅ Logo Sporty KIDS
- ✅ Nom du parent (prénom + nom)
- ✅ Type d'abonnement
- ✅ Montant payé avec devise
- ✅ Date de début
- ✅ Date de fin
- ✅ Design professionnel avec couleurs de la marque

---

## 📝 Checklist de Test

- [ ] Le serveur démarre sans erreur
- [ ] La configuration SMTP est correcte (ou Ethereal Email fonctionne)
- [ ] Le paiement est créé avec succès
- [ ] Le paiement est confirmé par Stripe
- [ ] L'abonnement est créé dans la base de données
- [ ] Le paiement est enregistré
- [ ] L'email est envoyé (logs montrent ✅)
- [ ] La réponse API contient `emailSent: true`
- [ ] L'email est reçu dans la boîte mail
- [ ] L'email contient toutes les informations correctes

---

## 🎯 Résultat Attendu

Après un test réussi :

1. ✅ Paiement traité par Stripe
2. ✅ Abonnement créé (status: ACTIVE, paymentStatus: PAID)
3. ✅ Email envoyé au parent
4. ✅ Email reçu avec toutes les informations
5. ✅ Logs montrent le succès de toutes les étapes

---

**Fichiers de référence :**
- `SOLUTION_EMAIL_PAIEMENT.md` - Diagnostic du problème
- `GUIDE_TEST_EMAIL_PAIEMENT.md` - Guide de test détaillé
- `RESUME_SOLUTION_EMAIL.md` - Résumé de la solution
