package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class GeneratedCredentials(
    val email: String,
    @SerializedName("motDePasse") val motDePasse: String
)

data class CreateChildResponse(
    val child: Child,
    val generatedCredentials: GeneratedCredentials?
)
