# 🚀 Guide de démarrage et test des emails

## ⚠️ PROBLÈME IDENTIFIÉ

Votre serveur NestJS n'est **PAS en cours d'exécution** !
C'est pour cela que les emails ne sont pas envoyés.

## ✅ ÉTAPES À SUIVRE

### Étape 1 : Vérifier le fichier .env

Ouvrez votre fichier `.env` et assurez-vous qu'il contient :

```env
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=uwohsyqjxcixjgbr
SMTP_FROM="Academie Sportive" <eya.boujnayah2020@gmail.com>
FRONTEND_URL=http://localhost:3000
```

**IMPORTANT** : Le mot de passe ne doit PAS contenir d'espaces !
- ❌ Incorrect : `uwoh syqj xcix jgbr`
- ✅ Correct : `uwohsyqjxcixjgbr`

### Étape 2 : Démarrer le serveur NestJS

Dans un terminal, exécutez :

```bash
npm run start:dev
```

Attendez que vous voyiez un message comme :
```
[Nest] ... - Application is running on: http://localhost:3001
```

### Étape 3 : Vérifier les logs du serveur

Quand le serveur démarre, vous devriez voir dans les logs :

```
📧 SMTP Configuration check: SMTP_PASS=***SET***, SMTP_USER=eya.boujnayah2020@gmail.com, SMTP_HOST=smtp.gmail.com
✅ Configuration SMTP chargée avec succès
```

Si vous voyez :
```
⚠️ SMTP non configuré, utilisation d'Ethereal Email pour le développement
```

Cela signifie que le `.env` n'est pas correctement chargé.

### Étape 4 : Tester l'envoi d'email

Une fois le serveur démarré, dans un NOUVEAU terminal, exécutez :

```bash
node test-nestjs-email.js
```

Cela va :
1. Vérifier que le serveur est en cours d'exécution
2. Créer un utilisateur de test
3. Déclencher l'envoi d'un email de vérification

### Étape 5 : Vérifier les logs

Dans le terminal où tourne votre serveur NestJS, vous devriez voir :

```
📤 Tentative d'envoi de l'email à test...@example.com...
✅ Code de vérification envoyé à test...@example.com. Message ID: <...>
```

Si vous voyez une erreur, notez-la et partagez-la.

## 🧪 Tests disponibles

### Test 1 : Test SMTP direct (sans NestJS)
```bash
node quick-test.js
```
Ce test vérifie que la configuration SMTP fonctionne.
✅ Ce test RÉUSSIT actuellement !

### Test 2 : Test via NestJS
```bash
node test-nestjs-email.js
```
Ce test vérifie que NestJS peut envoyer des emails.
❌ Ce test ÉCHOUE car le serveur n'est pas démarré.

## 🔍 Diagnostic

### Si le serveur ne démarre pas :

1. Vérifiez qu'il n'y a pas d'autre processus sur le port 3001 :
   ```bash
   netstat -ano | findstr :3001
   ```

2. Vérifiez les erreurs dans le terminal

3. Assurez-vous que toutes les dépendances sont installées :
   ```bash
   npm install
   ```

### Si les emails ne sont toujours pas envoyés :

1. Vérifiez que le `.env` est bien dans le dossier racine du projet

2. Redémarrez complètement le serveur (Ctrl+C puis `npm run start:dev`)

3. Vérifiez les logs du serveur pour voir les messages d'erreur

4. Testez avec `node quick-test.js` pour confirmer que SMTP fonctionne

## 📧 Vérification Gmail

Si le mot de passe d'application ne fonctionne pas :

1. Allez sur https://myaccount.google.com/apppasswords
2. Générez un nouveau mot de passe d'application
3. Copiez-le SANS espaces dans le `.env`
4. Redémarrez le serveur

## ⚡ Commandes rapides

```bash
# Démarrer le serveur
npm run start:dev

# Dans un autre terminal - Tester SMTP
node quick-test.js

# Dans un autre terminal - Tester NestJS
node test-nestjs-email.js
```

## 🎯 Checklist

- [ ] Le fichier `.env` contient le bon mot de passe (sans espaces)
- [ ] Le serveur NestJS est démarré (`npm run start:dev`)
- [ ] Les logs montrent "Configuration SMTP chargée avec succès"
- [ ] Le test `quick-test.js` réussit
- [ ] Le test `test-nestjs-email.js` réussit
- [ ] Les logs du serveur montrent l'envoi d'email

## 🆘 Besoin d'aide ?

Si après avoir suivi toutes ces étapes, les emails ne sont toujours pas envoyés :

1. Partagez les logs complets du serveur NestJS
2. Partagez le résultat de `node quick-test.js`
3. Partagez le résultat de `node test-nestjs-email.js`
4. Indiquez si vous voyez des erreurs spécifiques
