package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Child(
    @SerializedName("_id") val id: String,
    val prenom: String,
    val nom: String,
    val fullName: String,
    val ownedByRequester: Boolean,
    val dateNaissance: String? = null,
    val photoProfil: String? = null
)
