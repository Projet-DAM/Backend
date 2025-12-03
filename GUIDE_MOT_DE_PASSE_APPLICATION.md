# Guide : Générer un mot de passe d'application Gmail

## 📧 Email configuré
Votre email `eya.boujnayah2020@gmail.com` a été configuré dans le fichier `.env`.

## 🔐 Étape 1 : Activer l'authentification à deux facteurs

1. Allez sur https://myaccount.google.com/
2. Cliquez sur **Sécurité** dans le menu de gauche
3. Dans la section "Connexion à Google", vérifiez que **"Validation en deux étapes"** est activée
   - Si ce n'est pas le cas, cliquez dessus et suivez les instructions pour l'activer

## 🔑 Étape 2 : Générer un mot de passe d'application

1. Allez directement sur : **https://myaccount.google.com/apppasswords**
   - Ou : Sécurité → Validation en deux étapes → Mots de passe des applications

2. Dans "Sélectionner une application", choisissez **"Autre (nom personnalisé)"**

3. Entrez un nom, par exemple : **"Académie Sportive"**

4. Cliquez sur **"Générer"**

5. **Copiez le mot de passe généré** (16 caractères, format : `xxxx xxxx xxxx xxxx`)
   - ⚠️ **Important** : Vous ne pourrez plus voir ce mot de passe après, copiez-le maintenant !

## 📝 Étape 3 : Ajouter le mot de passe dans .env

1. Ouvrez le fichier `.env` dans `C:/Users/hatem/OneDrive/Bureau/Backend/`

2. Trouvez la ligne `SMTP_PASS=`

3. Collez le mot de passe d'application (sans les espaces) :
   ```env
   SMTP_PASS=xxxxxxxxxxxxxxxx
   ```
   Exemple : Si le mot de passe est `abcd efgh ijkl mnop`, écrivez `abcdefghijklmnop`

4. Sauvegardez le fichier

## ✅ Étape 4 : Tester la configuration

Une fois le mot de passe ajouté, testez avec :

```bash
cd C:/Users/hatem/OneDrive/Bureau/Backend
npm run test:email
```

Le script va :
- ✅ Vérifier la configuration
- ✅ Tester la connexion SMTP
- ✅ Envoyer un email de test à `eya.boujnayah2020@gmail.com`

## 🎯 Résultat attendu

Si tout fonctionne, vous devriez voir :
```
✅ Connexion SMTP réussie !
✅ Le serveur est prêt à envoyer des emails
✅ Email de test envoyé avec succès !
   Vérifiez votre boîte de réception: eya.boujnayah2020@gmail.com
```

Et vous recevrez un email de test dans votre boîte de réception.

## ⚠️ Problèmes courants

### "Invalid login" ou "EAUTH"
- Vérifiez que vous avez utilisé un **mot de passe d'application**, pas votre mot de passe Gmail normal
- Vérifiez qu'il n'y a pas d'espaces dans `SMTP_PASS`

### "Validation en deux étapes non activée"
- Vous devez d'abord activer la validation en deux étapes avant de pouvoir générer un mot de passe d'application

### Email non reçu
- Vérifiez le dossier spam
- Vérifiez que l'email de test a bien été envoyé (regardez les logs)

## 📋 Checklist

- [ ] Validation en deux étapes activée sur Gmail
- [ ] Mot de passe d'application généré
- [ ] Mot de passe copié dans `.env` (sans espaces)
- [ ] Fichier `.env` sauvegardé
- [ ] Test `npm run test:email` réussi
- [ ] Email de test reçu











