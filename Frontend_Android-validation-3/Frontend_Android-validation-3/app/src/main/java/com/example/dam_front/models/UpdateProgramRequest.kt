package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class UpdateProgramRequest(
    @SerializedName("nom_programme") val nomProgramme: String? = null,
    val description: String? = null,
    val objectif: String? = null,
    val niveau: String? = null,
    val prix: Double? = null,
    val statut: String? = null,
    val activites: List<String>? = null,
    val image: String? = null
)


