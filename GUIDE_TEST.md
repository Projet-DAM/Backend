# 🧪 Guide de Test - SportyConnect Kids API

Ce guide vous explique comment tester l'API étape par étape.

## 📋 Prérequis

1. **Node.js** installé (v18 ou supérieur)
2. **MongoDB** installé et en cours d'exécution
   - Option 1 : MongoDB Compass (interface graphique)
   - Option 2 : MongoDB en ligne de commande
3. **Postman** ou **curl** pour tester les endpoints (optionnel, Swagger est disponible)

---

## 🚀 Étape 1 : Installation

```bash
# Installer toutes les dépendances
npm install
```

---

## ⚙️ Étape 2 : Configuration

### Créer le fichier `.env`

À la racine du projet, créez un fichier `.env` avec le contenu suivant :

```env
PORT=3000
MONGO_URI=mongodb://localhost:27017/sportyconnect
JWT_SECRET=secretKey123
JWT_EXPIRES_IN=1d
```

### Vérifier MongoDB

Assurez-vous que MongoDB est en cours d'exécution :

```bash
# Vérifier que MongoDB tourne
# Windows PowerShell
Get-Service -Name MongoDB

# Ou ouvrir MongoDB Compass et vérifier la connexion
```

---

## 🏃 Étape 3 : Démarrer l'application

```bash
# Mode développement (avec hot-reload)
npm run start:dev
```

Vous devriez voir :
```
Application is running on: http://localhost:3000
Swagger documentation: http://localhost:3000/api
```

---

## 🧪 Étape 4 : Tester avec Swagger (Recommandé)

### Accéder à Swagger

Ouvrez votre navigateur et allez sur : **http://localhost:3000/api**

### Test 1 : Inscription (Register)

1. Dans Swagger, trouvez l'endpoint `POST /auth/register`
2. Cliquez sur "Try it out"
3. Utilisez cet exemple :

```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "motDePasse": "password123",
  "role": "parent"
}
```

4. Cliquez sur "Execute"
5. **Copiez le `access_token`** de la réponse - vous en aurez besoin !

**Réponse attendue :**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "...",
    "email": "jean.dupont@example.com",
    "nom": "Dupont",
    "prenom": "Jean",
    "role": "parent"
  }
}
```

### Test 2 : Connexion (Login)

1. Trouvez `POST /auth/login`
2. Utilisez les mêmes identifiants :

```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "password123"
}
```

3. Vous recevrez un nouveau token

### Test 3 : Authentification dans Swagger

Pour tester les endpoints protégés :

1. Cliquez sur le bouton **"Authorize"** en haut de la page Swagger
2. Dans le champ "Value", collez votre `access_token` (sans "Bearer ")
3. Cliquez sur "Authorize" puis "Close"

Maintenant, tous les appels aux endpoints protégés utiliseront ce token !

### Test 4 : Créer un utilisateur (Académie uniquement)

1. Créez d'abord un utilisateur avec le rôle `academie` via `/auth/register`
2. Connectez-vous avec cet utilisateur et récupérez le token
3. Autorisez-vous avec ce token dans Swagger
4. Testez `POST /users` pour créer un nouvel utilisateur

### Test 5 : Récupérer les utilisateurs

- `GET /users` - Liste tous les utilisateurs (Académie uniquement)
- `GET /users/:id` - Récupère un utilisateur par ID

### Test 6 : Lier un parent et un enfant

1. Créez un **parent** via `/auth/register` avec `role: "parent"`
2. Créez un **enfant** via `/auth/register` avec `role: "enfant"`
3. Notez les IDs des deux utilisateurs
4. Connectez-vous avec le compte **parent**
5. Utilisez `POST /users/{parentId}/link-child` avec :
```json
{
  "childId": "ID_DE_L_ENFANT"
}
```
6. Testez `GET /users/{parentId}/children` pour voir les enfants liés

---

## 🧪 Étape 5 : Tester avec curl (Alternative)

### Inscription

```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean@example.com",
    "motDePasse": "password123",
    "role": "parent"
  }'
```

### Connexion

```bash
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "jean@example.com",
    "motDePasse": "password123"
  }'
```

### Endpoint protégé (avec token)

```bash
# Remplacez YOUR_TOKEN par le token reçu
curl -X GET http://localhost:3000/users/YOUR_USER_ID \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 🧪 Étape 6 : Tester avec Postman

### Configuration

