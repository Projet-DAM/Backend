package com.example.dam_front.services

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SuiviGeminiService {
    private const val TAG = "SuiviGeminiService"
    // API Key provided by the user
    private const val API_KEY = "AIzaSyCxPVih7f01JaPoJayH84Z4lY-Bb4yZiS0"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent"

    // OkHttpClient instance
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // AI generation might take time
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun generateSummary(
        childName: String,
        suivis: List<String>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val suivisText = suivis.joinToString("\n- ") { it.trim() }

        // Construct the prompt - clean, formatted plain text with consistent markers
        val promptText = """
Tu es un expert en coaching sportif pour enfants chez SportyKids.
Génère un rapport de progression professionnel, encourageant et très organisé pour l'enfant nommé $childName.

Suivis récents à analyser :
- $suivisText

RÈGLES CRUCIALES :
1. NE JAMAIS utiliser de balises HTML (pas de <div>, <p>, etc.).
2. Utilise des emojis pour rendre le rapport vivant.
3. Utilise des lignes de séparation claires.
4. Le ton doit être bienveillant et constructif.

STRUCTURE EXACTE À SUIVRE :

[SECTION_START:RESUME]
🌟 RÉSUMÉ GLOBAL
[Paragraphe de 3-4 lignes résumant la performance et l'attitude]

[SECTION_START:FORTS]
✅ POINTS FORTS
• [Point fort 1]
• [Point fort 2]
• [Point fort 3]

[SECTION_START:AXES]
🚀 AXES D'AMÉLIORATION
• [Axe 1]
• [Axe 2]

[SECTION_START:CONSEILS]
💡 CONSEILS AUX PARENTS
[Conseils pratiques à appliquer à la maison]

[SECTION_START:OBJECTIF]
🎯 PROCHAIN DÉFI
[L'objectif principal pour la séance suivante]

_________________________________________________
Fait avec passion par SportyKids AI
""".trimIndent()


        // JSON Body
        // Structure: { "contents": [{ "parts": [{ "text": "..." }] }] }
        val jsonBody = JSONObject().apply {
            put("contents", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", promptText)
                        })
                    })
                })
            })
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL?key=$API_KEY")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Gemini API call failed", e)
                onError("Erreur de connexion : ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Gemini API Error: $errorBody")
                    onError("Erreur serveur (${response.code})")
                    return
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    try {
                        // Parse JSON response to extract text
                        // Structure: response.candidates[0].content.parts[0].text
                        val json = JSONObject(responseBody)
                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                onSuccess(text)
                            } else {
                                onError("Réponse vide de l'IA")
                            }
                        } else {
                            onError("Aucun résultat généré")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON Parsing Error", e)
                        onError("Erreur de lecture de la réponse")
                    }
                } else {
                    onError("Réponse vide")
                }
            }
        })
    }
}
