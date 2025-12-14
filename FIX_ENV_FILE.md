# ⚠️ FICHIER .ENV CORRIGÉ - INSTRUCTIONS

## Le fichier .env actuel est corrompu avec des sauts de ligne dans les clés Stripe

### ✅ Contenu correct à copier dans .env :

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

### 📝 Instructions :

1. **Ouvrez le fichier `.env`** dans VS Code
2. **Supprimez tout le contenu**
3. **Copiez-collez** le contenu ci-dessus
4. **Vérifiez** que chaque clé Stripe est sur UNE SEULE ligne
5. **Sauvegardez** le fichier
6. **Redémarrez** le serveur : `npm run start:dev`

### ⚠️ IMPORTANT :

- **STRIPE_SECRET_KEY** doit être sur UNE ligne (commence par `sk_test_`)
- **STRIPE_PUBLISHABLE_KEY** doit être sur UNE ligne (commence par `pk_test_`)
- **Pas de sauts de ligne** au milieu des clés
- **Pas d'espaces** avant ou après le signe `=`

### 🔍 Vérification :

Après correction, testez avec :
```bash
curl http://localhost:3000/test/email
```

Si vous voyez encore l'erreur "Invalid API Key", c'est que les clés Stripe sont toujours sur plusieurs lignes.
