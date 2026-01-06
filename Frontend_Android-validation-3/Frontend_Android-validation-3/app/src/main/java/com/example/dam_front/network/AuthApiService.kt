package com.example.dam_front.network

import retrofit2.http.*
import com.example.dam_front.models.*

interface AuthApiService {
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
    
    @POST("auth/register")
    suspend fun register(@Body request: SignUpRequest): AuthResponse
    
    @POST("auth/logout")
    suspend fun logout(): Unit
    
    @GET("users/{userId}")
    suspend fun getUser(@Path("userId") userId: String): User
    
    @GET("auth/profile")
    suspend fun getProfile(): User
    
    @PUT("auth/profile")
    suspend fun updateProfile(@Body user: User): User
}