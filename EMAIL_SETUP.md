# Configuration de l'envoi d'emails

## Installation de nodemailer

Pour activer l'envoi d'emails de vérification, vous devez installer le package `nodemailer` :

```bash
npm install nodemailer
npm install --save-dev @types/nodemailer
```

## Configuration des variables d'environnement

Ajoutez les variables suivantes dans votre fichier `.env` :

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=lxhz fasb pebp qwie
SMTP_FROM="Académie Sportive <eya.boujnayah2020@gmail.com>"


# URL du frontend pour les liens de vérification
FRONTEND_URL=http://localhost:3000
```

## Configuration Gmail (exemple)

Si vous utilisez Gmail :

1. Activez l'authentification à deux facteurs sur votre compte Gmail
2. Générez un mot de passe d'application :
   - Allez dans votre compte Google
   - Sécurité → Authentification à deux facteurs
   - Mots de passe des applications
   - Créez un nouveau mot de passe d'application
   - Utilisez ce mot de passe dans `SMTP_PASS`

## Configuration pour d'autres services SMTP

### Outlook/Hotmail
```env
SMTP_HOST=smtp-mail.outlook.com
SMTP_PORT=587
```

### Yahoo
```env
SMTP_HOST=smtp.mail.yahoo.com
SMTP_PORT=587
```

### Serveur SMTP personnalisé
Configurez selon les paramètres de votre fournisseur d'email.

## Test en développement

Pour tester sans configurer un vrai serveur SMTP, vous pouvez utiliser [Ethereal Email](https://ethereal.email/) qui génère des comptes de test automatiquement.

## Note importante

Le service d'email est configuré pour ne pas faire échouer l'inscription si l'email ne peut pas être envoyé. L'utilisateur peut toujours se connecter et demander un nouvel email de vérification si nécessaire.






