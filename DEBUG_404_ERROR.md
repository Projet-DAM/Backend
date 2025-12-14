# 🔍 Guide de Débogage - Erreur 404 Not Found

## ❌ Erreur Rencontrée
```
2025-12-12 14:45:18.616 13859-13859 Subscripti...Repository com.example.dam_front E  Erreur 404: Not Found
```

---

## 🔎 Causes Possibles

### 1. ⚠️ Serveur Backend Non Démarré
**Symptôme:** Le serveur NestJS n'est pas en cours d'exécution

**Solution:**
```bash
# Démarrer le serveur en mode développement
npm run start:dev

# OU en mode production
npm run start
```

**Vérification:**
- Le serveur devrait afficher: `Nest application successfully started`
- Port par défaut: `http://localhost:3000`

---

### 2. ⚠️ URL Incorrecte dans Android
**Symptôme:** L'URL appelée ne correspond pas aux routes du backend

**Routes Disponibles:**
```
GET    /subscriptions/available-options
GET    /subscriptions/my
GET    /subscriptions/by-child/:childId
GET    /subscriptions/:id
GET    /subscriptions
POST   /subscriptions
PATCH  /subscriptions/:id
POST   /subscriptions/:id/pay
POST   /subscriptions/:id/cancel
POST   /subscriptions/:id/suspend
POST   /subscriptions/:id/resume
POST   /subscriptions/:id/renew
```

**Vérifiez dans votre code Android:**
```kotlin
// ❌ INCORRECT
api.get("/subscription/available-options")  // Manque le 's'

// ✅ CORRECT
api.get("/subscriptions/available-options")
```

---

### 3. ⚠️ Base URL Incorrecte
**Symptôme:** L'adresse IP ou le port est incorrect

**Pour Android Emulator:**
```kotlin
// ❌ INCORRECT
private const val BASE_URL = "http://localhost:3000/"

// ✅ CORRECT (Android Emulator)
private const val BASE_URL = "http://10.0.2.2:3000/"

// ✅ CORRECT (Appareil physique sur même réseau)
private const val BASE_URL = "http://192.168.1.X:3000/"
```

**Pour trouver votre IP locale:**
```bash
# Windows
ipconfig

# Cherchez "IPv4 Address" sous votre connexion réseau
```

---

### 4. ⚠️ Authentification Manquante
**Symptôme:** Le JWT token n'est pas envoyé ou est invalide

**Tous les endpoints nécessitent un JWT token sauf:**
- Login
- Register

**Vérifiez dans Android:**
```kotlin
// ❌ INCORRECT - Sans token
val response = api.get("/subscriptions/available-options")

// ✅ CORRECT - Avec token
val response = api.get("/subscriptions/available-options") {
    header("Authorization", "Bearer $token")
}
```

---

## 🛠️ Étapes de Débogage

### Étape 1: Vérifier que le Serveur est Démarré
```bash
cd c:\Users\hatem\Backend2
npm run start:dev
```

**Attendez ce message:**
```
[Nest] 12345  - 12/12/2025, 2:45:18 PM     LOG [NestApplication] Nest application successfully started +2ms
```

---

### Étape 2: Tester l'Endpoint avec cURL ou Postman

**Option A: Avec cURL (Windows PowerShell)**
```powershell
# D'abord, obtenir un token (remplacez email/password)
$loginResponse = Invoke-RestMethod -Uri "http://localhost:3000/auth/login" -Method POST -Body (@{email="parent@example.com"; password="password123"} | ConvertTo-Json) -ContentType "application/json"
$token = $loginResponse.accessToken

# Tester l'endpoint
Invoke-RestMethod -Uri "http://localhost:3000/subscriptions/available-options" -Headers @{Authorization="Bearer $token"}
```

**Option B: Avec Postman**
1. POST `http://localhost:3000/auth/login`
   - Body: `{"email": "parent@example.com", "password": "password123"}`
2. Copier le `accessToken` de la réponse
3. GET `http://localhost:3000/subscriptions/available-options`
   - Headers: `Authorization: Bearer {token}`

---

### Étape 3: Vérifier les Logs du Serveur

Quand vous faites une requête, le serveur devrait afficher:
```
[Nest] 12345  - 12/12/2025, 2:45:18 PM     LOG [SubscriptionsController] GET /subscriptions/available-options
```

