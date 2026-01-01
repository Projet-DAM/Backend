package com.example.dam_front.models

data class UploadResponse(
    val url: String,
    val filename: String? = null,
    val size: Long? = null,
    val mimeType: String? = null
)
