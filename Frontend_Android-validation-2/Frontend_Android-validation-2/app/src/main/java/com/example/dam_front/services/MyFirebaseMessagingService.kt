package com.example.dam_front.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dam_front.MainActivity
import com.example.dam_front.models.Notification
import com.example.dam_front.repository.NotificationRepository
import com.example.dam_front.repository.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val TAG = "MyFirebaseMessaging"
    private val notificationRepository by lazy { 
        (applicationContext as com.example.dam_front.DamApplication).notificationRepository 
    }
    private val userRepository by lazy { UserRepository(applicationContext) }
    
    override fun onNewToken(token: String) {
        Log.d(TAG, "🔄 Nouveau token FCM reçu: $token")
        // Envoyer le token au backend
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenManager = com.example.dam_front.utils.TokenManager(applicationContext)
                val userId = tokenManager.getUserId().first()
                val userRole = tokenManager.getUserRole().first()
                
                Log.d(TAG, "📋 Informations utilisateur - userId: $userId, role: $userRole")
                
                if (userId != null) {
                    try {
                        userRepository.updateFcmToken(userId, token)
                        Log.d(TAG, "✅ Token FCM mis à jour via UserRepository pour l'utilisateur $userId")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erreur lors de la mise à jour via UserRepository", e)
                    }
                }
                
                if (userId != null && userRole != null && userRole.lowercase().contains("coach")) {
                    Log.d(TAG, "👨‍🏫 Coach détecté, envoi du nouveau token et abonnement au topic")
                    val firebaseTokenManager = com.example.dam_front.utils.FirebaseTokenManager(applicationContext)
                    firebaseTokenManager.sendTokenToBackend(token, userId, userRole)
                } else {
                    Log.d(TAG, "ℹ️ Utilisateur non-coach")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de l'envoi du nouveau token au backend", e)
                e.printStackTrace()
            }
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "📨 Message reçu de : ${remoteMessage.from}")
        
        // 1. Toujours traiter et sauvegarder les données si présentes
        val notificationFromData = if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        } else null
        
        // 2. Gérer l'affichage de la notification (pour le mode foreground)
        val notificationPayload = remoteMessage.notification
        
        if (notificationPayload != null) {
            // Cas A : Il y a un objet notification (prioritaire pour l'affichage)
            Log.d(TAG, "🔔 Affichage via payload notification (Foreground)")
            showNotification(
                notificationPayload.title ?: "SportyKids",
                notificationPayload.body ?: "",
                remoteMessage.data["channel_id"]
            )
        } else if (notificationFromData != null) {
            // Cas B : Data message uniquement
            Log.d(TAG, "🔔 Affichage via payload data")
            showNotification(
                notificationFromData.title,
                notificationFromData.body,
                remoteMessage.data["channel_id"]
            )
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>): Notification {
        Log.d(TAG, "📦 Traitement des données : $data")
        
        val enfantPrenom = data["enfantPrenom"] ?: data["enfant_prenom"] ?: ""
        val enfantNom = data["enfantNom"] ?: data["enfant_nom"] ?: ""
        val tournoiNom = data["tournoiNom"] ?: data["tournoi_nom"] ?: ""
        val programName = data["programName"] ?: data["programmeNom"] ?: ""
        
        val title = data["title"] ?: data["subject"] ?: "Mise à jour SportyKids"
        val body = when {
            enfantPrenom.isNotEmpty() && enfantNom.isNotEmpty() && tournoiNom.isNotEmpty() -> 
                "$enfantPrenom $enfantNom s'est inscrit au tournoi $tournoiNom"
            programName.isNotEmpty() -> "Le programme $programName a été mis à jour"
            else -> data["body"] ?: data["message"] ?: data["content"] ?: "Une nouvelle mise à jour est disponible"
        }
        
        val notification = Notification(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            body = body,
            enfantPrenom = enfantPrenom,
            enfantNom = enfantNom,
            tournoiNom = tournoiNom.ifEmpty { programName },
            timestamp = System.currentTimeMillis(),
            read = false
        )
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                notificationRepository.saveNotification(notification)
                Log.d(TAG, "✅ Notification sauvegardée localement (ID: ${notification.id})")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la sauvegarde locale", e)
            }
        }
        
        return notification
    }
    
    private fun showNotification(title: String, body: String, preferredChannelId: String? = null) {
        Log.d(TAG, "🔔 showNotification - Title: $title, Channel: $preferredChannelId")
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Choisir le canal (utiliser le préféré ou le canal par défaut)
        val channelIdToUse = preferredChannelId ?: CHANNEL_ID
        
        // Créer les canaux de notification (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Créer le canal principal
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SportyKids Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications générales de SportyKids"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
            
            // Créer le canal "general" si demandé et différent (utilisé par le backend par défaut)
            if (preferredChannelId != null && preferredChannelId != CHANNEL_ID) {
                val channelName = preferredChannelId.replaceFirstChar { it.uppercase() }
                val extraChannel = NotificationChannel(
                    preferredChannelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(extraChannel)
                Log.d(TAG, "✅ Canal spécifique créé: $preferredChannelId")
            }
        }
        
        // Créer l'intent pour ouvrir l'application
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Construire la notification
        val notification = NotificationCompat.Builder(this, channelIdToUse)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setColor(0xFF4CAF50.toInt())
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "✅ Notification affichée avec ID: $notificationId sur le canal: $channelIdToUse")
    }
    
    companion object {
        private const val CHANNEL_ID = "inscriptions_tournois_channel"
    }
}

