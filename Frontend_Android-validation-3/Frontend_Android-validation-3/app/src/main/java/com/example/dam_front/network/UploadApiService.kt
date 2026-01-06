package com.example.dam_front.network

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
import com.example.dam_front.models.UploadResponse

interface UploadApiService {
    
    @Multipart
    @POST("upload/image")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): UploadResponse
    
    @Multipart
    @POST("upload/audio")
    suspend fun uploadAudio(
        @Part audio: MultipartBody.Part
    ): UploadResponse
    
    @Multipart
    @POST("upload/message-image")
    suspend fun uploadMessageImage(
        @Part image: MultipartBody.Part
    ): UploadResponse
    
    @Multipart
    @POST("upload/message-audio")
    suspend fun uploadMessageAudio(
        @Part audio: MultipartBody.Part
    ): UploadResponse
    
    @Multipart
    @POST("upload/suivi-images")
    suspend fun uploadSuiviImages(
        @Part images: List<MultipartBody.Part>
    ): List<UploadResponse>
}