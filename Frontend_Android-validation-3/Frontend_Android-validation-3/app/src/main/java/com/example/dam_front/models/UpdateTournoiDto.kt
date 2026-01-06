package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

/**
 * DTO pour la mise à jour d'un tournoi
 * Correspond exactement au UpdateTournoiDto du backend
 * Ne contient QUE les champs autorisés par le backend
 */
data class UpdateTournoiDto(
    @SerializedName("nom")
    val nom: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("sport")
    val sport: String? = null,
    
    @SerializedName("categorieAge")
    val categorieAge: String? = null,
    
    @SerializedName("dateDebut")
    val dateDebut: String? = null,
    
    @SerializedName("dateFin")
    val dateFin: String? = null,
    
    @SerializedName("lieu")
    val lieu: String? = null,
    
    @SerializedName("nombreParticipantsMax")
    val nombreParticipantsMax: Int? = null,
    
    @SerializedName("fraisParticipation")
    val fraisParticipation: Double? = null,
    
    @SerializedName("etat")
    val etat: String? = null,
    
    @SerializedName("niveau")
    val niveau: String? = null,
    
    @SerializedName("recompense")
    val recompense: String? = null
)









