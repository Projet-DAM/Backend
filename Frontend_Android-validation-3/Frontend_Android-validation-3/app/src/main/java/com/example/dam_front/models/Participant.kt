package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Participant(
    @SerializedName("_id")
    val id: String? = null,
    
    @SerializedName("enfantPrenom")
    val enfantPrenom: String,
    
    @SerializedName("enfantNom")
    val enfantNom: String,
    
    @SerializedName("enfantDateNaissance")
    val enfantDateNaissance: String? = null,
    
    @SerializedName("parentPrenom")
    val parentPrenom: String,
    
    @SerializedName("parentNom")
    val parentNom: String,
    
    @SerializedName("parentTelephone")
    val parentTelephone: String? = null,
    
    @SerializedName("montantInscription")
    val montantInscription: Double? = null,
    
    @SerializedName("besoinsParticuliers")
    val besoinsParticuliers: String? = null,
    
    @SerializedName("tournoiId")
    val tournoiId: String? = null,
    
    @SerializedName("createdAt")
    val createdAt: String? = null
)








