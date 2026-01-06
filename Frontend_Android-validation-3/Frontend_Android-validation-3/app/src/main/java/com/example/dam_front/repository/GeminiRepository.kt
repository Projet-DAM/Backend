package com.example.dam_front.repository

import android.util.Log
import com.example.dam_front.config.ApiConfig
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

class GeminiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    // Liste mise à jour avec les noms EXACTS de votre liste de diagnostic
    private val modelsToTry = listOf(
        "gemini-flash-latest",     // C'est l'équivalent du 1.5 Flash dans votre projet
        "gemini-pro-latest",      // C'est l'équivalent du 1.5 Pro
        "gemini-2.5-flash",       // Modèle plus récent listé chez vous
        "gemini-3-flash-preview"  // Dernier recours
    )

    suspend fun generateActivityDescription(activityName: String, category: String): String = withContext(Dispatchers.IO) {
        val apiKey = ApiConfig.GEMINI_API_KEY
        var lastErrorMessage = ""

        for (modelName in modelsToTry) {
            try {
                Log.d("GeminiRepo", "Tentative avec le modèle EXACT : $modelName")

                val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val prompt = "Rédige une description très courte (2 phrases) pour l'activité '$activityName' (catégorie $category) pour des enfants. Sois motivant."

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
                    val text = jsonResponse.getAsJsonArray("candidates")
                        ?.get(0)?.asJsonObject
                        ?.getAsJsonObject("content")
                        ?.getAsJsonArray("parts")
                        ?.get(0)?.asJsonObject
                        ?.get("text")?.asString

                    if (!text.isNullOrBlank()) {
                        Log.d("GeminiRepo", "Félicitations ! Succès avec $modelName")
                        return@withContext text
                    }
                }

                val errorMsg = "Modèle $modelName a échoué (Code ${response.code})"
                Log.w("GeminiRepo", errorMsg)
                lastErrorMessage = responseBody ?: errorMsg

            } catch (e: Exception) {
                Log.w("GeminiRepo", "Erreur technique avec $modelName : ${e.message}")
                lastErrorMessage = e.message ?: "Erreur inconnue"
            }

            delay(300) // Petit délai avant le prochain essai
        }

        throw Exception("Échec de l'IA (Tous les modèles ont échoué).\nDernière erreur : $lastErrorMessage")
    }
}
