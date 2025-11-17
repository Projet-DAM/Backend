# 💻 Exemples de Code Android - Fonctionnalités Avancées

Ce fichier contient des exemples de code pour implémenter les fonctionnalités avancées de l'API dans votre application Android.

---

## 📦 Modèles de données supplémentaires

### Activity.kt

```kotlin
package com.sportyconnect.kids.models

import com.google.gson.annotations.SerializedName

enum class ActivityStatus {
    ACTIVE,
    ANNULEE,
    TERMINEE,
    BROUILLON
}

data class Activity(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nom_activite") val nomActivite: String,
    val description: String? = null,
    val categorie: String? = null,
    val date: String? = null, // Format: YYYY-MM-DD
    val heure: String? = null, // Format: HH:mm
    val duree: Int? = null, // en minutes
    @SerializedName("capacite_max") val capaciteMax: Int? = null,
    val prix: Double? = null,
    val statut: ActivityStatus? = null,
    val coach: String? = null, // ID du coach
    val academie: String? = null, // ID de l'académie
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateActivityRequest(
    @SerializedName("nom_activite") val nomActivite: String,
    val description: String? = null,
    val categorie: String? = null,
    val date: String? = null,
    val heure: String? = null,
    val duree: Int? = null,
    @SerializedName("capacite_max") val capaciteMax: Int? = null,
    val prix: Double? = null,
    val statut: ActivityStatus? = null
)

data class UpdateActivityRequest(
    @SerializedName("nom_activite") val nomActivite: String? = null,
    val description: String? = null,
    val categorie: String? = null,
    val date: String? = null,
    val heure: String? = null,
    val duree: Int? = null,
    @SerializedName("capacite_max") val capaciteMax: Int? = null,
    val prix: Double? = null,
    val statut: ActivityStatus? = null
)

data class QueryActivityParams(
    val categorie: String? = null,
    val date: String? = null,
    val coach: String? = null,
    val academie: String? = null,
    val statut: String? = null,
    val page: Int? = null,
    val limit: Int? = null,
    @SerializedName("sortBy") val sortBy: String? = null,
    val order: String? = null // "asc" or "desc"
)

data class PaginatedActivitiesResponse(
    val data: List<Activity>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int
)
```

---

## 🔌 Extension de ApiService pour les activités

```kotlin
// Dans ApiService.kt, ajoutez ces méthodes :

@GET("activities")
suspend fun getActivities(
    @Header("Authorization") token: String,
    @QueryMap params: Map<String, String>
): Response<PaginatedActivitiesResponse>

@GET("activities/{id}")
suspend fun getActivityById(
    @Header("Authorization") token: String,
    @Path("id") id: String
): Response<Activity>

@POST("activities")
suspend fun createActivity(
    @Header("Authorization") token: String,
    @Body request: CreateActivityRequest
): Response<Activity>

@PATCH("activities/{id}")
suspend fun updateActivity(
    @Header("Authorization") token: String,
    @Path("id") id: String,
    @Body request: UpdateActivityRequest
): Response<Activity>

@DELETE("activities/{id}")
suspend fun deleteActivity(
    @Header("Authorization") token: String,
    @Path("id") id: String
): Response<Unit>
```

---

## 🏗️ Repository pour les activités

```kotlin
package com.sportyconnect.kids.repository

import android.content.Context
import com.sportyconnect.kids.api.RetrofitClient
import com.sportyconnect.kids.models.*
import com.sportyconnect.kids.utils.TokenManager
import kotlinx.coroutines.flow.first

class ActivityRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.apiService
    private val tokenManager = TokenManager(context)
    
    private suspend fun getAuthToken(): String {
        val token = tokenManager.getToken().first()
        return "Bearer $token"
    }
    
    suspend fun getActivities(
        categorie: String? = null,
        date: String? = null,
        coach: String? = null,
        academie: String? = null,
        statut: String? = null,
        page: Int? = null,
        limit: Int? = null
    ): Result<PaginatedActivitiesResponse> {
        return try {
            val token = getAuthToken()
            val params = mutableMapOf<String, String>()
            
            categorie?.let { params["categorie"] = it }
            date?.let { params["date"] = it }
            coach?.let { params["coach"] = it }
            academie?.let { params["academie"] = it }
            statut?.let { params["statut"] = it }
            page?.let { params["page"] = it.toString() }
            limit?.let { params["limit"] = it.toString() }
            
            val response = apiService.getActivities(token, params)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getActivityById(id: String): Result<Activity> {
        return try {
            val token = getAuthToken()
            val response = apiService.getActivityById(token, id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createActivity(request: CreateActivityRequest): Result<Activity> {
        return try {
            val token = getAuthToken()
            val response = apiService.createActivity(token, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateActivity(id: String, request: UpdateActivityRequest): Result<Activity> {
        return try {
            val token = getAuthToken()
            val response = apiService.updateActivity(token, id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteActivity(id: String): Result<Unit> {
        return try {
            val token = getAuthToken()
            val response = apiService.deleteActivity(token, id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 👤 Repository pour les utilisateurs (extension)

```kotlin
package com.sportyconnect.kids.repository

