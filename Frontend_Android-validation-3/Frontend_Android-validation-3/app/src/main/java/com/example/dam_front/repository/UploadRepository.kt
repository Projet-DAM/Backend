package com.example.dam_front.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.network.UploadApiService
import com.example.dam_front.models.UploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.io.copyTo
import kotlin.io.use
import kotlin.jvm.java

class UploadRepository(private val context: Context) {
    private val api: UploadApiService = RetrofitClient.createAuthenticatedService(context, UploadApiService::class.java)

    suspend fun uploadMessageImage(uri: Uri): Result<UploadResponse> {
        return try {
            val file = uriToFile(uri, "image_${System.currentTimeMillis()}.jpg")
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = api.uploadMessageImage(body)
            
            // Clean up temporary file
            file.delete()
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("UploadRepository", "Failed to upload image", e)
            Result.failure(e)
        }
    }

    suspend fun uploadMessageAudio(uri: Uri): Result<UploadResponse> {
        return try {
            val file = uriToFile(uri, "audio_${System.currentTimeMillis()}.mp3")
            val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = api.uploadMessageAudio(body)
            
            // Clean up temporary file
            file.delete()
            
            Result.success(response)
        } catch (e: Exception) {
            Log.e("UploadRepository", "Failed to upload audio", e)
            Result.failure(e)
        }
    }

    private fun uriToFile(uri: Uri, fileName: String): File {
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = File(context.cacheDir, fileName)
        
        inputStream?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return tempFile
    }
}