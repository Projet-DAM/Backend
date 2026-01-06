package com.example.dam_front.api

import com.example.dam_front.models.FeedbackRequest
import com.example.dam_front.models.FeedbackResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API Service pour générer des feedbacks IA avec Gemini
 */
interface GeminiApiService {
    
    /**
     * Génère un feedback motivant personnalisé pour un enfant après un match
     */
    @POST("api/gemini/feedback/generate")
    suspend fun generateFeedback(@Body request: FeedbackRequest): FeedbackResponse
    
    /**
     * Envoie automatiquement le feedback dans les conversations du parent et coach
     */
    @POST("api/gemini/feedback/send-to-conversations")
    suspend fun sendFeedbackToConversations(@Body request: FeedbackRequest): FeedbackResponse
}
