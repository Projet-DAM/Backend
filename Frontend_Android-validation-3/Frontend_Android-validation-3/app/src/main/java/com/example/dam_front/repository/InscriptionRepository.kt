package com.example.dam_front.repository

import android.content.Context
import android.util.Log
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.InscriptionRequest
import com.example.dam_front.models.Participant
import kotlinx.coroutines.flow.first
import com.example.dam_front.utils.TokenManager
import androidx.glance.appwidget.updateAll
import com.example.dam_front.widget.SportyWidget
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class InscriptionRepository(private val context: Context) {
    private val apiService = RetrofitClient.createAuthenticatedApiService(context)
    private val tokenManager = TokenManager(context)
    private val prefs = context.getSharedPreferences("tournoi_cache", Context.MODE_PRIVATE)
    private val TAG = "InscriptionRepository"
    
    suspend fun inscrireAuTournoi(
        tournoiId: String,
        tournoiNom: String,
        enfantPrenom: String,
        enfantNom: String,
        enfantDateNaissance: String,
        parentTelephone: String,
        montantInscription: Double?,
        besoinsParticuliers: String?
    ): Result<Participant> {
        return try {
            // Récupérer les informations du parent
            val parentPrenom = tokenManager.getUserPrenom().first() ?: ""
            val parentNom = tokenManager.getUserNom().first() ?: ""
            
            val inscriptionRequest = InscriptionRequest(
                tournoiId = tournoiId,
                enfantPrenom = enfantPrenom,
                enfantNom = enfantNom,
                enfantDateNaissance = enfantDateNaissance,
                parentPrenom = parentPrenom,
                parentNom = parentNom,
                parentTelephone = parentTelephone,
                montantInscription = montantInscription,
                besoinsParticuliers = besoinsParticuliers
            )
            
            Log.d(TAG, "Inscription au tournoi $tournoiId")
            Log.d(TAG, "Données d'inscription: $inscriptionRequest")
            
            // Essayer d'abord l'endpoint /inscriptions
            var response = apiService.inscrireAuTournoi(inscriptionRequest)
            Log.d(TAG, "Tentative avec /inscriptions: code=${response.code()}, success=${response.isSuccessful}")
            
            // Si 404, essayer l'endpoint avec tournoiId dans le path
            if (!response.isSuccessful && response.code() == 404) {
                Log.d(TAG, "Endpoint /inscriptions non trouvé (404), essai avec /tournois/{tournoiId}/inscriptions")
                response = apiService.inscrireAuTournoiWithPath(tournoiId, inscriptionRequest)
                Log.d(TAG, "Tentative avec /tournois/{tournoiId}/inscriptions: code=${response.code()}, success=${response.isSuccessful}")
            }
            
            // Si toujours 404, essayer l'endpoint alternatif
            if (!response.isSuccessful && response.code() == 404) {
                Log.d(TAG, "Endpoint précédent non trouvé (404), essai avec /tournois/inscriptions")
                response = apiService.inscrireAuTournoiAlternative(inscriptionRequest)
                Log.d(TAG, "Tentative avec /tournois/inscriptions: code=${response.code()}, success=${response.isSuccessful}")
            }
            
            // Vérifier la réponse
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "✅ Inscription réussie avec l'endpoint qui a fonctionné")
                val participant = response.body()!!
                
                // Sauvegarder localement pour le widget
                prefs.edit().apply {
                    putString("last_tournoi_id", tournoiId)
                    putString("last_tournoi_nom", tournoiNom)
                    putString("last_child_name", "${participant.enfantPrenom} ${participant.enfantNom}")
                    putLong("last_inscription_date", System.currentTimeMillis())
                }.commit() // Utiliser commit pour forcer l'écriture immédiate

                // Notify widget of data change with delay (sequential)
                delay(600)
                SportyWidget.manualUpdate(context)
                
                return Result.success(participant)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = errorBody ?: "Erreur lors de l'inscription"
                Log.e(TAG, "❌ Erreur inscription: code=${response.code()}, message=$errorMessage")
                
                // Ne jamais simuler de succès, toujours retourner une erreur réelle
                return Result.failure(Exception("Erreur ${response.code()}: $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de l'inscription", e)
            Result.failure(e)
        }
    }
    
    suspend fun getParticipants(tournoiId: String): Result<List<Participant>> {
        return try {
            Log.d(TAG, "Récupération des participants pour le tournoi $tournoiId")
            
            // Essayer d'abord l'endpoint standard
            var response = apiService.getParticipants(tournoiId)
            val firstEndpointCode = response.code()
            val firstEndpointWas403 = !response.isSuccessful && firstEndpointCode == 403
            
            // Si 404 ou 403, essayer l'endpoint alternatif
            if (!response.isSuccessful && (firstEndpointCode == 404 || firstEndpointCode == 403)) {
                Log.d(TAG, "Endpoint /tournois/{id}/participants retourne ${firstEndpointCode}, essai avec /inscriptions/tournoi/{id}")
                response = apiService.getParticipantsAlternative(tournoiId)
            }
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Participants récupérés: ${response.body()!!.size}")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération"
                val finalCode = response.code()
                
                // Si le premier endpoint a retourné 403, même si le deuxième retourne autre chose, on considère ça comme 403
                if (firstEndpointWas403) {
                    Log.e(TAG, "Erreur récupération participants: 403 (premier endpoint) - $errorMessage")
                    Result.failure(Exception("Erreur: 403 - $errorMessage"))
                } else {
                    Log.e(TAG, "Erreur récupération participants: ${finalCode} - $errorMessage")
                    Result.failure(Exception("Erreur: ${finalCode} - $errorMessage"))
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Ignorer l'exception d'annulation - c'est normal quand on quitte l'écran
            Log.d(TAG, "Récupération des participants annulée")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la récupération des participants", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateInscription(
        inscriptionId: String,
        enfantPrenom: String? = null,
        enfantNom: String? = null,
        enfantDateNaissance: String? = null,
        parentPrenom: String? = null,
        parentNom: String? = null,
        parentTelephone: String? = null,
        montantInscription: Double? = null,
        besoinsParticuliers: String? = null
    ): Result<com.example.dam_front.models.Participant> {
        return try {
            val updateDto = com.example.dam_front.models.UpdateInscriptionDto(
                enfantPrenom = enfantPrenom,
                enfantNom = enfantNom,
                enfantDateNaissance = enfantDateNaissance,
                parentPrenom = parentPrenom,
                parentNom = parentNom,
                parentTelephone = parentTelephone,
                montantInscription = montantInscription,
                besoinsParticuliers = besoinsParticuliers
            )
            
            Log.d(TAG, "Mise à jour de l'inscription $inscriptionId")
            
            val response = apiService.updateInscription(inscriptionId, updateDto)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Inscription mise à jour avec succès")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour"
                Log.e(TAG, "Erreur mise à jour inscription: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la mise à jour de l'inscription", e)
            Result.failure(e)
        }
    }
    
    suspend fun deleteInscription(inscriptionId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Suppression de l'inscription $inscriptionId")
            
            val response = apiService.deleteInscription(inscriptionId)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Inscription supprimée avec succès")
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la suppression"
                Log.e(TAG, "Erreur suppression inscription: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la suppression de l'inscription", e)
            Result.failure(e)
        }
    }
}

