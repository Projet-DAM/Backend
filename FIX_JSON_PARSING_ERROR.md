# 🔧 Correction Erreur JSON - Expected BEGIN_OBJECT but was BEGIN_ARRAY

## ❌ Erreur Rencontrée

```
Expected BEGIN_OBJECT but was BEGIN_ARRAY at line 1 column 2 path $
```

## 🎯 Diagnostic

**Problème:** Le backend retourne un **tableau JSON** mais Android attend un **objet JSON**

**Ce que le backend retourne:**
```json
[
  {
    "type": "SPORTS_OUTFIT",
    "name": "Tenue sportive",
    "price": 50,
    "currency": "TND"
  },
  {
    "type": "INSURANCE",
    "name": "Assurance",
    "price": 30,
    "currency": "TND"
  },
  {
    "type": "TRANSPORT",
    "name": "Transport",
    "price": 40,
    "currency": "TND"
  }
]
```

**Ce qu'Android attend (probablement):**
```json
{
  "options": [...]
}
```

---

## ✅ Solution 1: Modifier le Code Android (Recommandé)

### Avant (Incorrect)
```kotlin
// ❌ Attend un objet avec une propriété
data class SubscriptionOptionsResponse(
    val options: List<SubscriptionOption>
)

@GET("subscription-options")
suspend fun getAvailableOptions(
    @Header("Authorization") authorization: String
): SubscriptionOptionsResponse
```

### Après (Correct)
```kotlin
// ✅ Attend directement un tableau
@GET("subscription-options")
suspend fun getAvailableOptions(
    @Header("Authorization") authorization: String
): List<SubscriptionOption>
```

### Data Class
```kotlin
data class SubscriptionOption(
    val type: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String
)
```

---

## ✅ Solution 2: Wrapper le Backend (Alternative)

Si vous ne pouvez pas modifier Android, je peux créer un endpoint qui retourne un objet:

```json
{
  "success": true,
  "data": [
    { "type": "SPORTS_OUTFIT", ... },
    { "type": "INSURANCE", ... },
    { "type": "TRANSPORT", ... }
  ]
}
```

**Dites-moi si vous préférez cette solution.**

---

## 📱 Code Android Complet (Solution 1)

### 1. Data Class
```kotlin
package com.example.dam_front.data.model

data class SubscriptionOption(
    val type: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String
)
```

### 2. API Service
```kotlin
package com.example.dam_front.data.api

import com.example.dam_front.data.model.SubscriptionOption
import retrofit2.http.GET
import retrofit2.http.Header

interface SubscriptionOptionsApiService {
    @GET("subscription-options")
    suspend fun getAvailableOptions(
        @Header("Authorization") authorization: String
    ): List<SubscriptionOption>  // ✅ Directement un List
}
```

### 3. Repository
```kotlin
package com.example.dam_front.data.repository

import com.example.dam_front.data.api.SubscriptionOptionsApiService
import com.example.dam_front.data.model.SubscriptionOption
import android.util.Log

class SubscriptionOptionsRepository(
    private val apiService: SubscriptionOptionsApiService,
    private val authRepository: AuthRepository
) {
    suspend fun getAvailableOptions(): Result<List<SubscriptionOption>> {
        return try {
            val token = "Bearer ${authRepository.getToken()}"
            Log.d("SubscriptionOptions", "Calling API with token: ${token.take(20)}...")
            
            val options = apiService.getAvailableOptions(token)
            
            Log.d("SubscriptionOptions", "✅ Success! Received ${options.size} options")
            Result.success(options)
        } catch (e: Exception) {
            Log.e("SubscriptionOptions", "❌ Error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
```

### 4. ViewModel
```kotlin
package com.example.dam_front.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_front.data.model.SubscriptionOption
import com.example.dam_front.data.repository.SubscriptionOptionsRepository
import kotlinx.coroutines.launch

class SubscriptionOptionsViewModel(
    private val repository: SubscriptionOptionsRepository
) : ViewModel() {
    
    private val _options = MutableLiveData<List<SubscriptionOption>>()
    val options: LiveData<List<SubscriptionOption>> = _options
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadOptions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getAvailableOptions()
                .onSuccess { optionsList ->
                    _options.value = optionsList
                    Log.d("ViewModel", "✅ Loaded ${optionsList.size} options")
                }
                .onFailure { exception ->
                    _error.value = "Erreur: ${exception.message}"
                    Log.e("ViewModel", "❌ Error loading options", exception)
                }
            
            _isLoading.value = false
        }
    }
}
```

