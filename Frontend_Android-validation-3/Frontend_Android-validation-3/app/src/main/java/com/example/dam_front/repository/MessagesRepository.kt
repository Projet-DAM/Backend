package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.MessagesApiService
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.data.CreateMessageRequest
import com.example.dam_front.data.MessageDto
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.first
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first
import kotlin.apply
import kotlin.collections.firstOrNull
import kotlin.jvm.java
import kotlin.text.removeSuffix
import kotlin.to

class MessagesRepository(private val context: Context) {
    private val api: MessagesApiService = RetrofitClient.retrofit.create(MessagesApiService::class.java)
    private val gson = Gson()
    private val tokenManager = TokenManager(context)

    private var socket: Socket? = null

    suspend fun getConversationMessages(conversationId: String): List<MessageDto> =
        api.getConversationMessages(conversationId)

    suspend fun sendMessage(request: CreateMessageRequest): MessageDto =
        api.sendMessage(request)

    suspend fun markMessageAsRead(messageId: String): MessageDto =
        api.markMessageAsRead(messageId)

    suspend fun getConversationIds(): List<String> = api.getMyConversationIds()

    suspend fun connect(conversationId: String, onMessage: (MessageDto) -> Unit) {
        val token = tokenManager.getToken().first() ?: return
        val opts = IO.Options().apply {
            reconnection = true
            timeout = 10_000L
            extraHeaders = mapOf("Authorization" to listOf("Bearer $token"))
        }
        val normalizedUrl = ApiConfig.BASE_URL.removeSuffix("/")
        socket?.disconnect()
        socket = IO.socket(normalizedUrl, opts).apply {
            on(Socket.EVENT_CONNECT) {
                emit("joinConversation", conversationId)
            }
            on("receiveMessage") { args ->
                val payload = args.firstOrNull()?.toString() ?: return@on
                val message = try {
                    gson.fromJson(payload, MessageDto::class.java)
                } catch (ignored: Exception) {
                    null
                }
                if (message != null) {
                    onMessage(message)
                }
            }
            connect()
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket = null
    }
}
