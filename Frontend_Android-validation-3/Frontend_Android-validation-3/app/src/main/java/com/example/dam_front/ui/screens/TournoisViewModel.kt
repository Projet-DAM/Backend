package com.example.dam_front.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.TournoiSocketService
import com.example.dam_front.models.Tournoi
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TournoisViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val TAG = "TournoisViewModel"
    
    private val _tournois = MutableStateFlow<List<Tournoi>>(emptyList())
    val tournois: StateFlow<List<Tournoi>> = _tournois.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        Log.d(TAG, "TournoisViewModel initialisé")
        
        // Connecter Socket.IO au démarrage
        TournoiSocketService.connect()
        Log.d(TAG, "Socket.IO connecté")
        
        // Écouter les nouveaux tournois
        viewModelScope.launch {
            TournoiSocketService.newTournoi.collect { newTournoi ->
                newTournoi?.let {
                    Log.d(TAG, "Nouveau tournoi reçu via Socket.IO: ${it.nom}")
                    // Ajouter le nouveau tournoi à la liste
                    _tournois.value = _tournois.value + it
                    // Effacer le nouveau tournoi après l'avoir ajouté
                    TournoiSocketService.clearNewTournoi()
                }
            }
        }
        
        // Charger la liste initiale
        loadTournois()
    }
    
    fun loadTournois() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d(TAG, "=== DEBUT CHARGEMENT DES TOURNOIS ===")
            Log.d(TAG, "URL de base: ${com.example.dam_front.config.ApiConfig.BASE_URL}")
            
            try {
                Log.d(TAG, "Envoi de la requête GET /tournois...")
                val response = apiService.getTournois()
                
                Log.d(TAG, "Réponse HTTP reçue:")
                Log.d(TAG, "  - Code: ${response.code()}")
                Log.d(TAG, "  - Success: ${response.isSuccessful}")
                Log.d(TAG, "  - Message: ${response.message()}")
                
                if (response.isSuccessful) {
                    // Récupérer le JSON brut depuis le ResponseBody
                    val responseBody = response.body()
                    val jsonString = responseBody?.string()
                    
                    if (jsonString == null) {
                        Log.w(TAG, "⚠️ Le body de la réponse est null")
                        _error.value = "Réponse vide du serveur"
                        _tournois.value = emptyList()
                    } else {
                        // Logger le JSON brut pour debug
                        Log.d(TAG, "📄 JSON brut reçu:")
                        Log.d(TAG, jsonString)
                        
                        // Parser le JSON (gère liste directe ou objet avec clé "tournois")
                        val tournoisList = parseTournoisResponse(jsonString)
                        
                        if (tournoisList == null) {
                            Log.e(TAG, "❌ Impossible de parser les tournois depuis le JSON")
                            _error.value = "Format de réponse inattendu. Vérifiez les logs pour le JSON reçu."
                            _tournois.value = emptyList()
                        } else {
                            Log.d(TAG, "✅ Nombre de tournois parsés: ${tournoisList.size}")
                            
                            if (tournoisList.isEmpty()) {
                                Log.w(TAG, "⚠️ La liste des tournois est vide")
                            } else {
                                Log.d(TAG, "📋 Détails des tournois:")
                                tournoisList.forEachIndexed { index, tournoi ->
                                    Log.d(TAG, "  Tournoi #${index + 1}:")
                                    Log.d(TAG, "    - ID: ${tournoi.id}")
                                    Log.d(TAG, "    - Nom: ${tournoi.nom}")
                                    Log.d(TAG, "    - Sport: ${tournoi.sport}")
                                    Log.d(TAG, "    - Lieu: ${tournoi.lieu}")
                                    Log.d(TAG, "    - Date début: ${tournoi.dateDebut}")
                                    Log.d(TAG, "    - Frais: ${tournoi.fraisParticipation}")
                                    val imagePath = tournoi.image ?: tournoi.imageUrl
                                    val imageUrl = com.example.dam_front.utils.ImageUtils.buildImageUrl(imagePath)
                                    Log.d(TAG, "    - Image path: $imagePath")
                                    Log.d(TAG, "    - Image URL complète: $imageUrl")
                                }
                            }
                            
                            _tournois.value = tournoisList
                        }
                    }
                } else {
                    // Erreur HTTP
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = "Erreur ${response.code()}: ${response.message()}\nBody: $errorBody"
                    Log.e(TAG, "❌ Erreur HTTP: $errorMsg")
                    _error.value = "Erreur ${response.code()}: ${response.message()}"
                    
                    // Afficher le body de l'erreur si disponible
                    if (errorBody != null) {
                        Log.e(TAG, "Body de l'erreur: $errorBody")
                    }
                }
            } catch (e: java.net.UnknownHostException) {
                val errorMsg = "Impossible de se connecter au serveur. Vérifiez l'URL: ${com.example.dam_front.config.ApiConfig.BASE_URL}"
                Log.e(TAG, "❌ Erreur de connexion (UnknownHostException): $errorMsg", e)
                _error.value = "Impossible de se connecter au serveur"
            } catch (e: java.net.SocketTimeoutException) {
                val errorMsg = "Timeout: Le serveur ne répond pas dans les délais"
                Log.e(TAG, "❌ Timeout: $errorMsg", e)
                _error.value = "Le serveur ne répond pas"
            } catch (e: java.io.IOException) {
                val errorMsg = "Erreur réseau: ${e.message}"
                Log.e(TAG, "❌ Erreur IO: $errorMsg", e)
                _error.value = "Erreur de connexion réseau"
            } catch (e: com.google.gson.JsonSyntaxException) {
                val errorMsg = "Erreur de parsing JSON: ${e.message}"
                Log.e(TAG, "❌ Erreur JSON: $errorMsg", e)
                Log.e(TAG, "Stack trace:", e)
                _error.value = "Erreur de format de données"
            } catch (e: Exception) {
                val errorMsg = "Erreur inattendue: ${e.message}"
                Log.e(TAG, "❌ Erreur: $errorMsg", e)
                Log.e(TAG, "Type d'erreur: ${e.javaClass.simpleName}")
                Log.e(TAG, "Stack trace:", e)
                _error.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "=== FIN CHARGEMENT DES TOURNOIS ===")
            }
        }
    }
    
    /**
     * Parse la réponse JSON qui peut être soit une liste directe, soit un objet avec clé "tournois"
     */
    private fun parseTournoisResponse(jsonString: String): List<Tournoi>? {
        return try {
            val gson = Gson()
            val jsonElement = JsonParser.parseString(jsonString)
            
            when {
                // Cas 1: Liste directe [...]
                jsonElement.isJsonArray -> {
                    Log.d(TAG, "📦 Format détecté: Liste directe (Array)")
                    val jsonArray = jsonElement.asJsonArray
                    val tournois = mutableListOf<Tournoi>()
                    
                    jsonArray.forEach { element ->
                        val tournoi = gson.fromJson(element, Tournoi::class.java)
                        tournois.add(tournoi)
                    }
                    tournois
                }
                
                // Cas 2: Objet avec clé "tournois" { "tournois": [...] }
                jsonElement.isJsonObject -> {
                    val jsonObject = jsonElement.asJsonObject
                    
                    if (jsonObject.has("tournois")) {
                        Log.d(TAG, "📦 Format détecté: Objet avec clé 'tournois'")
                        val tournoisArray = jsonObject.getAsJsonArray("tournois")
                        val tournois = mutableListOf<Tournoi>()
                        
                        tournoisArray.forEach { element ->
                            val tournoi = gson.fromJson(element, Tournoi::class.java)
                            tournois.add(tournoi)
                        }
                        tournois
                    } else {
                        // Essayer de parser comme un seul tournoi
                        Log.d(TAG, "📦 Format détecté: Objet unique (tournoi)")
                        listOf(gson.fromJson(jsonObject, Tournoi::class.java))
                    }
                }
                
                else -> {
                    Log.e(TAG, "❌ Format JSON inconnu")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du parsing manuel: ${e.message}", e)
            null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel détruit, déconnexion Socket.IO")
        TournoiSocketService.disconnect()
    }
}

// Factory pour créer le ViewModel
class TournoisViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TournoisViewModel::class.java)) {
            return TournoisViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

