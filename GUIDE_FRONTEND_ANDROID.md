# 📱 Guide : Créer un Frontend Android avec Kotlin et le connecter au Backend

Ce guide vous explique étape par étape comment créer une application Android avec Kotlin et la connecter à votre backend NestJS.

---

## 📋 Prérequis

- ✅ Android Studio (version récente)
- ✅ JDK 17 ou supérieur
- ✅ Backend NestJS démarré et accessible
- ✅ Connaissances de base en Kotlin et Android

---

## 🚀 Étape 1 : Créer un nouveau projet Android

### 1.1 Ouvrir Android Studio
1. Lancez Android Studio
2. Cliquez sur **"New Project"**

### 1.2 Choisir un template
1. Sélectionnez **"Empty Activity"** ou **"Basic Activity"**
2. Cliquez sur **"Next"**

### 1.3 Configurer le projet
- **Name** : `SportyConnectKids` (ou le nom de votre choix)
- **Package name** : `com.sportyconnect.kids` (ou selon vos préférences)
- **Save location** : Créez un nouveau dossier **sibling** à votre dossier Backend
  - Exemple : `C:\Users\dell\Documents\GitHub\Frontend` (si Backend est dans `C:\Users\dell\Documents\GitHub\Backend`)
- **Language** : **Kotlin** ✅
- **Minimum SDK** : API 24 (Android 7.0) ou supérieur
- **Build configuration language** : Kotlin DSL (recommandé) ou Groovy

### 1.4 Finaliser la création
1. Cliquez sur **"Finish"**
2. Attendez que Gradle synchronise le projet

---

## 🔧 Étape 2 : Configurer les dépendances (build.gradle.kts)

### 2.1 Ouvrir le fichier de configuration
Ouvrez le fichier `app/build.gradle.kts` (ou `app/build.gradle`)

### 2.2 Ajouter les dépendances nécessaires

Dans la section `dependencies`, ajoutez :

```kotlin
dependencies {
    // ... dépendances existantes ...
    
    // Retrofit pour les appels API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp pour la gestion des requêtes HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Gson pour la sérialisation JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines pour la programmation asynchrone
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // ViewModel et LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Navigation Component (optionnel mais recommandé)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    
    // DataStore pour stocker le token JWT
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Glide pour charger les images (pour les photos de profil)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
```

### 2.3 Ajouter les permissions dans AndroidManifest.xml

Ouvrez `app/src/main/AndroidManifest.xml` et ajoutez avant `<application>` :

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Ajoutez également dans `<application>` pour permettre les connexions HTTP (si votre backend est en local) :

```xml
<application
    android:usesCleartextTraffic="true"
    ...>
```

⚠️ **Note** : `usesCleartextTraffic="true"` permet les connexions HTTP non sécurisées. Pour la production, utilisez HTTPS ou configurez un network security config.

---

## 🌐 Étape 3 : Configurer l'URL du backend

### 3.1 Créer un fichier de configuration

Créez un nouveau fichier : `app/src/main/java/com/sportyconnect/kids/config/ApiConfig.kt`

```kotlin
package com.sportyconnect.kids.config

object ApiConfig {
    // Pour l'émulateur Android, utilisez 10.0.2.2 au lieu de localhost
    // Pour un appareil physique, utilisez l'IP de votre ordinateur (ex: 192.168.1.100)
    const val BASE_URL = "http://10.0.2.2:3000/" // Émulateur
    // const val BASE_URL = "http://192.168.1.100:3000/" // Appareil physique (remplacez par votre IP)
}
```

**Important** :
- **Émulateur Android** : Utilisez `10.0.2.2` pour accéder à `localhost` de votre machine
- **Appareil physique** : Utilisez l'IP locale de votre ordinateur (trouvez-la avec `ipconfig` sur Windows)

---

## 📦 Étape 4 : Créer les modèles de données (Data Classes)

### 4.1 Créer le modèle User

Créez : `app/src/main/java/com/sportyconnect/kids/models/User.kt`

```kotlin
package com.sportyconnect.kids.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id") val id: String? = null,
    val nom: String,
    val prenom: String,
    val email: String,
    val role: String,
    @SerializedName("photoProfil") val photoProfil: String? = null,
    val enfants: List<String>? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)
```

### 4.2 Créer le modèle AuthResponse

Créez : `app/src/main/java/com/sportyconnect/kids/models/AuthResponse.kt`

```kotlin
package com.sportyconnect.kids.models

data class AuthResponse(
    val access_token: String,
    val user: User
)
```

### 4.3 Créer les DTOs de requête

Créez : `app/src/main/java/com/sportyconnect/kids/models/LoginRequest.kt`

