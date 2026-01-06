package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

/**
 * Données nécessaires pour générer un feedback IA personnalisé
 */
data class FeedbackRequest(
    @SerializedName("childId")
    val childId: String,
    
    @SerializedName("childName")
    val childName: String,
    
    @SerializedName("childAge")
    val childAge: Int? = null,
    
    @SerializedName("matchId")
    val matchId: String,
    
    @SerializedName("matchResult")
    val matchResult: String, // "victoire", "defaite", "nul"
    
    @SerializedName("teamName")
    val teamName: String,
    
    @SerializedName("score")
    val score: String, // ex: "3-2"
    
    @SerializedName("phase")
    val phase: String, // ex: "finale", "demi-finale", "quart-finale"
    
    @SerializedName("performance")
    val performance: String? = null, // ex: "bon esprit d'équipe", "excellent joueur"
    
    @SerializedName("tournamentName")
    val tournamentName: String? = null
)

/**
 * Réponse du backend contenant le feedback généré par Gemini
 */
data class FeedbackResponse(
    @SerializedName("feedback")
    val feedback: String,
    
    @SerializedName("generatedAt")
    val generatedAt: String,
    
    @SerializedName("childId")
    val childId: String,
    
    @SerializedName("matchId")
    val matchId: String
)

/**
 * Métadonnées du feedback IA dans un message
 */
data class AiFeedbackMetadata(
    @SerializedName("matchId")
    val matchId: String,
    
    @SerializedName("matchResult")
    val matchResult: String,
    
    @SerializedName("teamName")
    val teamName: String,
    
    @SerializedName("score")
    val score: String,
    
    @SerializedName("phase")
    val phase: String,
    
    @SerializedName("emoji")
    val emoji: String // 🏆 pour victoire, 💪 pour défaite, ⚡ pour nul
)