### 5. Usage dans une Activity/Fragment
```kotlin
class SubscriptionOptionsActivity : AppCompatActivity() {
    
    private lateinit var viewModel: SubscriptionOptionsViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription_options)
        
        // Initialiser le ViewModel
        viewModel = ViewModelProvider(this)[SubscriptionOptionsViewModel::class.java]
        
        // Observer les données
        viewModel.options.observe(this) { options ->
            // Afficher les options
            options.forEach { option ->
                Log.d("Options", "${option.name}: ${option.price} ${option.currency}")
            }
        }
        
        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        
        viewModel.isLoading.observe(this) { isLoading ->
            // Afficher/masquer le loading
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Charger les options
        viewModel.loadOptions()
    }
}
```

---

## 🧪 Test Rapide

### Test dans une Activity
```kotlin
lifecycleScope.launch {
    try {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        val api = retrofit.create(SubscriptionOptionsApiService::class.java)
        val token = "Bearer YOUR_TOKEN"
        
        val options = api.getAvailableOptions(token)
        
        Log.d("Test", "✅ Success! Options:")
        options.forEach { option ->
            Log.d("Test", "  - ${option.name}: ${option.price} ${option.currency}")
        }
        
        Toast.makeText(this@TestActivity, 
            "✅ ${options.size} options chargées", 
            Toast.LENGTH_LONG).show()
            
    } catch (e: Exception) {
        Log.e("Test", "❌ Error", e)
        Toast.makeText(this@TestActivity, 
            "❌ ${e.message}", 
            Toast.LENGTH_LONG).show()
    }
}
```

---

## 📋 Checklist de Vérification

- [ ] Type de retour = `List<SubscriptionOption>` (pas un objet wrapper)
- [ ] Data class `SubscriptionOption` avec les bons champs
- [ ] Token JWT avec préfixe "Bearer "
- [ ] URL = `http://10.0.2.2:3000/subscription-options`
- [ ] Serveur backend redémarré
- [ ] Gson configuré dans Retrofit

---

## 🔍 Débogage

### Vérifier la Réponse Brute
```kotlin
val loggingInterceptor = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .addInterceptor(loggingInterceptor)
    .build()

val retrofit = Retrofit.Builder()
    .baseUrl("http://10.0.2.2:3000/")
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

Vous verrez dans les logs:
```
D/OkHttp: [
D/OkHttp:   {
D/OkHttp:     "type": "SPORTS_OUTFIT",
D/OkHttp:     "name": "Tenue sportive",
D/OkHttp:     ...
D/OkHttp:   }
D/OkHttp: ]
```

---

## ⚠️ Erreurs Communes

### Erreur 1: Mauvais Type de Retour
```kotlin
// ❌ FAUX
suspend fun getAvailableOptions(): SubscriptionOptionsResponse

// ✅ CORRECT
suspend fun getAvailableOptions(): List<SubscriptionOption>
```

### Erreur 2: Champs Manquants
```kotlin
// ❌ FAUX - Champs ne correspondent pas
data class SubscriptionOption(
    val id: String,  // N'existe pas dans la réponse
    val title: String  // Devrait être "name"
)

// ✅ CORRECT - Champs exacts
data class SubscriptionOption(
    val type: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String
)
```

### Erreur 3: Type de Données
```kotlin
// ❌ FAUX
val price: Int  // Le backend retourne un Double

// ✅ CORRECT
val price: Double
```

---

## 🎉 Résultat Attendu

Après correction, vous devriez voir dans les logs:
```
D/SubscriptionOptions: ✅ Success! Received 3 options
D/Options: Tenue sportive: 50.0 TND
D/Options: Assurance: 30.0 TND
D/Options: Transport: 40.0 TND
```

---

## 📞 Si le Problème Persiste

Partagez:
1. Votre code `SubscriptionOption` data class
2. Votre code `@GET` dans l'interface API
3. Les logs avec `HttpLoggingInterceptor`

---

**Date:** 2025-12-12  
**Status:** Solution fournie  
**Action:** Modifier le type de retour en `List<SubscriptionOption>`
