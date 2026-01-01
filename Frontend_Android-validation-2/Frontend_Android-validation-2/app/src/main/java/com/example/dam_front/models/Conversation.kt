package com.example.dam_front.models

// Backend-aligned models matching the NestJS API schema

data class MessageUser(
    val _id: String,
    val nom: String?,
    val prenom: String?,
    val email: String?,
    val photoProfil: String?
)

enum class MessageType {
    text, image, audio
}

data class Message(
    val _id: String? = null,
    val sender: MessageUser,
    val receiver: MessageUser,
    val conversationId: String,
    val type: MessageType,
    val content: String? = null,
    val mediaUrl: String? = null,
    val createdAt: String,
    val read: Boolean = false,
    // New features
    val edited: Boolean = false,
    val editedAt: String? = null,
    val isDeletedForAll: Boolean = false,
    val deletedFor: List<String> = emptyList(),
    val reactions: Map<String, String> = emptyMap(), // userId -> emoji
    val replyToMessageId: String? = null
)

data class Conversation(
    val conversationId: String,
    val otherUser: MessageUser,
    val lastMessage: String?,
    val lastMessageTime: String?,
    val unreadCount: Int = 0
)

data class ConversationPreview(
    val conversationId: String,
    val otherUser: MessageUser,
    val lastMessage: Message?,
    val unreadCount: Int = 0
)

data class ConversationSendMessageRequest(
    val receiver: String, // userId string
    val conversationId: String,
    val type: String = "text", // "text"|"image"|"audio"
    val content: String? = null,
    val mediaUrl: String? = null,
    val replyToMessageId: String? = null
)

data class InitConversationRequest(
    val otherUserId: String
)

data class EditMessageRequest(
    val content: String
)

data class ReactRequest(
    val emoji: String
)

data class ReplyMessageRequest(
    val replyToMessageId: String,
    val content: String? = null,
    val mediaUrl: String? = null
)

data class TypingRequest(
    val conversationUserId: String,
    val status: String // "start" or "stop"
)

// Response from backend for getting conversation ID
data class ConversationIdResponse(
    val conversationId: String
)