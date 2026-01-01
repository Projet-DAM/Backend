package com.example.dam_front.utils

import android.content.Context
import android.util.Log
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.FCMTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class FirebaseTokenManager(private val context: Context) {
    private val TAG = "FirebaseTokenManager"
    private val apiService = RetrofitClient.createAuthenticatedApiService(context)
    
    suspend fun getFCMToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "✅ FCM Token récupéré: $token")
            token
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la récupération du token FCM", e)
            null
        }
    }
    
    suspend fun subscribeToTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Log.d(TAG, "✅ Abonnement au topic '$topic' réussi")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'abonnement au topic '$topic'", e)
        }
    }
    
    suspend fun unsubscribeFromTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Log.d(TAG, "✅ Désabonnement du topic '$topic' réussi")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du désabonnement du topic '$topic'", e)
        }
    }
    
    suspend fun sendTokenToBackend(token: String, userId: String, userRole: String) {
        try {
            Log.d(TAG, "📤 Envoi du token FCM au backend (PATCH) pour userId=$userId")
            
            // S'abonner au topic "coaches" si c'est un coach
            if (userRole.lowercase().contains("coach") || userRole.lowercase().contains("acadé")) {
                Log.d(TAG, "🔔 Abonnement au topic 'coaches'...")
                subscribeToTopic("coaches")
            }
            
            val request = mapOf("fcmToken" to token)
            
            // On utilise PATCH users/{id}/fcm-token qui est l'endpoint qui fonctionne d'après les logs
            val response = apiService.updateFcmToken(userId, request)
            
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Token FCM enregistré avec succès via PATCH")
            } else {
                Log.w(TAG, "⚠️ Échec via PATCH (code=${response.code()}), tentative via POST legacy...")
                
                // Fallback vers le POST legacy au cas où
                val legacyRequest = FCMTokenRequest(token = token, userId = userId, role = userRole)
                apiService.registerFCMToken(userId, legacyRequest)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception lors de l'envoi du token FCM", e)
        }
    }
}

