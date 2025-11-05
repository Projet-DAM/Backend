# 🔍 Guide de Débogage : Problème d'Authentification (401 Unauthorized)

## ❌ Problème : Erreur 401 Unauthorized même après authentification

Si tu reçois une erreur `401 Unauthorized` même après avoir cliqué sur "Authorize" dans Swagger, voici comment résoudre le problème :

---

## 🔧 Solutions par étape

### 1. ✅ Vérifier que le token est bien collé dans Swagger

**Problème courant :** Le token n'est pas correctement collé ou il y a des espaces.

**Solution :**
1. Ouvre le bouton **"Authorize"** dans Swagger
2. Vérifie que le token est bien collé **sans espaces** avant/après
3. **⚠️ IMPORTANT :** Ne mets **PAS** "Bearer" devant le token
4. Le token doit ressembler à : `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI2Nz...`
5. Clique sur "Authorize" puis "Close"
6. Vérifie que tu vois le cadenas 🔒 fermé

### 2. ✅ Vérifier que le token est valide et récent

**Problème :** Le token a expiré ou est invalide.

**Solution :**
1. Génère un **nouveau token** via `POST /auth/login` ou `POST /auth/register`
2. Copie le token **immédiatement** après l'avoir reçu
3. Utilise ce nouveau token dans Swagger

**Vérifier l'expiration :**
- Les tokens expirent après 1 jour par défaut (`JWT_EXPIRES_IN=1d`)
- Si ton token a plus de 24h, génère-en un nouveau

### 3. ✅ Vérifier que l'utilisateur existe toujours dans la base de données

**Problème :** L'utilisateur dans le token a été supprimé de la base de données.

**Solution :**
1. Vérifie dans MongoDB que l'utilisateur existe toujours :
   ```bash
   # Dans MongoDB Compass ou mongosh
   use sportyconnect
   db.users.find({ email: "ton-email@test.com" })
   ```

2. Si l'utilisateur n'existe pas :
   - Crée un nouvel utilisateur via `POST /auth/register`
   - Utilise le nouveau token

### 4. ✅ Vérifier la configuration JWT_SECRET

**Problème :** Le `JWT_SECRET` utilisé pour signer le token ne correspond pas à celui utilisé pour le valider.

**Solution :**
1. Vérifie ton fichier `.env` :
   ```env
   JWT_SECRET=secretKey123
   JWT_EXPIRES_IN=1d
   ```

2. **⚠️ IMPORTANT :** Si tu changes `JWT_SECRET`, tous les tokens existants deviendront invalides
   - Tu devras générer de nouveaux tokens

3. Redémarre l'application après avoir modifié `.env` :
   ```bash
   # Arrête l'application (Ctrl+C)
   # Puis relance
   npm run start:dev
   ```

### 5. ✅ Vérifier que l'application est bien redémarrée

**Problème :** Les modifications du code ne sont pas prises en compte.

**Solution :**
1. Arrête l'application (Ctrl+C dans le terminal)
2. Relance l'application :
   ```bash
   npm run start:dev
   ```
3. Attends que l'application soit complètement démarrée
4. Rafraîchis la page Swagger (F5)
5. Réessaie l'authentification

### 6. ✅ Vérifier les logs du serveur

**Pour comprendre ce qui se passe :**

1. Regarde les logs dans le terminal où l'application tourne
2. Quand tu fais une requête, tu devrais voir :
   - Soit des erreurs de validation JWT
   - Soit des erreurs de connexion à MongoDB
   - Soit rien (ce qui signifie que la requête est passée)

### 7. ✅ Tester avec curl pour vérifier si le problème vient de Swagger

**Pour isoler le problème :**

```bash
# 1. Obtenir un token
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ton-email@test.com","motDePasse":"ton-password"}'

# Copie le access_token de la réponse

# 2. Tester un endpoint protégé avec le token
curl -X GET http://localhost:3000/users/TON_USER_ID \
  -H "Authorization: Bearer TON_TOKEN_ICI"
```

**Résultats :**
- ✅ Si ça marche avec curl → Le problème vient de Swagger
- ❌ Si ça ne marche pas avec curl → Le problème vient du backend

---

## 🔍 Checklist de débogage

Utilise cette checklist pour identifier le problème :

