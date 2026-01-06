package com.example.dam_front.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.Activity
import com.example.dam_front.models.CreateProgramRequest
import com.example.dam_front.models.Program
import com.example.dam_front.models.UpdateProgramRequest
import com.example.dam_front.repository.ActivityRepository
import com.example.dam_front.repository.ProgramRepository
import com.example.dam_front.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProgramsUiState(
    val programs: List<Program> = emptyList(),
    val availableActivities: List<Activity> = emptyList(),
    val coachNames: Map<String, String> = emptyMap(),
    val enrollments: List<com.example.dam_front.models.ProgramEnrollment> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ProgramsViewModel(context: Context) : ViewModel() {

    private val programRepository = ProgramRepository(context)
    private val activityRepository = ActivityRepository(context)
    private val userRepository = UserRepository(context)

    private val _uiState = MutableStateFlow(ProgramsUiState())
    val uiState: StateFlow<ProgramsUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val programsDeferred = async { programRepository.getPrograms(limit = 500) }
            val activitiesDeferred = async { activityRepository.getActivities(limit = 500) }

            val programsResult = programsDeferred.await()
            val activitiesResult = activitiesDeferred.await()

            programsResult.fold(
                onSuccess = { programs ->
                    _uiState.update { it.copy(programs = programs, isLoading = false, error = null) }
                    fetchCoachNames(programs)
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors du chargement des programmes") }
                }
            )

            activitiesResult.fold(
                onSuccess = { activities ->
                    _uiState.update { it.copy(availableActivities = activities) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: it.error) }
                }
            )
        }
    }

    fun reloadPrograms() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            programRepository.getPrograms(limit = 500).fold(
                onSuccess = { programs ->
                    _uiState.update { it.copy(programs = programs, isLoading = false) }
                    fetchCoachNames(programs)
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors du chargement des programmes") }
                }
            )
        }
    }

    fun refreshActivities() {
        viewModelScope.launch {
            activityRepository.getActivities(limit = 500).fold(
                onSuccess = { activities ->
                    _uiState.update { it.copy(availableActivities = activities) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(error = exception.message ?: it.error) }
                }
            )
        }
    }

    fun deleteProgram(programId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            programRepository.deleteProgram(programId).fold(
                onSuccess = {
                    reloadPrograms()
                    refreshActivities()
                    onSuccess()
                },
                onFailure = { exception ->
                    onError(exception.message ?: "Erreur lors de la suppression")
                }
            )
        }
    }

    fun createProgram(request: CreateProgramRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            programRepository.createProgram(request).fold(
                onSuccess = {
                    reloadPrograms()
                    refreshActivities()
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors de la création") }
                    onError(exception.message ?: "Erreur lors de la création")
                }
            )
        }
    }

    fun updateProgram(programId: String, request: UpdateProgramRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            programRepository.updateProgram(programId, request).fold(
                onSuccess = {
                    reloadPrograms()
                    refreshActivities()
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors de la mise à jour") }
                    onError(exception.message ?: "Erreur lors de la mise à jour")
                }
            )
        }
    }

    fun updateProgramActivities(programId: String, activityIds: List<String>, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            programRepository.updateProgramActivities(programId, activityIds).fold(
                onSuccess = {
                    reloadPrograms()
                    refreshActivities()
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors de la mise à jour des activités") }
                    onError(exception.message ?: "Erreur lors de la mise à jour des activités")
                }
            )
        }
    }
    fun createProgramWithImage(request: CreateProgramRequest, imageFile: java.io.File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            programRepository.createProgramWithImage(request, imageFile).fold(
                onSuccess = {
                    reloadPrograms()
                    refreshActivities()
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors de la création") }
                    onError(exception.message ?: "Erreur lors de la création")
                }
            )
        }
    }

    fun updateProgramImageWorkaround(programId: String, request: UpdateProgramRequest, imageFile: java.io.File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // 1. Créer un programme temporaire pour uploader l'image
            val tempRequest = CreateProgramRequest(
                nomProgramme = "temp_upload_${System.currentTimeMillis()}",
                statut = "BROUILLON"
            )

            programRepository.createProgramWithImage(tempRequest, imageFile).fold(
                onSuccess = { tempProgram ->
                    // 2. Récupérer l'URL de l'image
                    val imageUrl = tempProgram.image

                    if (imageUrl != null) {
                        // 3. Mettre à jour le programme cible avec l'URL de l'image
                        val updatedRequest = request.copy(image = imageUrl)

                        programRepository.updateProgram(programId, updatedRequest).fold(
                            onSuccess = {
                                // 4. Supprimer le programme temporaire (WAIT for it)
                                programRepository.deleteProgram(tempProgram.id)

                                // 5. Rafraîchir la liste et notifier le succès
                                reloadPrograms()
                                refreshActivities()
                                _uiState.update { it.copy(isLoading = false) }
                                onSuccess()
                            },
                            onFailure = { e ->
                                // Nettoyage en cas d'erreur
                                launch { programRepository.deleteProgram(tempProgram.id) }

                                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erreur lors de la mise à jour") }
                                onError(e.message ?: "Erreur lors de la mise à jour")
                            }
                        )
                    } else {
                        // Nettoyage si pas d'image
                        launch { programRepository.deleteProgram(tempProgram.id) }

                        _uiState.update { it.copy(isLoading = false, error = "Erreur: L'image n'a pas été sauvegardée correctement") }
                        onError("Erreur: L'image n'a pas été sauvegardée correctement")
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Erreur lors de l'upload de l'image") }
                    onError(e.message ?: "Erreur lors de l'upload de l'image")
                }
            )
        }
    }

    fun loadEnrollments(programId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            programRepository.getProgramEnrollments(programId).fold(
                onSuccess = { enrollments ->
                    _uiState.update { it.copy(enrollments = enrollments, isLoading = false) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message ?: "Erreur lors du chargement des inscriptions") }
                }
            )
        }
    }

    private fun fetchCoachNames(programs: List<Program>) {
        viewModelScope.launch {
            // Instead of fetching individually (which causes 403), try fetching the list of coaches
            userRepository.getCoaches().fold(
                onSuccess = { coaches ->
                    _uiState.update { currentState ->
                        val newMap = currentState.coachNames.toMutableMap()
                        coaches.forEach { coach ->
                            coach.id?.let { id ->
                                newMap[id] = "${coach.prenom} ${coach.nom}"
                            }
                        }
                        currentState.copy(coachNames = newMap.toMap())
                    }
                },
                onFailure = { error ->
                    // 403 or other errors: Just ignore. The UI will fallback to "Coach" or program.coachName if available.
                    // We do NOT want to show error codes to the user.
                }
            )
        }
    }
}

class ProgramsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProgramsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProgramsViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
