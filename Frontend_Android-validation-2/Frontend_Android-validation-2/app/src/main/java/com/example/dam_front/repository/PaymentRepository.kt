package com.example.dam_front.repository

import android.content.Context
import com.example.dam_front.api.RetrofitClient

class PaymentRepository(context: Context) {
    private val apiService = RetrofitClient.getApiService(context)

    suspend fun createPaymentIntent(amount: Double, currency: String = "eur", phoneNumber: String? = null): Result<String> {
        return try {
            // Amount in cents
            val amountInCents = (amount * 100).toInt()
            val request = mutableMapOf<String, Any>(
                "amount" to amountInCents,
                "currency" to currency
            )
            
            if (phoneNumber != null) {
                request["phoneNumber"] = phoneNumber
            }

            val response = apiService.createPaymentIntent(request)
            if (response.isSuccessful && response.body() != null) {
                val clientSecret = response.body()!!["clientSecret"]
                if (clientSecret != null) {
                    Result.success(clientSecret)
                } else {
                    Result.failure(Exception("Client secret not found"))
                }
            } else {
                Result.failure(Exception("Failed to create payment intent: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun confirmPayment(paymentIntentId: String): Result<Boolean> {
        return try {
            val request = mapOf("paymentIntentId" to paymentIntentId)
            val response = apiService.confirmPayment(request)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to confirm payment: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
