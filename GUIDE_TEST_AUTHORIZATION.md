# 🔐 Guide : Tester les parties avec Available Authorizations

## 📋 Table des matières

1. [Qu'est-ce que "Available Authorizations" ?](#quest-ce-que-available-authorizations)
2. [Étape 1 : Obtenir un token JWT](#étape-1--obtenir-un-token-jwt)
3. [Étape 2 : S'authentifier dans Swagger](#étape-2--sauthentifier-dans-swagger)
4. [Étape 3 : Tester les endpoints protégés](#étape-3--tester-les-endpoints-protégés)
5. [Étape 4 : Tester les endpoints avec rôles spécifiques](#étape-4--tester-les-endpoints-avec-rôles-spécifiques)
6. [Scénarios de test complets](#scénarios-de-test-complets)
7. [Résolution de problèmes](#résolution-de-problèmes)

---

## 🎯 Qu'est-ce que "Available Authorizations" ?

**"Available Authorizations"** dans Swagger est le système d'authentification qui permet de :

- ✅ Tester les endpoints qui nécessitent d'être connecté
- ✅ Simuler des requêtes authentifiées
- ✅ Tester les permissions selon les rôles (parent, enfant, coach, académie)

**Dans Swagger, tu verras un bouton "Authorize" 🔒 en haut à droite de la page.**

---

## 🚀 Étape 1 : Obtenir un token JWT

**Avant de pouvoir tester les endpoints protégés, tu dois obtenir un token JWT.**

### Méthode 1 : Via l'inscription (Register)

1. **Ouvre Swagger** : `http://localhost:3000/api`

2. **Trouve l'endpoint `POST /auth/register`** dans la section "Auth"

3. **Clique sur "Try it out"**

4. **Remplis le formulaire** avec les données d'un utilisateur :

   ```json
   {
     "nom": "Dupont",
     "prenom": "Jean",
     "email": "jean.dupont@test.com",
     "motDePasse": "password123",
     "role": "parent"
   }
   ```

5. **Clique sur "Execute"**

6. **Copie le `access_token`** de la réponse :

   ```json
   {
     "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "user": {
       "id": "507f1f77bcf86cd799439011",
       "email": "jean.dupont@test.com",
       "nom": "Dupont",
       "prenom": "Jean",
       "role": "parent"
     }
   }
   ```

   **💾 IMPORTANT : Copie TOUT le token (c'est une longue chaîne de caractères)**

### Méthode 2 : Via la connexion (Login)

Si tu as déjà créé un utilisateur :

1. **Trouve l'endpoint `POST /auth/login`** dans la section "Auth"

2. **Clique sur "Try it out"**

3. **Remplis le formulaire** :

   ```json
   {
     "email": "jean.dupont@test.com",
     "motDePasse": "password123"
   }
   ```

4. **Clique sur "Execute"**

5. **Copie le `access_token`** de la réponse

---

## 🔑 Étape 2 : S'authentifier dans Swagger

**Maintenant que tu as un token, tu dois l'utiliser dans Swagger.**

### Instructions détaillées :

1. **Clique sur le bouton "Authorize" 🔒** 
   - Il est en haut à droite de la page Swagger
   - Il peut aussi être étiqueté "Available authorizations"

2. **Une fenêtre modale s'ouvre** avec :
   - Un champ "Value" ou "JWT-auth
   - Un bouton "Authorize"
   - Un bouton "Close"

3. **Dans le champ "Value"** :
   - Colle ton token JWT que tu as copié
   - **⚠️ IMPORTANT : Ne mets PAS "Bearer" devant le token**
   - Swagger ajoute automatiquement "Bearer " devant
   - Juste colle le token tel quel : `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...`

4. **Clique sur "Authorize"** (bouton vert)

5. **Clique sur "Close"** pour fermer la fenêtre

6. **✅ Vérification :** 
   - Tu devrais voir un cadenas 🔒 fermé à côté du bouton "Authorize"
   - Cela signifie que tu es authentifié

### Exemple visuel :

```
┌─────────────────────────────────────┐
│ Swagger UI                          │
│                                     │
│  [🔒 Authorize] ← Clique ici       │
│                                     │
│  ┌───────────────────────────────┐ │
│  │ JWT-auth                      │ │
│  │ Value: [colle ton token ici]  │ │
│  │                               │ │
│  │  [Authorize]  [Close]         │ │
│  └───────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## 🧪 Étape 3 : Tester les endpoints protégés

**Maintenant que tu es authentifié, tu peux tester les endpoints qui nécessitent un token.**

### Test 1 : Récupérer un utilisateur par ID

**Cet endpoint nécessite une authentification mais pas de rôle spécifique.**

1. **Trouve l'endpoint `GET /users/{id}`** dans la section "Users"

2. **Clique sur "Try it out"**

3. **Remplis le paramètre `id`** :
   - Utilise l'ID d'un utilisateur (ex: `507f1f77bcf86cd799439011`)
   - Ou utilise ton propre ID (celui que tu as reçu lors de l'inscription)

4. **Clique sur "Execute"**

5. **Résultat attendu :**
   - ✅ **Si ça marche (200 OK) :** Tu verras les informations de l'utilisateur
   - ❌ **Si ça ne marche pas (401 Unauthorized) :** Vérifie que tu es bien authentifié

### Test 2 : Mettre à jour un utilisateur

1. **Trouve l'endpoint `PATCH /users/{id}`**

2. **Clique sur "Try it out"**

3. **Remplis les paramètres** :
   - `id` : ID de l'utilisateur
   - `body` : Données à mettre à jour
     ```json
     {
       "nom": "Martin",
       "prenom": "Sophie"
     }
     ```

4. **Clique sur "Execute"**

5. **Résultat attendu :** L'utilisateur devrait être mis à jour

### Test 3 : Uploader une photo de profil

1. **Trouve l'endpoint `POST /users/{id}/upload-photo`**

2. **Clique sur "Try it out"**

3. **Remplis les paramètres** :
   - `id` : ID de l'utilisateur
   - `photo` : Clique sur "Choose File" et sélectionne une image (JPG, PNG, GIF)

4. **Clique sur "Execute"**

5. **Résultat attendu :** La photo devrait être uploadée

---

## 👥 Étape 4 : Tester les endpoints avec rôles spécifiques

**Certains endpoints nécessitent un rôle spécifique. Voici les rôles disponibles :**

- `parent` - Parent d'enfant(s)
- `enfant` - Enfant
- `coach` - Coach sportif
- `academie` - Académie (accès administrateur)

### Test 1 : Endpoints réservés aux PARENTS

**Ces endpoints nécessitent un token avec le rôle `parent` :**

#### A. Lier un enfant à un parent

1. **Crée un parent** (si tu ne l'as pas déjà fait) :
   ```json
   {
     "nom": "Dupont",
     "prenom": "Pierre",
     "email": "pierre.dupont@test.com",
     "motDePasse": "password123",
     "role": "parent"
   }
   ```
   - Copie le `access_token` du parent
   - Copie l'`id` du parent (dans `user.id`)

2. **Crée un enfant** :
   ```json
   {
     "nom": "Dupont",
     "prenom": "Lucas",
     "email": "lucas.dupont@test.com",
     "motDePasse": "password123",
     "role": "enfant"
   }
   ```
   - Copie l'`id` de l'enfant (dans `user.id`)

3. **Authentifie-toi avec le token du parent** :
   - Clique sur "Authorize"
   - Colle le token du parent
   - Clique "Authorize"

4. **Trouve l'endpoint `POST /users/{id}/link-child`**

5. **Remplis les paramètres** :
   - `id` : ID du parent (ex: `507f1f77bcf86cd799439011`)
   - `body` :
     ```json
     {
       "childId": "ID_DE_L_ENFANT"
     }
     ```

6. **Clique sur "Execute"**

7. **Résultat attendu :**
   - ✅ **Si ça marche (200 OK) :** L'enfant est lié au parent
   - ❌ **Si ça ne marche pas (403 Forbidden) :** Tu n'as pas le bon rôle (doit être `parent`)

#### B. Récupérer les enfants d'un parent

1. **Assure-toi d'être authentifié avec un token de parent**

2. **Trouve l'endpoint `GET /users/{id}/children`**

3. **Remplis le paramètre `id`** : ID du parent

4. **Clique sur "Execute"**

5. **Résultat attendu :** Liste des enfants liés au parent

### Test 2 : Endpoints réservés aux ACADÉMIES

**Ces endpoints nécessitent un token avec le rôle `academie` :**

#### A. Récupérer tous les utilisateurs

1. **Crée un utilisateur avec le rôle `academie`** :
   ```json
   {
     "nom": "Académie",
     "prenom": "Admin",
     "email": "admin@academie.com",
     "motDePasse": "password123",
     "role": "academie"
   }
   ```
   - Copie le `access_token` de l'académie

2. **Authentifie-toi avec le token de l'académie**

3. **Trouve l'endpoint `GET /users`**

4. **Clique sur "Try it out"**

5. **Optionnel :** Tu peux filtrer par rôle avec le paramètre `role` :
   - `role` : `parent`, `enfant`, `coach`, ou `academie`

6. **Clique sur "Execute"**

7. **Résultat attendu :**
   - ✅ **Si ça marche (200 OK) :** Tu verras la liste de tous les utilisateurs
   - ❌ **Si ça ne marche pas (403 Forbidden) :** Tu n'as pas le bon rôle (doit être `academie`)

#### B. Créer un utilisateur (réservé à l'académie)

1. **Assure-toi d'être authentifié avec un token d'académie**

2. **Trouve l'endpoint `POST /users`**

3. **Clique sur "Try it out"**

4. **Remplis le body** :
   ```json
   {
     "nom": "Nouveau",
     "prenom": "Utilisateur",
     "email": "nouveau@test.com",
     "motDePasse": "password123",
     "role": "coach"
   }
   ```

5. **Clique sur "Execute"**

6. **Résultat attendu :** Nouvel utilisateur créé

#### C. Supprimer un utilisateur (réservé à l'académie)

1. **Assure-toi d'être authentifié avec un token d'académie**

2. **Trouve l'endpoint `DELETE /users/{id}`**

3. **Remplis le paramètre `id`** : ID de l'utilisateur à supprimer

4. **Clique sur "Execute"**

5. **Résultat attendu :** Utilisateur supprimé

---

## 🎯 Scénarios de test complets

### Scénario 1 : Créer un parent et lier un enfant

**Objectif :** Tester le workflow complet parent-enfant

1. **Créer un parent** (`POST /auth/register`)
   ```json
   {
     "nom": "Martin",
     "prenom": "Sophie",
     "email": "sophie.martin@test.com",
     "motDePasse": "password123",
     "role": "parent"
   }
   ```
   - Copie : `access_token` et `user.id`

2. **Créer un enfant** (`POST /auth/register`)
   ```json
   {
     "nom": "Martin",
     "prenom": "Emma",
     "email": "emma.martin@test.com",
     "motDePasse": "password123",
     "role": "enfant"
   }
   ```
   - Copie : `user.id` de l'enfant

3. **S'authentifier avec le token du parent** (bouton "Authorize")

4. **Lier l'enfant au parent** (`POST /users/{parentId}/link-child`)
   - `id` : ID du parent
   - `body` : `{ "childId": "ID_DE_L_ENFANT" }`

5. **Vérifier les enfants** (`GET /users/{parentId}/children`)
   - Tu devrais voir l'enfant dans la liste

### Scénario 2 : Tester les permissions selon les rôles

**Objectif :** Vérifier que les rôles fonctionnent correctement

1. **Créer 4 utilisateurs avec différents rôles** :
   - Parent : `parent@test.com`
   - Enfant : `enfant@test.com`
   - Coach : `coach@test.com`
   - Académie : `academie@test.com`

2. **Tester avec le token du parent** :
   - ✅ `GET /users/{id}` → Devrait fonctionner
   - ✅ `POST /users/{id}/link-child` → Devrait fonctionner
   - ❌ `GET /users` → Devrait échouer (403 Forbidden)
   - ❌ `DELETE /users/{id}` → Devrait échouer (403 Forbidden)

3. **Tester avec le token de l'enfant** :
   - ✅ `GET /users/{id}` → Devrait fonctionner
   - ❌ `POST /users/{id}/link-child` → Devrait échouer (403 Forbidden)
   - ❌ `GET /users` → Devrait échouer (403 Forbidden)

4. **Tester avec le token de l'académie** :
   - ✅ `GET /users` → Devrait fonctionner
   - ✅ `POST /users` → Devrait fonctionner
   - ✅ `DELETE /users/{id}` → Devrait fonctionner

### Scénario 3 : Tester sans authentification

**Objectif :** Vérifier que les endpoints protégés rejettent les requêtes non authentifiées

1. **Déconnecte-toi dans Swagger** :
   - Clique sur "Authorize"
   - Clique sur "Logout" pour tous les schémas

2. **Essaie d'accéder à un endpoint protégé** :
   - `GET /users/{id}` → Devrait échouer (401 Unauthorized)
   - `GET /users` → Devrait échouer (401 Unauthorized)

3. **Les endpoints publics devraient toujours fonctionner** :
   - ✅ `POST /auth/register` → Devrait fonctionner
   - ✅ `POST /auth/login` → Devrait fonctionner

---

## ❌ Résolution de problèmes

### Problème : "401 Unauthorized"

**Causes possibles :**

1. **Tu n'es pas authentifié**
   - Solution : Clique sur "Authorize" et colle ton token

2. **Le token est invalide ou mal collé**
   - Solution : Vérifie que tu as bien collé tout le token (sans "Bearer")
   - Solution : Génère un nouveau token avec `/auth/login`

3. **Le token a expiré**
   - Solution : Génère un nouveau token avec `/auth/login`
   - Les tokens expirent après 1 jour (selon `JWT_EXPIRES_IN`)

### Problème : "403 Forbidden - Accès refusé : rôle insuffisant"

**Causes possibles :**

1. **Tu n'as pas le bon rôle**
   - Solution : Utilise un token d'un utilisateur avec le bon rôle
   - Exemple : Pour `GET /users`, tu dois être `academie`

2. **Tu utilises le mauvais token**
   - Solution : Vérifie que tu utilises bien le token de l'utilisateur avec le bon rôle
   - Solution : Ré-authentifie-toi avec le bon token

### Problème : Le bouton "Authorize" ne fonctionne pas

**Causes possibles :**

1. **Swagger n'est pas correctement configuré**
   - Solution : Vérifie que l'application est bien lancée (`npm run start:dev`)
   - Solution : Rafraîchis la page Swagger

2. **Le token est trop long et mal collé**
   - Solution : Copie-colle le token en entier (souvent très long)
   - Solution : Vérifie qu'il n'y a pas d'espaces avant/après

### Problème : Je ne vois pas le cadenas 🔒 après authentification

**Causes possibles :**

1. **L'authentification n'a pas fonctionné**
   - Solution : Réessaie de cliquer sur "Authorize" puis "Authorize" dans la fenêtre
   - Solution : Vérifie que le token est valide

2. **Cache du navigateur**
   - Solution : Rafraîchis la page (F5)
   - Solution : Vide le cache du navigateur

### Problème : "Cannot read property 'authorization' of undefined"

**Causes possibles :**

1. **Le format du token est incorrect**
   - Solution : Assure-toi de ne pas mettre "Bearer" devant le token dans Swagger
   - Swagger ajoute automatiquement "Bearer " devant

---

## ✅ Checklist de test des autorisations

Pour vérifier que tout fonctionne bien :

- [ ] Je peux obtenir un token via `POST /auth/register`
- [ ] Je peux obtenir un token via `POST /auth/login`
- [ ] Je peux m'authentifier dans Swagger avec le bouton "Authorize"
- [ ] Je vois le cadenas 🔒 après authentification
- [ ] Je peux accéder à `GET /users/{id}` avec authentification
- [ ] Je reçois "401 Unauthorized" si je n'ai pas de token
- [ ] Je peux accéder à `POST /users/{id}/link-child` avec un token de parent
- [ ] Je reçois "403 Forbidden" si je n'ai pas le bon rôle
- [ ] Je peux accéder à `GET /users` avec un token d'académie
- [ ] Je peux me déconnecter et reconnecter avec différents tokens

---

## 🎓 Points importants à retenir

1. **Token JWT = Carte d'identité numérique**
   - Il prouve que tu es connecté
   - Il contient ton rôle (parent, enfant, coach, académie)

2. **"Authorize" dans Swagger = Authentification**
   - C'est comme se connecter à un site web
   - Tu dois le faire avant de tester les endpoints protégés

3. **Les rôles déterminent les permissions**
   - Chaque endpoint peut nécessiter un rôle spécifique
   - Un parent ne peut pas faire ce qu'une académie peut faire

4. **Les tokens expirent**
   - Généralement après 1 jour
   - Si tu reçois "401 Unauthorized", génère un nouveau token

5. **Toujours tester sans authentification**
   - Vérifie que les endpoints protégés rejettent bien les requêtes non authentifiées
   - C'est une bonne pratique de sécurité

---

## 📚 Référence rapide : Endpoints par rôle

### Endpoints publics (pas besoin de token)
- `POST /auth/register`
- `POST /auth/login`

### Endpoints authentifiés (besoin d'un token, n'importe quel rôle)
- `GET /users/{id}`
- `PATCH /users/{id}`
- `POST /users/{id}/upload-photo`

### Endpoints réservés aux PARENTS
- `POST /users/{id}/link-child`
- `GET /users/{id}/children`

### Endpoints réservés aux ACADÉMIES
- `GET /users`
- `POST /users`
- `DELETE /users/{id}`

---

**🎉 Félicitations ! Tu sais maintenant comment tester toutes les parties avec Available Authorizations !**

Pour plus d'informations, consulte :
- [GUIDE_TESTING_SIMPLE.md](./GUIDE_TESTING_SIMPLE.md) - Guide de base
- [GUIDE_TEST.md](./GUIDE_TEST.md) - Guide complet de test

