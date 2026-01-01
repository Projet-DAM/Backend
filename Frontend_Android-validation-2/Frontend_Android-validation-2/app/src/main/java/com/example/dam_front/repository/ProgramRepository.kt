package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.CreateProgramRequest
import com.example.dam_front.models.ManageProgramActivitiesRequest
import com.example.dam_front.models.Program
import com.example.dam_front.models.UpdateProgramRequest

import java.io.File
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProgramRepository(context: Context) {

    private val apiService = RetrofitClient.getApiService(context)
    private val prefs = context.getSharedPreferences("programs_cache", Context.MODE_PRIVATE)
    private val gson = com.google.gson.Gson()

    suspend fun getPrograms(
        page: Int? = null,
        limit: Int = 100,
        statut: String? = null,
        coach: String? = null,
        academie: String? = null,
        nom: String? = null,
        sortBy: String? = null,
        order: String? = null
    ): Result<List<Program>> {
        return try {
            val response = apiService.getPrograms(
                page = page,
                limit = limit,
                statut = statut,
                coach = coach,
                academie = academie,
                nom = nom,
                sortBy = sortBy,
                order = order
            )
            if (response.isSuccessful && response.body() != null) {
                val programs = response.body()!!.getProgramList()
                // Cache for widget
                if (coach != null) {
                    saveProgramsToCache(programs)
                }
                Result.success(programs)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération des programmes"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveProgramsToCache(programs: List<Program>) {
        try {
            val json = gson.toJson(programs)
            prefs.edit().putString("coach_programs", json).apply()
        } catch (e: Exception) {
            android.util.Log.e("ProgramRepo", "Cache write failed", e)
        }
    }

    fun getProgramsFromCache(): List<Program> {
        return try {
            val json = prefs.getString("coach_programs", null) ?: return emptyList()
            val type = object : com.google.gson.reflect.TypeToken<List<Program>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getProgramById(id: String): Result<Program> {
        return try {
            val response = apiService.getProgramById(id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération du programme"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProgram(request: CreateProgramRequest): Result<Program> {
        return try {
            val response = apiService.createProgram(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création du programme"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createProgramWithImage(request: CreateProgramRequest, imageFile: File?): Result<Program> {
        return try {
            val partMap = mutableMapOf<String, RequestBody>()
            partMap["nom_programme"] = request.nomProgramme.toRequestBody("text/plain".toMediaTypeOrNull())
            request.description?.let { partMap["description"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
            request.objectif?.let { partMap["objectif"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
            request.niveau?.let { partMap["niveau"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
            request.prix?.let { partMap["prix"] = it.toString().toRequestBody("text/plain".toMediaTypeOrNull()) }
            request.statut?.let { partMap["statut"] = it.toRequestBody("text/plain".toMediaTypeOrNull()) }
            
            if (request.activites.isNotEmpty()) {
                val activitiesString = request.activites.joinToString(",")
                partMap["activites"] = activitiesString.toRequestBody("text/plain".toMediaTypeOrNull())
            }


            val imagePart = imageFile?.let {
                // Determine MIME type based on file extension
                val mimeType = when (it.extension.lowercase()) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/jpeg" // Default to JPEG
                }
                val requestFile = it.asRequestBody(mimeType.toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", it.name, requestFile)
            }


            val response = apiService.createProgramWithImage(partMap, imagePart)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création du programme"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgram(id: String, request: UpdateProgramRequest): Result<Program> {
        return try {
            val response = apiService.updateProgram(id, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour du programme"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProgramActivities(id: String, activityIds: List<String>): Result<Program> {
        return try {
            val response = apiService.updateProgramActivities(id, ManageProgramActivitiesRequest(activityIds))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour des activités du programme"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProgram(id: String): Result<Unit> {
        return try {
            val response = apiService.deleteProgram(id)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la suppression du programme"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProgramEnrollments(programId: String): Result<List<com.example.dam_front.models.ProgramEnrollment>> {
        return try {
            val response = apiService.getProgramEnrollments(programId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération des inscriptions"
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


