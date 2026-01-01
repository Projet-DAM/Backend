package com.example.dam_front.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.repository.SuiviRepository
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.SuiviEnfantApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SuiviSharedViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = SuiviRepository(application) // ADAPTED
    private val suiviService: SuiviEnfantApiService = RetrofitClient.createAuthenticatedService(application, SuiviEnfantApiService::class.java) // ADAPTED

    // Map childId -> list of suivis
    private val _suivisByChild = MutableStateFlow<Map<String, List<SuiviEnfant>>>(emptyMap())
    val suivisByChild: StateFlow<Map<String, List<SuiviEnfant>>> get() = _suivisByChild

    fun refreshForChild(childId: String) {
        viewModelScope.launch {
            try {
                // Fetch suivis for the child from the API service and update cache
                val apiList = try { suiviService.getSuiviEnfantsByEnfantId(childId) } catch (_: Exception) { emptyList<com.example.dam_front.data.SuiviEnfant>() }
                val sorted = apiList.sortedByDescending { it.dateSuivi }
                _suivisByChild.value = _suivisByChild.value + (childId to sorted)
            } catch (_: Exception) {
                // ignore; keep existing cache
            }
        }
    }

    fun addOrUpdateLocal(suivi: SuiviEnfant) {
        val childId = suivi.enfant?.id ?: suivi.enfantId ?: return
        val current = _suivisByChild.value[childId] ?: emptyList()
        val updated = (listOf(suivi) + current).distinctBy { it.id }.sortedByDescending { it.dateSuivi }
        _suivisByChild.value = _suivisByChild.value + (childId to updated)
    }

    fun removeLocal(childId: String, suiviId: String) {
        val current = _suivisByChild.value[childId] ?: emptyList()
        val updated = current.filterNot { it.id == suiviId }
        _suivisByChild.value = _suivisByChild.value + (childId to updated)
    }

    fun loadAllSuivis() {
        viewModelScope.launch {
            try {
                // Get all suivis - backend will filter based on user role (parent/coach/academy)
                val allSuivis = suiviService.getAllSuiviEnfants()
                
                // Group suivis by child ID
                val suivisByChild = allSuivis.groupBy { suivi ->
                    suivi.enfant?.id ?: suivi.enfantId ?: "unknown"
                }.mapValues { (_, suivis) ->
                    suivis.sortedByDescending { it.dateSuivi }
                }
                
                _suivisByChild.value = suivisByChild
            } catch (e: Exception) {
                // Log error but don't crash - keep existing cache
            }
        }
    }
}

class SuiviSharedViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SuiviSharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SuiviSharedViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
