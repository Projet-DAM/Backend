package com.example.dam_front.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.*
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.repository.ConversationRepository
import com.example.dam_front.repository.SuiviRepository
import com.example.dam_front.repository.UploadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.dam_front.utils.TokenManager

data class ConversationUiState(
    val isLoading: Boolean = false,
    val conversations: List<ConversationPreview> = emptyList(),
    val currentConversationId: String? = null,
    val currentOtherUser: MessageUser? = null,
    val messages: List<Message> = emptyList(),
    val availableContacts: List<User> = emptyList(),
    val error: String? = null,
    val isLoadingMessages: Boolean = false,
    val isSendingMessage: Boolean = false,
    val replyingToMessage: Message? = null,
    val isTypingOtherUser: Boolean = false
)

class ConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val conversationRepository = ConversationRepository(application)
    private val authRepository = AuthRepository(application)
    private val suiviRepository = SuiviRepository(application)
    private val uploadRepository = UploadRepository(application)
    private val tokenManager = TokenManager(application)

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    // Generate conversationId using backend logic (sorted userIds)
    private fun generateConversationId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("-")
    }
    fun startAudioCall(userId: String) {
        android.util.Log.d("ConversationVM", "startAudioCall triggered for user: $userId")
        try {
            val ctx = getApplication<Application>()
            val intent = android.content.Intent(ctx, com.example.dam_front.ui.call.CallActivity::class.java)
            intent.putExtra("receiverId", userId)
            intent.putExtra("callType", "audio")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            android.util.Log.d("ConversationVM", "CallActivity launched (audio) for $userId")
        } catch (e: Exception) {
            android.util.Log.e("ConversationVM", "Failed to launch audio call", e)
            _uiState.value = _uiState.value.copy(error = "Impossible d'ouvrir l'appel audio: ${e.message}")
        }
    }

    fun startVideoCall(userId: String) {
        android.util.Log.d("ConversationVM", "startVideoCall triggered for user: $userId")
        try {
            val ctx = getApplication<Application>()
            val intent = android.content.Intent(ctx, com.example.dam_front.ui.call.CallActivity::class.java)
            intent.putExtra("receiverId", userId)
            intent.putExtra("callType", "video")
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
            android.util.Log.d("ConversationVM", "CallActivity launched (video) for $userId")
        } catch (e: Exception) {
            android.util.Log.e("ConversationVM", "Failed to launch video call", e)
            _uiState.value = _uiState.value.copy(error = "Impossible d'ouvrir l'appel vidéo: ${e.message}")
        }
    }

    fun loadMyConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            conversationRepository.getMyConversations()
                .onSuccess { conversationIds ->
                    // For now, just mark as loaded - conversation previews will be built later
                    _uiState.value = _uiState.value.copy(
                        conversations = emptyList(),
                        isLoading = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message,
                        isLoading = false
                    )
                }
        }
    }

    fun loadAvailableContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Wait for token to be available
                val token = tokenManager.getToken().first()
                if (token.isNullOrBlank()) {
                    android.util.Log.w("ConversationVM", "No authentication token available for loading contacts")
                    _uiState.value = _uiState.value.copy(
                        error = "Authentification requise. Veuillez vous reconnecter.",
                        isLoading = false
                    )
                    return@launch
                }

                val userId = authRepository.getUserId()
                val userRole = authRepository.getUserRole()

                if (userId.isNullOrBlank()) {
                    android.util.Log.w("ConversationVM", "User ID is null, cannot load contacts")
                    _uiState.value = _uiState.value.copy(
                        error = "Session expirée. Veuillez vous reconnecter.",
                        isLoading = false
                    )
                    return@launch
                }

                // Log debug info
                android.util.Log.d("ConversationVM", "Loading contacts for user: $userId with role: '$userRole' (uppercase: '${userRole?.uppercase()}')")

                val result = if (userRole?.uppercase() == "COACH") {
                    android.util.Log.d("ConversationVM", "Loading available parents for coach (role: $userRole)")
                    suiviRepository.getAvailableParents()
                } else {
                    android.util.Log.d("ConversationVM", "Loading available coaches for parent/other role (role: $userRole)")
                    suiviRepository.getAvailableCoaches()
                }

                result.onSuccess { contacts ->
                    android.util.Log.d("ConversationVM", "Successfully loaded ${contacts.size} contacts")
                    _uiState.value = _uiState.value.copy(
                        availableContacts = contacts,
                        isLoading = false
                    )
                }.onFailure { error ->
                    android.util.Log.e("ConversationVM", "Failed to load contacts", error)
                    _uiState.value = _uiState.value.copy(
                        error = "Erreur: ${error.message}",
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ConversationVM", "Exception loading contacts", e)
                _uiState.value = _uiState.value.copy(
                    error = "Exception: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun startConversationWith(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMessages = true, error = null)

            try {
                val currentUserId = authRepository.getUserId() ?: ""
                val conversationId = generateConversationId(currentUserId, userId)

                // Find the other user from available contacts
                val otherUser = _uiState.value.availableContacts.find { it.id == userId }

                _uiState.value = _uiState.value.copy(
                    currentConversationId = conversationId,
                    currentOtherUser = otherUser?.let { user ->
                        MessageUser(
                            _id = user.id ?: "",
                            nom = user.nom,
                            prenom = user.prenom,
                            email = user.email,
                            photoProfil = user.photoProfil
                        )
                    },
                    isLoadingMessages = false
                )

                loadMessages(conversationId)

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoadingMessages = false
                )
            }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMessages = true, error = null)

            conversationRepository.getMessages(conversationId)
                .onSuccess { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages.sortedBy { it.createdAt },
                        isLoadingMessages = false
                    )
                }
                .onFailure { error ->
                    // If the server returns 404 for conversation messages, surface empty list and keep UI usable
                    val is404 = error.message?.contains("404") == true
                    _uiState.value = _uiState.value.copy(
                        messages = if (is404) emptyList() else _uiState.value.messages,
                        error = if (is404) null else error.message,
                        isLoadingMessages = false
                    )
                }
        }
    }

    fun sendMessage(receiverId: String, content: String) {
        if (content.isBlank()) return

        val conversationId = _uiState.value.currentConversationId
        if (conversationId.isNullOrBlank()) {
            android.util.Log.w("ConversationVM", "sendMessage blocked: conversationId is null; attempting to start conversation")
            viewModelScope.launch {
                startConversationWith(receiverId)
                val cid = _uiState.value.currentConversationId
                if (cid.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(
                        error = "Conversation non initialisée. Réessayez ou rouvrez la conversation."
                    )
                } else {
                    android.util.Log.d("ConversationVM", "Conversation initialized; retrying send")
                    sendMessage(receiverId, content)
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingMessage = true, error = null)

            val trimmed = content.trim()
            val replyId = _uiState.value.replyingToMessage?._id
            conversationRepository.sendMessage(receiverId, conversationId, trimmed, replyId)
                .onSuccess { message ->
                    val updatedMessages = _uiState.value.messages + message
                    _uiState.value = _uiState.value.copy(
                        messages = updatedMessages.sortedBy { it.createdAt },
                        isSendingMessage = false,
                        replyingToMessage = null
                    )
                    // Also persist locally to survive reopening when server 404s
                    conversationRepository.saveLocalMessage(conversationId, message)
                    android.util.Log.d("ConversationVM", "sendMessage success id=${message._id} count=${updatedMessages.size}")
                }
                .onFailure { error ->
                    // If both primary and legacy endpoints failed (likely 404), optimistically append the message
                    val optimistic = Message(
                        _id = "local-${System.currentTimeMillis()}",
                        sender = MessageUser(authRepository.getUserId() ?: "", null, null, null, null),
                        receiver = MessageUser(receiverId, null, null, null, null),
                        conversationId = conversationId,
                        type = MessageType.text,
                        content = trimmed,
                        mediaUrl = null,
                        createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                        read = false,
                        replyToMessageId = replyId
                    )
                    val updatedMessages = _uiState.value.messages + optimistic
                    _uiState.value = _uiState.value.copy(
                        messages = updatedMessages.sortedBy { it.createdAt },
                        isSendingMessage = false,
                        error = "Le serveur a renvoyé une erreur (ex: 404). Message ajouté localement."
                    )
                    // Persist optimistic message locally so it appears after reopening
                    conversationRepository.saveLocalMessage(conversationId, optimistic)
                    android.util.Log.w("ConversationVM", "sendMessage fallback: appended optimistic message due to error=${error.message}")
                }
        }
    }

    fun setReplyingTo(message: Message?) {
        _uiState.value = _uiState.value.copy(replyingToMessage = message)
    }

    // Typing indicator emission (debounced)
    private var typingJob: kotlinx.coroutines.Job? = null
    fun onTyping(conversationId: String?) {
        if (conversationId.isNullOrBlank()) return
        val otherUserId = _uiState.value.currentOtherUser?._id
        if (otherUserId.isNullOrBlank()) return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            conversationRepository.sendTyping(otherUserId, "typing")
            kotlinx.coroutines.delay(1500)
        }
    }

    fun sendImageMessage(receiverId: String, imageUri: Uri) {
        val conversationId = _uiState.value.currentConversationId
        if (conversationId.isNullOrBlank()) {
            android.util.Log.w("ConversationVM", "sendImageMessage blocked: conversationId is null")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingMessage = true, error = null)

            try {
                // First upload the image
                uploadRepository.uploadMessageImage(imageUri)
                    .onSuccess { uploadResponse ->
                        // Then send message with image URL
                        conversationRepository.sendImageMessage(receiverId, conversationId, uploadResponse.url)
                            .onSuccess { message ->
                                // Add the new message to the current list
                                val updatedMessages = _uiState.value.messages + message
                                _uiState.value = _uiState.value.copy(
                                    messages = updatedMessages.sortedBy { it.createdAt },
                                    isSendingMessage = false
                                )
                                conversationRepository.saveLocalMessage(conversationId, message)
                                android.util.Log.d("ConversationVM", "sendImageMessage success id=${message._id} count=${updatedMessages.size}")
                            }
                            .onFailure { error ->
                                _uiState.value = _uiState.value.copy(
                                    error = error.message,
                                    isSendingMessage = false
                                )
                            }
                    }
                    .onFailure { error ->
                        _uiState.value = _uiState.value.copy(
                            error = "Erreur lors du téléchargement de l'image: ${error.message}",
                            isSendingMessage = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isSendingMessage = false
                )
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            conversationRepository.markMessageAsRead(messageId)
                .onSuccess { updatedMessage ->
                    // Update the message in our current list
                    val updatedMessages = _uiState.value.messages.map { message ->
                        if (message._id == messageId) updatedMessage else message
                    }
                    _uiState.value = _uiState.value.copy(messages = updatedMessages)
                }
        }
    }

    // Message actions: optimistic updates with repository stubs
    fun editMessage(messageId: String, newContent: String) {
        // Optimistically update local state
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date())
        val updatedLocal = _uiState.value.messages.map { msg ->
            if ((msg._id ?: "") == messageId) msg.copy(content = newContent, edited = true, editedAt = nowIso) else msg
        }
        _uiState.value = _uiState.value.copy(messages = updatedLocal)
        val conversationId = _uiState.value.currentConversationId ?: return
        viewModelScope.launch {
            conversationRepository.editMessage(messageId, newContent)
                .onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message)
                }
            updatedLocal.find { (it._id ?: "") == messageId }?.let { conversationRepository.saveLocalMessage(conversationId, it) }
        }
    }

    fun deleteMessageForMe(messageId: String) {
        // Remove from current list for this user
        val filtered = _uiState.value.messages.filterNot { (it._id ?: "") == messageId }
        _uiState.value = _uiState.value.copy(messages = filtered)
        viewModelScope.launch {
            conversationRepository.deleteMessageForMe(messageId)
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
        }
    }

    fun deleteMessageForEveryone(messageId: String) {
        // Mark as deleted for all with placeholder content
        val updated = _uiState.value.messages.map { msg ->
            if ((msg._id ?: "") == messageId) msg.copy(
                content = "Message supprimé",
                type = com.example.dam_front.models.MessageType.text,
                isDeletedForAll = true
            ) else msg
        }
        _uiState.value = _uiState.value.copy(messages = updated)
        val conversationId = _uiState.value.currentConversationId
        viewModelScope.launch {
            conversationRepository.deleteMessageForEveryone(messageId)
                .onFailure { _uiState.value = _uiState.value.copy(error = it.message) }
            conversationId?.let { cid ->
                updated.find { (it._id ?: "") == messageId }?.let { conversationRepository.saveLocalMessage(cid, it) }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearCurrentConversation() {
        _uiState.value = _uiState.value.copy(
            currentConversationId = null,
            currentOtherUser = null,
            messages = emptyList()
        )
    }

    fun reactToMessage(messageId: String, emoji: String) {
        android.util.Log.d("ConversationVM", "reactToMessage start id=$messageId emoji=$emoji")
        val conversationId = _uiState.value.currentConversationId
        if (conversationId.isNullOrBlank()) {
            android.util.Log.w("ConversationVM", "reactToMessage blocked: conversationId is null")
            return
        }

        viewModelScope.launch {
            try {
                val currentUserId = tokenManager.getUserId().first() ?: ""
                val optimistic = _uiState.value.messages.map { msg ->
                    if ((msg._id ?: "") == messageId) {
                        val existing: Map<String, String> = msg.reactions ?: emptyMap()
                        val updated = existing.toMutableMap().apply { put(currentUserId, emoji) }
                        msg.copy(reactions = updated)
                    } else msg
                }
                _uiState.value = _uiState.value.copy(messages = optimistic)

                conversationRepository.reactToMessage(messageId, emoji)
                    .onSuccess { updatedMsg ->
                        val final = _uiState.value.messages.map { m -> if ((m._id ?: "") == messageId) updatedMsg else m }
                        _uiState.value = _uiState.value.copy(messages = final)
                        android.util.Log.d("ConversationVM", "reactToMessage success id=$messageId")
                        conversationId.let { cid ->
                            conversationRepository.saveLocalMessage(cid, updatedMsg)
                        }
                    }
                    .onFailure { err ->
                        android.util.Log.e("ConversationVM", "reactToMessage failed id=$messageId: ${err.message}", err)
                        _uiState.value = _uiState.value.copy(error = err.message)
                    }
            } catch (e: Exception) {
                android.util.Log.e("ConversationVM", "reactToMessage exception id=$messageId", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun markAllVisibleMessagesAsRead() {
        viewModelScope.launch {
            val currentUserId = authRepository.getUserId() ?: return@launch
            val unreadMessages = _uiState.value.messages.filter {
                !it.read && it.receiver._id == currentUserId
            }
            
            unreadMessages.forEach { message ->
                message._id?.let { messageId ->
                    markMessageAsRead(messageId)
                }
            }
        }
    }
}

class ConversationViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ConversationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConversationViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}