```kotlin
package com.sportyconnect.kids.models

data class LoginRequest(
    val email: String,
    val motDePasse: String
)
```

Créez : `app/src/main/java/com/sportyconnect/kids/models/RegisterRequest.kt`

```kotlin
package com.sportyconnect.kids.models

data class RegisterRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val motDePasse: String,
    val role: String // "parent", "enfant", "coach", "academie"
)
```

---

## 🔌 Étape 5 : Créer l'interface API (Retrofit)

Créez : `app/src/main/java/com/sportyconnect/kids/api/ApiService.kt`

```kotlin
package com.sportyconnect.kids.api

import com.sportyconnect.kids.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // Authentification (Public)
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    // Utilisateurs (Authentifié)
    @GET("users")
    suspend fun getAllUsers(@Header("Authorization") token: String): Response<List<User>>
    
    @GET("users/{id}")
    suspend fun getUserById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<User>
    
    @PATCH("users/{id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body user: UpdateUserRequest
    ): Response<User>
    
    @DELETE("users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
    
    // Relations parent-enfant (Parent uniquement)
    @POST("users/{id}/link-child")
    suspend fun linkChild(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: LinkChildRequest
    ): Response<User>
    
    @GET("users/{id}/children")
    suspend fun getChildren(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<List<User>>
    
    // Upload photo de profil
    @Multipart
    @POST("users/{id}/upload-photo")
    suspend fun uploadPhoto(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Part photo: MultipartBody.Part
    ): Response<User>
}

// DTOs supplémentaires
data class UpdateUserRequest(
    val nom: String? = null,
    val prenom: String? = null,
    val email: String? = null
)

data class LinkChildRequest(
    val childId: String
)
```

---

## 🛠️ Étape 6 : Créer le client Retrofit

Créez : `app/src/main/java/com/sportyconnect/kids/api/RetrofitClient.kt`

```kotlin
package com.sportyconnect.kids.api

import com.sportyconnect.kids.config.ApiConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
```

---

## 💾 Étape 7 : Créer un gestionnaire de token (TokenManager)

Créez : `app/src/main/java/com/sportyconnect/kids/utils/TokenManager.kt`

```kotlin
package com.sportyconnect.kids.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("access_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
    }
    
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    suspend fun getToken(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[TOKEN_KEY]
        }
    }
    
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }
    
    suspend fun getUserId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID_KEY]
        }
    }
    
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }
}
```

---

## 📱 Étape 8 : Créer un Repository

Créez : `app/src/main/java/com/sportyconnect/kids/repository/AuthRepository.kt`

```kotlin
package com.sportyconnect.kids.repository

import android.content.Context
import com.sportyconnect.kids.api.RetrofitClient
import com.sportyconnect.kids.models.LoginRequest
import com.sportyconnect.kids.models.RegisterRequest
import com.sportyconnect.kids.utils.TokenManager
import kotlinx.coroutines.flow.first

class AuthRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.apiService
    private val tokenManager = TokenManager(context)
    
    suspend fun register(request: RegisterRequest): Result<Pair<String, String>> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveToken(authResponse.access_token)
                tokenManager.saveUserId(authResponse.user.id ?: "")
                Result.success(Pair(authResponse.access_token, authResponse.user.id ?: ""))
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(request: LoginRequest): Result<Pair<String, String>> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                tokenManager.saveToken(authResponse.access_token)
                tokenManager.saveUserId(authResponse.user.id ?: "")
                Result.success(Pair(authResponse.access_token, authResponse.user.id ?: ""))
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getToken(): String? {
        return tokenManager.getToken().first()
    }
    
    suspend fun getUserId(): String? {
        return tokenManager.getUserId().first()
    }
    
    suspend fun logout() {
        tokenManager.clearToken()
    }
    
    fun getAuthHeader(): String {
        return "Bearer ${getToken()}"
    }
}
```

---

## 🎨 Étape 9 : Créer les écrans (Activities/Fragments)

### 9.1 Écran de Login

Créez : `app/src/main/java/com/sportyconnect/kids/ui/login/LoginActivity.kt`

