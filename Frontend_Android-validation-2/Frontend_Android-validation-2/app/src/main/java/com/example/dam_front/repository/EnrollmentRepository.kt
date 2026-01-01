package com.example.dam_front.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.CreateEnrollmentRequest
import com.example.dam_front.models.Enrollment
import com.example.dam_front.models.EnrollmentResponse
import com.example.dam_front.models.EnrollmentResultResponse
import com.example.dam_front.models.EnrollmentStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

import com.example.dam_front.email.EmailRetrofitClient
import com.example.dam_front.email.EmailRequest
import com.example.dam_front.email.PaymentParams
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.appwidget.updateAll
import com.example.dam_front.widget.SportyWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay

class EnrollmentRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("enrollments", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val apiService = RetrofitClient.getApiService(context)
    private val tokenManager = TokenManager(context)
    
    /**
     * Enroll children in a program via Backend API
     */
    suspend fun enrollChildren(
        programId: String,
        programName: String,
        childIds: List<String>,
        childNames: Map<String, String>,
        amountPerChild: Double
    ): Result<List<Enrollment>> {
        return try {
            val request = CreateEnrollmentRequest(programId, childIds)
            val response = apiService.createEnrollments(request)
            
            if (response.isSuccessful && response.body() != null) {
                val result = response.body()!!
                if (result.success) {
                    // Map backend response to local model for UI consistency
                    val newEnrollments = result.enrollments.map { backendEnrollment: EnrollmentResponse ->
                        Enrollment(
                            id = backendEnrollment.id,
                            programId = backendEnrollment.program?.id ?: programId,
                            programName = backendEnrollment.program?.nomProgramme ?: programName,
                            childId = backendEnrollment.child?.id ?: "unknown",
                            childName = "${backendEnrollment.child?.prenom} ${backendEnrollment.child?.nom}",
                            amountPaid = backendEnrollment.amountPaid,
                            enrollmentDate = System.currentTimeMillis(), // Or parse backend date
                            status = EnrollmentStatus.valueOf(backendEnrollment.status)
                        )
                    }
                    
                    // Also save locally for offline access/cache
                    saveEnrollments(newEnrollments)
                    
                    // --- ENVOI DE L'EMAIL DE CONFIRMATION ---
                    try {
                        var userEmail = tokenManager.getUserEmail().first()
                        val userId = tokenManager.getUserId().first()
                        
                        // Fallback: Si l'email n'est pas en local, on va le chercher via l'API
                        if (userEmail.isNullOrBlank() && !userId.isNullOrBlank()) {
                             try {
                                 val userResponse = apiService.getUserById(userId)
                                 if (userResponse.isSuccessful && userResponse.body() != null) {
                                     userEmail = userResponse.body()!!.email
                                     // Sauvegarder pour la prochaine fois
                                     if (!userEmail.isNullOrBlank()) {
                                         tokenManager.saveUserEmail(userEmail)
                                     }
                                 }
                             } catch (e: Exception) {
                                 android.util.Log.e("EnrollmentRepo", "Impossible de récupérer l'email via API", e)
                             }
                        }

                        val parentPrenom = tokenManager.getUserPrenom().first() ?: "Parent"
                        val parentNom = tokenManager.getUserNom().first() ?: ""
                        val parentFullName = "$parentPrenom $parentNom".trim()

                        if (!userEmail.isNullOrBlank()) {
                             newEnrollments.forEach { enrollment ->
                                 // Formatage de la date
                                 val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                                 
                                 val params = PaymentParams(
                                     parentName = parentFullName,
                                     childName = enrollment.childName,
                                     programName = enrollment.programName,
                                     amount = String.format(Locale.US, "%.2f", enrollment.amountPaid),
                                     date = dateStr,
                                     userEmail = userEmail!!
                                 )
                                 
                                 val request = EmailRequest(
                                     serviceId = "service_3kgsa78",
                                     templateId = "template_iq4gq4m", 
                                     userId = "q82nwLspuKi44_dKh", 
                                     templateParams = params
                                 )
                                 
                                 // Envoi de l'email (non-bloquant pour l'UX si échoue)
                                 try {
                                    val emailResponse = EmailRetrofitClient.api.sendEmail(request)
                                    if (!emailResponse.isSuccessful) {
                                        android.util.Log.e("EnrollmentRepo", "Email error: ${emailResponse.code()}")
                                    } else {
                                        android.util.Log.d("EnrollmentRepo", "Email envoyé pour ${enrollment.childName} à $userEmail")
                                    }
                                 } catch (e: Exception) {
                                     android.util.Log.e("EnrollmentRepo", "Email exception", e)
                                 }
                             }
                        } else {
                            android.util.Log.e("EnrollmentRepo", "Email introuvable, impossible d'envoyer la confirmation")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    // ----------------------------------------
                    
                    Result.success(newEnrollments)
                } else {
                    Result.failure(Exception(result.message))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Erreur serveur: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if a child is already enrolled in a program
     */
    suspend fun isChildEnrolled(childId: String, programId: String): Boolean {
        return try {
            val response = apiService.checkEnrollment(childId, programId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!["isEnrolled"] ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get all enrollments stored locally
     */
    fun getEnrollments(): List<Enrollment> {
        val json = prefs.getString("enrollments_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Enrollment>>() {}.type
        return gson.fromJson(json, type)
    }
    
    /**
     * Get enrollments for a specific program
     */
    fun getEnrollmentsByProgram(programId: String): List<Enrollment> {
        return getEnrollments().filter { it.programId == programId }
    }
    
    /**
     * Get enrollments for a specific child
     */
    fun getEnrollmentsByChild(childId: String): List<Enrollment> {
        return getEnrollments().filter { it.childId == childId }
    }
    
    /**
     * Save enrollments to local storage
     */
    private suspend fun saveEnrollments(newEnrollments: List<Enrollment>) {
        val existingEnrollments = getEnrollments().toMutableList()
        existingEnrollments.addAll(newEnrollments)
        
        val json = gson.toJson(existingEnrollments)
        prefs.edit().putString("enrollments_list", json).commit() // commit pour forcer l'écriture
        
        // Notify widget of data change with delay (sequential)
        delay(600)
        SportyWidget.manualUpdate(context)
    }
    
    /**
     * Clear all enrollments (for testing purposes)
     */
    fun clearEnrollments() {
        prefs.edit().remove("enrollments_list").apply()
    }
}
