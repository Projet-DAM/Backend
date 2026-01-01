package com.example.dam_front.repository

import com.example.dam_front.api.NetworkErrorHandler
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.api.SuiviEnfantApiService
import com.example.dam_front.data.CreateSuiviEnfantDto
import com.example.dam_front.data.SuiviEnfant
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SuiviRepository(private val context: android.content.Context) {
    private val api: SuiviEnfantApiService = RetrofitClient.createAuthenticatedService(context, SuiviEnfantApiService::class.java)

    suspend fun createSuivi(dto: CreateSuiviEnfantDto): Result<SuiviEnfant> {
        return try {
            val resp = api.createSuiviEnfant(dto)
            
            // Notify widget with delay (sequential)
            delay(600)
            com.example.dam_front.widget.SportyWidget.manualUpdate(context)

            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.map(e))
        }
    }

    suspend fun updateSuivi(id: String, dto: com.example.dam_front.data.UpdateSuiviEnfantDto): Result<SuiviEnfant> {
        return try {
            val resp = api.updateSuiviEnfant(id, dto)
            
            // Notify widget with delay (sequential)
            delay(600)
            com.example.dam_front.widget.SportyWidget.manualUpdate(context)

            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.map(e))
        }
    }

    suspend fun getSuivi(id: String): Result<SuiviEnfant> {
        return try {
            val resp = api.getSuiviEnfantById(id)
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.map(e))
        }
    }

    suspend fun deleteSuivi(id: String): Result<Unit> {
        return try {
            android.util.Log.d("SuiviRepo", "Attempting to delete suivi with ID: $id")
            api.deleteSuiviEnfant(id)
            
            // Notify widget with delay (sequential)
            delay(600)
            com.example.dam_front.widget.SportyWidget.manualUpdate(context)

            android.util.Log.d("SuiviRepo", "Delete successful for suivi ID: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("SuiviRepo", "Delete failed for suivi ID: $id", e)
            Result.failure(NetworkErrorHandler.map(e))
        }
    }

    suspend fun getAvailableParents(): Result<List<com.example.dam_front.models.User>> {
        return try {
            val resp = api.getAvailableParents()
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.map(e))
        }
    }

    suspend fun getAvailableCoaches(): Result<List<com.example.dam_front.models.User>> {
        return try {
            val resp = api.getAvailableCoaches()
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(NetworkErrorHandler.map(e))
        }
    }
}
