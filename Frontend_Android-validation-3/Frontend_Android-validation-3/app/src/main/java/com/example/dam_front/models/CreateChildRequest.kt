package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class CreateChildRequest(
    val prenom: String,
    val nom: String,
    @SerializedName("dateNaissance")
    val dateNaissance: String, // Format: "YYYY-MM-DD"
    @SerializedName("photoProfil")
    val photoProfil: String? = null
)


