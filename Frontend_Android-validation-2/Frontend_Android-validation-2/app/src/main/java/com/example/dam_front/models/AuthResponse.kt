package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    val user: User? = null,
    val message: String? = null,
    val userId: String? = null,
    val email: String? = null
)


