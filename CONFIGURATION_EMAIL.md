# Configuration de l'envoi d'emails - Guide rapide

## ✅ Étape 1 : nodemailer est déjà installé

Le package `nodemailer` et ses types sont déjà installés dans le projet.

## 📝 Étape 2 : Configurer le fichier .env

Ouvrez le fichier `.env` dans le répertoire backend et remplissez les valeurs suivantes :

```env
# Configuration SMTP pour l'envoi d'emails
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=votre-email@gmail.com          # ← REMPLACER
SMTP_PASS=votre-mot-de-passe-app         # ← REMPLACER
SMTP_FROM="Académie Sportive" <votre-email@gmail.com>  # ← REMPLACER
FRONTEND_URL=http://localhost:3000
```

## 🔐 Étape 3 : Configuration Gmail (recommandé)

### Pour utiliser Gmail :

1. **Activez l'authentification à deux facteurs** sur votre compte Gmail
   - Allez sur https://myaccount.google.com/
   - Sécurité → Authentification à deux facteurs

2. **Générez un mot de passe d'application** :
   - Allez sur https://myaccount.google.com/apppasswords
   - Sélectionnez "Autre (nom personnalisé)" → Entrez "Académie Sportive"
   - Cliquez sur "Générer"
   - **Copiez le mot de passe généré** (16 caractères sans espaces)

3. **Remplissez le fichier .env** :
   ```env
   SMTP_USER=votre-email@gmail.com
   SMTP_PASS=xxxx xxxx xxxx xxxx  # Le mot de passe d'application généré (sans espaces)
   SMTP_FROM="Académie Sportive" <votre-email@gmail.com>
   ```

## 🧪 Étape 4 : Tester la configuration

Une fois le fichier `.env` configuré, testez la configuration avec :

```bash
npm run test:email
```

Ce script va :
- ✅ Vérifier que toutes les variables sont configurées
- ✅ Tester la connexion au serveur SMTP
- ✅ Envoyer un email de test à votre adresse

## 🔧 Configuration pour d'autres services

### Outlook/Hotmail
```env
SMTP_HOST=smtp-mail.outlook.com
SMTP_PORT=587
SMTP_USER=votre-email@outlook.com
SMTP_PASS=votre-mot-de-passe
```

### Yahoo
```env
SMTP_HOST=smtp.mail.yahoo.com
SMTP_PORT=587
SMTP_USER=votre-email@yahoo.com
SMTP_PASS=votre-mot-de-passe-app
```

## ⚠️ Mode développement (sans email réel)

Si vous ne pouvez pas configurer un vrai serveur SMTP, vous pouvez temporairement modifier `src/common/services/email.service.ts` pour logger le code au lieu de l'envoyer :

```typescript
async sendVerificationCode(email: string, nom: string, prenom: string, code: string): Promise<void> {
  // MODE DÉVELOPPEMENT : Logger le code
  this.logger.warn(`[DEV] Code de vérification pour ${email}: ${code}`);
  return; // Ne pas envoyer l'email réel
}
```

## 📋 Checklist

- [ ] Fichier `.env` créé et configuré
- [ ] `SMTP_USER` rempli avec votre email
- [ ] `SMTP_PASS` rempli avec le mot de passe d'application
- [ ] `SMTP_FROM` configuré
- [ ] Test `npm run test:email` réussi
- [ ] Email de test reçu dans votre boîte de réception

## 🆘 Problèmes courants

### "Invalid login" ou "EAUTH"
- Vérifiez que vous utilisez un **mot de passe d'application** pour Gmail, pas votre mot de passe normal
- Vérifiez que `SMTP_USER` et `SMTP_PASS` sont corrects

### "Connection timeout"
- Vérifiez votre connexion internet
- Vérifiez que le port 587 n'est pas bloqué

### Email non reçu
- Vérifiez le dossier spam
- Vérifiez les logs du serveur backend
- Vérifiez que l'adresse email est correcte

Pour plus de détails, consultez `EMAIL_TROUBLESHOOTING.md`.











