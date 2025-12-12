# SportyConnect Kids - Backend API

Backend NestJS pour la gestion des utilisateurs et authentification JWT du projet SportyConnect Kids. Une API REST complète avec système d'authentification sécurisé, gestion des rôles (RBAC), et documentation Swagger interactive.

## 📋 Table des matières

- [Vue d'ensemble](#vue-densemble)
- [Technologies utilisées](#technologies-utilisées)
- [Architecture du projet](#architecture-du-projet)
- [Installation](#installation)
- [Configuration](#configuration)
- [Démarrage](#démarrage)
- [Documentation API](#documentation-api)
- [Authentification & Autorisation](#authentification--autorisation)
- [Endpoints API](#endpoints-api)
- [Modèle de données](#modèle-de-données)
- [Sécurité](#sécurité)
- [Tests](#tests)
- [Structure du projet](#structure-du-projet)

---

## 🎯 Vue d'ensemble

**SportyConnect Kids** est une API backend construite avec NestJS qui gère :

- **Authentification JWT** : Inscription et connexion sécurisées
- **Gestion des utilisateurs** : CRUD complet avec validation des données
- **Système de rôles (RBAC)** : 4 types d'utilisateurs avec permissions différentes
- **Relations parent-enfant** : Liaison et gestion des familles
- **Upload de fichiers** : Photos de profil
- **Documentation Swagger** : Interface interactive pour tester l'API

### Types d'utilisateurs

1. **PARENT** 👨‍👩‍👧 - Parents d'enfant(s)
2. **ENFANT** 👶 - Enfants
3. **COACH** 🏃 - Coachs sportifs
4. **ACADEMIE** 🏛️ - Académies (accès administrateur)

---

## 🚀 Technologies utilisées

| Technologie | Version | Description |
|------------|---------|-------------|
| **NestJS** | ^11.0.1 | Framework Node.js progressif |
| **TypeScript** | ^5.7.3 | Langage de programmation |
| **MongoDB** | ^8.0.3 | Base de données NoSQL |
| **Mongoose** | ^11.0.3 | ODM pour MongoDB |
| **JWT** | ^11.0.1 | Authentification par tokens |
| **Passport** | ^11.0.5 | Middleware d'authentification |
| **Swagger** | ^11.2.1 | Documentation API interactive |
| **bcrypt** | ^5.1.1 | Hashage des mots de passe |
| **class-validator** | ^0.14.1 | Validation des DTOs |
| **multer** | ^2.0.0 | Upload de fichiers |
| **Config** | ^4.0.2 | Gestion des variables d'environnement |

---

## 🏗️ Architecture du projet

### Architecture générale

```
┌─────────────────────────────────────────┐
│         Client (Frontend/Postman)       │
└──────────────┬──────────────────────────┘
               │ HTTP/HTTPS
               ▼
┌─────────────────────────────────────────┐
│         NestJS Application              │
│  ┌────────────────────────────────────┐ │
│  │  Controllers (Routes)              │ │
│  │  - AuthController                  │ │
│  │  - UsersController                 │ │
│  └──────────────┬─────────────────────┘ │
│                 │                        │
│  ┌──────────────▼─────────────────────┐ │
│  │  Guards (JWT + Roles)              │ │
│  │  - JwtAuthGuard                    │ │
│  │  - Roles Decorator                 │ │
│  └──────────────┬─────────────────────┘ │
│                 │                        │
│  ┌──────────────▼─────────────────────┐ │
│  │  Services (Business Logic)         │ │
│  │  - AuthService                     │ │
│  │  - UsersService                    │ │
│  └──────────────┬─────────────────────┘ │
│                 │                        │
│  ┌──────────────▼─────────────────────┐ │
│  │  Database Layer (Mongoose)         │ │
│  └────────────────────────────────────┘ │
└──────────────┬───────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│         MongoDB Database                │
│  - Collection: users                    │
└─────────────────────────────────────────┘
```

### Flux d'authentification

```
1. Client → POST /auth/register
   ├─→ AuthService.register()
   ├─→ UsersService.create()
   ├─→ Hash password (bcrypt)
   ├─→ Save to MongoDB
   └─→ Generate JWT token

2. Client → POST /auth/login
   ├─→ AuthService.login()
   ├─→ Validate credentials
   ├─→ Compare password (bcrypt)
   └─→ Generate JWT token

3. Client → Protected Route (with JWT token)
   ├─→ JwtAuthGuard
   ├─→ JwtStrategy.validate()
   ├─→ Check user exists
   ├─→ Roles Decorator (if applicable)
   └─→ Execute Controller method
```

---

## 📦 Installation

### Prérequis

- **Node.js** >= 18.x
- **MongoDB** >= 6.x (local ou MongoDB Atlas)
- **npm** ou **yarn**

### Étapes d'installation

1. **Cloner le projet** (si nécessaire)
```bash
git clone <repository-url>
cd user
```

2. **Installer les dépendances**
```bash
npm install
```

3. **Configurer les variables d'environnement**

Créez un fichier `.env` à la racine du projet :

```env
# Port du serveur
PORT=3000

# Configuration MongoDB
MONGO_URI=mongodb://localhost:27017/sportyconnect

# Configuration JWT
JWT_SECRET=your-super-secret-key-change-in-production
JWT_EXPIRES_IN=1d
```




## ⚙️ Configuration

### Variables d'environnement

| Variable | Description | Exemple | Requis |
|----------|-------------|---------|--------|
| `PORT` | Port du serveur | `3000` | Non (défaut: 3000) |
| `MONGO_URI` | URI de connexion MongoDB | `mongodb://localhost:27017/sportyconnect` | Oui |
| `JWT_SECRET` | Clé secrète pour signer les tokens JWT | `your-secret-key` | Oui |
| `JWT_EXPIRES_IN` | Durée de validité du token | `1d`, `24h`, `3600s` | Non (défaut: 1d) |

### Configuration de la base de données

Le fichier `src/config/database.config.ts` configure la connexion MongoDB avec :
- Retry writes activés
- Write concern majoritaire
- URI depuis les variables d'environnement

---

## 🚀 Démarrage

### Mode développement

```bash
npm run start:dev
```

L'application sera accessible sur `http://localhost:3000` avec rechargement automatique lors des modifications.

### Mode production

```bash
# Compiler le projet
npm run build

# Lancer en production
npm run start:prod
```

### Mode debug

```bash
npm run start:debug
```

### Vérification

Une fois démarré, vous devriez voir :
```
Application is running on: http://localhost:3000
Swagger documentation: http://localhost:3000/api
```

---

## 📚 Documentation API

### Accès à Swagger

Une fois l'application démarrée, accédez à la documentation Swagger interactive :

**URL :** http://localhost:3000/api

### Fonctionnalités Swagger

- ✅ **Visualisation de tous les endpoints**
- ✅ **Test interactif des requêtes**
- ✅ **Authentification JWT intégrée**
- ✅ **Schémas de validation**
- ✅ **Exemples de requêtes/réponses**

### Guide de test rapide

> 📖 **Première fois avec NestJS et Swagger ?** 
> 
> 👉 Consultez le **[GUIDE_TESTING_SIMPLE.md](./GUIDE_TESTING_SIMPLE.md)** pour un guide étape par étape !

**Étapes rapides :**

1. Ouvrez http://localhost:3000/api dans votre navigateur
2. Testez `POST /auth/register` pour créer un utilisateur
3. Copiez le `access_token` reçu
4. Cliquez sur le bouton **"Authorize" 🔒** en haut de la page
5. Collez votre token (sans "Bearer") et cliquez "Authorize"
6. Testez maintenant tous les endpoints protégés !

### Scripts de test

**Windows (PowerShell) :**
```powershell
.\TEST_EXAMPLES.ps1
```

**Linux/Mac :**
```bash
chmod +x TEST_EXAMPLES.sh
./TEST_EXAMPLES.sh
```

---

## 🔐 Authentification & Autorisation

### Système d'authentification JWT

L'application utilise **JSON Web Tokens (JWT)** pour l'authentification :

1. **Inscription/Connexion** → Génère un token JWT
2. **Requêtes protégées** → Nécessitent le token dans le header `Authorization: Bearer <token>`
3. **Validation** → Le token est vérifié à chaque requête

### Structure du token JWT

Le payload contient :
```json
{
  "sub": "user_id",
  "email": "user@example.com",
  "role": "parent",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### Système de rôles (RBAC)

#### Rôles disponibles

| Rôle | Description | Permissions spéciales |
|------|-------------|----------------------|
| **PARENT** | Parent d'enfant(s) | Lier/gérer des enfants |
| **ENFANT** | Enfant | Accès limité |
| **COACH** | Coach sportif | Accès limité |
| **ACADEMIE** | Académie | Accès administrateur complet |

#### Matrice des permissions

| Endpoint | PARENT | ENFANT | COACH | ACADEMIE | Public |
|----------|:------:|:------:|:-----:|:--------:|:------:|
| `POST /auth/register` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `POST /auth/login` | ✅ | ✅ | ✅ | ✅ | ✅ |
| `GET /users` | ❌ | ❌ | ❌ | ✅ | ❌ |
| `POST /users` | ❌ | ❌ | ❌ | ✅ | ❌ |
| `GET /users/:id` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `PATCH /users/:id` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `DELETE /users/:id` | ❌ | ❌ | ❌ | ✅ | ❌ |
| `POST /users/:id/link-child` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `GET /users/:id/children` | ✅ | ❌ | ❌ | ❌ | ❌ |
| `POST /users/:id/upload-photo` | ✅ | ✅ | ✅ | ✅ | ❌ |

### Décorateurs de sécurité

#### `@Public()`
Marque un endpoint comme accessible sans authentification.

```typescript
@Public()
@Post('register')
register() { ... }
```

#### `@Roles()`
Restreint l'accès à certains rôles.

```typescript
@Roles(UserRole.ACADEMIE)
@Get()
findAll() { ... }
```

### Guard JWT global

Le `JwtAuthGuard` est appliqué globalement dans `main.ts` :
- Vérifie la présence et la validité du token JWT
- Vérifie les rôles requis (si `@Roles()` est présent)
- Permet les routes publiques (si `@Public()` est présent)

### Guide de test des autorisations

> 🔒 **Comment tester les autorisations ?**
> 
> 👉 Consultez le **[QUICK_REF_AUTHORIZATION.md](./QUICK_REF_AUTHORIZATION.md)** pour un guide rapide !
> 
> 👉 Consultez le **[GUIDE_TEST_AUTHORIZATION.md](./GUIDE_TEST_AUTHORIZATION.md)** pour un guide détaillé !

**Problème 401 Unauthorized ?**
👉 Consultez le **[DEBUG_AUTH.md](./DEBUG_AUTH.md)** pour le débogage !

---

## 🔌 Endpoints API

### Authentification (Public)

#### `POST /auth/register`
Inscription d'un nouvel utilisateur.

**Body :**
```json
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "motDePasse": "password123",
  "role": "parent"
}
```

**Réponse :**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "507f1f77bcf86cd799439011",
    "email": "jean.dupont@example.com",
    "nom": "Dupont",
    "prenom": "Jean",
    "role": "parent",
    "photoProfil": null
  }
}
```

#### `POST /auth/login`
Connexion d'un utilisateur existant.

**Body :**
```json
{
  "email": "jean.dupont@example.com",
  "motDePasse": "password123"
}
```

**Réponse :** Identique à `/auth/register`

---

### Gestion des utilisateurs

#### `GET /users` (Académie uniquement)
Récupère tous les utilisateurs. Peut filtrer par rôle.

**Headers :**
```
Authorization: Bearer <token>
```

**Query parameters :**
- `role` (optionnel) : Filtrer par rôle (`parent`, `enfant`, `coach`, `academie`)

**Exemple :**
```
GET /users?role=parent
```

#### `POST /users` (Académie uniquement)
Crée un nouvel utilisateur (alternative à `/auth/register`).

**Body :** Identique à `POST /auth/register`

#### `GET /users/:id` (Authentifié)
Récupère un utilisateur par ID.

**Response :**
```json
{
  "_id": "507f1f77bcf86cd799439011",
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean.dupont@example.com",
  "role": "parent",
  "enfants": ["507f1f77bcf86cd799439012"],
  "createdAt": "2024-01-01T00:00:00.000Z",
  "updatedAt": "2024-01-01T00:00:00.000Z"
}
```

#### `PATCH /users/:id` (Authentifié)
Met à jour un utilisateur.

**Body :** Champs à mettre à jour (tous optionnels)
```json
{
  "nom": "Nouveau nom",
  "prenom": "Nouveau prénom",
  "email": "nouveau@example.com"
}
```

#### `DELETE /users/:id` (Académie uniquement)
Supprime un utilisateur.

---

### Relations parent-enfant (Parent uniquement)

#### `POST /users/:id/link-child`
Lie un enfant à un parent.

**Body :**
```json
{
  "childId": "507f1f77bcf86cd799439012"
}
```

**Validation :**
- L'utilisateur `:id` doit être un parent
- L'utilisateur `childId` doit être un enfant
- L'enfant ne doit pas déjà avoir un parent

#### `GET /users/:id/children`
Récupère la liste des enfants d'un parent.

**Response :**
```json
[
  {
    "_id": "507f1f77bcf86cd799439012",
    "nom": "Dupont",
    "prenom": "Marie",
    "email": "marie@example.com",
    "role": "enfant",
    "parent": "507f1f77bcf86cd799439011"
  }
]
```

---

### Upload de fichiers

#### `POST /users/:id/upload-photo` (Authentifié)
Upload une photo de profil.

**Content-Type :** `multipart/form-data`

**Body :**
- `photo` : Fichier image (JPG, PNG, GIF, max 20MB)

**Validation :**
- Format : JPG, PNG, GIF
- Taille max : 20MB
- Fichier stocké dans `./uploads/`

**Response :**
```json
{
  "photoProfil": "/uploads/1762298603153-792188088.png"
}
```

---

## 📊 Modèle de données

### Schéma User (Mongoose)

```typescript
{
  _id: ObjectId,
  nom: string (requis),
  prenom: string (requis),
  email: string (requis, unique),
  motDePasse: string (requis, hashed),
  role: 'parent' | 'enfant' | 'coach' | 'academie' (requis),
  photoProfil?: string,
  
  // Relations
  enfants?: ObjectId[] (références vers User),
  parent?: ObjectId (référence vers User),
  
  // Attributs spécifiques au Coach
  certification?: string[],
  specialite?: string,
  experience?: number,
  
  // Attributs spécifiques à l'Enfant
  dateNaissance?: Date,
  
  // Attributs spécifiques à l'Académie
  nomAcademie?: string,
  adresse?: string,
  description?: string,
  horaires?: {
    [jour: string]: {
      debut: string,
      fin: string
    }
  },
  
  // Timestamps automatiques
  createdAt: Date,
  updatedAt: Date
}
```

### Contraintes par rôle

Le système valide automatiquement les attributs selon le rôle :

| Attribut | PARENT | ENFANT | COACH | ACADEMIE |
|----------|:------:|:------:|:-----:|:--------:|
| `enfants` | ✅ | ❌ | ❌ | ❌ |
| `parent` | ❌ | ✅ | ❌ | ❌ |
| `dateNaissance` | ❌ | ✅ | ❌ | ❌ |
| `certification` | ❌ | ❌ | ✅ | ❌ |
| `specialite` | ❌ | ❌ | ✅ | ❌ |
| `experience` | ❌ | ❌ | ✅ | ❌ |
| `nomAcademie` | ❌ | ❌ | ❌ | ✅ |
| `adresse` | ❌ | ❌ | ❌ | ✅ |
| `description` | ❌ | ❌ | ❌ | ✅ |
| `horaires` | ❌ | ❌ | ❌ | ✅ |

### Exemples de documents

#### Parent
```json
{
  "_id": "507f1f77bcf86cd799439011",
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean@example.com",
  "motDePasse": "$2b$10$...",
  "role": "parent",
  "enfants": ["507f1f77bcf86cd799439012"],
  "createdAt": "2024-01-01T00:00:00.000Z"
}
```

#### Enfant
```json
{
  "_id": "507f1f77bcf86cd799439012",
  "nom": "Dupont",
  "prenom": "Marie",
  "email": "marie@example.com",
  "motDePasse": "$2b$10$...",
  "role": "enfant",
  "dateNaissance": "2010-05-15T00:00:00.000Z",
  "parent": "507f1f77bcf86cd799439011",
  "createdAt": "2024-01-01T00:00:00.000Z"
}
```

#### Coach
```json
{
  "_id": "507f1f77bcf86cd799439013",
  "nom": "Martin",
  "prenom": "Pierre",
  "email": "pierre@example.com",
  "motDePasse": "$2b$10$...",
  "role": "coach",
  "certification": ["Certification FIFA"],
  "specialite": "Football",
  "experience": 5,
  "createdAt": "2024-01-01T00:00:00.000Z"
}
```

#### Académie
```json
{
  "_id": "507f1f77bcf86cd799439014",
  "nom": "Académie",
  "prenom": "Admin",
  "email": "admin@academie.com",
  "motDePasse": "$2b$10$...",
  "role": "academie",
  "nomAcademie": "Académie de Football Excellence",
  "adresse": "123 Rue de la Sport, 75000 Paris",
  "description": "Une académie dédiée au développement des jeunes talents",
  "horaires": {
    "lundi": { "debut": "09:00", "fin": "17:00" },
    "mardi": { "debut": "09:00", "fin": "17:00" }
  },
  "createdAt": "2024-01-01T00:00:00.000Z"
}
```

---

## 🔒 Sécurité

### Mesures de sécurité implémentées

1. **Hashage des mots de passe**
   - Utilisation de `bcrypt` avec 10 rounds de salage
   - Les mots de passe ne sont jamais stockés en clair

2. **Authentification JWT**
   - Tokens signés avec une clé secrète
   - Expiration configurable (défaut: 1 jour)
   - Validation à chaque requête

3. **Contrôle d'accès basé sur les rôles (RBAC)**
   - Vérification des rôles avant l'exécution des endpoints
   - Erreurs 403 Forbidden pour accès non autorisés

4. **Validation des données**
   - Utilisation de `class-validator` pour tous les DTOs
   - Rejet automatique des données invalides
   - Whitelist activée (supprime les propriétés non définies)

5. **CORS activé**
   - Permet les requêtes cross-origin configurées

6. **Validation des fichiers uploadés**
   - Types de fichiers autorisés (images uniquement)
   - Taille maximale (20MB)
   - Noms de fichiers uniques pour éviter les collisions

### Bonnes pratiques de sécurité

- ✅ Changez `JWT_SECRET` en production
- ✅ Utilisez HTTPS en production
- ✅ Ne commitez jamais le fichier `.env`
- ✅ Limitez le taux de requêtes (rate limiting) en production
- ✅ Validez tous les inputs côté serveur
- ✅ Utilisez des mots de passe forts (minimum 6 caractères, mais recommandé 12+)

---

## 🧪 Tests

### Tests unitaires

```bash
npm run test
```

### Tests en mode watch

```bash
npm run test:watch
```

### Tests e2e (end-to-end)

```bash
npm run test:e2e
```

### Couverture de code

```bash
npm run test:cov
```

### Vérification dans MongoDB

Après avoir créé un utilisateur via l'API, vérifiez dans MongoDB :

**Avec MongoDB Compass :**
1. Ouvrez MongoDB Compass
2. Connectez-vous à `mongodb://localhost:27017`
3. Sélectionnez la base `sportyconnect`
4. Ouvrez la collection `users`
5. Vérifiez vos données !

**Avec MongoDB Shell :**
```bash
mongosh
use sportyconnect
db.users.find().pretty()
```

---

## 📁 Structure du projet

```
backend/user/
├── src/
│   ├── app.module.ts              # Module racine de l'application
│   ├── main.ts                    # Point d'entrée, configuration Swagger
│   ├── app.controller.ts          # Contrôleur de base
│   ├── app.service.ts             # Service de base
│   │
│   ├── auth/                      # Module d'authentification
│   │   ├── auth.module.ts
│   │   ├── auth.controller.ts     # Routes: /auth/register, /auth/login
│   │   ├── auth.service.ts        # Logique métier: validation, génération JWT
│   │   ├── jwt.strategy.ts        # Stratégie Passport JWT
│   │   └── guards/
│   │       └── jwt-auth.guard.ts  # Guard global pour JWT + rôles
│   │
│   ├── users/                     # Module de gestion des utilisateurs
│   │   ├── users.module.ts
│   │   ├── users.controller.ts   # Routes: /users/*
│   │   ├── users.service.ts      # Logique métier: CRUD, relations
│   │   ├── entity/
│   │   │   └── user.entity.ts    # Schéma Mongoose User
│   │   ├── dto/
│   │   │   ├── create-user.dto.ts # DTO création utilisateur
│   │   │   ├── update-user.dto.ts # DTO mise à jour utilisateur
│   │   │   └── login-user.dto.ts  # DTO connexion
│   │   ├── interfaces/
│   │   │   └── user-role.enum.ts  # Enum des rôles
│   │   └── validators/
│   │       └── image-file.validator.ts # Validateur fichiers images
│   │
│   ├── common/                    # Modules partagés
│   │   └── decorators/
│   │       ├── roles.decorator.ts # Décorateur @Roles()
│   │       └── public.decorator.ts # Décorateur @Public()
│   │
│   └── config/
│       └── database.config.ts     # Configuration MongoDB
│
├── test/                          # Tests e2e
│   ├── app.e2e-spec.ts
│   └── jest-e2e.json
│
├── uploads/                       # Dossier des fichiers uploadés
│
├── dist/                          # Code compilé (généré)
│
├── .env                           # Variables d'environnement (à créer)
├── .prettierrc                    # Configuration Prettier
├── eslint.config.mjs              # Configuration ESLint
├── nest-cli.json                  # Configuration NestJS CLI
├── package.json                   # Dépendances et scripts
├── tsconfig.json                  # Configuration TypeScript
│
├── README.md                      # Ce fichier
├── QUICK_REF_AUTHORIZATION.md     # Référence rapide autorisations
├── GUIDE_TEST_AUTHORIZATION.md    # Guide test autorisations
├── GUIDE_TESTING_SIMPLE.md       # Guide test simple
├── GUIDE_TEST.md                  # Guide test complet
├── DEBUG_AUTH.md                  # Guide débogage auth
├── TEST_EXAMPLES.ps1              # Scripts test PowerShell
└── TEST_EXAMPLES.sh               # Scripts test Bash
```

### Description des modules

#### `auth/`
Gère l'authentification JWT :
- Inscription et connexion
- Génération et validation des tokens
- Stratégie Passport JWT

#### `users/`
Gère les utilisateurs :
- CRUD complet
- Relations parent-enfant
- Upload de photos
- Validation des données par rôle

#### `common/decorators/`
Décorateurs réutilisables :
- `@Public()` : Route publique
- `@Roles()` : Restriction par rôle

#### `config/`
Configuration de l'application :
- Connexion MongoDB
- Variables d'environnement

---

## 📝 Exemples d'utilisation

### 1. Créer un parent

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

### 2. Créer un enfant

```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Marie",
    "email": "marie@example.com",
    "motDePasse": "password123",
    "role": "enfant",
    "dateNaissance": "2010-05-15"
  }'
```

### 3. Lier un enfant à un parent

```bash
curl -X POST http://localhost:3000/users/{parentId}/link-child \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "childId": "{childId}"
  }'
```

### 4. Récupérer tous les utilisateurs (Académie)

```bash
curl -X GET http://localhost:3000/users \
  -H "Authorization: Bearer <token>"
```

### 5. Upload d'une photo de profil

```bash
curl -X POST http://localhost:3000/users/{id}/upload-photo \
  -H "Authorization: Bearer <token>" \
  -F "photo=@/path/to/photo.jpg"
```

---

## 🐛 Dépannage

### Erreur : "Cannot connect to MongoDB"

**Solution :**
1. Vérifiez que MongoDB est démarré
2. Vérifiez l'URI dans `.env`
3. Vérifiez les permissions de connexion

### Erreur : "401 Unauthorized"

**Solution :**
1. Vérifiez que le token est présent dans le header `Authorization: Bearer <token>`
2. Vérifiez que le token n'est pas expiré (générer un nouveau via `/auth/login`)
3. Vérifiez que `JWT_SECRET` est correct

👉 Consultez **[DEBUG_AUTH.md](./DEBUG_AUTH.md)** pour plus de détails !

### Erreur : "403 Forbidden"

**Solution :**
1. Vérifiez que votre utilisateur a le bon rôle
2. Vérifiez que le décorateur `@Roles()` correspond à votre rôle

### Erreur : "Email déjà utilisé"

**Solution :**
1. L'email doit être unique
2. Utilisez un autre email ou supprimez l'utilisateur existant

### Erreur : "Token invalide ou expiré"

**Solution :**
1. Générez un nouveau token via `/auth/login`
2. Vérifiez que `JWT_SECRET` n'a pas changé

---

## 📚 Documentation complémentaire

- **[QUICK_REF_AUTHORIZATION.md](./QUICK_REF_AUTHORIZATION.md)** - Référence rapide des autorisations
- **[GUIDE_TEST_AUTHORIZATION.md](./GUIDE_TEST_AUTHORIZATION.md)** - Guide détaillé de test des autorisations
- **[GUIDE_TESTING_SIMPLE.md](./GUIDE_TESTING_SIMPLE.md)** - Guide simple pour débuter avec Swagger
- **[GUIDE_TEST.md](./GUIDE_TEST.md)** - Guide complet de test
- **[DEBUG_AUTH.md](./DEBUG_AUTH.md)** - Guide de débogage de l'authentification

---

## 🤝 Contribution

Pour contribuer au projet :

1. Fork le projet
2. Créez une branche (`git checkout -b feature/AmazingFeature`)
3. Commitez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

---

## 📄 Licence

MIT

---

## 👨‍💻 Auteur

Développé pour SportyConnect Kids

---

**🎉 Félicitations ! Vous êtes maintenant prêt à utiliser l'API SportyConnect Kids !**

Pour toute question ou problème, consultez la documentation complémentaire ou ouvrez une issue.
