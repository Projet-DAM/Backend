package com.example.dam_front.data

import com.google.gson.annotations.SerializedName

enum class MessageType(val value: String) {
    @SerializedName("text") TEXT("text"),
    @SerializedName("image") IMAGE("image"),
    @SerializedName("audio") AUDIO("audio");

    companion object {
        fun fromValue(value: String?): MessageType = when (value?.lowercase()) {
            "image" -> IMAGE
            "audio" -> AUDIO
            else -> TEXT
        }
    }
}

data class MessageDto(
    @SerializedName("_id") val id: String,
    val sender: String,
    val receiver: String,
    val conversationId: String,
    val type: String,
    val content: String?,
    val mediaUrl: String?,
    val read: Boolean,
    @SerializedName("createdAt") val createdAt: String
)

data class CreateMessageRequest(
    val receiver: String,
    val conversationId: String,
    val type: MessageType,
    val content: String? = null,
    val mediaUrl: String? = null
)
