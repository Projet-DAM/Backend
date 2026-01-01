package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Equipe(
    @SerializedName("_id")
    val id: String? = null,
    
    @SerializedName("nom")
    val nom: String,
    
    @SerializedName("couleur")
    val couleur: String,
    
    @SerializedName("formatEquipe")
    val formatEquipe: String, // "5v5", "7v7", "11v11"
    
    @SerializedName("enfants")
    val enfants: List<EnfantEquipe>? = emptyList(),
    
    @SerializedName("tournoiId")
    val tournoiId: String,
    
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class EnfantEquipe(
    @SerializedName("_id")
    val id: String? = null, // ID de l'inscription (correspond à l'ID du participant)
    
    @SerializedName("enfantPrenom")
    val prenom: String,
    
    @SerializedName("enfantNom")
    val nom: String,
    
    @SerializedName("enfantDateNaissance")
    val dateNaissance: String? = null,
    
    @SerializedName("tournoiId")
    val tournoiId: String? = null,
    
    @SerializedName("parentPrenom")
    val parentPrenom: String? = null,
    
    @SerializedName("parentNom")
    val parentNom: String? = null,
    
    @SerializedName("parentTelephone")
    val parentTelephone: String? = null,
    
    @SerializedName("montantInscription")
    val montantInscription: Double? = null,
    
    @SerializedName("besoinsParticuliers")
    val besoinsParticuliers: String? = null,
    
    @SerializedName("participantId")
    val participantId: String? = null // Alias pour compatibilité (identique à id)
)

data class CreateEquipeRequest(
    @SerializedName("nom")
    val nom: String,
    
    @SerializedName("couleur")
    val couleur: String,
    
    @SerializedName("tournoiId")
    val tournoiId: String,
    
    @SerializedName("enfants")
    val enfants: List<String>, // Liste des IDs des participants
    
    @SerializedName("formatEquipe")
    val formatEquipe: String
)

data class UpdateEquipeRequest(
    @SerializedName("nom")
    val nom: String? = null,
    
    @SerializedName("couleur")
    val couleur: String? = null,
    
    @SerializedName("enfants")
    val enfants: List<String>? = null, // Liste des IDs des participants
    
    @SerializedName("formatEquipe")
    val formatEquipe: String? = null
)

// Modèle spécifique pour mettre à jour uniquement les enfants
data class UpdateEquipeEnfantsRequest(
    @SerializedName("ajouter")
    val ajouter: List<String> = emptyList(), // Liste des IDs des participants à ajouter
    
    @SerializedName("retirer")
    val retirer: List<String> = emptyList() // Liste des IDs des participants à retirer
)

