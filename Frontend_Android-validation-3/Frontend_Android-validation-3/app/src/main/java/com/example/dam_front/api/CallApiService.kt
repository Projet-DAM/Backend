package com.example.dam_front.api

import com.example.dam_front.models.*
import retrofit2.Response
import retrofit2.http.*

interface CallApiService {
    
    // Start a new call (caller creates offer)
    @POST("call/start")
    suspend fun startCall(@Body request: CallStartRequest): Response<CallResponse>
    
    // Answer an incoming call (callee creates answer)
    @POST("call/answer")
    suspend fun answerCall(@Body request: CallAnswerRequest): Response<CallResponse>
    
    // Send ICE candidate
    @POST("call/candidate")
    suspend fun sendCandidate(@Body request: CallCandidateRequest): Response<Unit>
    
    // End call
    @POST("call/end")
    suspend fun endCall(@Body request: CallEndRequest): Response<CallResponse>
    
    // Get call status
    @GET("call/{callId}")
    suspend fun getCallStatus(@Path("callId") callId: String): Response<CallResponse>
}
