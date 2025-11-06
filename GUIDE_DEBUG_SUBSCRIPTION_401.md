# 🔍 Guide de Débogage : Erreur 401 lors de la création d'abonnement

## ❌ Problème

Vous recevez une erreur `401 Unauthorized` lorsque vous essayez de créer un abonnement, même après avoir :
1. ✅ Fait le signup avec `role: "parent"`
2. ✅ Pris le token pour l'autorisation
3. ✅ Fait le login
4. ✅ Pris le token pour l'autorisation

## 🔧 Solutions étape par étape

### 1. ✅ Vérifier le format du token dans la requête

**Le problème le plus courant :** Le token n'est pas envoyé correctement dans le header HTTP.

**Solution :**

#### Si vous utilisez Swagger UI :

1. **Cliquez sur le bouton "Authorize" 🔒** en haut à droite de la page Swagger
2. **Dans le champ "Value"**, collez votre token **SANS "Bearer"**
   - ❌ **FAUX :** `Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
   - ✅ **CORRECT :** `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`
3. **Cliquez sur "Authorize"** puis **"Close"**
4. **Vérifiez** que vous voyez un cadenas 🔒 fermé à côté du bouton "Authorize"

#### Si vous utilisez Postman ou curl :

**Format correct du header :**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Exemple avec curl :**
```bash
curl -X POST http://localhost:3000/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer VOTRE_TOKEN_ICI" \
  -d '{
    "childId": "ID_DE_L_ENFANT",
    "offerId": "ID_DE_L_OFFRE",
    "autoRenew": false
  }'
```

**Exemple avec Postman :**
- Onglet **Headers**
- Key: `Authorization`
- Value: `Bearer VOTRE_TOKEN_ICI`

### 2. ✅ Vérifier que le token est récent et valide

**Problème :** Le token a expiré ou est invalide.

**Solution :**
1. **Générez un nouveau token** via `POST /auth/login` :
   ```json
   {
     "email": "votre-email@example.com",
     "motDePasse": "votre-mot-de-passe"
   }
   ```
2. **Copiez le `access_token`** immédiatement après l'avoir reçu
3. **Utilisez ce nouveau token** dans votre requête

**Note :** Les tokens expirent après 1 jour par défaut (`JWT_EXPIRES_IN=1d`)

### 3. ✅ Vérifier que l'utilisateur existe toujours dans la base de données

**Problème :** L'utilisateur dans le token a été supprimé de la base de données.

**Solution :**
1. **Vérifiez dans MongoDB** que l'utilisateur existe :
   ```javascript
   // Dans MongoDB Compass ou mongosh
   use sportyconnect
   db.users.find({ email: "votre-email@example.com" })
   ```

2. **Si l'utilisateur n'existe pas :**
   - Créez un nouvel utilisateur via `POST /auth/register`
   - Utilisez le nouveau token

### 4. ✅ Vérifier que le rôle dans le token est "parent"

**Problème :** Le rôle dans le token n'est pas "parent".

**Solution :**
1. **Décodez votre token JWT** sur [jwt.io](https://jwt.io)
2. **Vérifiez** que le champ `role` dans le payload est `"parent"` (en minuscules)
3. **Si le rôle est incorrect :**
   - Créez un nouvel utilisateur avec `role: "parent"` via `POST /auth/register`
   - Utilisez le nouveau token

**Exemple de payload JWT correct :**
```json
{
  "sub": "507f1f77bcf86cd799439011",
  "email": "parent@example.com",
  "role": "parent",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### 5. ✅ Vérifier la configuration JWT_SECRET

**Problème :** Le `JWT_SECRET` utilisé pour signer le token ne correspond pas à celui utilisé pour le valider.

**Solution :**
1. **Vérifiez votre fichier `.env`** :
   ```env
   JWT_SECRET=secretKey123
   ```

2. **Vérifiez** que le même `JWT_SECRET` est utilisé partout :
   - Dans `src/auth/auth.module.ts`
   - Dans `src/auth/jwt.strategy.ts`

3. **⚠️ IMPORTANT :** Si vous changez `JWT_SECRET`, tous les tokens existants deviendront invalides

### 6. ✅ Vérifier que l'enfant appartient au parent

**Problème :** Même si l'authentification fonctionne, vous pourriez recevoir une erreur si l'enfant n'est pas lié au parent.

**Solution :**
1. **Vérifiez** que l'enfant est bien lié au parent :
   - Utilisez `GET /users/{parentId}/children` pour voir les enfants liés
   - Ou vérifiez dans MongoDB :
     ```javascript
     db.users.find({ _id: ObjectId("ID_DE_L_ENFANT"), parent: ObjectId("ID_DU_PARENT") })
     ```

2. **Si l'enfant n'est pas lié :**
   - Utilisez `POST /users/{parentId}/link-child` pour lier l'enfant au parent

### 7. ✅ Vérifier les logs du serveur

**Solution :**
1. **Regardez les logs du serveur** pour voir le message d'erreur exact
2. **Les nouveaux messages d'erreur** vous indiqueront :
   - Si le token est invalide
   - Si l'utilisateur n'existe pas
   - Si le rôle est incorrect
   - Si le token n'est pas envoyé correctement

## 🧪 Test rapide

Pour tester rapidement si votre token fonctionne :

1. **Testez un endpoint simple** qui nécessite l'authentification :
   ```bash
   GET /users/{userId}
   ```
   Avec le header : `Authorization: Bearer VOTRE_TOKEN`

2. **Si cela fonctionne**, le problème est spécifique à la création d'abonnement
3. **Si cela ne fonctionne pas**, le problème est avec le token lui-même

## 📝 Checklist de vérification

Avant de créer un abonnement, vérifiez :

- [ ] Le token est envoyé dans le header `Authorization: Bearer <token>`
- [ ] Le token est récent (généré il y a moins de 24h)
- [ ] L'utilisateur existe dans la base de données
- [ ] Le rôle dans le token est `"parent"` (en minuscules)
- [ ] Le `JWT_SECRET` est correctement configuré
- [ ] L'enfant est lié au parent dans la base de données
- [ ] Vous utilisez le bon format dans Swagger (sans "Bearer" dans le champ Value)

## 🆘 Si le problème persiste

1. **Vérifiez les logs du serveur** pour voir le message d'erreur exact
2. **Testez avec un nouveau token** généré via login
3. **Vérifiez** que l'application est bien démarrée et que la base de données est accessible
4. **Vérifiez** que vous utilisez la bonne URL (http://localhost:3000 par défaut)

## 💡 Exemple complet de workflow

```bash
# 1. Créer un parent
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean@example.com",
    "motDePasse": "password123",
    "role": "parent"
  }'

# 2. Copier le access_token de la réponse

# 3. Créer un enfant
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Marie",
    "email": "marie@example.com",
    "motDePasse": "password123",
    "role": "enfant"
  }'

# 4. Lier l'enfant au parent (utiliser le token du parent)
curl -X POST http://localhost:3000/users/PARENT_ID/link-child \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_DU_PARENT" \
  -d '{
    "childId": "ENFANT_ID"
  }'

# 5. Créer un abonnement (utiliser le token du parent)
curl -X POST http://localhost:3000/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN_DU_PARENT" \
  -d '{
    "childId": "ENFANT_ID",
    "offerId": "OFFRE_ID",
    "autoRenew": false
  }'
```

