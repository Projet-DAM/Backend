package com.example.dam_front.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream == null) return null

            // Create a temporary file in the cache directory
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
