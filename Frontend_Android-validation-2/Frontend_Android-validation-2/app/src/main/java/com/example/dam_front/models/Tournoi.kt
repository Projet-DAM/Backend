package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Tournoi(
    @SerializedName("_id")
    val id: String? = null,
    
    @SerializedName("nom")
    val nom: String? = null,
    
    @SerializedName("sport")
    val sport: String? = null,
    
    @SerializedName("lieu")
    val lieu: String? = null,
    
    @SerializedName("dateDebut")
    val dateDebut: String? = null,
    
    @SerializedName("dateFin")
    val dateFin: String? = null,
    
    @SerializedName("description")
    val description: String? = null,
    
    @SerializedName("categorieAge")
    val categorieAge: String? = null,
    
    @SerializedName("nombreParticipantsMax")
    val nombreParticipantsMax: Int? = null,
    
    @SerializedName("fraisParticipation")
    val fraisParticipation: Double? = null,
    
    @SerializedName("prix")
    val prix: Double? = null, // Alias pour compatibilité
    
    @SerializedName("etat")
    val etat: String? = null,
    
    @SerializedName("niveau")
    val niveau: String? = null,
    
    @SerializedName("recompense")
    val recompense: String? = null,
    
    @SerializedName("image")
    val image: String? = null, // Champ principal du backend
    
    @SerializedName("imageUrl")
    val imageUrl: String? = null, // Alias pour compatibilité avec anciennes données
    
    @SerializedName("statut")
    val statut: String? = null,
    
    @SerializedName("participantsCount")
    val participantsCount: Int? = null,
    
    @SerializedName("nombreParticipantsActuels")
    val nombreParticipantsActuels: Int? = null,
    
    @SerializedName("inscriptionsCount")
    val inscriptionsCount: Int? = null,
    
    @SerializedName("participants")
    val participants: List<Participant>? = null,
    
    // Ces champs sont lus depuis le backend mais ne doivent PAS être envoyés lors des mises à jour
    // Ils seront exclus manuellement dans le ViewModel
    @SerializedName("createdAt")
    val createdAt: String? = null,
    
    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

fun Tournoi.currentParticipantsCount(): Int? {
    return participantsCount
        ?: nombreParticipantsActuels
        ?: inscriptionsCount
        ?: participants?.size
}

fun Tournoi.remainingSlots(currentParticipantsOverride: Int? = null): Int? {
    val max = nombreParticipantsMax ?: return null
    val current = currentParticipantsOverride ?: currentParticipantsCount() ?: return null
    return (max - current).coerceAtLeast(0)
}

fun Tournoi.isComplet(currentParticipantsOverride: Int? = null): Boolean {
    val statusFlag = listOf(statut, etat).any { it?.equals("complet", ignoreCase = true) == true }
    val max = nombreParticipantsMax
    val current = currentParticipantsOverride ?: currentParticipantsCount()
    val capacityFlag = max != null && current != null && current >= max
    return statusFlag || capacityFlag
}


