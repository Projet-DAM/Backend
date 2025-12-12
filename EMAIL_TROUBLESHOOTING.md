# Diagnostic et résolution des problèmes d'envoi d'email

## Vérifications à effectuer

### 1. Installation de nodemailer

Assurez-vous que nodemailer est installé :

```bash
cd C:/Users/hatem/OneDrive/Bureau/Backend
npm install nodemailer @types/nodemailer
```

### 2. Configuration dans .env

Vérifiez que votre fichier `.env` contient les variables suivantes :

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=votre-email@gmail.com
SMTP_PASS=votre-mot-de-passe-app
SMTP_FROM="Académie Sportive" <votre-email@gmail.com>
```

### 3. Configuration Gmail

Si vous utilisez Gmail :

1. **Activez l'authentification à deux facteurs** sur votre compte Gmail
2. **Générez un mot de passe d'application** :
   - Allez sur https://myaccount.google.com/
   - Sécurité → Authentification à deux facteurs
   - Mots de passe des applications
   - Créez un nouveau mot de passe d'application
   - **Utilisez ce mot de passe** (pas votre mot de passe Gmail normal) dans `SMTP_PASS`

### 4. Vérification des logs

Lors de l'inscription, vérifiez les logs du serveur backend. Vous devriez voir :

- ✅ `Code de vérification envoyé à [email]. Message ID: ...` → Email envoyé avec succès
- ❌ `Erreur lors de l'envoi de l'email à [email]:` → Problème d'envoi

### 5. Test de connexion SMTP

Pour tester la connexion SMTP, vous pouvez créer un script de test :

```typescript
// test-email.ts
import * as nodemailer from 'nodemailer';
import * as dotenv from 'dotenv';

dotenv.config();

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: parseInt(process.env.SMTP_PORT || '587'),
  secure: false,
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASS,
  },
});

transporter.verify((error, success) => {
  if (error) {
    console.log('❌ Erreur de connexion SMTP:', error);
  } else {
    console.log('✅ Serveur SMTP prêt à envoyer des emails');
  }
});
```

Exécutez avec : `npx ts-node test-email.ts`

### 6. Solutions alternatives pour le développement

Si vous ne pouvez pas configurer un vrai serveur SMTP, vous pouvez utiliser :

#### Option A : Ethereal Email (pour les tests)
```typescript
// Dans email.service.ts, remplacez temporairement :
import * as nodemailer from 'nodemailer';

// Créer un compte de test
const testAccount = await nodemailer.createTestAccount();

this.transporter = nodemailer.createTransport({
  host: 'smtp.ethereal.email',
  port: 587,
  secure: false,
  auth: {
    user: testAccount.user,
    pass: testAccount.pass,
  },
});
```

#### Option B : Console log (pour le développement)
Modifiez temporairement `email.service.ts` pour logger le code au lieu de l'envoyer :

```typescript
async sendVerificationCode(email: string, nom: string, prenom: string, code: string): Promise<void> {
  // Pour le développement, logger le code au lieu de l'envoyer
  this.logger.log(`[DEV] Code de vérification pour ${email}: ${code}`);
  
  // Commenter l'envoi réel pour les tests
  // ... reste du code d'envoi
}
```

### 7. Vérification du code généré

Le code est généré dans `auth.service.ts` ligne 57 :
```typescript
const verificationCode = Math.floor(100000 + Math.random() * 900000).toString();
```

Vérifiez dans les logs MongoDB ou ajoutez un log temporaire pour voir le code généré.

### 8. Vérification de la base de données

Vérifiez que le code est bien stocké dans MongoDB :

```javascript
// Dans MongoDB Compass ou mongo shell
db.users.findOne({ email: "votre-email@example.com" }, { verificationCode: 1, verificationCodeExpires: 1 })
```

## Erreurs courantes

### "Invalid login"
- Vérifiez que `SMTP_USER` et `SMTP_PASS` sont corrects
- Pour Gmail, utilisez un mot de passe d'application, pas votre mot de passe normal

### "Connection timeout"
- Vérifiez votre connexion internet
- Vérifiez que le port 587 n'est pas bloqué par un firewall

### "Email non reçu"
- Vérifiez le dossier spam
- Vérifiez que l'adresse email est correcte
- Vérifiez les logs du serveur pour voir si l'email a été envoyé

## Pour tester sans email réel

Pendant le développement, vous pouvez temporairement logger le code dans la console au lieu de l'envoyer par email. Modifiez `email.service.ts` :

```typescript
async sendVerificationCode(email: string, nom: string, prenom: string, code: string): Promise<void> {
  // MODE DÉVELOPPEMENT : Logger le code au lieu de l'envoyer
  this.logger.warn(`[DEV MODE] Code de vérification pour ${email}: ${code}`);
  this.logger.warn(`[DEV MODE] Ce code serait normalement envoyé par email`);
  return;
  
  // Code d'envoi réel (commenté pour les tests)
  // ... reste du code
}
```











