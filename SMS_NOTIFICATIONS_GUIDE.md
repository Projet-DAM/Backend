# Documentation des Notifications SMS

## Vue d'ensemble
Le système utilise **Twilio** pour envoyer des notifications SMS aux parents dans deux cas :
1. **Confirmation de paiement** : Envoyé immédiatement après un paiement réussi.
2. **Alerte d'expiration** : Envoyé 7 jours avant l'expiration de l'abonnement.

## Configuration Requise
Ajoutez ces variables à votre fichier `.env` :

```env
TWILIO_ACCOUNT_SID=votre_sid_twilio
TWILIO_AUTH_TOKEN=votre_token_twilio
TWILIO_PHONE_NUMBER=votre_numero_twilio
```

> **Note**: Si ces variables sont absentes, le service SMS sera automatiquement désactivé et logguera des avertissements au lieu d'échouer.

## Architecture

### 1. SmsService (`src/common/services/sms.service.ts`)
Service central gérant l'envoi des SMS.
- Vérifie si Twilio est configuré.
- Gère les erreurs d'envoi sans bloquer le flux principal.
- Loggue les succès et échecs.

### 2. Intégration Utilisateur (`src/users/entity/user.entity.ts`)
Le modèle `User` a été enrichi avec un champ `phoneNumber`.
- Ce champ est optionnel (`phoneNumber?: string`).
- Il doit être collecté lors de l'inscription ou du paiement (côté Android).

### 3. Workflow de Paiement (`src/subscriptions/subscriptions.service.ts`)
Lorsqu'un paiement est enregistré (`recordPayment`) :
1. Le paiement est marqué comme `SUCCESS`.
2. L'email de confirmation est envoyé.
3. **Le système vérifie si le parent a un numéro de téléphone.**
4. Si oui, un SMS est envoyé : *"Votre paiement de [Montant] pour l'offre [Nom] a été effectué avec succès."*

### 4. Workflow d'Expiration (`src/subscriptions/subscriptions-scheduler.service.ts`)
Une tâche planifiée (`Cron`) tourne tous les jours à 9h00 :
1. Recherche les abonnements expirant dans 7 jours exacts.
2. Vérifie si une alerte a déjà été envoyée.
3. Envoie un email d'alerte.
4. **Envoie un SMS si le numéro est disponible** : *"Votre abonnement pour [Nom] expire dans [X] jours..."*
5. Marque l'abonnement comme notifié (`expirationWarningSent: true`).

## Intégration Android

### Formulaire de Paiement
Ajoutez un champ pour saisir le numéro de téléphone si ce n'est pas déjà fait dans le profil.

```kotlin
// Exemple de payload pour création/mise à jour utilisateur
data class UpdateUserDto(
    val phoneNumber: String
)
```

Assurez-vous que le numéro est au format international (ex: `+21612345678`) pour garantir la compatibilité avec Twilio.
