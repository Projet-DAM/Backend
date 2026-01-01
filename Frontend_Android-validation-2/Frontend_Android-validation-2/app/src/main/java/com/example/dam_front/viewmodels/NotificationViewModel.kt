package com.example.dam_front.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.Notification
import com.example.dam_front.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        viewModelScope.launch {
            repository.notificationsFlow.collect { list ->
                _notifications.value = list
                _unreadCount.value = list.count { !it.read }
            }
        }
    }

    fun markAsRead(notificationId: String?) {
        if (notificationId == null) return
        repository.markAsRead(notificationId)
    }

    fun markAllAsRead() {
        repository.markAllAsRead()
    }

    fun clearAll() {
        repository.clearAll()
    }
}

class NotificationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
            val repository = (context.applicationContext as com.example.dam_front.DamApplication).notificationRepository
            @Suppress("UNCHECKED_CAST")
            return NotificationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
