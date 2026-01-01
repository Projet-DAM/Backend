package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class ChildResponse(
    @SerializedName("_id")
    val id: String,
    val prenom: String,
    val nom: String,
    @SerializedName("dateNaissance")
    val dateNaissance: String,
    @SerializedName("sportPratique")
    val sportPratique: String,
    @SerializedName("photoProfil")
    val photoProfil: String? = null
)


