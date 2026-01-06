package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.ImageUtils

/**
 * Composant réutilisable pour afficher l'image d'un tournoi avec :
 * - Placeholder pendant le chargement
 * - Image d'erreur si le chargement échoue
 * - Placeholder si pas d'image
 * - Cache désactivé pour les tests
 * - Vérification de la taille du composable
 * 
 * SOLUTION : Utilisation de SubcomposeAsyncImage pour un contrôle complet des états
 */
@Composable
fun TournoiImage(
    imagePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showSportName: Boolean = true,
    sportName: String? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var composableWidth by remember { mutableStateOf(0) }
    var composableHeight by remember { mutableStateOf(0) }
    
    // Détecter si c'est un URI local (content://) ou une URL HTTP
    val imageData = remember(imagePath) {
        when {
            imagePath.isNullOrEmpty() -> {
                android.util.Log.d("TournoiImage", "Image path est null ou vide")
                null
            }
            imagePath.startsWith("content://") -> {
                // URI local - essayer de l'utiliser, mais ces URIs peuvent ne plus être accessibles
                android.util.Log.d("TournoiImage", "⚠️ URI content:// détecté: $imagePath")
                android.util.Log.w("TournoiImage", "Les URIs content:// peuvent ne pas être accessibles après la création")
                try {
                    val uri = android.net.Uri.parse(imagePath)
                    android.util.Log.d("TournoiImage", "URI parsé avec succès")
                    uri
                } catch (e: Exception) {
                    android.util.Log.e("TournoiImage", "❌ Erreur lors du parsing de l'URI: ${e.message}")
                    null
                }
            }
            imagePath.startsWith("http://") || imagePath.startsWith("https://") -> {
                // URL HTTP - utiliser directement
                android.util.Log.d("TournoiImage", "URL HTTP détectée: $imagePath")
                imagePath
            }
            else -> {
                // Chemin relatif - construire l'URL complète
                val fullUrl = ImageUtils.buildImageUrl(imagePath)
                android.util.Log.d("TournoiImage", "Chemin relatif détecté: $imagePath -> $fullUrl")
                fullUrl
            }
        }
    }
    
    LaunchedEffect(imageData) {
        android.util.Log.d("TournoiImage", "=== DEBUT CHARGEMENT IMAGE ===")
        android.util.Log.d("TournoiImage", "Image data: '$imageData'")
        android.util.Log.d("TournoiImage", "Type: ${imageData?.javaClass?.simpleName}")
    }
    
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                composableWidth = with(density) { coordinates.size.width.toDp().value.toInt() }
                composableHeight = with(density) { coordinates.size.height.toDp().value.toInt() }
                if (composableWidth > 0 && composableHeight > 0) {
                    android.util.Log.d("TournoiImage", "Taille: ${composableWidth}x${composableHeight}px")
                }
            }
    ) {
        when {
            imageData != null -> {
                // Créer une requête d'image avec configuration correcte
                val imageRequest = remember(imageData) {
                    val builder = ImageRequest.Builder(context)
                        .data(imageData)
                        .crossfade(true)
                        .scale(Scale.FILL)
                        .allowHardware(true)
                    
                    // Pour les URLs HTTP, activer le cache réseau
                    if (imageData is String && (imageData.startsWith("http://") || imageData.startsWith("https://"))) {
                        builder.diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.ENABLED)
                    } else {
                        // Pour les URIs locaux, désactiver le cache réseau
                        builder.diskCachePolicy(CachePolicy.DISABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .networkCachePolicy(CachePolicy.DISABLED)
                    }
                    
                    builder.listener(
                        onStart = {
                            android.util.Log.d("TournoiImage", "⏳ LISTENER: Début chargement: $imageData")
                        },
                        onSuccess = { _, result ->
                            android.util.Log.d("TournoiImage", "✅✅✅ LISTENER: SUCCÈS: $imageData")
                            android.util.Log.d("TournoiImage", "Taille drawable: ${result.drawable?.intrinsicWidth}x${result.drawable?.intrinsicHeight}")
                        },
                        onError = { _, result ->
                            val error = result.throwable
                            android.util.Log.e("TournoiImage", "❌ LISTENER: ERREUR: $imageData")
                            android.util.Log.e("TournoiImage", "Type: ${error.javaClass.simpleName}")
                            android.util.Log.e("TournoiImage", "Message: ${error.message}")
                            android.util.Log.e("TournoiImage", "Cause: ${error.cause?.message}")
                            error.printStackTrace()
                        }
                    )
                    builder.build()
                }
                
                // Utiliser SubcomposeAsyncImage avec gestion des états
                SubcomposeAsyncImage(
                    model = imageRequest,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    loading = {
                        android.util.Log.d("TournoiImage", "🔄 STATE: Loading - Data: $imageData")
                        ImagePlaceholder(
                            modifier = Modifier.fillMaxSize(),
                            showLoading = true,
                            sportName = if (showSportName) sportName else null
                        )
                    },
                    error = { state ->
                        val error = state.result.throwable
                        android.util.Log.e("TournoiImage", "❌ STATE: Error - Data: $imageData")
                        if (error != null) {
                            android.util.Log.e("TournoiImage", "Erreur type: ${error.javaClass.simpleName}")
                            android.util.Log.e("TournoiImage", "Erreur message: ${error.message}")
                            android.util.Log.e("TournoiImage", "Stack trace:")
                            error.printStackTrace()
                        } else {
                            android.util.Log.e("TournoiImage", "Erreur mais pas de throwable disponible")
                        }
                        ImagePlaceholder(
                            modifier = Modifier.fillMaxSize(),
                            showLoading = false,
                            sportName = if (showSportName) sportName else null,
                            showError = true
                        )
                    },
                    success = {
                        android.util.Log.d("TournoiImage", "✅ STATE: Success - Data: $imageData")
                        SubcomposeAsyncImageContent()
                    }
                )
            }
            else -> {
                ImagePlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    showLoading = false,
                    sportName = if (showSportName) sportName else null,
                    showError = false
                )
            }
        }
    }
}

@Composable
private fun ImagePlaceholder(
    modifier: Modifier = Modifier,
    showLoading: Boolean = false,
    sportName: String? = null,
    showError: Boolean = false
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(IconOrangeLight, IconOrangeAccent)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showLoading) {
                CircularProgressIndicator(
                    color = IconOrange,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (showError) Color.Red else IconOrange
                )
            }
            
            if (!sportName.isNullOrEmpty()) {
                Text(
                    text = sportName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = IconOrangeDark
                )
            }
            
            if (showError) {
                Text(
                    text = "Erreur de chargement",
                    fontSize = 10.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
