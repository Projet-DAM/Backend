# 🧪 Guide Simple : Tester avec Swagger et Vérifier dans MongoDB

## 📋 Table des matières
1. [Qu'est-ce que Swagger ?](#quest-ce-que-swagger)
2. [Étape 1 : Ouvrir Swagger](#étape-1--ouvrir-swagger)
3. [Étape 2 : Créer un utilisateur (Test)](#étape-2--créer-un-utilisateur-test)
4. [Étape 3 : Vérifier dans MongoDB](#étape-3--vérifier-dans-mongodb)
5. [Étape 4 : Tester d'autres endpoints](#étape-4--tester-dautres-endpoints)
6. [Résolution de problèmes](#résolution-de-problèmes)

---

## 🎯 Qu'est-ce que Swagger ?

**Swagger** est une interface web qui te permet de :
- ✅ Voir tous les endpoints de ton API
- ✅ Tester tes endpoints directement depuis le navigateur
- ✅ Voir les exemples de requêtes
- ✅ Voir les réponses attendues

**C'est comme Postman, mais intégré à ton application !**

---

## 🚀 Étape 1 : Ouvrir Swagger

### Prérequis :
1. ✅ L'application doit être lancée (`npm run start:dev`)
2. ✅ MongoDB doit être démarré
3. ✅ Pas d'erreur dans la console

### Ouvrir Swagger :

1. **Lance ton application :**
   ```bash
   npm run start:dev
   ```

2. **Ouvre ton navigateur et va sur :**
   ```
   http://localhost:3000/api
   ```

3. **Tu devrais voir une page avec :**
   - Une liste d'endpoints (Auth, Users, etc.)
   - Des boutons "Try it out" à côté de chaque endpoint
   - Un bouton "Authorize" en haut à droite

**🎉 Félicitations ! Swagger est ouvert !**

---

## 📝 Étape 2 : Créer un utilisateur (Test)

### Test 1 : Inscription (Register)

**But :** Créer un nouvel utilisateur dans la base de données

**Étapes :**

1. **Trouve l'endpoint `POST /auth/register`**
   - Il devrait être dans la section "Auth"

2. **Clique sur "Try it out"**
   - Le bouton est à droite de l'endpoint

3. **Remplis le formulaire avec ces données :**
   ```json
   {
     "nom": "Martin",
     "prenom": "Sophie",
     "email": "sophie.martin@test.com",
     "motDePasse": "motdepasse123",
     "role": "parent"
   }
   ```

4. **Clique sur "Execute"** (bouton bleu en bas)

5. **Regarde la réponse :**
   - **Si ça marche (201 Created) :** Tu verras un `access_token` et les infos de l'utilisateur
   - **Si ça ne marche pas :** Tu verras un message d'erreur (ex: "Cet email est déjà utilisé")

**✅ Succès si tu vois :**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "65abc123def456...",
    "email": "sophie.martin@test.com",
    "nom": "Martin",
    "prenom": "Sophie",
    "role": "parent"
  }
}
```

**💾 IMPORTANT : Copie le `access_token` ! Tu en auras besoin pour les autres tests.**

---

### Test 2 : Connexion (Login)

**But :** Vérifier que la connexion fonctionne

1. **Trouve `POST /auth/login`**
2. **Clique sur "Try it out"**
3. **Utilise les mêmes identifiants :**
   ```json
   {
     "email": "sophie.martin@test.com",
     "motDePasse": "motdepasse123"
   }
   ```
4. **Clique sur "Execute"**
5. **Tu devrais recevoir un nouveau token**

**✅ Si tu reçois un token, ça fonctionne !**

---

## 🔍 Étape 3 : Vérifier dans MongoDB

**Maintenant, vérifions que les données sont bien dans la base de données !**

### Option 1 : Avec MongoDB Compass (Recommandé - Interface graphique)

1. **Ouvre MongoDB Compass**
   - Si tu ne l'as pas, télécharge-le : https://www.mongodb.com/products/compass

2. **Connecte-toi à ta base :**
   - **Connection String :** `mongodb://localhost:27017`
   - Clique sur "Connect"

3. **Trouve ta base de données :**
   - Regarde dans ton fichier `.env` : `MONGO_URI=mongodb://localhost:27017/sportyconnect`
   - Le nom de la base est **`sportyconnect`**
   - Clique dessus dans la liste à gauche

4. **Trouve la collection `users` :**
   - Dans la base `sportyconnect`, clique sur la collection **`users`**

5. **Regarde les documents :**
   - Tu devrais voir ton utilisateur créé !
   - Tu verras : `nom`, `prenom`, `email`, `role`, etc.
   - Le `motDePasse` sera hashé (c'est normal pour la sécurité)

**📊 Exemple de ce que tu devrais voir :**
```json
{
  "_id": ObjectId("65abc123def456..."),
  "nom": "Martin",
  "prenom": "Sophie",
  "email": "sophie.martin@test.com",
  "motDePasse": "$2b$10$abcd1234...", // Hashé (c'est normal !)
  "role": "parent",
  "createdAt": ISODate("2024-01-15T10:30:00.000Z"),
  "updatedAt": ISODate("2024-01-15T10:30:00.000Z")
}
```

**✅ Si tu vois ton utilisateur, c'est que tout fonctionne !**

---

### Option 2 : Avec MongoDB Shell (ligne de commande)

1. **Ouvre un terminal PowerShell**

2. **Connecte-toi à MongoDB :**
   ```bash
   mongosh
   ```
   (Ou `mongo` si tu as une ancienne version)

3. **Sélectionne ta base :**
   ```bash
   use sportyconnect
   ```

4. **Affiche tous les utilisateurs :**
   ```bash
   db.users.find().pretty()
   ```

5. **Ou cherche un utilisateur spécifique :**
   ```bash
   db.users.findOne({ email: "sophie.martin@test.com" })
   ```

**✅ Tu devrais voir ton utilisateur dans la console !**

---

## 🔐 Étape 4 : Tester d'autres endpoints

### Tester les endpoints protégés (qui nécessitent un token)

**Les endpoints protégés sont ceux qui nécessitent que tu sois connecté.**

1. **Autorise-toi dans Swagger :**
   - Clique sur le bouton **"Authorize"** (en haut à droite)
   - Dans le champ "Value", colle ton `access_token` (celui que tu as copié plus tôt)
   - **Important :** Ne mets PAS "Bearer" devant, juste le token
   - Clique sur "Authorize" puis "Close"

2. **Teste un endpoint protégé :**
   - Trouve `GET /users` ou `GET /users/{id}`
   - Clique sur "Try it out"
   - Clique sur "Execute"
   - **Si ça marche :** Tu verras la liste des utilisateurs
   - **Si ça ne marche pas :** Tu verras "Unauthorized" (token invalide ou expiré)

---

## 🎯 Test complet : Scénario complet

Voici un scénario complet pour tester tout le système :

### 1. Créer un parent
- **Endpoint :** `POST /auth/register`
- **Données :**
  ```json
  {
    "nom": "Dupont",
    "prenom": "Pierre",
    "email": "pierre.dupont@test.com",
    "motDePasse": "password123",
    "role": "parent"
  }
  ```
- **Copie le `access_token` du parent**

### 2. Créer un enfant
- **Endpoint :** `POST /auth/register`
- **Données :**
  ```json
  {
    "nom": "Dupont",
    "prenom": "Lucas",
    "email": "lucas.dupont@test.com",
    "motDePasse": "password123",
    "role": "enfant"
  }
  ```
- **Copie l'`id` de l'enfant (dans la réponse `user.id`)**

### 3. Autorise-toi avec le token du parent
- Clique sur "Authorize" dans Swagger
- Colle le token du parent
- Clique "Authorize"

### 4. Lier l'enfant au parent
- **Endpoint :** `POST /users/{parentId}/link-child`
- **Remplace `{parentId}` par l'ID du parent**
- **Body :**
  ```json
  {
    "childId": "ID_DE_L_ENFANT"
  }
  ```

### 5. Vérifier dans MongoDB Compass
- Ouvre MongoDB Compass
- Va dans `sportyconnect` > `users`
- Trouve le parent : Tu devrais voir un champ `enfants` avec l'ID de l'enfant
- Trouve l'enfant : Tu devrais voir un champ `parent` avec l'ID du parent

**✅ Si tu vois ça, tout fonctionne parfaitement !**

---

## ❌ Résolution de problèmes

### Problème : "Cet email est déjà utilisé"
**Solution :** Utilise un email différent ou supprime l'utilisateur dans MongoDB Compass

### Problème : "Unauthorized" ou "Token invalide"
**Solutions :**
- Vérifie que tu as bien collé le token dans "Authorize"
- Vérifie que le token n'a pas expiré (crée un nouveau token avec `/auth/login`)
- Vérifie que tu n'as pas mis "Bearer" devant le token dans Swagger

### Problème : "Cannot GET /api"
**Solution :** Vérifie que l'application est bien lancée (`npm run start:dev`)

### Problème : "Connection refused" dans MongoDB Compass
**Solutions :**
- Vérifie que MongoDB est démarré
- Vérifie la connection string dans `.env`
- Vérifie que MongoDB écoute sur le port 27017

### Problème : Je ne vois pas mes données dans MongoDB
**Solutions :**
- Vérifie que tu es dans la bonne base (`sportyconnect`)
- Vérifie que tu es dans la bonne collection (`users`)
- Rafraîchis la page dans MongoDB Compass (bouton Refresh)

---

## ✅ Checklist de test

Pour vérifier que tout fonctionne bien :

- [ ] Swagger s'ouvre sur `http://localhost:3000/api`
- [ ] Je peux créer un utilisateur avec `POST /auth/register`
- [ ] Je reçois un `access_token` après l'inscription
- [ ] Je peux me connecter avec `POST /auth/login`
- [ ] Je vois mon utilisateur dans MongoDB Compass
- [ ] Je peux m'autoriser dans Swagger avec le token
- [ ] Je peux accéder aux endpoints protégés (`GET /users`, etc.)
- [ ] Les données sont bien sauvegardées dans MongoDB

---

## 🎓 Points importants à retenir

1. **Swagger = Interface de test** : Tu peux tester ton API sans Postman

2. **Token JWT = Carte d'identité** : Il prouve que tu es connecté

3. **MongoDB = Base de données** : C'est là que toutes les données sont stockées

4. **Tester = Vérifier** : Toujours vérifier dans MongoDB que les données sont bien sauvegardées

5. **Les mots de passe sont hashés** : C'est normal de voir un hash dans MongoDB, pas le vrai mot de passe

---

**🎉 Félicitations ! Tu sais maintenant comment tester avec Swagger et vérifier dans MongoDB !**

Pour plus d'informations, consulte le [GUIDE_TEST.md](./GUIDE_TEST.md) pour des scénarios plus avancés.
