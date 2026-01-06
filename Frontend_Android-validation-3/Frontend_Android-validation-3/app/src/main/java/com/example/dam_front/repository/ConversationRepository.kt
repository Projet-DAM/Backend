package com.example.dam_front.repository

import android.content.Context
import android.util.Log
import com.example.dam_front.network.ConversationApiService
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.apply
import kotlin.collections.count
import kotlin.collections.joinToString
import kotlin.collections.lastOrNull
import kotlin.collections.map
import kotlin.collections.sorted
import kotlin.fold
import kotlin.jvm.java
import kotlin.let
import kotlin.ranges.until
import kotlin.sequences.asSequence
import kotlin.sequences.associateWith

class ConversationRepository(private val context: Context) {

    private val api = RetrofitClient.createAuthenticatedService(context, ConversationApiService::class.java)
    private val prefs = context.getSharedPreferences("conversation_cache", Context.MODE_PRIVATE)

    // ------------------------------------------------------------
    // LOCAL DELETE
    // ------------------------------------------------------------
    fun removeLocalMessage(conversationId: String, messageId: String) {
        val key = "msgs_$conversationId"
        val arrStr = prefs.getString(key, null)
        if (arrStr != null) {
            try {
                val arr = JSONArray(arrStr)
                val newArr = JSONArray()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    if (obj.optString("_id") != messageId) newArr.put(obj)
                }
                prefs.edit().putString(key, newArr.toString()).apply()
            } catch (_: Exception) { }
        }
    }

    // ------------------------------------------------------------
    // LOAD CONVERSATIONS
    // ------------------------------------------------------------
    suspend fun getMyConversations(): Result<List<ConversationPreview>> {
        return try {
            val userId = getCurrentUserId()
            val contactsResult = getAvailableCoaches()

            contactsResult.fold(
                onSuccess = { contacts ->
                    val previews = contacts.map { contact ->
                        val conversationId =
                            listOf(userId, contact.id ?: "").sorted().joinToString("-")
                        createConversationPreview(conversationId, contact)
                    }
                    Result.success(previews)
                },
                onFailure = { error -> Result.failure(error) }
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // GET MESSAGES
    // ------------------------------------------------------------
    suspend fun getMessages(conversationId: String): Result<List<Message>> {
        return try {
            val response = api.getConversationMessages(conversationId)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                if (response.code() == 404) {
                    Result.success(getLocalMessages(conversationId))
                } else {
                    Result.failure(kotlin.Exception("Failed to get messages: ${response.code()}"))
                }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // SEND TEXT MESSAGE
    // ------------------------------------------------------------
    suspend fun sendMessage(
        receiverId: String,
        conversationId: String,
        content: String,
        replyToMessageId: String? = null
    ): Result<Message> {
        return try {
            val request = ConversationSendMessageRequest(
                receiver = receiverId,
                conversationId = conversationId,
                type = "text",
                content = content,
                mediaUrl = null,
                replyToMessageId = replyToMessageId
            )

            // New API uses global messages endpoint
            val response = api.sendMessage(conversationId, request)

            if (response.isSuccessful) {
                response.body()?.let {
                    saveLocalMessage(conversationId, it)
                    return Result.success(it)
                }
                Result.failure(kotlin.Exception("Empty response"))
            } else {
                if (response.code() == 404) {
                    // Fallback not available on messages API; surface failure cleanly
                    Result.failure(kotlin.Exception("Failed to send message: ${response.code()}"))
                } else {
                    Result.failure(kotlin.Exception("Failed to send message: ${response.code()}"))
                }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // SEND IMAGE MESSAGE
    // ------------------------------------------------------------
    suspend fun sendImageMessage(
        receiverId: String,
        conversationId: String,
        imageUrl: String
    ): Result<Message> {

        return try {
            val request = ConversationSendMessageRequest(
                receiver = receiverId,
                conversationId = conversationId,
                type = "image",
                mediaUrl = imageUrl
            )

            val response = api.sendMessage(conversationId, request)

            if (response.isSuccessful) {
                response.body()?.let {
                    saveLocalMessage(conversationId, it)
                    return Result.success(it)
                }
                Result.failure(kotlin.Exception("Empty response"))

            } else {
                Result.failure(kotlin.Exception("Failed to send image message: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // MARK READ
    // ------------------------------------------------------------
    suspend fun markMessageAsRead(messageId: String): Result<Message> {
        // Ignorer les messages de l'IA (générés localement) pour éviter les erreurs 404 sur le serveur
        if (messageId.startsWith("ai_")) {
            return Result.failure(Exception("AI Message - local only"))
        }

        return try {
            val response = api.markMessageAsRead(messageId)
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(kotlin.Exception("Empty response"))
            } else {
                Result.failure(kotlin.Exception("Failed to mark message as read: ${response.code()}"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ------------------------------------------------------------
    // USERS
    // ------------------------------------------------------------
    suspend fun getAvailableParents(): Result<List<User>> {
        return try {
            val r = api.getAvailableParents()
            if (r.isSuccessful) Result.success(r.body() ?: emptyList())
            else Result.failure(kotlin.Exception("Failed to get parents: ${r.code()}"))

        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getAvailableCoaches(): Result<List<User>> {
        return try {
            val r = api.getAvailableCoaches()
            if (r.isSuccessful) Result.success(r.body() ?: emptyList())
            else Result.failure(kotlin.Exception("Failed to get coaches: ${r.code()}"))

        } catch (e: Exception) { Result.failure(e) }
    }

    // ------------------------------------------------------------
    // PREVIEW BUILDER
    // ------------------------------------------------------------
    suspend fun createConversationPreview(
        conversationId: String,
        otherUser: User
    ): ConversationPreview {

        val messages = getMessages(conversationId).getOrNull() ?: emptyList()

        val lastMessage = messages.lastOrNull()
        val currentUserId = getCurrentUserId()

        val unreadCount = messages.count {
            !it.read && it.receiver._id == currentUserId
        }

        return ConversationPreview(
            conversationId = conversationId,
            otherUser = MessageUser(
                _id = otherUser.id ?: "",
                nom = otherUser.nom,
                prenom = otherUser.prenom,
                email = otherUser.email,
                photoProfil = otherUser.photoProfil
            ),
            lastMessage = lastMessage,
            unreadCount = unreadCount
        )
    }

    // ------------------------------------------------------------
    // CURRENT USER
    // ------------------------------------------------------------
    private fun getCurrentUserId(): String {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_id", "") ?: ""
    }

    // ------------------------------------------------------------
    // LOCAL STORAGE
    // ------------------------------------------------------------
    private fun getLocalMessages(conversationId: String): List<Message> {
        val key = "msgs_$conversationId"
        val json = prefs.getString(key, null) ?: return emptyList()

        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Message>()

            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)

                val sender = MessageUser(
                    _id = o.getJSONObject("sender").optString("_id"),
                    nom = o.getJSONObject("sender").optString("nom", null),
                    prenom = o.getJSONObject("sender").optString("prenom", null),
                    email = o.getJSONObject("sender").optString("email", null),
                    photoProfil = o.getJSONObject("sender").optString("photoProfil", null)
                )

                val receiver = MessageUser(
                    _id = o.getJSONObject("receiver").optString("_id"),
                    nom = o.getJSONObject("receiver").optString("nom", null),
                    prenom = o.getJSONObject("receiver").optString("prenom", null),
                    email = o.getJSONObject("receiver").optString("email", null),
                    photoProfil = o.getJSONObject("receiver").optString("photoProfil", null)
                )

                list.add(
                    Message(
                        _id = o.optString("_id", null),
                        sender = sender,
                        receiver = receiver,
                        conversationId = o.optString("conversationId"),
                        type = MessageType.valueOf(o.optString("type", "text")),
                        content = o.optString("content", null),
                        mediaUrl = o.optString("mediaUrl", null),
                        createdAt = o.optString("createdAt"),
                        read = o.optBoolean("read", false),
                        edited = o.optBoolean("edited", false),
                        editedAt = o.optString("editedAt", null),
                        isDeletedForAll = o.optBoolean("isDeletedForAll", false),
                        deletedFor = if (o.has("deletedFor")) {
                            val arrDel = o.optJSONArray("deletedFor")
                            (0 until (arrDel?.length() ?: 0))
                                .map { idx -> arrDel!!.optString(idx) }
                        } else emptyList(),
                        reactions = if (o.has("reactions")) {
                            val r = o.optJSONObject("reactions")
                            r?.keys()?.asSequence()?.associateWith { k -> r.optString(k) } ?: emptyMap()
                        } else emptyMap(),
                        replyToMessageId = o.optString("replyToMessageId", null),
                        metadata = if (o.has("metadata")) {
                            val m = o.optJSONObject("metadata")
                            if (m != null) {
                                AiFeedbackMetadata(
                                    matchId = m.optString("matchId", ""),
                                    matchResult = m.optString("matchResult", ""),
                                    teamName = m.optString("teamName", ""),
                                    score = m.optString("score", ""),
                                    phase = m.optString("phase", ""),
                                    emoji = m.optString("emoji", "")
                                )
                            } else null
                        } else null
                    )
                )
            }

            list

        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveLocalMessage(conversationId: String, message: Message) {
        val key = "msgs_$conversationId"
        val arr = try {
            JSONArray(prefs.getString(key, null))
        } catch (_: Exception) {
            JSONArray()
        }

        val obj = JSONObject().apply {
            put("_id", message._id)
            put("conversationId", message.conversationId)
            put("type", message.type.name)
            put("content", message.content)
            put("mediaUrl", message.mediaUrl)
            put("createdAt", message.createdAt)
            put("read", message.read)
            put("edited", message.edited)
            put("editedAt", message.editedAt)
            put("isDeletedForAll", message.isDeletedForAll)
            put("deletedFor", JSONArray(message.deletedFor))
            put("reactions", JSONObject(message.reactions))
            put("replyToMessageId", message.replyToMessageId)
            
            message.metadata?.let { meta ->
                put("metadata", JSONObject().apply {
                    put("matchId", meta.matchId)
                    put("matchResult", meta.matchResult)
                    put("teamName", meta.teamName)
                    put("score", meta.score)
                    put("phase", meta.phase)
                    put("emoji", meta.emoji)
                })
            }

            put("sender", JSONObject().apply {
                put("_id", message.sender._id)
                put("nom", message.sender.nom)
                put("prenom", message.sender.prenom)
                put("email", message.sender.email)
                put("photoProfil", message.sender.photoProfil)
            })

            put("receiver", JSONObject().apply {
                put("_id", message.receiver._id)
                put("nom", message.receiver.nom)
                put("prenom", message.receiver.prenom)
                put("email", message.receiver.email)
                put("photoProfil", message.receiver.photoProfil)
            })
        }

        arr.put(obj)
        prefs.edit().putString(key, arr.toString()).apply()
    }

    // ------------------------------------------------------------
    // NEW FEATURES — BACKEND API CALLS
    // ------------------------------------------------------------
    suspend fun deleteMessageForMe(messageId: String): Result<Unit> {
        return try {
            val response = api.deleteMessageForMe(messageId)
            if (response.isSuccessful) {
                Log.d("ConversationRepo", "deleteMessageForMe success: $messageId")
                Result.success(Unit)
            } else {
                Log.e("ConversationRepo", "deleteMessageForMe failed: ${response.code()}")
                Result.failure(kotlin.Exception("Delete for me failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "deleteMessageForMe exception", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMessageForEveryone(messageId: String): Result<Unit> {
        return try {
            val response = api.deleteMessageForEveryone(messageId)
            if (response.isSuccessful) {
                Log.d("ConversationRepo", "deleteMessageForEveryone success: $messageId")
                Result.success(Unit)
            } else {
                Log.e("ConversationRepo", "deleteMessageForEveryone failed: ${response.code()}")
                Result.failure(kotlin.Exception("Delete for everyone failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "deleteMessageForEveryone exception", e)
            Result.failure(e)
        }
    }

    suspend fun editMessage(messageId: String, newText: String): Result<Message> {
        return try {
            val response = api.editMessage(messageId, EditMessageRequest(content = newText))
            if (response.isSuccessful && response.body() != null) {
                Log.d("ConversationRepo", "editMessage success: $messageId")
                Result.success(response.body()!!)
            } else {
                Log.e("ConversationRepo", "editMessage failed: ${response.code()}")
                Result.failure(kotlin.Exception("Edit failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "editMessage exception", e)
            Result.failure(e)
        }
    }

    suspend fun reactToMessage(messageId: String, emoji: String): Result<Message> {
        return try {
            val response = api.reactToMessage(messageId, ReactRequest(emoji = emoji))
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                Log.d("ConversationRepo", "reactToMessage success: $messageId emoji=$emoji")
                Result.success(updated)
            } else {
                Log.e("ConversationRepo", "reactToMessage failed: ${response.code()}")
                Result.failure(kotlin.Exception("React failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ConversationRepo", "reactToMessage exception", e)
            Result.failure(e)
        }
    }

    suspend fun sendTyping(conversationUserId: String, status: String): Result<Unit> {
        return try {
            val request = TypingRequest(conversationUserId = conversationUserId, status = status)
            val response = api.sendTyping(request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(kotlin.Exception("Failed to send typing: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