import android.content.Context
import com.sportyconnect.kids.api.RetrofitClient
import com.sportyconnect.kids.models.*
import com.sportyconnect.kids.utils.TokenManager
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class UserRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.apiService
    private val tokenManager = TokenManager(context)
    
    private suspend fun getAuthToken(): String {
        val token = tokenManager.getToken().first()
        return "Bearer $token"
    }
    
    suspend fun getUserById(id: String): Result<User> {
        return try {
            val token = getAuthToken()
            val response = apiService.getUserById(token, id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(id: String, request: UpdateUserRequest): Result<User> {
        return try {
            val token = getAuthToken()
            val response = apiService.updateUser(token, id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadPhoto(id: String, imageFile: File): Result<User> {
        return try {
            val token = getAuthToken()
            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("photo", imageFile.name, requestFile)
            
            val response = apiService.uploadPhoto(token, id, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun linkChild(parentId: String, childId: String): Result<User> {
        return try {
            val token = getAuthToken()
            val response = apiService.linkChild(token, parentId, LinkChildRequest(childId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getChildren(parentId: String): Result<List<User>> {
        return try {
            val token = getAuthToken()
            val response = apiService.getChildren(token, parentId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erreur: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## 🎨 ViewModel avec LiveData

```kotlin
package com.sportyconnect.kids.ui.activities

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sportyconnect.kids.models.Activity
import com.sportyconnect.kids.models.PaginatedActivitiesResponse
import com.sportyconnect.kids.repository.ActivityRepository
import kotlinx.coroutines.launch

class ActivitiesViewModel(private val activityRepository: ActivityRepository) : ViewModel() {
    
    private val _activities = MutableLiveData<List<Activity>>()
    val activities: LiveData<List<Activity>> = _activities
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    fun loadActivities(
        categorie: String? = null,
        date: String? = null,
        page: Int = 1,
        limit: Int = 10
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = activityRepository.getActivities(
                categorie = categorie,
                date = date,
                page = page,
                limit = limit
            )
            
            result.onSuccess {
                _activities.value = it.data
                _isLoading.value = false
            }.onFailure {
                _error.value = it.message
                _isLoading.value = false
            }
        }
    }
    
    fun createActivity(request: CreateActivityRequest) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = activityRepository.createActivity(request)
            
            result.onSuccess {
                // Recharger la liste
                loadActivities()
            }.onFailure {
                _error.value = it.message
                _isLoading.value = false
            }
        }
    }
}
```

---

## 📱 Exemple d'Activity avec RecyclerView

```kotlin
package com.sportyconnect.kids.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.sportyconnect.kids.R
import com.sportyconnect.kids.databinding.ActivityActivitiesBinding
import com.sportyconnect.kids.models.Activity
import com.sportyconnect.kids.repository.ActivityRepository

class ActivitiesActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityActivitiesBinding
    private val viewModel: ActivitiesViewModel by viewModels {
        ActivitiesViewModelFactory(ActivityRepository(this))
    }
    private lateinit var adapter: ActivitiesAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupRecyclerView()
        setupObservers()
        
        // Charger les activités
        viewModel.loadActivities()
        
        // Refresh
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadActivities()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = ActivitiesAdapter { activity ->
            // Naviguer vers les détails
            // startActivity(Intent(this, ActivityDetailActivity::class.java).apply {
            //     putExtra("activity_id", activity.id)
            // })
        }
        
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }
    
    private fun setupObservers() {
        viewModel.activities.observe(this, Observer { activities ->
            adapter.submitList(activities)
            binding.swipeRefresh.isRefreshing = false
        })
        
        viewModel.isLoading.observe(this, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })
        
        viewModel.error.observe(this, Observer { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }
}

// Adapter pour RecyclerView
class ActivitiesAdapter(
    private val onItemClick: (Activity) -> Unit
) : RecyclerView.Adapter<ActivitiesAdapter.ActivityViewHolder>() {
    
    private var activities = listOf<Activity>()
    
    fun submitList(newList: List<Activity>) {
        activities = newList
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_activity, parent, false)
        return ActivityViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(activities[position])
    }
    
    override fun getItemCount() = activities.size
    
    inner class ActivityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(activity: Activity) {
            itemView.findViewById<TextView>(R.id.tvActivityName).text = activity.nomActivite
            itemView.findViewById<TextView>(R.id.tvCategory).text = activity.categorie
            itemView.findViewById<TextView>(R.id.tvDate).text = activity.date
            itemView.findViewById<TextView>(R.id.tvTime).text = activity.heure
            itemView.findViewById<TextView>(R.id.tvPrice).text = "${activity.prix}€"
            
            itemView.setOnClickListener {
                onItemClick(activity)
            }
        }
    }
}

// ViewModelFactory
class ActivitiesViewModelFactory(
    private val activityRepository: ActivityRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivitiesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivitiesViewModel(activityRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

## 🖼️ Gestion des images avec Glide

```kotlin
// Dans votre Activity/Fragment
import com.bumptech.glide.Glide
import com.sportyconnect.kids.config.ApiConfig

// Charger une image de profil
fun loadProfileImage(imageView: ImageView, photoUrl: String?) {
    if (photoUrl != null) {
        val fullUrl = if (photoUrl.startsWith("http")) {
            photoUrl
        } else {
            "${ApiConfig.BASE_URL}${photoUrl.removePrefix("/")}"
        }
        
        Glide.with(this)
            .load(fullUrl)
            .placeholder(R.drawable.placeholder_profile)
            .error(R.drawable.error_profile)
            .circleCrop()
            .into(imageView)
    } else {
        imageView.setImageResource(R.drawable.placeholder_profile)
    }
}
```

---

## 🔐 Interceptor pour ajouter automatiquement le token

```kotlin
package com.sportyconnect.kids.api

import android.content.Context
import com.sportyconnect.kids.utils.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    
    private val tokenManager = TokenManager(context)
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Vérifier si c'est une requête d'authentification (pas besoin de token)
        if (originalRequest.url.encodedPath.contains("/auth/")) {
            return chain.proceed(originalRequest)
        }
        
        // Ajouter le token pour les autres requêtes
        val token = runBlocking {
            tokenManager.getToken().first()
        }
        
        val newRequest = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }
        
        return chain.proceed(newRequest)
    }
}

// Modifier RetrofitClient.kt pour utiliser l'interceptor
object RetrofitClient {
    
    private fun createOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authInterceptor = AuthInterceptor(context)
        
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun create(context: Context): ApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        return retrofit.create(ApiService::class.java)
    }
}
```

---

## 📝 Utilisation de l'interceptor (simplifie les appels)

Avec l'interceptor, vous n'avez plus besoin de passer le token manuellement :

```kotlin
// Avant (sans interceptor)
suspend fun getUserById(id: String): Result<User> {
    val token = getAuthToken()
    val response = apiService.getUserById(token, id)
    // ...
}

// Après (avec interceptor)
suspend fun getUserById(id: String): Result<User> {
    val response = apiService.getUserById(id) // Pas besoin de token !
    // ...
}

// Et dans ApiService.kt, retirez le paramètre @Header("Authorization")
@GET("users/{id}")
suspend fun getUserById(@Path("id") id: String): Response<User>
```

---

## 🎯 Bonnes pratiques

1. **Gestion d'erreurs** : Créez une classe `ApiException` pour gérer les erreurs HTTP
2. **Loading states** : Utilisez `LiveData<Boolean>` pour gérer les états de chargement
3. **Cache** : Utilisez Room ou DataStore pour mettre en cache les données
4. **Retry logic** : Implémentez une logique de retry pour les requêtes échouées
5. **Pagination** : Gèrez la pagination pour les listes longues
6. **Refresh token** : Implémentez le refresh token si votre backend le supporte

---

Bon développement ! 🚀