1. Créez une nouvelle collection "SportyConnect Kids"
2. Ajoutez une variable de collection : `baseUrl = http://localhost:3000`
3. Ajoutez une variable de collection : `token` (vide pour l'instant)

### Requêtes à créer

#### 1. Register
- **Method** : POST
- **URL** : `{{baseUrl}}/auth/register`
- **Body** (raw JSON) :
```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean@example.com",
  "motDePasse": "password123",
  "role": "parent"
}
```
- **Tests** (dans l'onglet Tests) :
```javascript
if (pm.response.code === 201) {
    const jsonData = pm.response.json();
    pm.collectionVariables.set("token", jsonData.access_token);
}
```

#### 2. Login
- **Method** : POST
- **URL** : `{{baseUrl}}/auth/login`
- **Body** :
```json
{
  "email": "jean@example.com",
  "motDePasse": "password123"
}
```

#### 3. Get User (avec authentification)
- **Method** : GET
- **URL** : `{{baseUrl}}/users/YOUR_USER_ID`
- **Headers** :
  - Key: `Authorization`
  - Value: `Bearer {{token}}`

---

## 📝 Scénarios de test complets

### Scénario 1 : Workflow complet Parent-Enfant

1. ✅ Créer un parent
2. ✅ Créer un enfant
3. ✅ Se connecter en tant que parent
4. ✅ Lier l'enfant au parent
5. ✅ Récupérer la liste des enfants du parent

### Scénario 2 : Gestion des rôles

1. ✅ Créer un utilisateur avec rôle `academie`
2. ✅ Se connecter avec ce compte
3. ✅ Créer d'autres utilisateurs (endpoint `/users`)
4. ✅ Lister tous les utilisateurs
5. ✅ Essayer d'accéder à `/users` avec un compte `parent` → doit échouer (403)

### Scénario 3 : Upload de photo

1. ✅ Se connecter avec un compte
2. ✅ Uploader une photo via `POST /users/:id/upload-photo`
   - Utiliser form-data avec champ `photo`
3. ✅ Vérifier que `photoProfil` est mis à jour

---

## 🔍 Vérification dans MongoDB Compass

1. Ouvrez MongoDB Compass
2. Connectez-vous à `mongodb://localhost:27017`
3. Sélectionnez la base de données `sportyconnect`
4. Vérifiez la collection `users` :
   - Les mots de passe doivent être hashés
   - Les relations parent-enfant doivent être présentes
   - Les timestamps `createdAt` et `updatedAt` doivent être présents

---

## ❌ Tests d'erreurs (à vérifier)

### Test 1 : Email déjà utilisé
```json
POST /auth/register
{
  "email": "jean@example.com",  // Email déjà existant
  ...
}
```
**Attendu** : Erreur 409 (Conflict)

### Test 2 : Token invalide
```bash
GET /users/:id
Authorization: Bearer invalid_token
```
**Attendu** : Erreur 401 (Unauthorized)

### Test 3 : Rôle insuffisant
- Se connecter avec un compte `parent`
- Essayer d'accéder à `GET /users` (réservé à `academie`)
**Attendu** : Erreur 403 (Forbidden)

### Test 4 : Validation des données
```json
POST /auth/register
{
  "email": "email-invalide",  // Email invalide
  "motDePasse": "123"  // Trop court
}
```
**Attendu** : Erreur 400 (Bad Request) avec détails de validation

---

## 🐛 Dépannage

### Erreur : "Cannot connect to MongoDB"
- Vérifiez que MongoDB est démarré
- Vérifiez l'URI dans `.env`
- Testez la connexion dans MongoDB Compass

### Erreur : "JWT_SECRET is not defined"
- Vérifiez que le fichier `.env` existe
- Vérifiez que les variables sont correctement définies

### Erreur : "Port 3000 already in use"
- Changez le port dans `.env` : `PORT=3001`
- Ou arrêtez le processus utilisant le port 3000

### Erreur : "Validation failed"
- Vérifiez que tous les champs requis sont présents
- Vérifiez les formats (email, longueur du mot de passe, etc.)

---

## ✅ Checklist de test

- [ ] Application démarre sans erreur
- [ ] Swagger accessible sur `/api`
- [ ] Inscription fonctionne
- [ ] Connexion fonctionne
- [ ] Token JWT est valide
- [ ] Endpoints protégés fonctionnent avec token
- [ ] Endpoints protégés refusent sans token
- [ ] Contrôle des rôles fonctionne
- [ ] Liaison parent-enfant fonctionne
- [ ] Upload de photo fonctionne
- [ ] Validation des données fonctionne
- [ ] Erreurs sont correctement gérées

---

## 🎯 Prochaines étapes

Une fois les tests de base réussis, vous pouvez :
1. Créer des tests automatisés (Jest)
2. Ajouter plus de validation
3. Implémenter la pagination pour les listes
4. Ajouter des filtres avancés
5. Implémenter la gestion des permissions plus fine

---

**Bon test ! 🚀**

