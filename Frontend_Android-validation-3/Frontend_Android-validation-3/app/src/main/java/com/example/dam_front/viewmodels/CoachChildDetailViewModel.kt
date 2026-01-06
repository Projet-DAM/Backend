package com.example.dam_front.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.repository.CoachRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CoachChildDetailUiState(
    val childId: String = "",
    val childName: String = "",
    val childPhoto: String? = null,
    val category: String = "",
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val suivis: List<SuiviEnfant> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class CoachChildDetailViewModel(
    application: Application,
    private val childId: String
) : AndroidViewModel(application) {

    private val repository = CoachRepository(getApplication())
    private val _uiState = MutableStateFlow(CoachChildDetailUiState(childId = childId))
    val uiState: StateFlow<CoachChildDetailUiState> = _uiState

    init {
        fetchChildData()
    }

    private fun fetchChildData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val child = repository.getChildById(childId)
                val suivis = repository.getPlayerSuivi(childId)
                val latest = suivis.lastOrNull()
                _uiState.value = _uiState.value.copy(
                    childName = "${child.prenom} ${child.nom}",
                    childPhoto = child.photoProfil,
                    category = child.role,
                    strengths = latest?.focusAreas ?: emptyList(),
                    weaknesses = latest?.nextSessionGoals ?: emptyList(),
                    improvements = latest?.nextSessionGoals ?: emptyList(),
                    suivis = suivis,
                    isLoading = false
                )
            } catch (ex: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = ex.message ?: "Erreur lors du chargement"
                )
            }
        }
    }

    // Public refresher so UI can trigger a reload (e.g., after navigation returns)
    fun refresh() { fetchChildData() }

    fun addStrength(value: String) {
        if (value.isBlank()) return
        // Optimistically update UI and persist to backend (update latest suivi if present)
        _uiState.value = _uiState.value.copy(
            strengths = _uiState.value.strengths + value
        )
        persistFocusChanges()
    }

    fun removeStrength(value: String) {
        _uiState.value = _uiState.value.copy(
            strengths = _uiState.value.strengths.filterNot { it == value }
        )
        persistFocusChanges()
    }

    fun addWeakness(value: String) {
        if (value.isBlank()) return
        _uiState.value = _uiState.value.copy(
            weaknesses = _uiState.value.weaknesses + value
        )
        persistFocusChanges()
    }

    fun removeWeakness(value: String) {
        _uiState.value = _uiState.value.copy(
            weaknesses = _uiState.value.weaknesses.filterNot { it == value }
        )
        persistFocusChanges()
    }

    fun addImprovement(value: String) {
        if (value.isBlank()) return
        _uiState.value = _uiState.value.copy(
            improvements = _uiState.value.improvements + value
        )
        persistFocusChanges()
    }

    fun removeImprovement(value: String) {
        _uiState.value = _uiState.value.copy(
            improvements = _uiState.value.improvements.filterNot { it == value }
        )
        persistFocusChanges()
    }

    private fun persistFocusChanges() {
        viewModelScope.launch {
            try {
                val suivis = _uiState.value.suivis
                val latest = suivis.lastOrNull()
                val repo = com.example.dam_front.repository.SuiviRepository(getApplication())
                if (latest != null) {
                    val updateDto = com.example.dam_front.data.UpdateSuiviEnfantDto(
                        dateSuivi = null,
                        presence = null,
                        performance = null,
                        commentaire = null,
                        focusAreas = _uiState.value.strengths.ifEmpty { null },
                        nextSessionGoals = (_uiState.value.weaknesses + _uiState.value.improvements).ifEmpty { null }
                    )
                    val res = repo.updateSuivi(latest.id, updateDto)
                    res.onSuccess { updated ->
                        // refresh local data from backend
                        fetchChildData()
                    }
                } else {
                    // No existing suivi: create a minimal suivi to persist focus areas
                    val dto = com.example.dam_front.data.CreateSuiviEnfantDto(
                        dateSuivi = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                        presence = false,
                        performance = 0,
                        commentaire = null,
                        enfantId = _uiState.value.childId,
                        focusAreas = _uiState.value.strengths,
                        nextSessionGoals = (_uiState.value.weaknesses + _uiState.value.improvements),
                        activityType = null,
                        effortLevel = null,
                        emotionalState = null
                    )
                    val res = repo.createSuivi(dto)
                    res.onSuccess { _ -> fetchChildData() }
                }
            } catch (e: Exception) {
                // ignore persistence errors for optimistic UI; optionally surface error
            }
        }
    }

    fun deleteSuivi(suiviId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                android.util.Log.d("CoachChildDetailVM", "Attempting to delete suivi: $suiviId")
                
                // Switch to IO context for network operation
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val repo = com.example.dam_front.repository.SuiviRepository(getApplication())
                    repo.deleteSuivi(suiviId)
                }
                
                // Back on Main context for UI updates
                if (result.isSuccess) {
                    android.util.Log.d("CoachChildDetailVM", "Delete successful, refreshing data")
                    fetchChildData() // refresh list
                    onResult(Result.success(Unit))
                } else {
                    val error = result.exceptionOrNull()
                    android.util.Log.e("CoachChildDetailVM", "Delete failed: ${error?.message}")
                    
                    val mappedError = when {
                        error is com.example.dam_front.api.ApiHttpException && error.code == 403 -> Exception(error.message)
                        error?.message?.contains("401") == true -> Exception("Session expirée - Veuillez vous reconnecter")
                        error?.message?.contains("404") == true -> Exception("Suivi introuvable ou déjà supprimé")
                        else -> Exception(error?.message ?: "Erreur lors de la suppression")
                    }
                    onResult(Result.failure(mappedError))
                }
            } catch (e: Exception) {
                android.util.Log.e("CoachChildDetailVM", "Delete exception: ${e.message}", e)
                
                val mappedError = when {
                    e is com.example.dam_front.api.ApiHttpException && e.code == 403 -> Exception(e.message)
                    e.message?.contains("401") == true -> Exception("Session expirée - Veuillez vous reconnecter")
                    e.message?.contains("404") == true -> Exception("Suivi introuvable ou déjà supprimé")
                    else -> Exception("Erreur de connexion: ${e.message ?: "Vérifiez votre réseau"}")
                }
                onResult(Result.failure(mappedError))
            }
        }
    }
}

class CoachChildDetailViewModelFactory(
    private val application: Application,
    private val childId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoachChildDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoachChildDetailViewModel(application, childId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

