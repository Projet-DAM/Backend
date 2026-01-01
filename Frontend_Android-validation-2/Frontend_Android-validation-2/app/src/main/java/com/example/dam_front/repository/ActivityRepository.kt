package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.Activity
import com.example.dam_front.models.CreateActivityRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ActivityRepository(private val context: Context) {
    
    private val apiService = RetrofitClient.getApiService(context)
    suspend fun getActivities(
        page: Int? = null,
        limit: Int = 100,
        categorie: String? = null,
        coach: String? = null,
        academie: String? = null,
        programmeId: String? = null,
        statut: String? = null,
        date: String? = null,
        sortBy: String? = null,
        order: String? = null
    ): Result<List<Activity>> {
        return try {
            val response = apiService.getActivities(
                page = page,
                limit = limit,
                categorie = categorie,
                coach = coach,
                academie = academie,
                programmeId = programmeId,
                statut = statut,
                date = date,
                sortBy = sortBy,
                order = order
            )
            if (response.isSuccessful && response.body() != null) {
                val activitiesList = response.body()!!.getActivitiesList()
                Result.success(activitiesList)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération des activités"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Erreur lors du parsing: ${e.message}", e))
        }
    }
    
    suspend fun getActivityById(id: String): Result<Activity> {
        return try {
            val response = apiService.getActivityById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération de l'activité"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserById(userId: String): Result<com.example.dam_front.models.User> {
        return try {
            val response = apiService.getUserById(userId)
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
    
    suspend fun deleteActivity(id: String): Result<Unit> {
        return try {
            val response = apiService.deleteActivity(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la suppression de l'activité"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createActivity(request: CreateActivityRequest): Result<Activity> {
        return try {
            val response = apiService.createActivity(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création de l'activité"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateActivity(id: String, request: CreateActivityRequest): Result<Activity> {
        return try {
            println("DEBUG Repository: updateActivity appelé avec id: $id")
            println("DEBUG Repository: request: $request")
            
            // Essayer d'abord avec PATCH (plus commun pour les mises à jour)
            var response = apiService.updateActivityPatch(id, request)
            println("DEBUG Repository: PATCH Response code: ${response.code()}")
            println("DEBUG Repository: PATCH Response isSuccessful: ${response.isSuccessful}")
            
            // Si PATCH échoue avec 404, essayer PUT
            if (!response.isSuccessful && response.code() == 404) {
                println("DEBUG Repository: PATCH a échoué, essai avec PUT")
                response = apiService.updateActivity(id, request)
                println("DEBUG Repository: PUT Response code: ${response.code()}")
                println("DEBUG Repository: PUT Response isSuccessful: ${response.isSuccessful}")
            }
            
            if (response.isSuccessful && response.body() != null) {
                println("DEBUG Repository: Mise à jour réussie")
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                println("DEBUG Repository: Erreur - code: ${response.code()}, body: $errorBody")
                val errorMessage = errorBody ?: "Erreur lors de la mise à jour de l'activité"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            println("DEBUG Repository: Exception lors de la mise à jour: ${e.message}")
            println("DEBUG Repository: Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }
    suspend fun createActivityWithImage(request: CreateActivityRequest, imageFile: File?): Result<Activity> {
        return try {
            val partMap = request.toPartMap()
            val imagePart = imageFile?.let {
                // Detect MIME type based on file extension
                val mimeType = when (it.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    else -> "image/*"
                }
                println("DEBUG Repository: Using MIME type: $mimeType for file: ${it.name}")
                
                val requestFile = it.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", it.name, requestFile)
            }
            
            val response = apiService.createActivityWithImage(partMap, imagePart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création de l'activité"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateActivityWithImage(id: String, request: CreateActivityRequest, imageFile: File?): Result<Activity> {
        return try {
            println("DEBUG Repository: updateActivityWithImage appelé")
            println("DEBUG Repository: Activity ID: $id")
            println("DEBUG Repository: Request: $request")
            
            val partMap = request.toPartMap()
            println("DEBUG Repository: PartMap créé avec ${partMap.size} champs")
            
            val imagePart = imageFile?.let {
                println("DEBUG Repository: Création de l'image part")
                println("DEBUG Repository: Nom du fichier: ${it.name}")
                println("DEBUG Repository: Taille du fichier: ${it.length()} bytes")
                println("DEBUG Repository: Chemin: ${it.absolutePath}")
                println("DEBUG Repository: Existe: ${it.exists()}")
                
                // Detect MIME type based on file extension
                val mimeType = when (it.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    else -> "image/*"
                }
                println("DEBUG Repository: Using MIME type: $mimeType for file: ${it.name}")
                
                val requestFile = it.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", it.name, requestFile)
            }
            
            if (imagePart != null) {
                println("DEBUG Repository: Image part créée avec succès")
            } else {
                println("DEBUG Repository: ATTENTION - Image part est null")
            }
            
            // Essayer d'abord avec PATCH
            var response = apiService.updateActivityWithImage(id, partMap, imagePart)
            println("DEBUG Repository: PATCH Response code: ${response.code()}")
            println("DEBUG Repository: PATCH Response isSuccessful: ${response.isSuccessful}")
            
            // Si PATCH échoue avec 404 ou 405, essayer PUT
            if (!response.isSuccessful && (response.code() == 404 || response.code() == 405)) {
                println("DEBUG Repository: PATCH a échoué (${response.code()}), essai avec PUT")
                response = apiService.updateActivityWithImagePut(id, partMap, imagePart)
                println("DEBUG Repository: PUT Response code: ${response.code()}")
                println("DEBUG Repository: PUT Response isSuccessful: ${response.isSuccessful}")
            }
            
            if (response.isSuccessful && response.body() != null) {
                val updatedActivity = response.body()!!
                println("DEBUG Repository: Activité mise à jour avec succès")
                println("DEBUG Repository: Nouvelle image path: ${updatedActivity.image}")
                Result.success(updatedActivity)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour de l'activité"
                println("DEBUG Repository: Erreur - code: ${response.code()}, message: $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            println("DEBUG Repository: Exception lors de la mise à jour avec image: ${e.message}")
            println("DEBUG Repository: Stack trace: ${e.stackTraceToString()}")
            Result.failure(e)
        }
    }

    private fun CreateActivityRequest.toPartMap(): Map<String, RequestBody> {
        val map = mutableMapOf<String, RequestBody>()
        val textType = "text/plain".toMediaTypeOrNull()
        
        map["nom_activite"] = nomActivite.toRequestBody(textType)
        description?.let { map["description"] = it.toRequestBody(textType) }
        categorie?.let { map["categorie"] = it.toRequestBody(textType) }
        date?.let { map["date"] = it.toRequestBody(textType) }
        heure?.let { map["heure"] = it.toRequestBody(textType) }
        duree?.let { map["duree"] = it.toString().toRequestBody(textType) }
        capaciteMax?.let { map["capacite_max"] = it.toString().toRequestBody(textType) }
        prix?.let { map["prix"] = it.toString().toRequestBody(textType) }
        statut?.let { map["statut"] = it.toRequestBody(textType) }
        academie?.let { map["academie"] = it.toRequestBody(textType) }
        academie?.let { map["academie"] = it.toRequestBody(textType) }
        coach?.let { map["coach"] = it.toRequestBody(textType) }
        image?.let { map["image"] = it.toRequestBody(textType) }
        
        return map
    }
}

