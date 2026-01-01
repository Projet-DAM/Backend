package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class UpdateMatchDto(
    @SerializedName("scoreEquipeA")
    val scoreEquipe1: Int? = null,
    
    @SerializedName("scoreEquipeB")
    val scoreEquipe2: Int? = null,
    
    @SerializedName("statut")
    val statut: String? = null // "en_attente", "en_cours", "termine"
)


