package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName(value="id", alternate = ["_id"]) val id: String? = null,
    val email: String,
    val nom: String,
    val prenom: String,
    val role: String,
    @SerializedName("photoProfil") val photoProfil: String? = null,
    val ownedByRequester: Boolean? = null
)
