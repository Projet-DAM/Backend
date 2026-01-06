package com.example.dam_front.api

import android.content.Context
import android.util.Log
import com.example.dam_front.config.ApiConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val TAG = "RetrofitClient"
    
    // Créer Gson avec configuration pour gérer les champs null et les dates
    private val gson: Gson = GsonBuilder()
        .setLenient() // Permet de parser des JSON avec des variations
        .serializeNulls() // Seralize les valeurs null
        .setPrettyPrinting() // Pour un meilleur logging
        .create()
    
    private val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
        override fun log(message: String) {
            // Log détaillé avec préfixe pour faciliter le filtrage
            if (message.startsWith("{")) {
                // C'est probablement un JSON
                Log.d(TAG, "📄 JSON Response Body:")
                Log.d(TAG, message)
            } else {
                Log.d(TAG, message)
            }
        }
    }).apply {
        level = HttpLoggingInterceptor.Level.BODY // Affiche le body des requêtes/réponses
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    
    val apiService: ApiService = retrofit.create(ApiService::class.java)
    
    fun createOkHttpClient(context: Context): OkHttpClient {
        val authInterceptor = AuthInterceptor(context)
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    fun createAuthenticatedApiService(context: Context): ApiService {
        return createAuthenticatedService(context, ApiService::class.java)
    }

    // For backward compatibility with existing code
    fun getApiService(context: Context): ApiService {
        return createAuthenticatedApiService(context)
    }

    fun <T> createAuthenticatedService(context: Context, serviceClass: Class<T>): T {
        val authenticatedRetrofit = Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return authenticatedRetrofit.create(serviceClass)
    }
    
    init {
        Log.d(TAG, "RetrofitClient initialisé avec baseUrl: ${ApiConfig.BASE_URL}")
    }
}


