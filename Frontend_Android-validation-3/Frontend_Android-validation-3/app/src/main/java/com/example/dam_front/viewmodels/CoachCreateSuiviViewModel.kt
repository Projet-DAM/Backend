package com.example.dam_front.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.User
import com.example.dam_front.repository.CoachRepository
import com.example.dam_front.repository.SuiviRepository
import com.example.dam_front.data.CreateSuiviEnfantDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.dam_front.api.ApiHttpException

data class CoachCreateSuiviUiState(
    val dateMillis: Long = System.currentTimeMillis(),
    val dateDisplay: String = "",
    val presence: Boolean = true,
    val performance: Int = 8,
    val commentaire: String? = null,
    val activityType: String? = null,
    val focusAreas: List<String> = emptyList(),
    val nextSessionGoals: List<String> = emptyList(),
    val effortLevel: Int = 7,
    val emotionalState: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val availablePlayers: List<User> = emptyList()
)

class CoachCreateSuiviViewModel(
    application: Application,
    private val initialEnfantId: String = "",
    private val initialSuiviId: String? = null
) : AndroidViewModel(application) {

    // ADAPTATION: Use application context for repositories
    private val repo = SuiviRepository(application)
    private val coachRepo = CoachRepository(application)
    private val apiService = com.example.dam_front.api.RetrofitClient.createAuthenticatedService(application, com.example.dam_front.api.ApiService::class.java)
    private val suiviService = com.example.dam_front.api.RetrofitClient.createAuthenticatedService(application, com.example.dam_front.api.SuiviEnfantApiService::class.java)

    private val _uiState = MutableStateFlow(CoachCreateSuiviUiState())
    val uiState: StateFlow<CoachCreateSuiviUiState> = _uiState

    init {
        // Preload players if needed and load existing suivi when editing
        if (initialEnfantId.isBlank()) loadPlayers()
        initialSuiviId?.let { loadExistingSuivi(it) }
    }

    private fun loadPlayers() {
        viewModelScope.launch {
            try {
                println("🎮 CoachCreateSuiviViewModel.loadPlayers: starting multi-tier fetch...")
                
                // Strategy 1: Try coach-specific children endpoint
                var players = try { 
                    println("  📍 Strategy 1: Trying suiviService.getCoachChildren()")
                    suiviService.getCoachChildren() 
                } catch (e: Exception) {
                    println("  ❌ Strategy 1 failed: ${e.message}")
                    emptyList()
                }
                
                // Strategy 2: If empty, try general enfants endpoint
                if (players.isEmpty()) {
                    players = try {
                        println("  📍 Strategy 2: Trying apiService.getEnfants()")
                        apiService.getEnfants()
                    } catch (e: Exception) {
                        println("  ❌ Strategy 2 failed: ${e.message}")
                        emptyList()
                    }
                }
                
                // Strategy 3: If still empty, try getUsersByRole(ENFANT)
                if (players.isEmpty()) {
                    players = try {
                        println("  📍 Strategy 3: Trying apiService.getUsersByRole(ENFANT)")
                        apiService.getUsersByRole("ENFANT")
                    } catch (e: Exception) {
                        println("  ❌ Strategy 3 failed: ${e.message}")
                        emptyList()
                    }
                }
                
                println("✅ CoachCreateSuiviViewModel.loadPlayers: received ${players.size} players")
                players.forEachIndexed { idx, player -> 
                    println("  [$idx] ${player.prenom} ${player.nom} - ID: ${player.id}")
                }
                
                if (players.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        availablePlayers = emptyList(), 
                        error = "Aucun enfant trouvé. Vérifiez qu'il existe des enfants dans le système."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(availablePlayers = players, error = null)
                }
            } catch (e: Exception) {
                println("💥 loadPlayers exception: ${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private fun loadExistingSuivi(suiviId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val res = repo.getSuivi(suiviId)
                res.onSuccess { suivi ->
                    _uiState.value = _uiState.value.copy(
                        dateMillis = suivi.dateSuivi.time,
                        presence = suivi.presence,
                        performance = suivi.performance,
                        commentaire = suivi.commentaire,
                        activityType = suivi.activityType,
                        focusAreas = suivi.focusAreas ?: emptyList(),
                        nextSessionGoals = suivi.nextSessionGoals ?: emptyList(),
                        effortLevel = suivi.effortLevel ?: _uiState.value.effortLevel,
                        emotionalState = suivi.emotionalState
                    )
                }.onFailure { ex -> _uiState.value = _uiState.value.copy(error = ex.message) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun createSuivi(dto: CreateSuiviEnfantDto, onResult: (Result<com.example.dam_front.data.SuiviEnfant>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repo.createSuivi(dto)
                // If backend returned a mapped ApiHttpException (e.g. 403), translate to a clearer message
                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    if (ex is ApiHttpException && ex.code == 403) {
                        onResult(Result.failure(Exception("Permission refusée")))
                    } else {
                        onResult(result)
                    }
                } else {
                    onResult(result)
                }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateSuivi(id: String, updateDto: com.example.dam_front.data.UpdateSuiviEnfantDto, onResult: (Result<com.example.dam_front.data.SuiviEnfant>) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = repo.updateSuivi(id, updateDto)
                onResult(result)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}
