package com.example.dam_front.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.CreateChildRequest
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.utils.TokenManager
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.SuiviEnfantApiService
import com.example.dam_front.data.SuiviEnfant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import retrofit2.HttpException

// Small local container for computed UI values to keep background computation tidy
private data class ComputedResult(
    val presence: Int,
    val effort: Int,
    val performance: Int,
    val progression: List<Float>,
    val latest: com.example.dam_front.data.SuiviEnfant?,
    val latestDate: String?
)

data class ParentHomeUiState(
    val parentName: String = "",
    val parentPhoto: String? = null,
    val children: List<com.example.dam_front.models.User> = emptyList(),
    val selectedChildId: String? = null,
    val presencePercent: Int = -1,
    val effortPercent: Int = -1,
    val performancePercent: Int = -1,
    val progression: List<Float> = emptyList(),
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val latestComment: String? = null,
    val latestSuiviDate: String? = null,
    val latestActivityType: String? = null,
    val latestEmotionalState: String? = null,
    val latestEffortLevel: Int? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class ParentHomeViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = AuthRepository(application.applicationContext)
    private val apiService = RetrofitClient.createAuthenticatedService(application, com.example.dam_front.api.ApiService::class.java)
    private val suiviService: SuiviEnfantApiService = RetrofitClient.createAuthenticatedService(application, SuiviEnfantApiService::class.java)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(ParentHomeUiState())
    val uiState: StateFlow<ParentHomeUiState> = _uiState

    init {
        // Wait for TokenManager to provide a non-blank userId before loading data.
        Log.d("ParentHomeVM", "init: starting collector for userId")
        viewModelScope.launch {
            tokenManager.getUserId().collect { id ->
                Log.d("ParentHomeVM", "collected userId='$id'")
                if (!id.isNullOrBlank()) {
                    // Also verify we have a token
                    val token = tokenManager.getToken().first()
                    if (!token.isNullOrBlank()) {
                        loadDataForUser(id)
                    } else {
                        Log.w("ParentHomeVM", "UserId available but no token, waiting...")
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                } else {
                    // keep loading state until we have a user id
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    // Public helper to trigger a manual refresh. It will attempt to read the current userId
    // and load data for that user if available.
    fun loadData() {
        viewModelScope.launch {
            val id = tokenManager.getUserId().first()
            if (!id.isNullOrBlank()) {
                loadDataForUser(id)
            } else {
                Log.d("ParentHomeVM", "loadData: userId blank, trying enfants fallback")
                loadDataUsingEnfantsFallback()
            }
        }
    }

    private fun loadDataUsingEnfantsFallback() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d("ParentHomeVM", "loadDataUsingEnfantsFallback: calling GET users/enfants")
                val enfants = try { apiService.getEnfants() } catch (ex: Exception) {
                    Log.e("ParentHomeVM", "getEnfants fallback failed", ex)
                    emptyList()
                }

                // Use the enfants list as returned by the server for the dropdown.
                // Do not filter them out here; selection will show an error if the backend denies access.
                val childId = enfants.firstOrNull()?.id

                // Only attempt to fetch suivis if we have an auth token available.
                val suivis = if (!childId.isNullOrBlank()) {
                    val token = try { tokenManager.getToken().first() } catch (_: Exception) { "" }
                    if (token.isNullOrBlank()) {
                        Log.d("ParentHomeVM", "Skipping suivi fetch in fallback: no auth token yet")
                        emptyList<SuiviEnfant>()
                    } else {
                        try { suiviService.getSuiviEnfantsByEnfantId(childId) } catch (ex: Exception) {
                            Log.e("ParentHomeVM", "getSuivis error in fallback", ex)
                            emptyList<SuiviEnfant>()
                        }
                    }
                } else emptyList()

                // Offload heavier computation to background dispatcher to avoid UI thread hiccups
                val computed = withContext(Dispatchers.Default) {
                    val presence = if (suivis.isNotEmpty()) (100f * suivis.count { it.presence } / suivis.size).toInt() else 0
                    val effort = suivis.mapNotNull { it.effortLevel }.let { if (it.isNotEmpty()) it.average().toInt() else suivis.map { s -> s.performance }.average().toInt() }
                    val performance = suivis.lastOrNull()?.performance ?: effort
                    val prog = suivis.sortedBy { it.dateSuivi }.map { it.performance.toFloat() }
                    val latestLocal = suivis.lastOrNull()
                    val latestDate = latestLocal?.dateSuivi?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(it) }
                    ComputedResult(presence, effort, performance, prog, latestLocal, latestDate)
                }
                val presencePercent = computed.presence
                val effortPercent = computed.effort
                val performancePercent = computed.performance
                val progression = computed.progression
                val latest = computed.latest
                val latestDateStr = computed.latestDate

                Log.d("ParentHomeVM", "fallback enfants.size=${enfants.size}")
                _uiState.value = _uiState.value.copy(
                    parentName = "",
                    parentPhoto = null,
                    children = enfants,
                    selectedChildId = childId,
                    presencePercent = presencePercent,
                    effortPercent = effortPercent,
                    performancePercent = performancePercent,
                    progression = progression,
                    strengths = latest?.focusAreas ?: emptyList(),
                    weaknesses = latest?.nextSessionGoals ?: emptyList(),
                    improvements = latest?.nextSessionGoals ?: emptyList(),
                    latestComment = latest?.commentaire,
                    latestSuiviDate = latestDateStr,
                    latestActivityType = latest?.activityType,
                    latestEmotionalState = latest?.emotionalState,
                    latestEffortLevel = latest?.effortLevel,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ParentHomeVM", "loadDataUsingEnfantsFallback error", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun loadDataForUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                Log.d("ParentHomeVM", "loadDataForUser: starting for userId=$userId")
                val userRole = tokenManager.getUserRole().first().orEmpty()
                Log.d("ParentHomeVM", "userRole=$userRole")
                val user = try { authRepo.getUser(userId) } catch (_: Exception) { null }
                val isParent = userRole.equals("parent", ignoreCase = true)

                val children = if (isParent && userId.isNotBlank()) {
                    try { authRepo.getChildren(userId) } catch (ex: Exception) {
                        Log.e("ParentHomeVM", "getChildren error", ex)
                        emptyList()
                    }
                } else emptyList()

                // If the parent-scoped children call returned nothing, try the authenticated compact endpoint
                val finalChildren = if (children.isEmpty()) {
                    Log.d("ParentHomeVM", "no children from getChildren; trying getEnfants() fallback")
                    try { apiService.getEnfants() } catch (ex: Exception) {
                        Log.e("ParentHomeVM", "getEnfants fallback failed", ex)
                        emptyList()
                    }
                } else children

                // Use the finalChildren list returned by server (don't pre-filter by access here).
                val childId = finalChildren.firstOrNull()?.id

                // Fetch suivis for selected child (only if we have a valid child id)
                val suivis = if (!childId.isNullOrBlank()) {
                    try { suiviService.getSuiviEnfantsByEnfantId(childId) } catch (ex: Exception) { 
                        Log.e("ParentHomeVM", "getSuivis error", ex)
                        emptyList<SuiviEnfant>()
                    }
                } else emptyList()

                // Offload heavier computation to background dispatcher to avoid UI thread hiccups
                val computed2 = withContext(Dispatchers.Default) {
                    val presence = if (suivis.isNotEmpty()) (100f * suivis.count { it.presence } / suivis.size).toInt() else 0
                    val effort = suivis.mapNotNull { it.effortLevel }.let { if (it.isNotEmpty()) it.average().toInt() else suivis.map { s -> s.performance }.average().toInt() }
                    val performance = suivis.lastOrNull()?.performance ?: effort
                    val prog = suivis.sortedBy { it.dateSuivi }.map { it.performance.toFloat() }
                    val latestLocal = suivis.lastOrNull()
                    val latestDate = latestLocal?.dateSuivi?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(it) }
                    ComputedResult(presence, effort, performance, prog, latestLocal, latestDate)
                }
                val presencePercent = computed2.presence
                val effortPercent = computed2.effort
                val performancePercent = computed2.performance
                val progression = computed2.progression
                val latest = computed2.latest
                val latestDateStr = computed2.latestDate

                Log.d("ParentHomeVM", "children.size=${finalChildren.size}")
                _uiState.value = _uiState.value.copy(
                    parentName = user?.let { "${it.prenom} ${it.nom}" } ?: "",
                    parentPhoto = user?.photoProfil,
                    children = finalChildren,
                    selectedChildId = childId,
                    presencePercent = presencePercent,
                    effortPercent = effortPercent,
                    performancePercent = performancePercent,
                    progression = progression,
                    strengths = latest?.focusAreas ?: emptyList(),
                    weaknesses = latest?.nextSessionGoals ?: emptyList(),
                    improvements = latest?.nextSessionGoals ?: emptyList(),
                    latestComment = latest?.commentaire,
                    latestSuiviDate = latestDateStr,
                    latestActivityType = latest?.activityType,
                    latestEmotionalState = latest?.emotionalState,
                    latestEffortLevel = latest?.effortLevel,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ParentHomeVM", "loadDataForUser error", e)
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun createChild(parentId: String, dto: CreateChildRequest, onResult: (Result<com.example.dam_front.models.CreateChildResponse>) -> Unit) {
        viewModelScope.launch {
            try {
                val auth = authRepo
                val res = auth.createChild(parentId, dto)
                res.onSuccess { onResult(Result.success(it)) }.onFailure { ex -> onResult(Result.failure(ex)) }
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun selectChild(childId: String) {
        viewModelScope.launch {
            // Immediately reflect the selected child in UI so the header updates while suivis load
            _uiState.value = _uiState.value.copy(selectedChildId = childId, isLoading = true, error = null)
            Log.d("ParentHomeVM", "selectChild: fetching suivis for childId=$childId")
            try {
                // Ensure we have an auth token before attempting to fetch secured suivi data.
                val token = try { tokenManager.getToken().first() } catch (_: Exception) { "" }
                if (token.isNullOrBlank()) {
                    Log.w("ParentHomeVM", "selectChild: no auth token available when selecting child $childId")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "Authentification requise — connectez-vous ou attendez que la session soit prête.")
                    return@launch
                }

                val suivis = if (childId.isNotBlank()) {
                    try {
                        suiviService.getSuiviEnfantsByEnfantId(childId)
                    } catch (ex: Exception) {
                        Log.e("ParentHomeVM", "getSuivis error in selectChild", ex)
                        throw ex
                    }
                } else emptyList()

                val sorted = suivis.sortedBy { it.dateSuivi }
                val presencePercent = if (sorted.isNotEmpty()) (100f * sorted.count { it.presence } / sorted.size).toInt() else 0
                val effortPercent = sorted.mapNotNull { it.effortLevel }.let { if (it.isNotEmpty()) it.average().toInt() else sorted.map { s -> s.performance }.average().toInt() }
                val performancePercent = sorted.lastOrNull()?.performance ?: effortPercent
                val progression = sorted.map { it.performance.toFloat() }
                val latest = sorted.lastOrNull()
                val latestDateStr = latest?.dateSuivi?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(it) }

                // Update the UI while preserving the selectedChildId
                _uiState.value = _uiState.value.copy(
                    presencePercent = presencePercent,
                    effortPercent = effortPercent,
                    performancePercent = performancePercent,
                    progression = progression,
                    strengths = latest?.focusAreas ?: emptyList(),
                    weaknesses = latest?.nextSessionGoals ?: emptyList(),
                    improvements = latest?.nextSessionGoals ?: emptyList(),
                    latestComment = latest?.commentaire,
                    latestSuiviDate = latestDateStr,
                    isLoading = false
                )
            } catch (e: Exception) {
                Log.e("ParentHomeVM", "selectChild error", e)
                if (e is HttpException && e.code() == 403) {
                    // Backend refused access to this child's suivis — keep the child visible in the list
                    // and surface a readable error message. Do not remove the child from the dropdown.
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = (try { e.response()?.errorBody()?.string() } catch (_: Exception) { e.message })
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    // Check which children the current parent actually has access to. Some backend endpoints
    // may return a superset; attempt to fetch suivis for each child and filter out ones
    // that return 403 Forbidden.
    private suspend fun filterAccessibleChildren(children: List<com.example.dam_front.models.User>): List<com.example.dam_front.models.User> {
        val accessible = mutableListOf<com.example.dam_front.models.User>()
        for (c in children) {
            val id = c.id
            if (id.isNullOrBlank()) continue
            try {
                // We only need to check access; ignore the returned list
                suiviService.getSuiviEnfantsByEnfantId(id)
                accessible.add(c)
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 403) {
                    Log.w("ParentHomeVM", "filterAccessibleChildren: child $id inaccessible: ${e.message}")
                } else {
                    Log.e("ParentHomeVM", "filterAccessibleChildren error for child $id", e)
                }
            }
        }
        return accessible
    }
}
