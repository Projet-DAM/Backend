package com.example.dam_front.viewmodels

import android.util.Log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.User
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.repository.CoachRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.SuiviEnfantApiService
import com.example.dam_front.data.SuiviEnfant

data class CoachPlayerUi(
    val id: String,
    val name: String,
    val category: String,
    val presencePercent: Int,
    val effortPercent: Int,
    val photo: String?
)

data class CoachHomeUiState(
    val coachName: String = "",
    val coachPhoto: String? = null,
    val players: List<CoachPlayerUi> = emptyList(),
    val allChildren: List<com.example.dam_front.models.User> = emptyList(),
    val suivis: List<SuiviEnfant> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class CoachHomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    init {
        println("🚀🚀🚀 CoachHomeViewModel CONSTRUCTOR CALLED 🚀🚀🚀")
    }

    private val authRepository = AuthRepository(application)
    private val coachRepository = CoachRepository(application) // ADAPTED

    private val _uiState = MutableStateFlow(CoachHomeUiState())
    val uiState: StateFlow<CoachHomeUiState> = _uiState

    init {
        println("🚀 CoachHomeViewModel INIT STARTED")
        try {
            println("🔧 CoachHomeViewModel: calling fetchCoachData()")
            fetchCoachData()
            println("🔧 CoachHomeViewModel: calling fetchAllChildren()")
            fetchAllChildren()
            println("🔧 CoachHomeViewModel: calling fetchAllSuivis()")
            fetchAllSuivis()
            println("✅ CoachHomeViewModel INIT COMPLETED")
        } catch (e: Exception) {
            println("❌ CoachHomeViewModel INIT ERROR: ${e.message}")
            e.printStackTrace()
        }
    }

    // allow UI to request a refresh (e.g., after navigation returns)
    fun refresh() {
        // Refresh coach data and the global suivis list so UI can show newly-created suivis
        fetchCoachData()
        fetchAllSuivis()
    }

    private fun fetchCoachData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val coachId = authRepository.getUserId() ?: throw IllegalStateException("Coach non connecté")
                val coach = coachRepository.getCoach(coachId)
                var players = try { coachRepository.getCoachPlayers() } catch (_: Exception) { emptyList() }

                // If players list is empty (e.g., role-based listing forbidden), try deriving players from suivis
                if (players.isEmpty()) {
                    try {
                        val suiviService: SuiviEnfantApiService = RetrofitClient.createAuthenticatedService(getApplication(), SuiviEnfantApiService::class.java) // ADAPTED
                        val allSuivis = try { suiviService.getAllSuiviEnfants() } catch (_: Exception) { emptyList<SuiviEnfant>() }
                        val enfantIds = allSuivis.mapNotNull { it.enfant?.id ?: it.enfantId }.distinct()
                        val derived = enfantIds.mapNotNull { id ->
                            try { coachRepository.getChildById(id) } catch (_: Exception) { null }
                        }
                        if (derived.isNotEmpty()) players = derived
                    } catch (_: Exception) {
                        // ignore and continue with empty players
                    }
                }
                val playerUi = players.map { player ->
                    // protect per-player suivi fetch so a failure for one player doesn't fail the whole screen
                    val suivis = try {
                        player.id?.let { coachRepository.getPlayerSuivi(it) }.orEmpty()
                    } catch (ex: Exception) {
                        Log.w("CoachHomeVM", "getPlayerSuivi failed for player=${player.id}", ex)
                        emptyList<com.example.dam_front.data.SuiviEnfant>()
                    }

                    val presencePercent = if (suivis.isNotEmpty()) {
                        (100f * suivis.count { it.presence } / suivis.size).toInt()
                    } else 0

                    val effortPercent = run {
                        val effortLevels = suivis.mapNotNull { it.effortLevel }
                        if (effortLevels.isNotEmpty()) {
                            effortLevels.average().toInt()
                        } else {
                            val perfList = suivis.map { s -> s.performance }
                            if (perfList.isNotEmpty()) perfList.average().toInt() else 0
                        }
                    }

                    CoachPlayerUi(
                        id = player.id ?: "",
                        name = "${player.prenom} ${player.nom}",
                        category = player.role ?: "ENFANT",
                        presencePercent = presencePercent,
                        effortPercent = effortPercent,
                        photo = player.photoProfil
                    )
                }
                _uiState.value = CoachHomeUiState(
                    coachName = "${coach.prenom} ${coach.nom}",
                    coachPhoto = coach.photoProfil,
                    players = playerUi,
                    isLoading = false
                )
            } catch (ex: Exception) {
                when (ex) {
                    is HttpException -> {
                        if (ex.code() == 403) {
                            // Coach token is not allowed to access this endpoint.
                            // Show an empty list instead of a raw HTTP 403 message.
                            _uiState.value = CoachHomeUiState(
                                coachName = "",
                                coachPhoto = null,
                                players = emptyList(),
                                isLoading = false,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Erreur réseau (${ex.code()})"
                            )
                        }
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = ex.message ?: "Erreur inattendue"
                        )
                    }
                }
            }
        }
    }

    fun refreshChildren() {
        println("🔄 CoachHomeViewModel.refreshChildren() called")
        fetchAllChildren()
        println("✅ CoachHomeViewModel.refreshChildren() completed")
    }

    private fun fetchAllChildren() {
        println("🔥🔥🔥 CoachHomeViewModel.fetchAllChildren() STARTING")
        viewModelScope.launch {
            try {
                println("🔥 Inside viewModelScope.launch - about to call repository")
                Log.d("CoachHomeVM", "fetchAllChildren: STARTING - calling coachRepository.getAllChildren()")
                val children = try { 
                    coachRepository.getAllChildren() 
                } catch (ex: Exception) {
                    Log.e("CoachHomeVM", "getAllChildren failed with exception: ${ex.message}", ex)
                    emptyList()
                }
                Log.d("CoachHomeVM", "fetchAllChildren: RESULT - got ${children.size} children")
                
                if (children.isNotEmpty()) {
                    children.forEachIndexed { index, child ->
                        Log.d("CoachHomeVM", "  Child[$index]: ID=${child.id}, Name=${child.prenom} ${child.nom}, Photo='${child.photoProfil}'")
                    }
                } else {
                    Log.w("CoachHomeVM", "fetchAllChildren: No children found - this will cause AllChildren: 0 in UI")
                }
                
                _uiState.value = _uiState.value.copy(allChildren = children)
                Log.d("CoachHomeVM", "fetchAllChildren: UI state updated, allChildren.size = ${_uiState.value.allChildren.size}")
            } catch (_: Exception) {
                // ignore
            }
        }
    }

    private fun fetchAllSuivis() {
        viewModelScope.launch {
            try {
                val suiviService: SuiviEnfantApiService = RetrofitClient.createAuthenticatedService(getApplication(), SuiviEnfantApiService::class.java)
                
                // Get coach's children to filter suivis
                val coachChildren = try { suiviService.getCoachChildren() } catch (_: Exception) { emptyList<User>() }
                val childIds = coachChildren.map { it.id }.toSet()
                
                // Get all suivis and filter by coach's children
                val allSuivis = try { suiviService.getAllSuiviEnfants() } catch (_: Exception) { emptyList<SuiviEnfant>() }
                val coachSuivis = allSuivis.filter { suivi ->
                    suivi.enfantId in childIds || suivi.enfant?.id in childIds
                }
                
                println("📊 All suivis: ${allSuivis.size}, Coach's children: ${childIds.size}, Coach's suivis: ${coachSuivis.size}")
                _uiState.value = _uiState.value.copy(suivis = coachSuivis)
            } catch (_: Exception) {
                // ignore
            }
        }
    }
}

class CoachHomeViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        println("🏭 CoachHomeViewModelFactory.create() called for ${modelClass.simpleName}")
        if (modelClass.isAssignableFrom(CoachHomeViewModel::class.java)) {
            println("🏭 Creating CoachHomeViewModel instance...")
            @Suppress("UNCHECKED_CAST")
            val vm = CoachHomeViewModel(application) as T
            println("✅ CoachHomeViewModel created successfully")
            return vm
        }
        println("❌ CoachHomeViewModelFactory: Unknown ViewModel class: ${modelClass.simpleName}")
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
