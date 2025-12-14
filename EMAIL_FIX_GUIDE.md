# 🔧 Correction du problème d'envoi d'email

## Problèmes identifiés

### 1. ❌ Mot de passe SMTP avec espaces
**Problème actuel dans `.env` :**
```
SMTP_PASS=uwoh syqj xcix jgbr
```

**Correction :**
```
SMTP_PASS=uwohsyqjxcixjgbr
```
⚠️ **IMPORTANT** : Les mots de passe d'application Gmail ne doivent PAS contenir d'espaces !

### 2. ❌ Certificat SSL auto-signé
**Erreur** : `self-signed certificate in certificate chain`

**Solution** : Le code dans `email.service.ts` a déjà la configuration TLS, mais elle ne fonctionne pas correctement.

### 3. ⚠️ Caractère spécial dans SMTP_FROM
**Problème actuel :**
```
SMTP_FROM="Académie Sportive" <eya.boujnayah2020@gmail.com>
```

**Recommandation :**
```
SMTP_FROM="Academie Sportive" <eya.boujnayah2020@gmail.com>
```

## ✅ Solution testée

Le test avec le script `quick-test.js` a réussi avec cette configuration :
- Host: smtp.gmail.com
- Port: 587
- User: eya.boujnayah2020@gmail.com
- Pass: uwohsyqjxcixjgbr (SANS espaces)
- TLS: rejectUnauthorized = false

## 📝 Actions à effectuer

### Étape 1 : Corriger le fichier `.env`
Modifiez votre fichier `.env` avec ces valeurs :

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=uwohsyqjxcixjgbr
SMTP_FROM="Academie Sportive" <eya.boujnayah2020@gmail.com>
FRONTEND_URL=http://localhost:3000
```

### Étape 2 : Vérifier le service email
Le fichier `src/common/services/email.service.ts` contient déjà la bonne configuration TLS, mais assurez-vous que la ligne 76 est bien présente :

```typescript
tls: {
    rejectUnauthorized: process.env.NODE_ENV === 'production' ? true : false,
},
```

### Étape 3 : Redémarrer l'application
Après avoir modifié le `.env`, redémarrez votre serveur NestJS pour que les nouvelles variables d'environnement soient chargées.

## 🧪 Test de vérification

Vous pouvez tester l'envoi d'email avec :
```bash
node quick-test.js
```

Si vous voyez "✅ Email sent!", la configuration fonctionne !

## 📧 Vérification Gmail

1. Assurez-vous que la **validation en 2 étapes** est activée sur le compte Gmail
2. Le mot de passe d'application `uwohsyqjxcixjgbr` doit être valide
3. Si le mot de passe ne fonctionne pas, générez-en un nouveau sur : https://myaccount.google.com/apppasswords

## ⚠️ Note importante

En production, vous devriez utiliser `rejectUnauthorized: true` pour plus de sécurité. Le problème de certificat peut être lié à :
- Un antivirus qui intercepte les connexions SSL
- Un proxy d'entreprise
- Un pare-feu

Pour le développement, `rejectUnauthorized: false` est acceptable.
