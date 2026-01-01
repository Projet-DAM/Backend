package com.example.dam_front.api

import android.content.Context
import android.util.Log
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {
    private val TAG = "AuthInterceptor"
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val tokenManager = TokenManager(context)
        val token = runBlocking {
            tokenManager.getToken().first()
        }
        
        val request = chain.request().newBuilder()
        token?.let {
            request.addHeader("Authorization", "Bearer $it")
        }
        
        val response = chain.proceed(request.build())
        
        // Si on reçoit un 401, le token est expiré ou invalide
        if (response.code == 401) {
            Log.w(TAG, "⚠️ Token expiré ou invalide (401) - Nettoyage du token")
            Log.w(TAG, "⚠️ Token expiré ou invalide (401) - TODO: Gérer le refresh token ou la déconnexion contrôlée")
            // runBlocking {
            //     tokenManager.clearToken()
            // }
        }
        
        return response
    }
}










