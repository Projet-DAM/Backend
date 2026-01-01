package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.ApiService
import com.example.dam_front.models.User
import com.example.dam_front.models.CreateChildRequest
import com.example.dam_front.models.CreateChildResponse
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first

/**
 * Repository spécifique pour les fonctionnalités de suivi d'enfants
 * Utilisé par ParentHomeViewModel
 */
class SuiviAuthRepository(private val context: Context) {
    
    // CORRECTION: Utiliser le service authentifié !!
    private val apiService: ApiService = RetrofitClient.createAuthenticatedService(context, ApiService::class.java)
    private val tokenManager = TokenManager(context)
    
    suspend fun getUserId(): String? {
        return try {
            tokenManager.getUserId().first()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getUser(userId: String): User {
        return apiService.getUser(userId)
    }
    
    suspend fun getChildren(parentId: String): List<User> {
        return try {
            val response = apiService.getMyChildren()
            if (response.isSuccessful && response.body() != null) {
                val childrenList = response.body()!!
                childrenList.map { child ->
                    User(
                        id = child.id,
                        prenom = child.prenom,
                        nom = child.nom,
                        email = "${child.prenom.lowercase()}.${child.nom.lowercase()}@enfant.local",
                        role = "ENFANT",
                        photoProfil = child.photoProfil
                    )
                }
            } else {
                android.util.Log.e("SuiviAuthRepository", "Failed to get children: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("SuiviAuthRepository", "Exception getting children", e)
            emptyList()
        }
    }
    
    suspend fun createChild(parentId: String, dto: CreateChildRequest): Result<CreateChildResponse> {
        return try {
            val response = apiService.createChild(dto)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création de l'enfant"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
