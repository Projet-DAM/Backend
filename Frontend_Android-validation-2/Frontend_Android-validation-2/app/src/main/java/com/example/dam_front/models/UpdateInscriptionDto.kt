package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class UpdateInscriptionDto(
    @SerializedName("enfantPrenom")
    val enfantPrenom: String? = null,
    
    @SerializedName("enfantNom")
    val enfantNom: String? = null,
    
    @SerializedName("enfantDateNaissance")
    val enfantDateNaissance: String? = null,
    
    @SerializedName("parentPrenom")
    val parentPrenom: String? = null,
    
    @SerializedName("parentNom")
    val parentNom: String? = null,
    
    @SerializedName("parentTelephone")
    val parentTelephone: String? = null,
    
    @SerializedName("montantInscription")
    val montantInscription: Double? = null,
    
    @SerializedName("besoinsParticuliers")
    val besoinsParticuliers: String? = null
)







