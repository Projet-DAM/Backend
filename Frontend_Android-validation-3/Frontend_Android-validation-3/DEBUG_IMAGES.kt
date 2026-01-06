// Fichier de test pour déboguer les images
// Ce fichier contient du code de test que vous pouvez utiliser temporairement

package com.example.dam_front.debug

import android.util.Log
import com.example.dam_front.utils.ImageUtils

/**
 * Fonction de test pour vérifier la construction des URLs d'images
 * 
 * Utilisation :
 * 1. Ajouter cette fonction dans votre ViewModel ou Activity
 * 2. L'appeler pour tester différentes valeurs
 */
fun testImageUrlConstruction() {
    val testCases = listOf(
        "/uploads/tournois/image.png",
        "uploads/tournois/image.png",
        "http://10.0.2.2:3000/uploads/tournois/image.png",
        "https://example.com/image.png",
        null,
        "",
        "   ",
    )
    
    testCases.forEach { path ->
        val url = ImageUtils.buildImageUrl(path)
        Log.d("TEST_IMAGE_URL", "Path: '$path' -> URL: '$url'")
    }
}

/**
 * Test d'une URL d'image directement
 * 
 * Utilisation :
 * 1. Remplacer temporairement l'URL dans TournoiImage.kt
 * 2. Tester avec une URL connue (ex: https://picsum.photos/400/300)
 */
val TEST_IMAGE_URL = "https://picsum.photos/400/300"

// Pour tester avec une URL locale :
// val TEST_IMAGE_URL = "http://10.0.2.2:3000/uploads/tournois/test-image.png"

