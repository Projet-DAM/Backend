package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

// --- Requête vers Google Gemini API ---
data class GoogleGeminiRequest(
    @SerializedName("contents") val contents: List<Content>
)

data class Content(
    @SerializedName("parts") val parts: List<Part>
)

data class Part(
    @SerializedName("text") val text: String
)

// --- Réponse de Google Gemini API ---
data class GoogleGeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>?
)

data class Candidate(
    @SerializedName("content") val content: Content?,
    @SerializedName("finishReason") val finishReason: String?
)

// --- Modèle interne pour l'UI ---
data class GeneratedFeedback(
    val text: String,
    val metadata: AiFeedbackMetadata
)
