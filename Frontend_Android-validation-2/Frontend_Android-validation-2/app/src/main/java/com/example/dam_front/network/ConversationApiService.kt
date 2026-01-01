package com.example.dam_front.network

import retrofit2.Response
import retrofit2.http.*
import com.example.dam_front.models.*

interface ConversationApiService {
    
    @GET("conversations/conversation-id/{userId}")
    suspend fun getConversationId(@Path("userId") userId: String): Response<ConversationIdResponse>
    
    @GET("conversations/my-conversations")
    suspend fun getMyConversations(): Response<List<String>>
    
    @GET("conversations/{conversationId}/messages")
    suspend fun getConversationMessages(@Path("conversationId") conversationId: String): Response<List<Message>>

    // Backend expects messages scoped by conversation; the previous '/conversations/send-message' 404s
    @POST("conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @Path("conversationId") conversationId: String,
        @Body request: ConversationSendMessageRequest
    ): Response<Message>

    // Legacy endpoint fallback used by the backend in some cases
    @POST("conversations/send-message")
    suspend fun sendMessageLegacy(@Body request: ConversationSendMessageRequest): Response<Message>
    
    @PUT("conversations/messages/{messageId}/read")
    suspend fun markMessageAsRead(@Path("messageId") messageId: String): Response<Message>
    
    @GET("conversations/available-parents")
    suspend fun getAvailableParents(): Response<List<User>>
    
    @GET("conversations/available-coaches")
    suspend fun getAvailableCoaches(): Response<List<User>>
    
    @GET("conversations/available-contacts")
    suspend fun getAvailableContacts(): Response<List<User>>
    
    // Message management endpoints
    @DELETE("conversations/messages/{messageId}/delete-for-me")
    suspend fun deleteMessageForMe(@Path("messageId") messageId: String): Response<Unit>
    
    @DELETE("conversations/messages/{messageId}/delete-for-everyone")
    suspend fun deleteMessageForEveryone(@Path("messageId") messageId: String): Response<Unit>
    
    @PUT("conversations/messages/{messageId}/edit")
    suspend fun editMessage(
        @Path("messageId") messageId: String,
        @Body request: EditMessageRequest
    ): Response<Message>
    
    @POST("conversations/messages/{messageId}/react")
    suspend fun reactToMessage(
        @Path("messageId") messageId: String,
        @Body request: ReactRequest
    ): Response<Message>
    
    @POST("conversations/typing")
    suspend fun sendTyping(@Body request: TypingRequest): Response<Unit>
}