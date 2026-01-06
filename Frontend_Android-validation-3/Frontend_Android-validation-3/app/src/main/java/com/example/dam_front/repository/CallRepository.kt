package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.CallApiService
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.*

class CallRepository(context: Context) {
    
    private val api = RetrofitClient.createAuthenticatedService(context, CallApiService::class.java)
    
    /**
     * Start a new call
     */
    suspend fun startCall(
        callId: String,
        callerId: String,
        calleeId: String,
        offer: String
    ): Result<CallResponse> {
        return try {
            val request = CallStartRequest(callId, callerId, calleeId, offer)
            val response = api.startCall(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start call: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Answer an incoming call
     */
    suspend fun answerCall(
        callId: String,
        answer: String
    ): Result<CallResponse> {
        return try {
            val request = CallAnswerRequest(callId, answer)
            val response = api.answerCall(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to answer call: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send ICE candidate
     */
    suspend fun sendCandidate(
        callId: String,
        candidate: String
    ): Result<Unit> {
        return try {
            val request = CallCandidateRequest(callId, candidate)
            val response = api.sendCandidate(request)
            
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send candidate: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * End call
     */
    suspend fun endCall(callId: String): Result<CallResponse> {
        return try {
            val request = CallEndRequest(callId)
            val response = api.endCall(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to end call: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get call status
     */
    suspend fun getCallStatus(callId: String): Result<CallResponse> {
        return try {
            val response = api.getCallStatus(callId)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get call status: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
