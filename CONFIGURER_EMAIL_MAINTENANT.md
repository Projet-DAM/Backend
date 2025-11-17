# ⚡ Configuration Email - Guide Rapide

## 🎯 Objectif
Configurer l'envoi d'emails pour que les codes de vérification soient envoyés à `eya.boujnayah2020@gmail.com`

## 📋 Étapes (5 minutes)

### Étape 1 : Générer un mot de passe d'application Gmail

1. **Allez sur** : https://myaccount.google.com/apppasswords
   - Si vous n'avez pas activé la validation en deux étapes, vous devrez d'abord l'activer

2. **Créez un mot de passe d'application** :
   - Sélectionnez "Autre (nom personnalisé)"
   - Nom : `Académie Sportive Backend`
   - Cliquez sur "Générer"

3. **Copiez le mot de passe** (16 caractères, format : `xxxx xxxx xxxx xxxx`)
   - ⚠️ **Important** : Copiez-le maintenant, vous ne pourrez plus le voir après !

### Étape 2 : Ajouter le mot de passe dans .env

1. Ouvrez le fichier : `C:/Users/hatem/OneDrive/Bureau/Backend/.env`

2. Trouvez la ligne : `SMTP_PASS=`

3. Collez le mot de passe (sans espaces) :
   ```env
   SMTP_PASS=abcdefghijklmnop
   ```
   Exemple : Si Gmail vous donne `abcd efgh ijkl mnop`, écrivez `abcdefghijklmnop`

4. **Sauvegardez le fichier**

### Étape 3 : Redémarrer le backend

Le backend doit être redémarré pour prendre en compte la nouvelle configuration :

1. Arrêtez le backend (Ctrl+C dans le terminal)
2. Redémarrez-le :
   ```bash
   cd C:/Users/hatem/OneDrive/Bureau/Backend
   npm run start:dev
   ```

### Étape 4 : Tester l'envoi d'email

```bash
npm run test:email
```

Vous devriez recevoir un email de test à `eya.boujnayah2020@gmail.com`

## ✅ Résultat attendu

Après configuration :
- ✅ Les codes de vérification seront envoyés par email
- ✅ L'utilisateur recevra l'email avec le code à 6 chiffres
- ✅ Plus besoin de regarder les logs du backend

## 🆘 Si ça ne fonctionne pas

1. Vérifiez que vous avez utilisé un **mot de passe d'application**, pas votre mot de passe Gmail normal
2. Vérifiez qu'il n'y a pas d'espaces dans `SMTP_PASS`
3. Vérifiez que le backend a bien redémarré
4. Consultez les logs du backend pour voir les erreurs






