package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Program(
    @SerializedName("_id") val id: String,
    @SerializedName("nom_programme") val nomProgramme: String,
    val image: String? = null,
    val description: String? = null,
    val objectif: String? = null,
    val niveau: String? = null,
    val prix: Double? = null,
    val statut: String = ProgramStatus.BROUILLON.name,
    val activites: List<ProgramActivity> = emptyList(),
    val coach: String? = null,
    @SerializedName("coachName") val coachName: String? = null,
    @SerializedName("coach_name") val coachNameUnderscore: String? = null,
    val academie: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
    @SerializedName("capaciteMaximale") val capaciteMaximale: Int? = null,
    @SerializedName("nombreInscrits") val nombreInscrits: Int? = null
)

enum class ProgramStatus {
    BROUILLON,
    ACTIF,
    ARCHIVE
}


