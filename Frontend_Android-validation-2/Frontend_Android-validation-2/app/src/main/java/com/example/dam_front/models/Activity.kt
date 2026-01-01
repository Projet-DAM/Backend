package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Activity(
    @SerializedName("_id") val id: String,
    @SerializedName("nom_activite") val nomActivite: String,
    val image: String? = null,
    val description: String? = null,
    val categorie: String? = null,
    val date: String? = null, // ISO 8601 format
    val heure: String? = null,
    val duree: Int? = null,
    @SerializedName("capacite_max") val capaciteMax: Int? = null,
    val prix: Double? = null,
    val statut: String? = null,
    val academie: String? = null,
    val coach: String? = null, // ID du coach/propriétaire de l'activité
    @SerializedName("programme") val programmeId: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

