package com.example.dam_front.repository

import android.content.Context
import android.util.Log
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.Match
import com.example.dam_front.models.MatchDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class MatchRepository(private val context: Context) {
    private val apiService = RetrofitClient.createAuthenticatedApiService(context)
    private val TAG = "MatchRepository"
    private val gson = GsonBuilder()
        .registerTypeAdapter(Match::class.java, MatchDeserializer())
        .create()
    
    suspend fun getTournamentMatches(tournoiId: String, showFuture: Boolean = false): Result<List<Match>> {
        return try {
            Log.d(TAG, "Récupération des matchs pour le tournoi $tournoiId (showFuture=$showFuture)")
            
            val response = apiService.getTournamentMatches(tournoiId, showFuture)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                val jsonString = responseBody?.string()
                
                if (jsonString.isNullOrBlank()) {
                    Log.d(TAG, "Aucun match trouvé")
                    Result.success(emptyList())
                } else {
                    Log.d(TAG, "JSON reçu: $jsonString")
                    
                    // Parser directement comme tableau JSON avec le désérialiseur personnalisé
                    val matchList = try {
                        val matchArray = gson.fromJson(jsonString, Array<Match>::class.java)
                        matchArray.toList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing JSON comme tableau", e)
                        emptyList()
                    }
                    
                    Log.d(TAG, "✅ ${matchList.size} match(s) récupéré(s)")
                    Result.success(matchList)
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la récupération"
                Log.e(TAG, "Erreur récupération matchs: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la récupération des matchs", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateMatch(matchId: String, scoreEquipe1: Int?, scoreEquipe2: Int?, statut: String?): Result<Match> {
        return try {
            Log.d(TAG, "Mise à jour du match $matchId")
            
            val updateDto = com.example.dam_front.models.UpdateMatchDto(
                scoreEquipe1 = scoreEquipe1,
                scoreEquipe2 = scoreEquipe2,
                statut = statut
            )
            
            val response = apiService.updateMatch(matchId, updateDto)
            
            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody != null) {
                    // Parser manuellement avec le deserializer
                    val jsonString = responseBody.string()
                    Log.d(TAG, "Réponse mise à jour: $jsonString")
                    
                    try {
                        val match = gson.fromJson(jsonString, Match::class.java)
                        Log.d(TAG, "Match mis à jour avec succès")
                        Result.success(match)
                    } catch (e: Exception) {
                        Log.e(TAG, "Erreur parsing réponse match", e)
                        val errorMessage = "Erreur lors du parsing de la réponse: ${e.message}"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour"
                    Log.e(TAG, "Erreur mise à jour match: ${response.code()} - $errorMessage")
                    Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la mise à jour"
                Log.e(TAG, "Erreur mise à jour match: ${response.code()} - $errorMessage")
                Result.failure(Exception("Erreur: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception lors de la mise à jour du match", e)
            Result.failure(e)
        }
    }
}

