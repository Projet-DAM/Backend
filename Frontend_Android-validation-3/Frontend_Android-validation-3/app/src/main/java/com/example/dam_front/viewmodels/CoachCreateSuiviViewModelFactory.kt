package com.example.dam_front.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class CoachCreateSuiviViewModelFactory(
    private val application: Application,
    private val enfantId: String = "",
    private val suiviId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoachCreateSuiviViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CoachCreateSuiviViewModel(application, enfantId, suiviId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
