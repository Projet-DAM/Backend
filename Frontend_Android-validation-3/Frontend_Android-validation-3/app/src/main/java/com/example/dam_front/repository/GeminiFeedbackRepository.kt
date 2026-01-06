package com.example.dam_front.repository

import android.util.Log
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class GeminiFeedbackRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Liste identique au GeminiRepository pour une robustesse totale
    private val modelsToTry = listOf(
        "gemini-flash-latest",
        "gemini-pro-latest",
        "gemini-2.5-flash",
        "gemini-3-flash-preview"
    )

    suspend fun generateCoachingAdvice(childName: String, topic: String): Result<GeneratedFeedback> = withContext(Dispatchers.IO) {
        val apiKey = ApiConfig.GEMINI_API_KEY
        var lastError = ""

        for (modelName in modelsToTry) {
            try {
                Log.d("GeminiFeedback", "Tentative Coach avec : $modelName")
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val prompt = "Tu es un coach sportif professionnel. Donne un conseil technique et motivant de 20 mots maximum à l'enfant nommé $childName sur le thème : $topic. Utilise des emojis."

                val text = callApi(url, prompt)
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(GeneratedFeedback(
                        text = text,
                        metadata = AiFeedbackMetadata("ia_direct", "coach", topic, "", "Virtual Coach", "🤖")
                    ))
                }
            } catch (e: Exception) {
                lastError = e.message ?: "Erreur"
                Log.w("GeminiFeedback", "Échec $modelName: $lastError")
            }
            delay(200)
        }
        
        // Fallback ultime si tout échoue (sécurité démo)
        Result.success(GeneratedFeedback(
            text = "Continue tes efforts $childName, ta progression sur le thème $topic est impressionnante ! 💪✨",
            metadata = AiFeedbackMetadata("ia_local", "coach", topic, "", "Virtual Coach", "🤖")
        ))
    }

    suspend fun generateFeedbackForChild(
        childName: String,
        matchResult: String,
        teamName: String,
        score: String,
        phase: String
    ): Result<GeneratedFeedback> = withContext(Dispatchers.IO) {
        val apiKey = ApiConfig.GEMINI_API_KEY
        
        for (modelName in modelsToTry) {
            try {
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val prompt = "Coach sportif. Message motivant pour $childName (Match $matchResult, score $score contre $teamName). Très court (15 mots)."

                val text = callApi(url, prompt)
                if (!text.isNullOrBlank()) {
                    return@withContext Result.success(GeneratedFeedback(
                        text = text,
                        metadata = AiFeedbackMetadata("ia_direct", matchResult, teamName, score, phase, "🤖")
                    ))
                }
            } catch (e: Exception) {
                Log.w("GeminiFeedback", "Échec match feedback $modelName")
            }
            delay(200)
        }

        Result.success(GeneratedFeedback(
            text = "Magnifique match $childName ! L'important est d'avoir donné le meilleur de toi-même contre $teamName. 💪⚽",
            metadata = AiFeedbackMetadata("sim", matchResult, teamName, score, phase, "🤖")
        ))
    }

    private fun callApi(url: String, prompt: String): String? {
        val jsonObject = JsonObject().apply {
            val contentsArray = com.google.gson.JsonArray()
            val contentObj = JsonObject()
            val partsArray = com.google.gson.JsonArray()
            val partObj = JsonObject()
            partObj.addProperty("text", prompt)
            partsArray.add(partObj)
            contentObj.add("parts", partsArray)
            contentsArray.add(contentObj)
            add("contents", contentsArray)
        }

        val body = jsonObject.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (response.isSuccessful && responseBody != null) {
            val jsonResponse = gson.fromJson(responseBody, JsonObject::class.java)
            return jsonResponse.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
        }
        return null
    }
}
