# 🧪 Guide de Test - Envoi d'Email après Paiement

## ✅ Modifications Appliquées

J'ai corrigé le problème d'envoi d'email en modifiant les chemins des templates dans `email-template.service.ts` :

### Avant (❌ Ne fonctionnait pas)
```typescript
const templatePath = path.join(__dirname, '../templates/emails/abonnement-confirmation.html');
// Pointait vers: dist/common/services/../templates/emails/... (fichier inexistant)
```

### Après (✅ Fonctionne maintenant)
```typescript
const templatePath = path.join(process.cwd(), 'src/common/templates/emails/abonnement-confirmation.html');
// Pointe vers: C:\Users\hatem\DamBack\src\common\templates\emails\... (fichier existant)
```

## 📋 Checklist de Vérification

### 1. ✅ Vérifier que le serveur démarre sans erreur

Attendez que vous voyiez dans les logs :
```
[Nest] ... LOG [NestApplication] Nest application successfully started
```

### 2. ✅ Vérifier la configuration SMTP

Les logs devraient afficher (lors du premier envoi d'email) :
```
📧 SMTP Configuration check: SMTP_PASS=***SET***, SMTP_USER=..., SMTP_HOST=...
✅ Configuration SMTP chargée avec succès
```

**OU** si SMTP n'est pas configuré (mode développement) :
```
⚠️  SMTP non configuré, utilisation d'Ethereal Email pour le développement
🔄 Création d'un compte Ethereal Email...
✅ Compte Ethereal créé: ...
```

### 3. 🧪 Tester le Flux de Paiement Complet

#### A. Depuis l'Application Mobile

1. **Connectez-vous** en tant que parent
2. **Sélectionnez un enfant** et une **offre d'abonnement**
3. **Accédez à l'écran de paiement**
4. **Entrez les informations de test Stripe** :
   - **Numéro de carte** : `4242 4242 4242 4242`
   - **Date d'expiration** : `12/25` (ou n'importe quelle date future)
   - **CVC** : `123` (ou n'importe quel 3 chiffres)
   - **Numéro de téléphone** : Votre numéro (optionnel)

5. **Validez le paiement**

#### B. Vérifier les Logs Backend

Après avoir validé le paiement, vous devriez voir dans les logs du serveur :

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

**🔴 Si vous voyez une erreur :**
```
❌ ========== ERREUR ENVOI EMAIL ==========
❌ Message: ENOENT: no such file or directory, open '...'
```
Cela signifie que le template n'a pas été trouvé. Vérifiez que les fichiers existent.

#### C. Vérifier la Réponse API

L'application mobile devrait recevoir une réponse comme :
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

**Important :**
- ✅ `emailSent: true` = Email envoyé avec succès
- ❌ `emailSent: false` + `emailError: "..."` = Erreur lors de l'envoi

### 4. 📧 Vérifier la Réception de l'Email

#### Si vous utilisez Gmail (SMTP configuré) :
1. Ouvrez la boîte mail du parent
2. Cherchez un email de **"Sporty KIDS"**
3. Sujet : **"✅ Confirmation d'abonnement - Sporty KIDS"**
4. Vérifiez que l'email contient :
   - Le nom du parent
   - Le type d'abonnement
   - Le montant payé
   - Les dates de début et fin
   - Le logo Sporty KIDS

#### Si vous utilisez Ethereal Email (mode développement) :
1. Cherchez dans les logs le lien de prévisualisation :
   ```
   📬 Prévisualisation : https://ethereal.email/message/...
   ```
2. Ouvrez ce lien dans votre navigateur
3. Vous verrez l'email tel qu'il aurait été envoyé

## 🔧 Dépannage

### Problème : Template non trouvé

**Symptôme :**
```
❌ Erreur lors de l'envoi de l'email à ...: ENOENT: no such file or directory
```

**Solution :**
1. Vérifiez que les fichiers existent :
   ```bash
   ls src/common/templates/emails/
   ```
   Devrait afficher :
   - `abonnement-confirmation.html`
   - `abonnement-expire-7j.html`
   - `logo.png`

2. Exécutez le script de test :
   ```bash
   npx ts-node test-email-paths.ts
   ```

### Problème : Erreur SMTP

**Symptôme :**
```
❌ Erreur lors de l'envoi de l'email: Invalid login: 535-5.7.8 Username and Password not accepted
```

**Solution :**
1. Vérifiez votre fichier `.env` :
   ```env
   SMTP_HOST=smtp.gmail.com
   SMTP_PORT=587
   SMTP_USER=votre.email@gmail.com
   SMTP_PASS=votre_mot_de_passe_application  # Pas votre mot de passe Gmail normal !
   ```

2. Pour Gmail, vous devez utiliser un **mot de passe d'application** :
   - Allez sur https://myaccount.google.com/apppasswords
   - Créez un nouveau mot de passe d'application
   - Copiez-le dans `SMTP_PASS`

### Problème : Email envoyé mais non reçu

**Symptôme :**
- Les logs montrent `✅ Email envoyé avec succès`
- Mais l'email n'arrive pas dans la boîte de réception

**Solution :**
1. Vérifiez le dossier **Spam/Courrier indésirable**
2. Vérifiez que l'adresse email du parent est correcte
3. Si vous utilisez Gmail, vérifiez les **Onglets** (Promotions, Social, etc.)

## 📊 Flux Complet (Rappel)

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Client : Crée PaymentIntent                              │
│    POST /payments/create-intent                             │
│    { childId, offerId, phoneNumber }                        │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 2. Client : Affiche Stripe PaymentSheet                     │
│    L'utilisateur entre ses informations de carte            │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 3. Stripe : Traite le paiement                              │
│    Status devient "succeeded"                               │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 4. Client : Complète le paiement                            │
│    POST /payments/complete                                  │
│    { paymentIntentId, childId, offerId }                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 5. Backend : Vérifie le paiement                            │
│    - Récupère le PaymentIntent depuis Stripe               │
│    - Vérifie que status === "succeeded"                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 6. Backend : Crée l'abonnement                              │
│    - Crée un nouvel abonnement dans la base de données     │
│    - Status: ACTIVE, PaymentStatus: PAID                    │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 7. Backend : Enregistre le paiement                         │
│    - Ajoute le paiement à l'historique de l'abonnement     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 8. Backend : Envoie l'email de confirmation ✅              │
│    - Charge le template HTML                                │
│    - Remplace les variables (nom, montant, dates)          │
│    - Envoie via SMTP ou Ethereal Email                     │
└─────────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────────┐
│ 9. Backend : Retourne la réponse                            │
│    { success: true, subscription: {...}, emailSent: true }  │
└─────────────────────────────────────────────────────────────┘
```

## 🎯 Résultat Attendu

Après un paiement réussi :

1. ✅ L'abonnement est créé dans la base de données
2. ✅ Le statut de l'abonnement est **ACTIVE**
3. ✅ Le statut de paiement est **PAID**
4. ✅ Un email de confirmation est envoyé au parent
5. ✅ L'email contient toutes les informations correctes
6. ✅ Le parent reçoit l'email dans sa boîte de réception

## 📝 Notes Supplémentaires

### Templates Disponibles

1. **`abonnement-confirmation.html`** : Email de confirmation après paiement
   - Variables : `{{ prenom }}`, `{{ nom }}`, `{{ type }}`, `{{ price }}`, `{{ dateStart }}`, `{{ dateEnd }}`

2. **`abonnement-expire-7j.html`** : Email d'avertissement 7 jours avant expiration
   - Variables : `{{ prenom }}`, `{{ nom }}`, `{{ childName }}`, `{{ offerName }}`, `{{ endDate }}`

### Tâche Cron pour les Expirations

Un job cron s'exécute quotidiennement pour envoyer des emails d'avertissement 7 jours avant l'expiration des abonnements.

Vérifiez dans les logs :
```
[SubscriptionsSchedulerService] Vérification des abonnements qui expirent dans 7 jours...
```

---

**Dernière mise à jour :** 2025-12-29
**Fichiers modifiés :** `src/common/services/email-template.service.ts`
