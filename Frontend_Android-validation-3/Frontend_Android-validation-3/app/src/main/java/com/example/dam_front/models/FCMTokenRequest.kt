package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class FCMTokenRequest(
    @SerializedName("token")
    val token: String,
    
    @SerializedName("userId")
    val userId: String? = null,
    
    @SerializedName("role")
    val role: String? = null
)


