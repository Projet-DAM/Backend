package com.example.dam_front.email

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object EmailRetrofitClient {
    private const val BASE_URL = "https://api.emailjs.com/"

    // Singleton de l'API EmailJS
    val api: EmailJsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EmailJsApi::class.java)
    }
}
