package com.example.dam_front.repository

import android.content.Context
import android.util.Log
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.CreateEquipeRequest
import com.example.dam_front.models.Equipe
import com.example.dam_front.models.UpdateEquipeRequest
import com.example.dam_front.models.UpdateEquipeEnfantsRequest

class EquipeRepository(private val context: Context) {
    private val apiService = RetrofitClient.createAuthenticatedApiService(context)
    private val TAG = "EquipeRepository"
    
    suspend fun createEquipe(createEquipeRequest: CreateEquipeRequest): Result<Equipe> {
        return try {
            Log.d(TAG, "Création d'équipe: ${createEquipeRequest.nom}")
            
            val response = apiService.createEquipe(createEquipeRequest)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Équipe créée avec succès")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création"
                Log.e(TAG, "Erreur création équipe: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la création de l'équipe", e)
            Result.failure(e)
        }
    }
    
    suspend fun getTournamentEquipes(tournamentId: String): Result<List<Equipe>> {
        return try {
            Log.d(TAG, "Récupération des équipes pour le tournoi $tournamentId")
            
            val response = apiService.getTournamentEquipes(tournamentId)
            
            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Équipes récupérées: ${response.body()!!.size}")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération"
                Log.e(TAG, "Erreur récupération équipes: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la récupération des équipes", e)
            Result.failure(e)
        }
    }

    suspend fun updateEquipe(
        equipeId: String,
        updateData: UpdateEquipeRequest,
        currentEquipe: Equipe? = null
    ): Result<Equipe> {
        return try {
            Log.d(TAG, "Mise à jour de l'équipe $equipeId")

            // Calculer les différences entre l'ancienne et la nouvelle liste d'enfants
            // Le backend retourne les enfants avec _id qui correspond à l'ID de l'inscription
            // Les IDs dans updateData.enfants sont les IDs d'inscription, donc on compare avec id
            val anciensEnfantsIds = currentEquipe?.enfants
                ?.mapNotNull { it.id }
                ?.toSet() ?: emptySet()

            val nouveauxEnfantsIds = updateData.enfants?.toSet() ?: emptySet()

            // Calculer ce qui doit être ajouté et retiré
            val ajouter = (nouveauxEnfantsIds - anciensEnfantsIds).toList()
            val retirer = (anciensEnfantsIds - nouveauxEnfantsIds).toList()

            Log.d(TAG, "Ajouter: $ajouter, Retirer: $retirer")

            val updateEnfantsRequest = UpdateEquipeEnfantsRequest(
                ajouter = ajouter,
                retirer = retirer
            )

            val response = apiService.updateEquipe(equipeId, updateEnfantsRequest)

            if (response.isSuccessful && response.body() != null) {
                Log.d(TAG, "Équipe mise à jour avec succès")
                Result.success(response.body()!!)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour"
                Log.e(TAG, "Erreur mise à jour équipe: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la mise à jour de l'équipe", e)
            Result.failure(e)
        }
    }
    suspend fun deleteEquipe(equipeId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Suppression de l'équipe $equipeId")
            
            val response = apiService.deleteEquipe(equipeId)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Équipe supprimée avec succès")
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la suppression"
                Log.e(TAG, "Erreur suppression équipe: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la suppression de l'équipe", e)
            Result.failure(e)
        }
    }
    
    suspend fun generateBracket(tournoiId: String): Result<Unit> {
        return try {
            Log.d(TAG, "Génération de l'arbre de tournoi pour: $tournoiId")
            
            val response = apiService.generateBracket(tournoiId)
            
            if (response.isSuccessful) {
                Log.d(TAG, "Arbre de tournoi généré avec succès")
                Result.success(Unit)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la génération"
                val errorCode = response.code()
                Log.e(TAG, "Erreur génération arbre: $errorCode - $errorMessage")
                
                // Message d'erreur spécifique pour 400 (arbre déjà généré)
                val finalErrorMessage = if (errorCode == 400) {
                    "L'arbre de tournoi a déjà été généré pour ce tournoi."
                } else {
                    "Erreur: $errorCode - $errorMessage"
                }
                
                Result.failure(Exception(finalErrorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la génération de l'arbre", e)
            Result.failure(e)
        }
    }
}

