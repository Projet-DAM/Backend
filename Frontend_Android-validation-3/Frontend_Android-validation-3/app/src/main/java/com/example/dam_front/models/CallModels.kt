package com.example.dam_front.models

// WebRTC Call Models

data class CallStartRequest(
    val callId: String,
    val callerId: String,
    val calleeId: String,
    val offer: String // SDP offer as JSON string
)

data class CallAnswerRequest(
    val callId: String,
    val answer: String // SDP answer as JSON string
)

data class CallCandidateRequest(
    val callId: String,
    val candidate: String // ICE candidate as JSON string
)

data class CallEndRequest(
    val callId: String
)

data class CallResponse(
    val callId: String,
    val status: CallStatus,
    val callerId: String?,
    val calleeId: String?,
    val offer: String?,
    val answer: String?,
    val createdAt: String?
)

enum class CallStatus {
    ringing,
    in_call,
    ended,
    rejected,
    missed
}

data class CallState(
    val callId: String,
    val isIncoming: Boolean,
    val otherUser: MessageUser,
    val status: CallStatus = CallStatus.ringing,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val isFrontCamera: Boolean = true
)
