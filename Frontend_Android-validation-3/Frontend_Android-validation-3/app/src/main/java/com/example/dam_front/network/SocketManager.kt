package com.example.dam_front.network

import android.util.Log
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.*
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Singleton Socket.io manager for real-time messaging features
 */
object SocketManager {
    
    private const val TAG = "SocketManager"
    
    private var socket: Socket? = null
    private val gson = Gson()
    
    // State flows for real-time events
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()
    
    private val _newMessage = MutableStateFlow<Message?>(null)
    val newMessage: StateFlow<Message?> = _newMessage.asStateFlow()
    
    private val _messageEdited = MutableStateFlow<Message?>(null)
    val messageEdited: StateFlow<Message?> = _messageEdited.asStateFlow()
    
    private val _messageDeletedForMe = MutableStateFlow<Pair<String, String>?>(null) // messageId, userId
    val messageDeletedForMe: StateFlow<Pair<String, String>?> = _messageDeletedForMe.asStateFlow()
    
    private val _messageDeletedForAll = MutableStateFlow<String?>(null) // messageId
    val messageDeletedForAll: StateFlow<String?> = _messageDeletedForAll.asStateFlow()
    
    private val _messageReaction = MutableStateFlow<Triple<String, String, String>?>(null) // messageId, userId, emoji
    val messageReaction: StateFlow<Triple<String, String, String>?> = _messageReaction.asStateFlow()
    
    private val _typingEvent = MutableStateFlow<TypingEvent?>(null)
    val typingEvent: StateFlow<TypingEvent?> = _typingEvent.asStateFlow()
    
    /**
     * Connect to Socket.io server with JWT authentication
     */
    fun connect(token: String) {
        if (socket?.connected() == true) {
            Log.d(TAG, "Already connected")
            return
        }
        
        try {
            val opts = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                timeout = 10_000L
                extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
            }
            
            val normalizedUrl = ApiConfig.BASE_URL.removeSuffix("/")
            
            socket = IO.socket(normalizedUrl, opts).apply {
                
                on(Socket.EVENT_CONNECT) {
                    Log.d(TAG, "Socket connected")
                    _connectionState.value = true
                }
                
                on(Socket.EVENT_DISCONNECT) {
                    Log.d(TAG, "Socket disconnected")
                    _connectionState.value = false
                }
                
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Log.e(TAG, "Connection error: ${args.firstOrNull()}")
                    _connectionState.value = false
                }
                
                // Receive new message
                on("receiveMessage") { args ->
                    handleReceiveMessage(args)
                }
                
                // Message edited
                on("messageEdited") { args ->
                    handleMessageEdited(args)
                }
                
                // Message deleted for me
                on("messageDeletedForMe") { args ->
                    handleMessageDeletedForMe(args)
                }
                
                // Message deleted for all
                on("messageDeletedForAll") { args ->
                    handleMessageDeletedForAll(args)
                }
                
                // Message reaction
                on("messageReaction") { args ->
                    handleMessageReaction(args)
                }
                
                // Typing indicator
                on("typing") { args ->
                    handleTyping(args)
                }
                
                connect()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect socket", e)
        }
    }
    
    /**
     * Disconnect from Socket.io server
     */
    fun disconnect() {
        socket?.disconnect()
        socket = null
        _connectionState.value = false
        Log.d(TAG, "Socket disconnected manually")
    }
    
    /**
     * Join a conversation room
     */
    fun joinConversation(conversationId: String) {
        socket?.emit("joinConversation", conversationId)
        Log.d(TAG, "Joined conversation: $conversationId")
    }
    
    /**
     * Leave a conversation room
     */
    fun leaveConversation(conversationId: String) {
        socket?.emit("leaveConversation", conversationId)
        Log.d(TAG, "Left conversation: $conversationId")
    }
    
    /**
     * Send a message via Socket.io
     */
    fun sendMessage(
        receiver: String,
        conversationId: String,
        type: String,
        content: String? = null,
        mediaUrl: String? = null,
        replyToMessageId: String? = null
    ) {
        val payload = JSONObject().apply {
            put("receiver", receiver)
            put("conversationId", conversationId)
            put("type", type)
            content?.let { put("content", it) }
            mediaUrl?.let { put("mediaUrl", it) }
            replyToMessageId?.let { put("replyToMessageId", it) }
        }
        
        socket?.emit("sendMessage", payload)
        Log.d(TAG, "Sent message via socket: $payload")
    }
    
    /**
     * Send typing indicator
     */
    fun sendTyping(conversationUserId: String, status: String) {
        val payload = JSONObject().apply {
            put("conversationUserId", conversationUserId)
            put("status", status) // "start" or "stop"
        }
        
        socket?.emit("typing", payload)
        Log.d(TAG, "Sent typing: $status for $conversationUserId")
    }
    
    // Event handlers
    
    private fun handleReceiveMessage(args: Array<Any>) {
        try {
            val data = args.firstOrNull()?.toString() ?: return
            val message = gson.fromJson(data, Message::class.java)
            _newMessage.value = message
            Log.d(TAG, "Received message: ${message._id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing received message", e)
        }
    }
    
    private fun handleMessageEdited(args: Array<Any>) {
        try {
            val data = args.firstOrNull()?.toString() ?: return
            val message = gson.fromJson(data, Message::class.java)
            _messageEdited.value = message
            Log.d(TAG, "Message edited: ${message._id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing edited message", e)
        }
    }
    
    private fun handleMessageDeletedForMe(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val messageId = data.getString("messageId")
            val userId = data.getString("userId")
            _messageDeletedForMe.value = Pair(messageId, userId)
            Log.d(TAG, "Message deleted for me: $messageId by $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing deleted for me", e)
        }
    }
    
    private fun handleMessageDeletedForAll(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val messageId = data.getString("messageId")
            _messageDeletedForAll.value = messageId
            Log.d(TAG, "Message deleted for all: $messageId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing deleted for all", e)
        }
    }
    
    private fun handleMessageReaction(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val messageId = data.getString("messageId")
            val userId = data.getString("userId")
            val emoji = data.getString("emoji")
            _messageReaction.value = Triple(messageId, userId, emoji)
            Log.d(TAG, "Message reaction: $messageId by $userId with $emoji")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reaction", e)
        }
    }
    
    private fun handleTyping(args: Array<Any>) {
        try {
            val data = args.firstOrNull() as? JSONObject ?: return
            val userId = data.getString("userId")
            val status = data.getString("status")
            val conversationId = data.getString("conversationId")
            _typingEvent.value = TypingEvent(userId, status, conversationId)
            Log.d(TAG, "Typing event: $userId - $status in $conversationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing typing event", e)
        }
    }
}

data class TypingEvent(
    val userId: String,
    val status: String, // "start" or "stop"
    val conversationId: String
)
