package com.example.dam_front.models

data class UpdateUserRequest(
    val nom: String? = null,
    val prenom: String? = null,
    val email: String? = null,
    val dateNaissance: String? = null,
    val photoProfil: String? = null
)
