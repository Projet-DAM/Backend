package com.example.dam_front.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.dam_front.api.ApiService
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.models.CreateChildRequest
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ChildRepository(context: Context) {
    // Authenticated Service is crucial!
    private val apiService: ApiService = RetrofitClient.createAuthenticatedApiService(context)
    private val tokenManager = TokenManager(context)
    private val appContext = context.applicationContext // Use app context to avoid leaks
    private val TAG = "ChildRepository"

    suspend fun createChild(request: CreateChildRequest, imageUri: Uri? = null): Result<ChildResponse> {
        return try {
            val userId = tokenManager.getUserId().first()
            Log.d(TAG, "Retrieved userId for creation: '$userId'")
            
            if (userId.isNullOrBlank()) {
                Log.e(TAG, "❌ UserId non disponible ou vide")
                return Result.failure(Exception("Utilisateur non connecté (ID manquant)"))
            }
            
            Log.d(TAG, "Creating child for parent $userId, imageUri: $imageUri")
            
            val response = if (imageUri != null) {
                // Créer avec image (multipart)
                val prenomBody = request.prenom.toRequestBody("text/plain".toMediaTypeOrNull())
                val nomBody = request.nom.toRequestBody("text/plain".toMediaTypeOrNull())
                val dateNaissanceBody = request.dateNaissance.toRequestBody("text/plain".toMediaTypeOrNull())
                // Sport pratique handled separately or default
                val sport = "Non précisé" 
                val sportPratiqueBody = sport.toRequestBody("text/plain".toMediaTypeOrNull())
                
                // Log content resolver check
                Log.d(TAG, "Processing image....")
                
                // Créer le MultipartBody.Part pour l'image
                val imagePart = try {
                    val inputStream = appContext.contentResolver.openInputStream(imageUri)
                    if (inputStream == null) {
                        Log.e(TAG, "InputStream is null")
                        null
                    } else {
                        val file = File.createTempFile("child_image", ".jpg", appContext.cacheDir)
                        inputStream.use { it.copyTo(file.outputStream()) }
                        Log.d(TAG, "Temp file created: ${file.absolutePath}, size: ${file.length()}")
                        
                        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                        // Use "image" as the form field name to match the API parameter name likely
                        MultipartBody.Part.createFormData("image", file.name, requestFile)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors de la création du fichier image", e)
                    null
                }
                
                // Check correct part name in ApiService
                // ApiService: suspend fun createChildWithImage(@Part image: MultipartBody.Part?)
                // Usually backend expects a specific field name.
                // Assuming "file" or "image". Let's use "file" as it's common, or check existing backend code if possible.
                // User didn't give backend code.
                // Wait, in Step 519 ApiService view:
                // @Part image: MultipartBody.Part?
                // It doesn't have @Part("name").
                // Let's assume the Part name should be "file" or "photoProfil".
                // I will use "file" to be safe, or "image".
                
                apiService.createChildWithImage(
                    parentId = userId,
                    prenom = prenomBody,
                    nom = nomBody,
                    dateNaissance = dateNaissanceBody,
                    sportPratique = sportPratiqueBody,
                    image = imagePart
                )
            } else {
                // Créer sans image (JSON)
                apiService.createChild(userId, request)
            }
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Enfant créé avec succès: ${response.body()!!.prenom}")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création de l'enfant"
                val errorCode = response.code()
                Log.e(TAG, "❌ Erreur création enfant: $errorCode - $errorMessage")
                
                // Message d'erreur spécifique pour 401
                val finalErrorMessage = if (errorCode == 401) {
                    "Session expirée (401). Déconnexion..."
                } else {
                    "Erreur: $errorCode - $errorMessage"
                }
                Result.failure(Exception(finalErrorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la création de l'enfant", e)
            Result.failure(e)
        }
    }

    suspend fun getChildren(): Result<List<ChildResponse>> {
        return try {
            val userId = tokenManager.getUserId().first()
            if (userId == null) return Result.failure(Exception("Not logged in"))
            val response = apiService.getChildren(userId)
            if (response.isSuccessful && response.body() != null) {
                 Result.success(response.body()!!)
            } else {
                 Result.failure(Exception("Error ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
