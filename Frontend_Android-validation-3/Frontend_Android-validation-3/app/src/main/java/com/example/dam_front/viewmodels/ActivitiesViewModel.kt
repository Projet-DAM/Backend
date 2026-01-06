package com.example.dam_front.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivitiesUiState(
    val activities: List<com.example.dam_front.models.Activity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ActivitiesViewModel(context: Context) : ViewModel() {
    
    private val repository = ActivityRepository(context)
    
    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()
    
    init {
        loadActivities()
    }
    
    fun loadActivities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.getActivities(limit = 500).fold(
                onSuccess = { activities ->
                    // Filtrer les activités temporaires
                    val filteredActivities = activities.filter { 
                        !it.nomActivite.startsWith("temp_upload_") 
                    }
                    _uiState.value = _uiState.value.copy(
                        activities = filteredActivities,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Une erreur est survenue"
                    )
                }
            )
        }
    }
    
    fun refreshActivities() {
        loadActivities()
    }
    
    fun deleteActivity(activityId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.deleteActivity(activityId).fold(
                onSuccess = {
                    // Recharger la liste après suppression
                    loadActivities()
                    onSuccess()
                },
                onFailure = { exception ->
                    onError(exception.message ?: "Erreur lors de la suppression")
                }
            )
        }
    }
    
    fun createActivity(
        request: com.example.dam_front.models.CreateActivityRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.createActivity(request).fold(
                onSuccess = { activity ->
                    // Recharger la liste après création
                    loadActivities()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Erreur lors de la création"
                    )
                    onError(exception.message ?: "Erreur lors de la création")
                }
            )
        }
    }
    
    fun updateActivity(
        activityId: String,
        request: com.example.dam_front.models.CreateActivityRequest,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            println("DEBUG ViewModel: updateActivity appelé avec activityId: $activityId")
            println("DEBUG ViewModel: request: $request")
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            repository.updateActivity(activityId, request).fold(
                onSuccess = { activity ->
                    println("DEBUG ViewModel: Mise à jour réussie, activité: $activity")
                    // Recharger la liste après mise à jour
                    loadActivities()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    println("DEBUG ViewModel: Erreur lors de la mise à jour: ${exception.message}")
                    println("DEBUG ViewModel: Exception: $exception")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Erreur lors de la mise à jour"
                    )
                    onError(exception.message ?: "Erreur lors de la mise à jour")
                }
            )
        }
    }

    fun createActivityWithImage(
        request: com.example.dam_front.models.CreateActivityRequest,
        imageFile: java.io.File?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.createActivityWithImage(request, imageFile).fold(
                onSuccess = {
                    loadActivities()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Erreur lors de la création"
                    )
                    onError(exception.message ?: "Erreur lors de la création")
                }
            )
        }
    }

    fun updateActivityWithImage(
        activityId: String,
        request: com.example.dam_front.models.CreateActivityRequest,
        imageFile: java.io.File?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repository.updateActivityWithImage(activityId, request, imageFile).fold(
                onSuccess = {
                    loadActivities()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message ?: "Erreur lors de la mise à jour"
                    )
                    onError(exception.message ?: "Erreur lors de la mise à jour")
                }
            )
        }
    }

    fun updateActivityImageWorkaround(
        activityId: String,
        request: com.example.dam_front.models.CreateActivityRequest,
        imageFile: java.io.File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            // 1. Créer une activité temporaire pour uploader l'image
            val tempRequest = com.example.dam_front.models.CreateActivityRequest(
                nomActivite = "temp_upload_${System.currentTimeMillis()}",
                statut = "BROUILLON"
            )
            
            repository.createActivityWithImage(tempRequest, imageFile).fold(
                onSuccess = { tempActivity ->
                    // 2. Récupérer l'URL de l'image
                    val imageUrl = tempActivity.image
                    
                    if (imageUrl != null) {
                        // 3. Mettre à jour l'activité cible avec l'URL de l'image
                        val updatedRequest = request.copy(image = imageUrl)
                        
                        repository.updateActivity(activityId, updatedRequest).fold(
                            onSuccess = {
                                // 4. Supprimer l'activité temporaire
                                launch {
                                    repository.deleteActivity(tempActivity.id)
                                }
                                
                                // 5. Rafraîchir la liste et notifier le succès
                                loadActivities()
                                _uiState.value = _uiState.value.copy(isLoading = false)
                                onSuccess()
                            },
                            onFailure = { e ->
                                // Nettoyage en cas d'erreur
                                launch { repository.deleteActivity(tempActivity.id) }
                                
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = e.message ?: "Erreur lors de la mise à jour"
                                )
                                onError(e.message ?: "Erreur lors de la mise à jour")
                            }
                        )
                    } else {
                        // Nettoyage si pas d'image
                        launch { repository.deleteActivity(tempActivity.id) }
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Erreur: L'image n'a pas été sauvegardée correctement"
                        )
                        onError("Erreur: L'image n'a pas été sauvegardée correctement")
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Erreur lors de l'upload de l'image"
                    )
                    onError(e.message ?: "Erreur lors de l'upload de l'image")
                }
            )
        }
    }
}

// ViewModelFactory for ActivitiesViewModel
class ActivitiesViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivitiesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActivitiesViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