```kotlin
package com.sportyconnect.kids.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sportyconnect.kids.R
import com.sportyconnect.kids.databinding.ActivityLoginBinding
import com.sportyconnect.kids.models.LoginRequest
import com.sportyconnect.kids.repository.AuthRepository
import com.sportyconnect.kids.ui.main.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        authRepository = AuthRepository(this)
        
        binding.btnLogin.setOnClickListener {
            login()
        }
        
        binding.tvRegister.setOnClickListener {
            // Naviguer vers l'écran d'inscription
            // startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun login() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()
        
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return
        }
        
        lifecycleScope.launch {
            binding.progressBar.visibility = android.view.View.VISIBLE
            val result = authRepository.login(LoginRequest(email, password))
            binding.progressBar.visibility = android.view.View.GONE
            
            result.onSuccess {
                Toast.makeText(this@LoginActivity, "Connexion réussie", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            }.onFailure {
                Toast.makeText(this@LoginActivity, "Erreur: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### 9.2 Layout pour Login (activity_login.xml)

Créez : `app/src/main/res/layout/activity_login.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp"
    android:gravity="center">

    <EditText
        android:id="@+id/etEmail"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Email"
        android:inputType="textEmailAddress"
        android:layout_marginBottom="16dp" />

    <EditText
        android:id="@+id/etPassword"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Mot de passe"
        android:inputType="textPassword"
        android:layout_marginBottom="16dp" />

    <Button
        android:id="@+id/btnLogin"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Se connecter" />

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:visibility="gone"
        android:layout_marginTop="16dp" />

    <TextView
        android:id="@+id/tvRegister"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Pas de compte ? S'inscrire"
        android:textColor="@android:color/holo_blue_dark"
        android:layout_marginTop="16dp" />

</LinearLayout>
```

---

## ✅ Étape 10 : Tester la connexion

### 10.1 Démarrer le backend
```bash
cd Backend
npm run start:dev
```

### 10.2 Configurer l'URL dans ApiConfig.kt
- Pour l'émulateur : `http://10.0.2.2:3000/`
- Pour un appareil physique : `http://VOTRE_IP_LOCALE:3000/`

### 10.3 Lancer l'application Android
1. Connectez un appareil ou lancez un émulateur
2. Cliquez sur "Run" dans Android Studio
3. Testez la connexion avec les identifiants du backend

---

## 🔒 Étape 11 : Gérer l'authentification automatique

Modifiez votre `MainActivity` pour vérifier le token au démarrage :

```kotlin
class MainActivity : AppCompatActivity() {
    
    private lateinit var authRepository: AuthRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authRepository = AuthRepository(this)
        
        lifecycleScope.launch {
            val token = authRepository.getToken()
            if (token == null) {
                // Rediriger vers LoginActivity
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
            } else {
                // Charger les données utilisateur
                loadUserData(token)
            }
        }
    }
    
    private suspend fun loadUserData(token: String) {
        // Charger les données de l'utilisateur
    }
}
```

---

## 📝 Résumé des étapes

1. ✅ Créer un nouveau projet Android avec Kotlin
2. ✅ Ajouter les dépendances (Retrofit, OkHttp, Gson, Coroutines, etc.)
3. ✅ Configurer les permissions dans AndroidManifest.xml
4. ✅ Créer ApiConfig.kt avec l'URL du backend
5. ✅ Créer les modèles de données (User, AuthResponse, etc.)
6. ✅ Créer l'interface API (ApiService.kt)
7. ✅ Créer le client Retrofit (RetrofitClient.kt)
8. ✅ Créer TokenManager pour gérer le JWT
9. ✅ Créer AuthRepository pour gérer l'authentification
10. ✅ Créer les écrans (LoginActivity, etc.)
11. ✅ Tester la connexion

---

## 🐛 Résolution de problèmes

### Problème : "Unable to resolve host"
- Vérifiez que l'URL dans `ApiConfig.kt` est correcte
- Pour l'émulateur : utilisez `10.0.2.2`
- Pour un appareil physique : utilisez l'IP locale de votre ordinateur

### Problème : "Connection refused"
- Vérifiez que le backend est démarré
- Vérifiez que le port 3000 est accessible
- Vérifiez que CORS est activé dans le backend (déjà fait dans votre main.ts)

### Problème : "401 Unauthorized"
- Vérifiez que le token est correctement envoyé dans le header `Authorization`
- Vérifiez que le format est : `Bearer <token>`
- Vérifiez que le token n'est pas expiré

### Problème : "Cleartext HTTP traffic not permitted"
- Ajoutez `android:usesCleartextTraffic="true"` dans AndroidManifest.xml
- Ou configurez un Network Security Config pour la production

---

## 🚀 Prochaines étapes

1. Créer l'écran d'inscription (RegisterActivity)
2. Créer l'écran de profil utilisateur
3. Implémenter la gestion des activités (activities)
4. Ajouter la gestion des images (Glide)
5. Implémenter la navigation avec Navigation Component
6. Ajouter la gestion d'erreurs plus robuste
7. Implémenter le refresh token si nécessaire

---

## 📚 Ressources utiles

- [Retrofit Documentation](https://square.github.io/retrofit/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)

---

Bon développement ! 🎉




