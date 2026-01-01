package com.example.dam_front.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.dam_front.models.Notification
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.asStateFlow


class NotificationRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("SportyKidsPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val TAG = "NotificationRepository"
    private val NOTIFICATIONS_KEY = "notifications_list"
    
    private val _notificationsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<Notification>>(emptyList())
    val notificationsFlow = _notificationsFlow.asStateFlow()

    init {
        _notificationsFlow.value = getAllNotificationsFromPrefs()
    }

    private fun getAllNotificationsFromPrefs(): List<Notification> {
        return try {
            val json = prefs.getString(NOTIFICATIONS_KEY, null)
            if (json != null) {
                val type = object : TypeToken<List<Notification>>() {}.type
                gson.fromJson<List<Notification>>(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la récupération des notifications", e)
            emptyList()
        }
    }
    
    suspend fun saveNotification(notification: Notification) {
        try {
            val notifications = getAllNotificationsFromPrefs().toMutableList()
            notifications.add(0, notification) // Ajouter au début de la liste
            
            // Limiter à 100 notifications maximum
            if (notifications.size > 100) {
                notifications.removeAt(notifications.size - 1)
            }
            
            val json = gson.toJson(notifications)
            prefs.edit().putString(NOTIFICATIONS_KEY, json).apply()
            _notificationsFlow.value = notifications
            Log.d(TAG, "Notification sauvegardée: ${notification.body}")
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la sauvegarde de la notification", e)
        }
    }
    
    fun getAllNotifications(): List<Notification> = _notificationsFlow.value
    
    fun getUnreadCount(): Int {
        return _notificationsFlow.value.count { !it.read }
    }
    
    fun markAsRead(notificationId: String?) {
        if (notificationId == null) return
        
        try {
            val notifications = _notificationsFlow.value.toMutableList()
            val index = notifications.indexOfFirst { it.id == notificationId }
            if (index != -1) {
                notifications[index] = notifications[index].copy(read = true)
                val json = gson.toJson(notifications)
                prefs.edit().putString(NOTIFICATIONS_KEY, json).apply()
                _notificationsFlow.value = notifications
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du marquage de la notification comme lue", e)
        }
    }
    
    fun markAllAsRead() {
        try {
            val notifications = _notificationsFlow.value.map { it.copy(read = true) }
            val json = gson.toJson(notifications)
            prefs.edit().putString(NOTIFICATIONS_KEY, json).apply()
            _notificationsFlow.value = notifications
        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du marquage de toutes les notifications comme lues", e)
        }
    }
    
    fun clearAll() {
        prefs.edit().remove(NOTIFICATIONS_KEY).apply()
        _notificationsFlow.value = emptyList()
    }
}