- [ ] Le token est bien collé dans Swagger (sans espaces, sans "Bearer")
- [ ] Le token est récent (moins de 24h)
- [ ] J'ai généré un nouveau token après avoir modifié le code
- [ ] L'utilisateur existe toujours dans MongoDB
- [ ] Le fichier `.env` contient `JWT_SECRET=secretKey123`
- [ ] J'ai redémarré l'application après avoir modifié `.env`
- [ ] J'ai rafraîchi la page Swagger (F5)
- [ ] Le cadenas 🔒 est fermé dans Swagger après "Authorize"
- [ ] J'ai testé avec curl pour isoler le problème

---

## 🎯 Étapes de test recommandées

### Test 1 : Vérifier l'authentification de base

```bash
1. POST /auth/register
   {
     "nom": "Test",
     "prenom": "User",
     "email": "test@test.com",
     "motDePasse": "password123",
     "role": "parent"
   }
   → Copie access_token

2. Dans Swagger : Clique "Authorize"
   → Colle le token (sans "Bearer")
   → Clique "Authorize" puis "Close"

3. GET /users/{id} (utilise l'id de la réponse du register)
   → Devrait fonctionner ✅
```

### Test 2 : Vérifier que sans token ça échoue

```bash
1. Dans Swagger : Clique "Authorize" → "Logout"

2. GET /users/{id}
   → Devrait échouer avec 401 Unauthorized ✅
   (C'est normal, ça prouve que l'authentification fonctionne)
```

---

## 💡 Problèmes courants et solutions

### Problème : "Token invalide ou expiré"

**Cause :** Le token est mal formaté, expiré, ou signé avec un mauvais secret.

**Solution :**
1. Génère un nouveau token
2. Vérifie que `JWT_SECRET` est le même partout
3. Vérifie que le token n'a pas plus de 24h

### Problème : "Utilisateur non trouvé dans la base de données"

**Cause :** L'utilisateur dans le token a été supprimé de MongoDB.

**Solution :**
1. Vérifie dans MongoDB que l'utilisateur existe
2. Si l'utilisateur n'existe pas, crée-en un nouveau
3. Génère un nouveau token avec ce nouvel utilisateur

### Problème : Le cadenas reste ouvert dans Swagger

**Cause :** Le token n'a pas été correctement enregistré dans Swagger.

**Solution :**
1. Vérifie que tu as bien cliqué sur "Authorize" dans la fenêtre modale
2. Vérifie qu'il n'y a pas d'erreur dans la console du navigateur (F12)
3. Rafraîchis la page Swagger (F5)
4. Réessaie l'authentification

### Problème : "Cannot read property 'authorization' of undefined"

**Cause :** Le header Authorization n'est pas correctement envoyé.

**Solution :**
1. Vérifie que tu ne mets PAS "Bearer" devant le token dans Swagger
2. Swagger ajoute automatiquement "Bearer " devant
3. Le format final doit être : `Authorization: Bearer eyJhbGci...`

---

## 🚀 Solution rapide (si rien ne fonctionne)

Si rien ne fonctionne, essaie cette procédure complète :

```bash
1. Arrête l'application (Ctrl+C)

2. Vérifie le fichier .env :
   PORT=3000
   MONGO_URI=mongodb://localhost:27017/sportyconnect
   JWT_SECRET=secretKey123
   JWT_EXPIRES_IN=1d

3. Redémarre l'application :
   npm run start:dev

4. Ouvre Swagger : http://localhost:3000/api

5. Crée un NOUVEAU utilisateur :
   POST /auth/register
   {
     "nom": "Nouveau",
     "prenom": "Test",
     "email": "nouveau@test.com",
     "motDePasse": "password123",
     "role": "parent"
   }

6. Copie le access_token IMMÉDIATEMENT

7. Clique "Authorize" dans Swagger

8. Colle le token (sans "Bearer")

9. Clique "Authorize" puis "Close"

10. Vérifie que le cadenas 🔒 est fermé

11. Teste GET /users/{id} avec l'id de la réponse
```

---

## 📞 Besoin d'aide supplémentaire ?

Si le problème persiste après avoir essayé toutes ces solutions :

1. Vérifie les logs du serveur pour voir les erreurs exactes
2. Vérifie la console du navigateur (F12) pour voir les erreurs
3. Vérifie que MongoDB est bien connecté et accessible
4. Vérifie que tous les modules sont correctement importés

---

**🎯 Souviens-toi :** La plupart des problèmes d'authentification viennent de :
- Token mal collé ou avec des espaces
- Token expiré
- Utilisateur supprimé de la base de données
- JWT_SECRET qui ne correspond pas

