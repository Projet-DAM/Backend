package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class InscriptionRequest(
    @SerializedName("tournoiId")
    val tournoiId: String,
    
    @SerializedName("enfantPrenom")
    val enfantPrenom: String,
    
    @SerializedName("enfantNom")
    val enfantNom: String,
    
    @SerializedName("enfantDateNaissance")
    val enfantDateNaissance: String,
    
    @SerializedName("parentPrenom")
    val parentPrenom: String,
    
    @SerializedName("parentNom")
    val parentNom: String,
    
    @SerializedName("parentTelephone")
    val parentTelephone: String,
    
    @SerializedName("montantInscription")
    val montantInscription: Double? = null,
    
    @SerializedName("besoinsParticuliers")
    val besoinsParticuliers: String? = null
)








