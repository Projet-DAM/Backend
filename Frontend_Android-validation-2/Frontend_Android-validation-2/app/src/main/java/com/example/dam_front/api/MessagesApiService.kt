package com.example.dam_front.api

import com.example.dam_front.data.CreateMessageRequest
import com.example.dam_front.data.MessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface MessagesApiService {
    @POST("messages")
    suspend fun sendMessage(@Body request: CreateMessageRequest): MessageDto

    @GET("messages/conversation/{conversationId}")
    suspend fun getConversationMessages(@Path("conversationId") conversationId: String): List<MessageDto>

    @GET("messages/conversations/my")
    suspend fun getMyConversationIds(): List<String>

    @PATCH("messages/{messageId}/read")
    suspend fun markMessageAsRead(@Path("messageId") messageId: String): MessageDto
}
