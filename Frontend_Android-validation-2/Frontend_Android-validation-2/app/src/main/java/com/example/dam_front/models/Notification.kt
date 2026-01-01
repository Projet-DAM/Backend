package com.example.dam_front.models

import com.google.gson.annotations.SerializedName

data class Notification(
    @SerializedName("id")
    val id: String? = null,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("body")
    val body: String,
    
    @SerializedName("enfantPrenom")
    val enfantPrenom: String,
    
    @SerializedName("enfantNom")
    val enfantNom: String,
    
    @SerializedName("tournoiNom")
    val tournoiNom: String,
    
    @SerializedName("timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @SerializedName("read")
    var read: Boolean = false
)


