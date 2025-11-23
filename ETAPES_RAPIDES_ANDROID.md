# ⚡ Étapes Rapides : Frontend Android

## 📋 Checklist rapide

### Phase 1 : Configuration du projet (30 minutes)
- [ ] Créer un nouveau projet Android dans Android Studio
- [ ] Choisir Kotlin comme language
- [ ] Configurer le minimum SDK (API 24+)
- [ ] Ajouter les dépendances dans `build.gradle.kts` :
  - Retrofit + Gson
  - OkHttp + Logging Interceptor
  - Coroutines
  - DataStore
  - ViewModel + LiveData
- [ ] Ajouter les permissions Internet dans `AndroidManifest.xml`
- [ ] Configurer `usesCleartextTraffic="true"` pour HTTP

### Phase 2 : Configuration API (15 minutes)
- [ ] Créer `ApiConfig.kt` avec l'URL du backend
  - Émulateur : `http://10.0.2.2:3000/`
  - Appareil physique : `http://VOTRE_IP:3000/`
- [ ] Créer les modèles de données (User, AuthResponse, LoginRequest, RegisterRequest)
- [ ] Créer `ApiService.kt` avec toutes les interfaces
- [ ] Créer `RetrofitClient.kt` avec la configuration Retrofit

### Phase 3 : Gestion de l'authentification (20 minutes)
- [ ] Créer `TokenManager.kt` pour stocker le JWT
- [ ] Créer `AuthRepository.kt` pour gérer login/register
- [ ] Implémenter la sauvegarde et récupération du token

### Phase 4 : Interface utilisateur (30 minutes)
- [ ] Créer `LoginActivity.kt` avec le layout
- [ ] Créer `activity_login.xml`
- [ ] Implémenter la logique de connexion
- [ ] Créer `RegisterActivity.kt` (optionnel pour commencer)
- [ ] Créer `MainActivity.kt` avec vérification du token

### Phase 5 : Test (15 minutes)
- [ ] Démarrer le backend (`npm run start:dev`)
- [ ] Configurer l'URL correcte dans `ApiConfig.kt`
- [ ] Lancer l'application Android
- [ ] Tester la connexion avec un utilisateur existant
- [ ] Vérifier que le token est sauvegardé

---

## 🔑 Points clés à retenir

### URL du backend
```kotlin
// Émulateur Android
const val BASE_URL = "http://10.0.2.2:3000/"

// Appareil physique (remplacez par votre IP locale)
const val BASE_URL = "http://192.168.1.100:3000/"
```

### Format du header d'authentification
```kotlin
@Header("Authorization") token: String
// Format: "Bearer <access_token>"
```

### Structure des réponses API
```kotlin
// Login/Register
data class AuthResponse(
    val access_token: String,
    val user: User
)

// User
data class User(
    val _id: String?,
    val nom: String,
    val prenom: String,
    val email: String,
    val role: String,
    val photoProfil: String?
)
```

---

## 🚀 Commandes utiles

### Démarrer le backend
```bash
cd Backend
npm run start:dev
```

### Trouver l'IP locale (Windows)
```powershell
ipconfig
# Cherchez "IPv4 Address" sous votre carte réseau active
```

### Tester l'API avec curl (optionnel)
```bash
# Login
curl -X POST http://localhost:3000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","motDePasse":"password123"}'
```

---

## 📱 Structure du projet Android recommandée

```
app/src/main/java/com/sportyconnect/kids/
├── api/
│   ├── ApiService.kt
│   └── RetrofitClient.kt
├── config/
│   └── ApiConfig.kt
├── models/
│   ├── User.kt
│   ├── AuthResponse.kt
│   ├── LoginRequest.kt
│   └── RegisterRequest.kt
├── repository/
│   └── AuthRepository.kt
├── ui/
│   ├── login/
│   │   └── LoginActivity.kt
│   └── main/
│       └── MainActivity.kt
└── utils/
    └── TokenManager.kt
```

---

## ⚠️ Erreurs courantes et solutions

| Erreur | Solution |
|--------|----------|
| `Unable to resolve host` | Vérifier l'URL dans `ApiConfig.kt` |
| `Connection refused` | Vérifier que le backend est démarré et accessible |
| `401 Unauthorized` | Vérifier le format du token : `Bearer <token>` |
| `Cleartext HTTP traffic not permitted` | Ajouter `usesCleartextTraffic="true"` dans AndroidManifest.xml |
| `NetworkOnMainThreadException` | Utiliser Coroutines avec `lifecycleScope.launch` |

---

## 🎯 Prochaines étapes après la connexion de base

1. **Créer l'écran d'inscription**
   - RegisterActivity
   - Validation des champs
   - Sélection du rôle

2. **Créer l'écran de profil**
   - Afficher les informations utilisateur
   - Modifier le profil
   - Upload de photo de profil

3. **Gérer les activités**
   - Liste des activités
   - Créer une activité
   - Modifier/Supprimer une activité

4. **Gérer les relations parent-enfant**
   - Lier un enfant à un parent
   - Afficher la liste des enfants

5. **Améliorer l'UX**
   - Loading states
   - Gestion d'erreurs plus robuste
   - Navigation avec Navigation Component
   - Material Design

---

Bon développement ! 🚀



