package com.example.dam_front.email

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface EmailJsApi {
    @Headers(
        "Content-Type: application/json",
        "origin: http://localhost" // Helps bypass some origin checks
    )
    @POST("api/v1.0/email/send")
    suspend fun sendEmail(@Body emailRequest: EmailRequest): Response<Void>
}