**Si vous ne voyez rien:**
- Le serveur ne reçoit pas la requête
- Vérifiez l'URL et le port

**Si vous voyez une erreur 401:**
- Le token JWT est invalide ou manquant

**Si vous voyez une erreur 403:**
- Le rôle de l'utilisateur n'a pas la permission

---

### Étape 4: Vérifier le Code Android

**Fichier: `SubscriptionRepository.kt` (ou similaire)**

```kotlin
interface SubscriptionApiService {
    @GET("subscriptions/available-options")
    suspend fun getAvailableOptions(
        @Header("Authorization") token: String
    ): List<SubscriptionOption>
}

// Dans le Repository
suspend fun getAvailableOptions(): Result<List<SubscriptionOption>> {
    return try {
        val token = "Bearer ${authRepository.getToken()}"
        val options = apiService.getAvailableOptions(token)
        Result.success(options)
    } catch (e: HttpException) {
        if (e.code() == 404) {
            Log.e("SubscriptionRepository", "Erreur 404: Not Found")
            Log.e("SubscriptionRepository", "URL appelée: ${e.response()?.raw()?.request?.url}")
        }
        Result.failure(e)
    }
}
```

---

## 🔧 Solutions Rapides

### Solution 1: Vérifier la Configuration Retrofit

```kotlin
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/" // Pour émulateur
    
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request().newBuilder()
                            .addHeader("Content-Type", "application/json")
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
    }
}
```

### Solution 2: Logger les Requêtes

```kotlin
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()
```

### Solution 3: Vérifier le Manifest Android

```xml
<manifest>
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application
        android:usesCleartextTraffic="true">
        <!-- Pour permettre HTTP (non-HTTPS) en développement -->
    </application>
</manifest>
```

---

## 📋 Checklist de Vérification

- [ ] Le serveur NestJS est démarré (`npm run start:dev`)
- [ ] Le serveur affiche "Nest application successfully started"
- [ ] L'URL de base est correcte (`http://10.0.2.2:3000/` pour émulateur)
- [ ] Le chemin de l'endpoint est correct (`subscriptions/available-options`)
- [ ] Le token JWT est présent dans le header `Authorization`
- [ ] Le token JWT est valide (pas expiré)
- [ ] L'utilisateur a le bon rôle (PARENT, COACH, ACADEMIE, ou ADMIN)
- [ ] Le manifest Android autorise INTERNET
- [ ] `usesCleartextTraffic` est à `true` (pour HTTP)

---

## 🧪 Test Rapide

**1. Tester depuis le navigateur (sans auth):**
```
http://localhost:3000/api
```
Vous devriez voir la documentation Swagger.

**2. Tester l'endpoint avec Swagger:**
1. Aller sur `http://localhost:3000/api`
2. Cliquer sur "Authorize"
3. Entrer le token JWT
4. Tester `GET /subscriptions/available-options`

---

## 📞 Informations de Débogage à Fournir

Si le problème persiste, fournissez:

1. **URL complète appelée depuis Android:**
   ```
   Exemple: http://10.0.2.2:3000/subscriptions/available-options
   ```

2. **Code Android qui fait l'appel:**
   ```kotlin
   // Votre code ici
   ```

3. **Logs du serveur NestJS:**
   ```
   [Nest] 12345  - 12/12/2025, 2:45:18 PM ...
   ```

4. **Réponse HTTP complète:**
   ```json
   {
     "statusCode": 404,
     "message": "...",
     "error": "Not Found"
   }
   ```

---

## ✅ Solution la Plus Probable

**Pour un émulateur Android:**

```kotlin
// Retrofit configuration
private const val BASE_URL = "http://10.0.2.2:3000/"

// API call avec token
@GET("subscriptions/available-options")
suspend fun getAvailableOptions(
    @Header("Authorization") authorization: String
): List<SubscriptionOption>

// Usage
val token = "Bearer ${getStoredToken()}"
val options = apiService.getAvailableOptions(token)
```

**Vérifiez aussi que le serveur est bien démarré:**
```bash
npm run start:dev
```

---

**Date:** 2025-12-12  
**Status:** Guide de débogage  
**Prochaine étape:** Vérifier le serveur et l'URL Android
