# 🔐 Référence Rapide : Tester les Autorisations

## ⚡ Démarrage rapide (30 secondes)

```
1. POST /auth/register → Copie access_token
2. Clique "Authorize" 🔒 dans Swagger
3. Colle le token → Clique "Authorize"
4. Teste les endpoints protégés ✅
```

---

## 🎯 Rôles et Permissions

### 📋 Rôles définis dans le système

Le système utilise 4 rôles différents :

1. **PARENT** (parent)
2. **ENFANT** (enfant)
3. **COACH** (coach)
4. **ACADEMIE** (académie)

---

### 🔐 Autorisations détaillées par rôle

#### 🌐 Endpoints accessibles à tous les rôles authentifiés

Ces endpoints sont accessibles à tous les utilisateurs authentifiés (parent, enfant, coach, académie) :

- `GET /users/{id}` - Récupérer un utilisateur par ID
- `PATCH /users/{id}` - Mettre à jour un utilisateur
- `POST /users/{id}/upload-photo` - Uploader une photo de profil

#### 👨‍👩‍👧 Rôle : PARENT

Endpoints exclusifs aux parents :

- `POST /users/{id}/link-child` - Lier un enfant à un parent
- `GET /users/{id}/children` - Récupérer la liste des enfants d'un parent

**Résumé des permissions PARENT :**
- ✅ Accès à tous les endpoints communs
- ✅ Gestion des enfants (lier et consulter)

#### 👶 Rôle : ENFANT

Aucun endpoint spécifique défini. Accès uniquement aux endpoints communs :
- `GET /users/{id}`
- `PATCH /users/{id}`
- `POST /users/{id}/upload-photo`

**Résumé des permissions ENFANT :**
- ✅ Accès aux endpoints communs uniquement
- ❌ Pas d'accès aux fonctionnalités parent/académie

#### 🏃 Rôle : COACH

Aucun endpoint spécifique défini. Accès uniquement aux endpoints communs :
- `GET /users/{id}`
- `PATCH /users/{id}`
- `POST /users/{id}/upload-photo`

**Résumé des permissions COACH :**
- ✅ Accès aux endpoints communs uniquement
- ❌ Pas d'accès aux fonctionnalités parent/académie

#### 🏛️ Rôle : ACADEMIE

Endpoints exclusifs aux académies (accès administrateur) :

- `GET /users` - Récupérer tous les utilisateurs (peut filtrer par rôle avec `?role=parent`)
- `POST /users` - Créer un nouvel utilisateur
- `DELETE /users/{id}` - Supprimer un utilisateur

**Résumé des permissions ACADEMIE :**
- ✅ Accès à tous les endpoints communs
- ✅ Gestion complète des utilisateurs (création, lecture, suppression)
- ✅ Accès administrateur complet

---

### 📊 Tableau récapitulatif

| Rôle | Endpoints Spécifiques | Endpoints Communs |
|------|----------------------|-------------------|
| **PARENT** | `POST /users/{id}/link-child`<br>`GET /users/{id}/children` | `GET /users/{id}`<br>`PATCH /users/{id}`<br>`POST /users/{id}/upload-photo` |
| **ENFANT** | Aucun | `GET /users/{id}`<br>`PATCH /users/{id}`<br>`POST /users/{id}/upload-photo` |
| **COACH** | Aucun | `GET /users/{id}`<br>`PATCH /users/{id}`<br>`POST /users/{id}/upload-photo` |
| **ACADEMIE** | `GET /users`<br>`POST /users`<br>`DELETE /users/{id}` | `GET /users/{id}`<br>`PATCH /users/{id}`<br>`POST /users/{id}/upload-photo` |

---

### 🔓 Endpoints publics (sans authentification)

Ces endpoints sont accessibles sans token JWT :

- `POST /auth/register` - Inscription d'un nouvel utilisateur
- `POST /auth/login` - Connexion d'un utilisateur existant

---

### ⚠️ Notes importantes

- Les endpoints spécifiques sont protégés par le décorateur `@Roles()`
- Si un utilisateur tente d'accéder à un endpoint sans le bon rôle, il recevra une erreur **`403 Forbidden`**
- Les endpoints communs nécessitent simplement une authentification valide (token JWT)
- Les tokens JWT contiennent le rôle de l'utilisateur et sont vérifiés à chaque requête

---

## 🧪 Tests Rapides par Rôle

### Test Parent (3 minutes)

```bash
# 1. Créer un parent
POST /auth/register
{
  "nom": "Dupont",
  "prenom": "Jean",
  "email": "jean@test.com",
  "motDePasse": "password123",
  "role": "parent"
}
→ Copie access_token

# 2. S'authentifier
[Authorize] → Colle token → [Authorize]

# 3. Tester
✅ POST /users/{parentId}/link-child → Devrait fonctionner
✅ GET /users/{parentId}/children → Devrait fonctionner
❌ GET /users → 403 Forbidden (rôle insuffisant)
```

### Test Académie (3 minutes)

```bash
# 1. Créer une académie
POST /auth/register
{
  "nom": "Admin",
  "prenom": "Académie",
  "email": "admin@academie.com",
  "motDePasse": "password123",
  "role": "academie"
}
→ Copie access_token

# 2. S'authentifier
[Authorize] → Colle token → [Authorize]

# 3. Tester
✅ GET /users → Liste tous les utilisateurs
✅ POST /users → Créer un utilisateur
✅ DELETE /users/{id} → Supprimer un utilisateur
```

---

## ❌ Erreurs Courantes

| Erreur | Cause | Solution |
|--------|-------|----------|
| `401 Unauthorized` | Pas de token / Token invalide | Vérifie que tu es authentifié dans Swagger |
| `403 Forbidden` | Mauvais rôle | Utilise un token avec le bon rôle |
| Token expiré | Token trop vieux | Génère un nouveau token via `/auth/login` |

> 🔍 **Erreur 401 persistante ?** Consulte **[DEBUG_AUTH.md](./DEBUG_AUTH.md)** pour un guide de débogage complet.

---

## 🔑 Obtenir un Token

### Méthode 1 : Inscription
```
POST /auth/register
→ Réponse: { "access_token": "..." }
```

### Méthode 2 : Connexion
```
POST /auth/login
→ Réponse: { "access_token": "..." }
```

---

## 📝 Checklist de Test

- [ ] Obtenir un token via `/auth/register` ou `/auth/login`
- [ ] S'authentifier dans Swagger (bouton "Authorize")
- [ ] Voir le cadenas 🔒 fermé après authentification
- [ ] Tester un endpoint protégé (ex: `GET /users/{id}`)
- [ ] Vérifier que sans token → `401 Unauthorized`
- [ ] Tester avec mauvais rôle → `403 Forbidden`
- [ ] Tester avec bon rôle → `200 OK`

---

## 🎓 Points Clés

1. **Token JWT** = Carte d'identité numérique (contient ton rôle)
2. **"Authorize" dans Swagger** = Se connecter
3. **Ne pas mettre "Bearer"** devant le token dans Swagger
4. **Les tokens expirent** après 1 jour (génère un nouveau si 401)

---

**📚 Pour plus de détails, consulte [GUIDE_TEST_AUTHORIZATION.md](./GUIDE_TEST_AUTHORIZATION.md)**

