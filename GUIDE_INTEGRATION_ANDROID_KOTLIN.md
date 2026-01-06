# 🤖 Guide d'Intégration Android/Kotlin - Backend NestJS

## 📋 Table des Matières
1. [Configuration Retrofit](#configuration-retrofit)
2. [Modèles de Données](#modèles-de-données)
3. [Service API](#service-api)
4. [Gestion de l'Authentification](#gestion-de-lauthentification)
5. [Exemples d'Utilisation](#exemples-dutilisation)
6. [Gestion des Erreurs](#gestion-des-erreurs)

---

## 🔧 Configuration Retrofit

### 1. Dépendances Gradle

Ajoutez dans `build.gradle.kts` (Module: app) :

```kotlin
dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // ViewModel & LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    
    // DataStore (pour stocker le token)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Gson
    implementation("com.google.code.gson:gson:2.10.1")
}
```

### 2. Configuration Réseau

Créez `network/RetrofitClient.kt` :

```kotlin
package com.example.sportyconnect.network

import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/" // Pour émulateur Android
    // Pour device physique, utilisez l'IP de votre machine : "http://192.168.1.X:3000/"
    
    private var token: String? = null
    
    fun setToken(newToken: String?) {
        token = newToken
    }
    
    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        
        // Ajouter le token si disponible
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }
        
        chain.proceed(requestBuilder.build())
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
```

---

## 📦 Modèles de Données

Créez `models/` :

### User.kt
```kotlin
package com.example.sportyconnect.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id")
    val id: String,
    val email: String,
    val nom: String,
    val prenom: String,
    val role: String,
    val telephone: String? = null,
    val photoProfil: String? = null,
    val emailVerified: Boolean = false,
    val createdAt: String? = null
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    val user: User
)

data class RegisterRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    val motDePasse: String,
    val role: String
)

data class RegisterResponse(
    val message: String,
    val userId: String,
    val email: String
)

data class LoginRequest(
    val email: String,
    val motDePasse: String
)

data class VerifyEmailRequest(
    val userId: String,
    val code: String
)
```

### Offer.kt
```kotlin
package com.example.sportyconnect.models

import com.google.gson.annotations.SerializedName

data class Offer(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val durationDays: Int,
    val price: Double,
    val currency: String,
    val discountPct: Double,
    val isActive: Boolean,
    val academyId: String? = null,
    val conditions: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class OffersResponse(
    val data: List<Offer>,
    val total: Int,
    val page: Int,
    val limit: Int
)

data class CreateOfferRequest(
    val name: String,
    val description: String,
    val type: String,
    val durationDays: Int,
    val price: Double,
    val currency: String = "EUR",
    val discountPct: Double = 0.0,
    val conditions: String? = null,
    val isActive: Boolean = true
)

data class UpdateOfferRequest(
    val name: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val discountPct: Double? = null,
    val isActive: Boolean? = null
)
```

### Subscription.kt
```kotlin
package com.example.sportyconnect.models

import com.google.gson.annotations.SerializedName

data class Subscription(
    @SerializedName("_id")
    val id: String,
    val parentId: String,
    val childId: ChildInfo,
    val offerId: OfferInfo,
    val status: String,
    val paymentStatus: String,
    val startDate: String,
    val endDate: String,
    val totalAmount: Double,
    val currency: String,
    val autoRenew: Boolean,
    val createdAt: String? = null
)

data class ChildInfo(
    @SerializedName("_id")
    val id: String,
    val nom: String,
    val prenom: String
)

data class OfferInfo(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val price: Double
)

data class CreateSubscriptionRequest(
    val childId: String,
    val offerId: String,
    val autoRenew: Boolean = true
)

data class SubscriptionsResponse(
    val data: List<Subscription>,
    val total: Int,
    val page: Int,
    val limit: Int
)
```

### Child.kt
```kotlin
package com.example.sportyconnect.models

import com.google.gson.annotations.SerializedName

data class Child(
    @SerializedName("_id")
    val id: String,
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val genre: String,
    val niveau: String? = null,
    val parentId: String
)

data class CreateChildRequest(
    val nom: String,
    val prenom: String,
    val dateNaissance: String,
    val genre: String,
    val niveau: String? = null
)
```

### Payment.kt
```kotlin
package com.example.sportyconnect.models

import com.google.gson.annotations.SerializedName

data class CreatePaymentIntentRequest(
    val offerId: String,
    val selectedOptions: List<String> = emptyList()
)

data class PaymentIntentResponse(
    val clientSecret: String,
    val paymentIntentId: String,
    val amount: Int,
    val currency: String
)

data class CompletePaymentRequest(
    val paymentIntentId: String,
    val childId: String,
    val offerId: String
)

data class CompletePaymentResponse(
    val success: Boolean,
    val subscription: Subscription,
    val message: String
)
```

---

## 🌐 Service API

Créez `network/ApiService.kt` :

```kotlin
package com.example.sportyconnect.network

import com.example.sportyconnect.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    
    // ==================== AUTH ====================
    
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): Response<AuthResponse>
    
    // ==================== USERS ====================
    
    @GET("users/me")
    suspend fun getMyProfile(): Response<User>
    
    @PATCH("users/me")
    suspend fun updateMyProfile(@Body updates: Map<String, Any>): Response<User>
    
    @GET("users/children")
    suspend fun getMyChildren(): Response<List<Child>>
    
    @POST("users/children")
    suspend fun addChild(@Body request: CreateChildRequest): Response<Child>
    
    // ==================== OFFERS ====================
    
    @GET("offers")
    suspend fun getOffers(
        @Query("isActive") isActive: Boolean? = null,
        @Query("academyId") academyId: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<OffersResponse>
    
    @GET("offers/{id}")
    suspend fun getOffer(@Path("id") id: String): Response<Offer>
    
    @POST("offers")
    suspend fun createOffer(@Body request: CreateOfferRequest): Response<Offer>
    
    @PATCH("offers/{id}")
    suspend fun updateOffer(
        @Path("id") id: String,
        @Body updates: UpdateOfferRequest
    ): Response<Offer>
    
    @DELETE("offers/{id}")
    suspend fun deleteOffer(@Path("id") id: String): Response<Unit>
    
    @GET("offers/all-subscribers")
    suspend fun getAllSubscribers(): Response<List<OfferSubscribers>>
    
    // ==================== SUBSCRIPTIONS ====================
    
    @POST("subscriptions")
    suspend fun createSubscription(@Body request: CreateSubscriptionRequest): Response<Subscription>
    
    @GET("subscriptions/my")
    suspend fun getMySubscriptions(): Response<List<Subscription>>
    
    @GET("subscriptions")
    suspend fun getAllSubscriptions(
        @Query("status") status: String? = null,
        @Query("paymentStatus") paymentStatus: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): Response<SubscriptionsResponse>
    
    @GET("subscriptions/{id}")
    suspend fun getSubscription(@Path("id") id: String): Response<Subscription>
    
    @POST("subscriptions/{id}/cancel")
    suspend fun cancelSubscription(@Path("id") id: String): Response<Subscription>
    
    @POST("subscriptions/{id}/renew")
    suspend fun renewSubscription(@Path("id") id: String): Response<Subscription>
    
    @GET("subscriptions/forecast/revenue")
    suspend fun getRevenueForecast(): Response<RevenueForecast>
    
    // ==================== PAYMENTS ====================
    
    @POST("payments/create-payment-intent")
    suspend fun createPaymentIntent(@Body request: CreatePaymentIntentRequest): Response<PaymentIntentResponse>
    
    @POST("payments/complete")
    suspend fun completePayment(@Body request: CompletePaymentRequest): Response<CompletePaymentResponse>
}

data class OfferSubscribers(
    val offerId: String,
    val offerName: String,
    val subscribers: List<Subscriber>,
    val totalSubscribers: Int
)

data class Subscriber(
    val subscriptionId: String,
    val parentId: String,
    val parentName: String,
    val childId: String,
    val childName: String,
    val status: String,
    val startDate: String,
    val endDate: String
)

data class RevenueForecast(
    val currentMonthRevenue: Double,
    val nextMonthForecast: Double,
    val growthRate: Double,
    val activeSubscriptions: Int
)
```

---

## 🔐 Gestion de l'Authentification

### TokenManager.kt

```kotlin
package com.example.sportyconnect.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {
    
    companion object {
        private val TOKEN_KEY = stringPreferencesKey("access_token")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USER_ROLE_KEY = stringPreferencesKey("user_role")
    }
    
    val token: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[TOKEN_KEY]
    }
    
    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }
    
    val userRole: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_ROLE_KEY]
    }
    
    suspend fun saveToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
        }
    }
    
    suspend fun saveUserInfo(userId: String, role: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[USER_ROLE_KEY] = role
        }
    }
    
    suspend fun clearToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
            preferences.remove(USER_ROLE_KEY)
        }
    }
}
```

---

## 📱 Repository Pattern

### AuthRepository.kt

```kotlin
package com.example.sportyconnect.repository

import com.example.sportyconnect.models.*
import com.example.sportyconnect.network.RetrofitClient
import com.example.sportyconnect.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    
    private val apiService = RetrofitClient.apiService
    
    suspend fun register(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        role: String
    ): Resource<RegisterResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.register(
                RegisterRequest(nom, prenom, email, password, role)
            )
            
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(response.message() ?: "Erreur lors de l'inscription")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Erreur réseau")
        }
    }
    
    suspend fun login(email: String, password: String): Resource<AuthResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email, password))
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    RetrofitClient.setToken(authResponse.accessToken)
                    Resource.Success(authResponse)
                } else {
                    Resource.Error(response.message() ?: "Email ou mot de passe incorrect")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
    
    suspend fun verifyEmail(userId: String, code: String): Resource<AuthResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyEmail(VerifyEmailRequest(userId, code))
                
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    RetrofitClient.setToken(authResponse.accessToken)
                    Resource.Success(authResponse)
                } else {
                    Resource.Error(response.message() ?: "Code de vérification incorrect")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
}
```

### OffersRepository.kt

```kotlin
package com.example.sportyconnect.repository

import com.example.sportyconnect.models.*
import com.example.sportyconnect.network.RetrofitClient
import com.example.sportyconnect.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OffersRepository {
    
    private val apiService = RetrofitClient.apiService
    
    suspend fun getOffers(isActive: Boolean? = null): Resource<List<Offer>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOffers(isActive = isActive)
                
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!.data)
                } else {
                    Resource.Error(response.message() ?: "Erreur lors du chargement des offres")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
    
    suspend fun createOffer(request: CreateOfferRequest): Resource<Offer> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createOffer(request)
                
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error(response.message() ?: "Erreur lors de la création de l'offre")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
    
    suspend fun updateOffer(id: String, updates: UpdateOfferRequest): Resource<Offer> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateOffer(id, updates)
                
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error(response.message() ?: "Erreur lors de la mise à jour")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
    
    suspend fun deleteOffer(id: String): Resource<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteOffer(id)
                
                if (response.isSuccessful) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(response.message() ?: "Erreur lors de la suppression")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
    
    suspend fun getAllSubscribers(): Resource<List<OfferSubscribers>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAllSubscribers()
                
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error(response.message() ?: "Erreur lors du chargement")
                }
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Erreur réseau")
            }
        }
}
```

### Resource.kt (Helper)

```kotlin
package com.example.sportyconnect.utils

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T> : Resource<T>()
}
```

---

## 🎯 ViewModel Examples

### AuthViewModel.kt

```kotlin
package com.example.sportyconnect.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.sportyconnect.models.AuthResponse
import com.example.sportyconnect.repository.AuthRepository
import com.example.sportyconnect.utils.Resource
import com.example.sportyconnect.utils.TokenManager
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AuthRepository()
    private val tokenManager = TokenManager(application)
    
    private val _loginState = MutableLiveData<Resource<AuthResponse>>()
    val loginState: LiveData<Resource<AuthResponse>> = _loginState
    
    private val _registerState = MutableLiveData<Resource<Any>>()
    val registerState: LiveData<Resource<Any>> = _registerState
    
    fun login(email: String, password: String) {
        _loginState.value = Resource.Loading()
        
        viewModelScope.launch {
            val result = repository.login(email, password)
            _loginState.value = result
            
            if (result is Resource.Success) {
                result.data?.let { authResponse ->
                    tokenManager.saveToken(authResponse.accessToken)
                    tokenManager.saveUserInfo(authResponse.user.id, authResponse.user.role)
                }
            }
        }
    }
    
    fun register(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        role: String
    ) {
        _registerState.value = Resource.Loading()
        
        viewModelScope.launch {
            val result = repository.register(nom, prenom, email, password, role)
            _registerState.value = result
        }
    }
    
    fun verifyEmail(userId: String, code: String) {
        _loginState.value = Resource.Loading()
        
        viewModelScope.launch {
            val result = repository.verifyEmail(userId, code)
            _loginState.value = result
            
            if (result is Resource.Success) {
                result.data?.let { authResponse ->
                    tokenManager.saveToken(authResponse.accessToken)
                    tokenManager.saveUserInfo(authResponse.user.id, authResponse.user.role)
                }
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
        }
    }
}
```

### OffersViewModel.kt

```kotlin
package com.example.sportyconnect.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sportyconnect.models.CreateOfferRequest
import com.example.sportyconnect.models.Offer
import com.example.sportyconnect.models.UpdateOfferRequest
import com.example.sportyconnect.repository.OffersRepository
import com.example.sportyconnect.utils.Resource
import kotlinx.coroutines.launch

class OffersViewModel : ViewModel() {
    
    private val repository = OffersRepository()
    
    private val _offers = MutableLiveData<Resource<List<Offer>>>()
    val offers: LiveData<Resource<List<Offer>>> = _offers
    
    private val _createOfferState = MutableLiveData<Resource<Offer>>()
    val createOfferState: LiveData<Resource<Offer>> = _createOfferState
    
    fun loadOffers(isActive: Boolean? = null) {
        _offers.value = Resource.Loading()
        
        viewModelScope.launch {
            val result = repository.getOffers(isActive)
            _offers.value = result
        }
    }
    
    fun createOffer(
        name: String,
        description: String,
        type: String,
        durationDays: Int,
        price: Double,
        discountPct: Double = 0.0
    ) {
        _createOfferState.value = Resource.Loading()
        
        viewModelScope.launch {
            val request = CreateOfferRequest(
                name = name,
                description = description,
                type = type,
                durationDays = durationDays,
                price = price,
                discountPct = discountPct
            )
            
            val result = repository.createOffer(request)
            _createOfferState.value = result
        }
    }
    
    fun updateOffer(id: String, price: Double? = null, isActive: Boolean? = null) {
        viewModelScope.launch {
            val updates = UpdateOfferRequest(price = price, isActive = isActive)
            repository.updateOffer(id, updates)
            // Recharger les offres après la mise à jour
            loadOffers()
        }
    }
    
    fun deleteOffer(id: String) {
        viewModelScope.launch {
            repository.deleteOffer(id)
            // Recharger les offres après la suppression
            loadOffers()
        }
    }
}
```

---

## 🎨 Exemples d'Utilisation dans une Activity/Fragment

### LoginActivity.kt

```kotlin
package com.example.sportyconnect.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.sportyconnect.databinding.ActivityLoginBinding
import com.example.sportyconnect.ui.main.MainActivity
import com.example.sportyconnect.utils.Resource
import com.example.sportyconnect.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupObservers()
        setupListeners()
    }
    
    private fun setupObservers() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                    
                    // Rediriger vers MainActivity
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    
                    Toast.makeText(this, resource.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            
            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }
        
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email requis"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = "Email invalide"
            return false
        }
        
        if (password.isEmpty()) {
            binding.etPassword.error = "Mot de passe requis"
            return false
        }
        
        if (password.length < 6) {
            binding.etPassword.error = "Minimum 6 caractères"
            return false
        }
        
        return true
    }
}
```

### OffersFragment.kt

```kotlin
package com.example.sportyconnect.ui.offers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sportyconnect.databinding.FragmentOffersBinding
import com.example.sportyconnect.ui.adapters.OffersAdapter
import com.example.sportyconnect.utils.Resource
import com.example.sportyconnect.viewmodel.OffersViewModel

class OffersFragment : Fragment() {
    
    private var _binding: FragmentOffersBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: OffersViewModel by viewModels()
    private lateinit var adapter: OffersAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOffersBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        
        // Charger les offres actives
        viewModel.loadOffers(isActive = true)
    }
    
    private fun setupRecyclerView() {
        adapter = OffersAdapter(
            onItemClick = { offer ->
                // Naviguer vers les détails de l'offre
                // ou ouvrir un dialog pour s'abonner
            }
        )
        
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@OffersFragment.adapter
        }
    }
    
    private fun setupObservers() {
        viewModel.offers.observe(viewLifecycleOwner) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                }
                is Resource.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    
                    resource.data?.let { offers ->
                        adapter.submitList(offers)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    
                    Toast.makeText(
                        requireContext(),
                        resource.message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
```

### OffersAdapter.kt

```kotlin
package com.example.sportyconnect.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sportyconnect.databinding.ItemOfferBinding
import com.example.sportyconnect.models.Offer

class OffersAdapter(
    private val onItemClick: (Offer) -> Unit
) : ListAdapter<Offer, OffersAdapter.OfferViewHolder>(OfferDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferViewHolder {
        val binding = ItemOfferBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OfferViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: OfferViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class OfferViewHolder(
        private val binding: ItemOfferBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(offer: Offer) {
            binding.apply {
                tvOfferName.text = offer.name
                tvOfferDescription.text = offer.description
                tvOfferPrice.text = "${offer.price} ${offer.currency}"
                tvOfferDuration.text = "${offer.durationDays} jours"
                
                if (offer.discountPct > 0) {
                    tvDiscount.visibility = View.VISIBLE
                    tvDiscount.text = "-${offer.discountPct.toInt()}%"
                } else {
                    tvDiscount.visibility = View.GONE
                }
                
                root.setOnClickListener {
                    onItemClick(offer)
                }
            }
        }
    }
    
    class OfferDiffCallback : DiffUtil.ItemCallback<Offer>() {
        override fun areItemsTheSame(oldItem: Offer, newItem: Offer): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Offer, newItem: Offer): Boolean {
            return oldItem == newItem
        }
    }
}
```

---

## ⚠️ Gestion des Erreurs

### ApiErrorHandler.kt

```kotlin
package com.example.sportyconnect.utils

import retrofit2.Response

object ApiErrorHandler {
    
    fun <T> handleError(response: Response<T>): String {
        return when (response.code()) {
            400 -> "Données invalides"
            401 -> "Session expirée. Veuillez vous reconnecter"
            403 -> "Accès refusé"
            404 -> "Ressource introuvable"
            409 -> "Cette ressource existe déjà"
            500 -> "Erreur serveur. Réessayez plus tard"
            else -> response.message() ?: "Une erreur est survenue"
        }
    }
}
```

---

## 📝 Notes Importantes

### 1. Configuration IP pour Android

- **Émulateur Android**: Utilisez `10.0.2.2` au lieu de `localhost`
- **Device physique**: Utilisez l'IP locale de votre machine (ex: `192.168.1.10`)

### 2. Permissions AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application
    android:usesCleartextTraffic="true"
    ...>
```

### 3. ProGuard Rules (pour release)

```proguard
# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.example.sportyconnect.models.** { *; }
```

---

**Dernière mise à jour**: Janvier 2024  
**Version API**: 1.0
