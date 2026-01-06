package com.example.dam_front.api

import com.example.dam_front.models.GoogleGeminiRequest
import com.example.dam_front.models.GoogleGeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GoogleGeminiService {
    @POST
    suspend fun generateContent(
        @retrofit2.http.Url url: String,
        @Query("key") apiKey: String,
        @Body request: GoogleGeminiRequest
    ): GoogleGeminiResponse
}
