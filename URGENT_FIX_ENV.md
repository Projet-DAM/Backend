# 🚨 INSTRUCTIONS URGENTES - Fichier .env corrompu

## Le problème

Le fichier `.env` a des **sauts de ligne invisibles** dans les clés Stripe qui empêchent Stripe de fonctionner.

## ✅ Solution (MANUEL - 2 minutes)

### Étape 1 : Ouvrir le fichier
1. Dans VS Code, ouvrez le fichier `.env`
2. Vous devriez voir quelque chose comme :
   ```
   STRIPE_SECRET_KEY=sk_test_51QYHCVBk6W00VGzt9lMG1gWrRdk4m
   
   
   Krm6ZmNmlrYZmIdBubtxOyzD5POs5Imq
   ```

### Étape 2 : Supprimer TOUT le contenu
1. Sélectionnez tout (Ctrl+A)
2. Supprimez (Delete)

### Étape 3 : Copier-coller ce contenu EXACT

```
MONGO_URI=mongodb://localhost:27017/sportyconnect
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=eya.boujnayah2020@gmail.com
SMTP_PASS=uwoh syqj xcix jgbr
SMTP_FROM=Académie Sportive <eya.boujnayah2020@gmail.com>
STRIPE_SECRET_KEY=sk_test_51QYHCVBk6W00VGzt9lMG1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5Imq
STRIPE_PUBLISHABLE_KEY=pk_test_51QYHCVBk6W00VGztPqCVBk6W00VGzt9lMG1gWrRdk4mKrm6ZmNmlrYZmIdBubtxOyzD5POs5Imq
STRIPE_WEBHOOK_SECRET=whsec_test_placeholder9DAIM06ZwtZcd
PORT=3000
JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRES_IN=7d
```

### Étape 4 : Vérifier visuellement
Assurez-vous que :
- ✅ `STRIPE_SECRET_KEY=sk_test_...` est sur UNE SEULE ligne
- ✅ `STRIPE_PUBLISHABLE_KEY=pk_test_...` est sur UNE SEULE ligne
- ✅ Pas de lignes vides entre les variables
- ✅ Pas d'espaces avant ou après le `=`

### Étape 5 : Sauvegarder
1. Sauvegardez (Ctrl+S)
2. Fermez le fichier
3. Rouvrez-le pour vérifier

### Étape 6 : Redémarrer le serveur
```bash
# Arrêtez le serveur (Ctrl+C dans le terminal)
# Puis redémarrez :
npm run start:dev
```

### Étape 7 : Tester
```bash
# Dans un nouveau terminal :
curl http://localhost:3000/test/email
```

## 🔍 Comment vérifier que c'est corrigé

Après redémarrage, vous ne devriez PLUS voir cette erreur :
```
Invalid API Key provided: sk_test_**********************************************************5Imq
```

Si vous voyez toujours cette erreur, c'est que les clés sont encore sur plusieurs lignes.

## 📝 Note importante

Les clés Stripe sont TRÈS longues (environ 100+ caractères). Elles doivent être sur UNE SEULE ligne sans aucun saut de ligne.

## ⚠️ Si ça ne marche toujours pas

1. Vérifiez que vous avez bien sauvegardé le fichier
2. Vérifiez que le serveur a bien été redémarré
3. Vérifiez dans VS Code qu'il n'y a pas de caractères invisibles (activez "Render Whitespace")
