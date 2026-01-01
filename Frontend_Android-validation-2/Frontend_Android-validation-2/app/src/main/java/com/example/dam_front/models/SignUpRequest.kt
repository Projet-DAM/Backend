package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class SignUpRequest(
    val nom: String,
    val prenom: String,
    val email: String,
    @SerializedName("motDePasse")
    val motDePasse: String,
    val role: String, // "parent", "coach", "academie"
    val photoProfil: String? = null,
    
    // Champs spécifiques Coach
    val certification: List<String>? = null,
    val specialite: String? = null,
    val experience: Int? = null,
    
    // Champs spécifiques Académie
    val nomAcademie: String? = null,
    val adresse: String? = null,
    val description: String? = null,
    val horaires: Map<String, Map<String, String>>? = null
)






