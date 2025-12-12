# 🔧 Solution : Erreur 401 dans Swagger - Token non inclus dans la requête

## ❌ Problème identifié

D'après votre capture d'écran, la requête curl **ne contient PAS le header `Authorization`**, même si vous avez cliqué sur "Authorize" dans Swagger.

**La requête montre :**
```bash
curl -X POST 'http://localhost:3001/subscriptions' \
  -H 'accept: */*' \
  -H 'Content-Type: application/json' \
  -d '{...}'
```

**Il manque :** `-H 'Authorization: Bearer VOTRE_TOKEN'`

## ✅ Solution étape par étape

### Étape 1 : Vérifier que le token est bien enregistré dans Swagger

1. **Cliquez à nouveau sur le bouton "Authorize" 🔒** en haut à droite
2. **Vérifiez** que vous voyez "Authorized" dans la fenêtre modale
3. **Vérifiez** que le token est bien affiché (même s'il est masqué avec `******`)
4. **Si le token n'est pas là :**
   - Collez à nouveau votre token (sans "Bearer")
   - Cliquez sur "Authorize"
   - Cliquez sur "Close"

### Étape 2 : Rafraîchir la page Swagger

Parfois, Swagger ne met pas à jour les headers correctement :

1. **Rafraîchissez la page** (F5 ou Ctrl+R)
2. **Ré-authentifiez-vous** en cliquant sur "Authorize"
3. **Réessayez** la requête

### Étape 3 : Vérifier que le cadenas est fermé

1. **Regardez** le bouton "Authorize" en haut à droite
2. **Le cadenas doit être fermé** 🔒 (pas ouvert 🔓)
3. **Si le cadenas est ouvert :**
   - Cliquez sur "Authorize"
   - Collez votre token
   - Cliquez sur "Authorize" puis "Close"

### Étape 4 : Générer un nouveau token

Si le problème persiste, générez un nouveau token :

1. **Allez sur** `POST /auth/login`
2. **Connectez-vous** avec vos identifiants
3. **Copiez le nouveau `access_token`**
4. **Allez sur "Authorize"** et collez le nouveau token
5. **Réessayez** la requête

### Étape 5 : Vérifier manuellement dans la requête

Avant de cliquer sur "Execute", vérifiez que la requête curl inclut le header Authorization :

1. **Cliquez sur "Try it out"** sur l'endpoint `POST /subscriptions`
2. **Remplissez** les champs (childId, offerId, autoRenew)
3. **Regardez la section "Curl"** qui s'affiche en bas
4. **Vérifiez** qu'il y a une ligne comme :
   ```bash
   -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'
   ```
5. **Si cette ligne n'existe pas**, le token n'est pas inclus

## 🚨 Solution alternative : Utiliser Postman ou curl directement

Si Swagger continue à avoir des problèmes, utilisez Postman ou curl directement :

### Avec curl :

```bash
# 1. D'abord, obtenez un token
TOKEN=$(curl -s -X POST http://localhost:3001/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "votre-email@example.com",
    "motDePasse": "votre-mot-de-passe"
  }' | jq -r '.access_token')

# 2. Créez l'abonnement avec le token
curl -X POST http://localhost:3001/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "childId": "65b1f0...child",
    "offerId": "65b1f0...offer",
    "autoRenew": true
  }'
```

### Avec Postman :

1. **Créez une nouvelle requête** POST vers `http://localhost:3001/subscriptions`
2. **Onglet Headers**, ajoutez :
   - Key: `Authorization`
   - Value: `Bearer VOTRE_TOKEN_ICI`
3. **Onglet Body**, sélectionnez "raw" et "JSON", puis entrez :
   ```json
   {
     "childId": "65b1f0...child",
     "offerId": "65b1f0...offer",
     "autoRenew": true
   }
   ```
4. **Cliquez sur "Send"**

## 🔍 Vérification du problème

Pour vérifier si le problème vient de Swagger ou de votre token :

1. **Testez un endpoint simple** qui nécessite l'authentification :
   - `GET /users/{userId}` (remplacez {userId} par votre ID)
   - Si cela fonctionne, le problème est spécifique à `/subscriptions`
   - Si cela ne fonctionne pas, le problème est avec l'authentification en général

2. **Vérifiez les logs du serveur** pour voir le message d'erreur exact

3. **Vérifiez la console du navigateur** (F12) pour voir s'il y a des erreurs JavaScript

## 📝 Checklist de vérification

Avant de créer un abonnement :

- [ ] Le cadenas 🔒 est fermé dans Swagger
- [ ] Le token est visible dans la fenêtre "Authorize" (même masqué)
- [ ] La requête curl inclut `-H 'Authorization: Bearer ...'`
- [ ] Le token est récent (généré il y a moins de 24h)
- [ ] Le rôle dans le token est "parent"
- [ ] L'utilisateur existe dans la base de données

## 💡 Astuce : Redémarrer le serveur

Si rien ne fonctionne :

1. **Arrêtez** le serveur (Ctrl+C)
2. **Redémarrez** avec `npm run start:dev`
3. **Rafraîchissez** la page Swagger (F5)
4. **Ré-authentifiez-vous** avec un nouveau token
5. **Réessayez** la requête

## 🆘 Si le problème persiste

1. **Vérifiez** que vous utilisez la bonne URL (http://localhost:3001 dans votre cas)
2. **Vérifiez** que le serveur est bien démarré
3. **Vérifiez** que MongoDB est connecté
4. **Testez** avec Postman ou curl directement pour contourner le problème Swagger

