package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Match(
    @SerializedName("_id")
    val id: String? = null,
    
    @SerializedName("tournoiId")
    val tournoiId: String,
    
    // IDs des équipes (peuvent être des strings ou des objets)
    val equipeAId: String? = null,
    val equipeBId: String? = null,
    
    // Objets équipes complets (enrichis après parsing)
    var equipe1: EquipeMatch? = null,
    var equipe2: EquipeMatch? = null,
    
    @SerializedName("scoreEquipeA")
    val scoreEquipe1: Int? = null, // Mappé depuis scoreEquipeA du backend
    
    @SerializedName("scoreEquipeB")
    val scoreEquipe2: Int? = null, // Mappé depuis scoreEquipeB du backend
    
    @SerializedName("dateMatch")
    val dateMatch: String? = null,
    
    @SerializedName("heureMatch")
    val heureMatch: String? = null,
    
    @SerializedName("statut")
    val statut: String? = null, // "en_attente", "en_cours", "termine"
    
    @SerializedName("phase")
    val phase: String? = null, // "finale", "demi_final", "quart_final", etc.
    
    @SerializedName("round")
    val round: Int? = null, // Pour compatibilité, calculé depuis phase si nécessaire
    
    @SerializedName("ordre")
    val ordre: Int? = null,
    
    @SerializedName("vainqueur")
    val vainqueur: String? = null,
    
    @SerializedName("matchSuivantId")
    val matchSuivantId: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class EquipeMatch(
    @SerializedName("_id")
    val id: String? = null,
    
    @SerializedName("nom")
    val nom: String? = null,
    
    @SerializedName("couleur")
    val couleur: String? = null
)


