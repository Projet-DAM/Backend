package com.example.dam_front.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.dam_front.models.*
import com.example.dam_front.repository.CallRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel for managing WebRTC voice/video calls
 * 
 * This handles the signaling layer (offer/answer/ICE candidates) via the backend.
 * For the actual WebRTC peer connection implementation, you'll need to:
 * 1. Use Stream's WebRTC SDK (io.getstream:stream-webrtc-android)
 * 2. Or implement using the standard WebRTC library
 * 
 * This provides the foundation for call state management and backend signaling.
 */
class CallViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "CallViewModel"
    private val repository = CallRepository(application)
    
    // Call state
    private val _callState = MutableStateFlow<CallState?>(null)
    val callState: StateFlow<CallState?> = _callState.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Start outgoing call
     * This sends the call offer to the backend
     */
    fun startCall(otherUser: MessageUser, isVideoCall: Boolean, sdpOffer: String) {
        viewModelScope.launch {
            try {
                val callId = UUID.randomUUID().toString()
                val currentUserId = getCurrentUserId()
                
                // Create call state
                _callState.value = CallState(
                    callId = callId,
                    isIncoming = false,
                    otherUser = otherUser,
                    status = CallStatus.ringing,
                    isCameraOn = isVideoCall
                )
                
                // Send offer to backend
                repository.startCall(
                    callId = callId,
                    callerId = currentUserId,
                    calleeId = otherUser._id,
                    offer = sdpOffer
                ).fold(
                    onSuccess = { response ->
                        Log.d(TAG, "Call started: $callId")
                        _callState.value = _callState.value?.copy(status = response.status)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to start call", error)
                        _error.value = "Failed to start call"
                        _callState.value = null
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting call", e)
                _error.value = "Error starting call"
            }
        }
    }
    
    /**
     * Answer incoming call
     * This sends the answer SDP to the backend
     */
    fun answerCall(callId: String, sdpAnswer: String) {
        viewModelScope.launch {
            try {
                repository.answerCall(callId, sdpAnswer).fold(
                    onSuccess = { response ->
                        _callState.value = _callState.value?.copy(
                            status = CallStatus.in_call
                        )
                        Log.d(TAG, "Call answered: $callId")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to answer call", error)
                        _error.value = "Failed to answer call"
                        endCall()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error answering call", e)
                _error.value = "Error answering call"
            }
        }
    }
    
    /**
     * Send ICE candidate to backend
     */
    fun sendIceCandidate(callId: String, candidate: String) {
        viewModelScope.launch {
            try {
                repository.sendCandidate(callId, candidate).fold(
                    onSuccess = {
                        Log.d(TAG, "ICE candidate sent")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to send ICE candidate", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error sending ICE candidate", e)
            }
        }
    }
    
    /**
     * End call
     */
    fun endCall() {
        viewModelScope.launch {
            try {
                val callId = _callState.value?.callId
                
                if (callId != null) {
                    repository.endCall(callId).fold(
                        onSuccess = {
                            Log.d(TAG, "Call ended")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Failed to end call", error)
                        }
                    )
                }
                
                _callState.value = null
                
            } catch (e: Exception) {
                Log.e(TAG, "Error ending call", e)
            }
        }
    }
    
    /**
     * Set incoming call state
     * Call this when receiving a call notification
     */
    fun setIncomingCall(callId: String, caller: MessageUser, offer: String, isVideoCall: Boolean) {
        _callState.value = CallState(
            callId = callId,
            isIncoming = true,
            otherUser = caller,
            status = CallStatus.ringing,
            isCameraOn = isVideoCall
        )
    }
    
    /**
     * Reject incoming call
     */
    fun rejectCall() {
        viewModelScope.launch {
            endCall()
        }
    }
    
    /**
     * Toggle mute state
     */
    fun toggleMute() {
        val currentState = _callState.value ?: return
        _callState.value = currentState.copy(isMuted = !currentState.isMuted)
    }
    
    /**
     * Toggle camera state
     */
    fun toggleCamera() {
        val currentState = _callState.value ?: return
        _callState.value = currentState.copy(isCameraOn = !currentState.isCameraOn)
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        val currentState = _callState.value ?: return
        _callState.value = currentState.copy(isFrontCamera = !currentState.isFrontCamera)
    }
    
    /**
     * Get current user ID
     */
    private fun getCurrentUserId(): String {
        val sharedPreferences = getApplication<Application>()
            .getSharedPreferences("auth_prefs", android.content.Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_id", "") ?: ""
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        endCall()
    }
}
