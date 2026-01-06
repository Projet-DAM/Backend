package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.Child
import com.example.dam_front.models.CreateChildRequest
import com.example.dam_front.models.User


import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import kotlinx.coroutines.flow.first
import com.example.dam_front.utils.TokenManager

class UserRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.getApiService(context)
    
    suspend fun getUserById(id: String): Result<User> {
        return try {
            val response = apiService.getUserById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération de l'utilisateur"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChildren(): Result<List<Child>> {
        return try {
            val response = apiService.getMyChildren()
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération des enfants"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createChild(request: CreateChildRequest): Result<Child> {
        return try {
            val response = apiService.createChild(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.child)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création de l'enfant"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateChild(childId: String, updates: Map<String, Any?>): Result<User> {
        return try {
            // Filter out null values to avoid sending them to backend
            val cleanUpdates = updates.filterValues { it != null } as Map<String, @JvmSuppressWildcards Any>
            
            val response = apiService.updateUser(childId, cleanUpdates)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour de l'enfant"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadChildPhoto(childId: String, file: java.io.File): Result<String> {
        return try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = okhttp3.MultipartBody.Part.createFormData("photo", file.name, requestFile)
            
            val response = apiService.uploadUserPhoto(childId, body)
            if (response.isSuccessful && response.body() != null) {
                val photoUrl = response.body()!!["photoProfil"] as? String
                if (photoUrl != null) {
                    Result.success(photoUrl)
                } else {
                    Result.failure(Exception("URL de la photo non reçue"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de l'upload de la photo"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(userId: String, token: String): Result<User> {
        return try {
            val request = mapOf("fcmToken" to token)
            val response = apiService.updateFcmToken(userId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour du token FCM"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getCoaches(): Result<List<User>> {
        return try {
            val coaches = apiService.getUsersByRole("coach")
            Result.success(coaches)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

