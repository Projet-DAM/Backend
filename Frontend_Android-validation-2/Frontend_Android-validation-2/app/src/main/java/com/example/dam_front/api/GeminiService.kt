package com.example.dam_front.api

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiService {
    private const val API_KEY = "AIzaSyDxaiFbuWjUourPzOHxFeEY0pAVOZBSuYQ"
    
    // We use gemini-flash-latest as discovered to be available and working for this key
    private val model = GenerativeModel(
        modelName = "gemini-flash-latest",
        apiKey = API_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
            maxOutputTokens = 2048
        }
    )

    suspend fun generateSummary(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            println("🤖 Gemini: Generating summary...")
            val response = model.generateContent(prompt)
            response.text
        } catch (e: Exception) {
            println("❌ Gemini Error: ${e.message}")
            null
        }
    }
}
