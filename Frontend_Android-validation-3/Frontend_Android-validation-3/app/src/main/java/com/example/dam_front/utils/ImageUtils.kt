package com.example.dam_front.utils

import com.example.dam_front.config.ApiConfig

object ImageUtils {
    /**
     * Converts a photo profile path/URL to a complete valid URL.
     * Handles:
     * - Full URLs (http/https) - returns as-is
     * - Relative paths (/uploads/...) - prepends BASE_URL
     * - Null/empty - returns null
     * 
     * @param photoPath The photo path from the backend (can be null, relative, or absolute)
     * @return A valid complete URL or null if no photo
     */
    fun getPhotoUrl(photoPath: String?): String? {
        return when {
            photoPath.isNullOrBlank() -> null
            photoPath.startsWith("http://") || photoPath.startsWith("https://") -> photoPath
            photoPath.startsWith("/") -> "${ApiConfig.BASE_URL.trimEnd('/')}$photoPath"
            else -> "${ApiConfig.BASE_URL.trimEnd('/')}/$photoPath"
        }
    }

    /**
     * Construit l'URL complète de l'image à partir du chemin relatif
     * @param imagePath Le chemin relatif de l'image (ex: /uploads/tournois/image.png)
     * @return L'URL complète de l'image (ex: http://10.0.2.2:3000/uploads/tournois/image.png)
     */
    fun buildImageUrl(imagePath: String?): String? {
        if (imagePath.isNullOrBlank()) {
            android.util.Log.d("ImageUtils", "Image path est null ou vide")
            return null
        }

        val trimmedPath = imagePath.trim()
        
        val fullUrl = when {
            // Si l'URL est déjà complète (commence par http:// ou https://)
            trimmedPath.startsWith("http://") || trimmedPath.startsWith("https://") -> {
                android.util.Log.d("ImageUtils", "URL absolue détectée: $trimmedPath")
                trimmedPath
            }
            // Si le chemin commence par /, on l'ajoute directement à la BASE_URL
            trimmedPath.startsWith("/") -> {
                val url = "${ApiConfig.BASE_URL.trimEnd('/')}$trimmedPath"
                android.util.Log.d("ImageUtils", "Construction URL relative (commence par /):")
                android.util.Log.d("ImageUtils", "  Base URL: ${ApiConfig.BASE_URL}")
                android.util.Log.d("ImageUtils", "  Path: $trimmedPath")
                android.util.Log.d("ImageUtils", "  URL complète: $url")
                url
            }
            // Sinon, on ajoute un / entre la BASE_URL et le chemin
            else -> {
                val url = "${ApiConfig.BASE_URL.trimEnd('/')}/$trimmedPath"
                android.util.Log.d("ImageUtils", "Construction URL relative (sans /):")
                android.util.Log.d("ImageUtils", "  Base URL: ${ApiConfig.BASE_URL}")
                android.util.Log.d("ImageUtils", "  Path: $trimmedPath")
                android.util.Log.d("ImageUtils", "  URL complète: $url")
                url
            }
        }
        
        android.util.Log.d("ImageUtils", "✅ URL finale construite: $fullUrl")
        return fullUrl
    }

    /**
     * Vérifie si une URL d'image est valide
     */
    fun isValidImageUrl(url: String?): Boolean {
        return !url.isNullOrBlank() && (
            url.startsWith("http://") || 
            url.startsWith("https://") || 
            url.startsWith("/")
        )
    }
}

