package com.example.dam_front.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dam_front.api.RetrofitClient
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.UpdateTournoiDto
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CoachTournoisViewModel(private val context: Context) : ViewModel() {
    private val apiService = RetrofitClient.createAuthenticatedApiService(context)
    private val TAG = "CoachTournoisViewModel"
    
    fun createTournoi(
        tournoi: Tournoi,
        imageUri: Uri? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== CRÉATION TOURNOI ===")
                Log.d(TAG, "Nom: ${tournoi.nom}")
                Log.d(TAG, "Sport: ${tournoi.sport}")
                Log.d(TAG, "Image URI: $imageUri")
                
                // Si on a une image URI (content://), utiliser l'endpoint multipart
                if (imageUri != null && imageUri.toString().startsWith("content://")) {
                    Log.d(TAG, "📤 Upload avec image (multipart)")
                    
                    // Convertir l'URI en File temporaire et détecter le type MIME
                    val (imageFile, mimeType) = uriToFileWithMimeType(context, imageUri)
                    val imagePart = if (imageFile != null && mimeType != null) {
                        Log.d(TAG, "Type MIME détecté: $mimeType")
                        val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                    } else {
                        Log.e(TAG, "❌ Impossible de créer le fichier ou de détecter le type MIME")
                        null
                    }
                    
                    // Créer les RequestBody pour chaque champ
                    // IMPORTANT: S'assurer que les nombres sont valides avant de les envoyer
                    val nomBody = tournoi.nom?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val sportBody = tournoi.sport?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val categorieAgeBody = tournoi.categorieAge?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val dateDebutBody = tournoi.dateDebut?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val dateFinBody = tournoi.dateFin?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lieuBody = tournoi.lieu?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val descriptionBody = tournoi.description?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    // Pour les nombres, s'assurer qu'ils sont valides (> 0) avant de les envoyer
                    val nombreParticipantsMaxBody = tournoi.nombreParticipantsMax?.takeIf { it > 0 }?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val fraisParticipationBody = tournoi.fraisParticipation?.takeIf { it >= 0 }?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    val etatBody = tournoi.etat?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val niveauBody = tournoi.niveau?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val recompenseBody = tournoi.recompense?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    if (nomBody == null || sportBody == null || categorieAgeBody == null || 
                        dateDebutBody == null || dateFinBody == null || lieuBody == null) {
                        onError("Champs requis manquants")
                        return@launch
                    }
                    
                    val response = apiService.createTournoiWithImage(
                        nom = nomBody,
                        sport = sportBody,
                        categorieAge = categorieAgeBody,
                        dateDebut = dateDebutBody,
                        dateFin = dateFinBody,
                        lieu = lieuBody,
                        description = descriptionBody,
                        nombreParticipantsMax = nombreParticipantsMaxBody,
                        fraisParticipation = fraisParticipationBody,
                        etat = etatBody,
                        niveau = niveauBody,
                        recompense = recompenseBody,
                        image = imagePart
                    )
                    
                    if (response.isSuccessful) {
                        val createdTournoi = response.body()
                        if (createdTournoi?.id != null) {
                            Log.d(TAG, "✅ Tournoi créé avec succès (avec image) - ID: ${createdTournoi.id}")
                            onSuccess()
                        } else {
                            Log.e(TAG, "❌ Backend retourné null ID - Tournoi non sauvegardé")
                            onError("Le tournoi n'a pas été sauvegardé par le serveur (ID manquant)")
                        }
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création"
                        Log.e(TAG, "❌ Erreur création: ${response.code()} - $errorMessage")
                        onError(errorMessage)
                    }
                } else {
                    // Pas d'image ou image déjà sur le serveur, utiliser l'endpoint JSON normal
                    Log.d(TAG, "📤 Upload sans image (JSON)")
                    val response = apiService.createTournoi(tournoi)
                    if (response.isSuccessful) {
                        val createdTournoi = response.body()
                        if (createdTournoi?.id != null) {
                            Log.d(TAG, "✅ Tournoi créé avec succès - ID: ${createdTournoi.id}")
                            onSuccess()
                        } else {
                            Log.e(TAG, "❌ Backend retourné null ID - Tournoi non sauvegardé")
                            onError("Le tournoi n'a pas été sauvegardé par le serveur (ID manquant)")
                        }
                    } else {
                        val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la création"
                        Log.e(TAG, "❌ Erreur création: ${response.code()} - $errorMessage")
                        onError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception lors de la création", e)
                e.printStackTrace()
                onError(e.message ?: "Erreur inconnue")
            }
        }
    }
    
    private fun uriToFileWithMimeType(context: Context, uri: Uri): Pair<File?, String?> {
        return try {
            // Détecter le type MIME depuis l'URI
            val mimeType = context.contentResolver.getType(uri)
            Log.d(TAG, "Type MIME détecté depuis ContentResolver: $mimeType")
            
            // Normaliser le type MIME pour correspondre aux types acceptés par le backend
            val normalizedMimeType = when {
                mimeType == null -> {
                    // Essayer de détecter depuis l'extension du fichier
                    val uriString = uri.toString()
                    when {
                        uriString.contains(".jpg", ignoreCase = true) || uriString.contains(".jpeg", ignoreCase = true) -> "image/jpeg"
                        uriString.contains(".png", ignoreCase = true) -> "image/png"
                        uriString.contains(".gif", ignoreCase = true) -> "image/gif"
                        else -> "image/jpeg" // Par défaut, utiliser JPEG
                    }
                }
                mimeType == "image/jpg" -> "image/jpeg" // Normaliser jpg en jpeg
                mimeType.startsWith("image/") -> mimeType
                else -> {
                    Log.w(TAG, "Type MIME non-image détecté: $mimeType, utilisation de image/jpeg par défaut")
                    "image/jpeg"
                }
            }
            
            // Vérifier que le type est accepté par le backend
            val acceptedTypes = listOf("image/png", "image/jpeg", "image/jpg", "image/gif")
            val finalMimeType = if (normalizedMimeType in acceptedTypes) {
                normalizedMimeType
            } else {
                Log.w(TAG, "Type MIME non accepté: $normalizedMimeType, utilisation de image/jpeg par défaut")
                "image/jpeg"
            }
            
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e(TAG, "❌ Impossible d'ouvrir l'input stream pour l'URI: $uri")
                return Pair(null, null)
            }
            
            // Déterminer l'extension du fichier depuis le type MIME
            val extension = when (finalMimeType) {
                "image/png" -> ".png"
                "image/jpeg", "image/jpg" -> ".jpg"
                "image/gif" -> ".gif"
                else -> ".jpg"
            }
            
            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}$extension")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            
            Log.d(TAG, "✅ Fichier temporaire créé: ${tempFile.absolutePath}")
            Log.d(TAG, "✅ Type MIME final: $finalMimeType")
            Pair(tempFile, finalMimeType)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la conversion URI vers File", e)
            e.printStackTrace()
            Pair(null, null)
        }
    }
    
    fun updateTournoi(
        tournoi: Tournoi,
        imageUri: Uri? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = tournoi.id ?: run {
                    Log.e(TAG, "❌ ID du tournoi manquant")
                    onError("ID du tournoi manquant")
                    return@launch
                }
                
                Log.d(TAG, "=== DÉBUT MISE À JOUR TOURNOI ===")
                Log.d(TAG, "ID: $id")
                Log.d(TAG, "Nom: ${tournoi.nom}")
                Log.d(TAG, "Sport: ${tournoi.sport}")
                Log.d(TAG, "État: ${tournoi.etat}")
                Log.d(TAG, "Niveau: ${tournoi.niveau}")
                Log.d(TAG, "Image URI: $imageUri")
                Log.d(TAG, "nombreParticipantsMax: ${tournoi.nombreParticipantsMax} (type: ${tournoi.nombreParticipantsMax?.javaClass?.simpleName})")
                Log.d(TAG, "fraisParticipation: ${tournoi.fraisParticipation} (type: ${tournoi.fraisParticipation?.javaClass?.simpleName})")
                
                // Si on a une nouvelle image URI (content://), utiliser l'endpoint multipart
                if (imageUri != null && imageUri.toString().startsWith("content://")) {
                    Log.d(TAG, "📤 Mise à jour avec nouvelle image (multipart)")
                    
                    // Convertir l'URI en File temporaire et détecter le type MIME
                    val (imageFile, mimeType) = uriToFileWithMimeType(context, imageUri)
                    val imagePart = if (imageFile != null && mimeType != null) {
                        Log.d(TAG, "Type MIME détecté: $mimeType")
                        val requestFile = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        MultipartBody.Part.createFormData("image", imageFile.name, requestFile)
                    } else {
                        Log.e(TAG, "❌ Impossible de créer le fichier ou de détecter le type MIME")
                        null
                    }
                    
                    // Créer les RequestBody pour chaque champ (tous optionnels pour PATCH)
                    // IMPORTANT: Ne pas envoyer les champs null, et convertir correctement les nombres
                    val nomBody = tournoi.nom?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val descriptionBody = tournoi.description?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val sportBody = tournoi.sport?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val categorieAgeBody = tournoi.categorieAge?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val dateDebutBody = tournoi.dateDebut?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val dateFinBody = tournoi.dateFin?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val lieuBody = tournoi.lieu?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    // Pour les nombres, TOUJOURS les envoyer s'ils existent dans le tournoi
                    // IMPORTANT: Le backend attend des nombres, mais en multipart ils sont envoyés comme strings
                    // NestJS devrait les convertir automatiquement, mais on s'assure qu'ils sont valides
                    val nombreParticipantsMaxBody = tournoi.nombreParticipantsMax?.let { 
                        if (it > 0) {
                            Log.d(TAG, "Envoi nombreParticipantsMax: $it (comme string: '${it.toString()}')")
                            it.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        } else {
                            Log.w(TAG, "nombreParticipantsMax invalide ($it), ne sera pas envoyé")
                            null
                        }
                    } ?: run {
                        // Si null, ne pas envoyer (le backend utilisera la valeur existante)
                        Log.d(TAG, "nombreParticipantsMax est null, ne sera pas envoyé")
                        null
                    }
                    
                    val fraisParticipationBody = tournoi.fraisParticipation?.let {
                        if (it >= 0) {
                            Log.d(TAG, "Envoi fraisParticipation: $it (comme string: '${it.toString()}')")
                            it.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                        } else {
                            Log.w(TAG, "fraisParticipation invalide ($it), ne sera pas envoyé")
                            null
                        }
                    } ?: run {
                        // Si null, ne pas envoyer (le backend utilisera la valeur existante)
                        Log.d(TAG, "fraisParticipation est null, ne sera pas envoyé")
                        null
                    }
                    
                    val etatBody = tournoi.etat?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val niveauBody = tournoi.niveau?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val recompenseBody = tournoi.recompense?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    
                    Log.d(TAG, "📋 Champs préparés pour multipart:")
                    Log.d(TAG, "  nom: ${nomBody != null}")
                    Log.d(TAG, "  sport: ${sportBody != null}")
                    Log.d(TAG, "  lieu: ${lieuBody != null}")
                    Log.d(TAG, "  nombreParticipantsMax: ${tournoi.nombreParticipantsMax} -> envoyé: ${nombreParticipantsMaxBody != null}")
                    Log.d(TAG, "  fraisParticipation: ${tournoi.fraisParticipation} -> envoyé: ${fraisParticipationBody != null}")
                    Log.d(TAG, "  image: ${imagePart != null}")
                    
                    // IMPORTANT: Avec multipart, tous les champs doivent être envoyés, même s'ils sont optionnels
                    // Le backend attend UpdateTournoiDto mais reçoit multipart, donc il faut envoyer tous les champs
                    if (nombreParticipantsMaxBody == null && tournoi.nombreParticipantsMax != null) {
                        Log.w(TAG, "⚠️ ATTENTION: nombreParticipantsMax existe (${tournoi.nombreParticipantsMax}) mais n'est pas envoyé car invalide")
                    }
                    if (fraisParticipationBody == null && tournoi.fraisParticipation != null) {
                        Log.w(TAG, "⚠️ ATTENTION: fraisParticipation existe (${tournoi.fraisParticipation}) mais n'est pas envoyé car invalide")
                    }
                    
                    val response = apiService.updateTournoiWithImage(
                        id = id,
                        nom = nomBody,
                        description = descriptionBody,
                        sport = sportBody,
                        categorieAge = categorieAgeBody,
                        dateDebut = dateDebutBody,
                        dateFin = dateFinBody,
                        lieu = lieuBody,
                        nombreParticipantsMax = nombreParticipantsMaxBody,
                        fraisParticipation = fraisParticipationBody,
                        etat = etatBody,
                        niveau = niveauBody,
                        recompense = recompenseBody,
                        image = imagePart
                    )
                    
                    Log.d(TAG, "Réponse HTTP: code=${response.code()}, success=${response.isSuccessful}")
                    
                    if (response.isSuccessful) {
                        val updatedTournoi = response.body()
                        Log.d(TAG, "✅ Tournoi mis à jour avec succès (avec nouvelle image)")
                        Log.d(TAG, "Tournoi mis à jour: ${updatedTournoi?.nom}")
                        Log.d(TAG, "Nouvelle image: ${updatedTournoi?.image}")
                        onSuccess()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = errorBody ?: "Erreur lors de la mise à jour (code: ${response.code()})"
                        Log.e(TAG, "❌ Erreur mise à jour: ${response.code()} - $errorMessage")
                        Log.e(TAG, "Message: ${response.message()}")
                        onError(errorMessage)
                    }
                } else {
                    // Pas de nouvelle image, utiliser l'endpoint JSON normal
                    Log.d(TAG, "📤 Mise à jour sans nouvelle image (JSON)")
                    
                    // Créer un UpdateTournoiDto avec SEULEMENT les champs autorisés
                    // IMPORTANT: Inclure les nombres seulement s'ils sont valides (> 0 pour nombreParticipantsMax, >= 0 pour fraisParticipation)
                    // Si null, ne pas les inclure (le backend les ignorera)
                    val updateDto = UpdateTournoiDto(
                        nom = tournoi.nom,
                        description = tournoi.description,
                        sport = tournoi.sport,
                        categorieAge = tournoi.categorieAge,
                        dateDebut = tournoi.dateDebut,
                        dateFin = tournoi.dateFin,
                        lieu = tournoi.lieu,
                        // Inclure seulement si valide (null sera ignoré par le backend)
                        nombreParticipantsMax = tournoi.nombreParticipantsMax?.takeIf { it > 0 },
                        fraisParticipation = tournoi.fraisParticipation?.takeIf { it >= 0 },
                        etat = tournoi.etat,
                        niveau = tournoi.niveau,
                        recompense = tournoi.recompense
                    )
                    
                    Log.d(TAG, "✅ UpdateTournoiDto créé:")
                    Log.d(TAG, "  nombreParticipantsMax: ${updateDto.nombreParticipantsMax}")
                    Log.d(TAG, "  fraisParticipation: ${updateDto.fraisParticipation}")
                    Log.d(TAG, "Champs inclus: nom, description, sport, categorieAge, dateDebut, dateFin, lieu, nombreParticipantsMax, fraisParticipation, etat, niveau, recompense")
                    
                    val response = apiService.updateTournoi(id, updateDto)
                    
                    Log.d(TAG, "Réponse HTTP: code=${response.code()}, success=${response.isSuccessful}")
                    
                    if (response.isSuccessful) {
                        val updatedTournoi = response.body()
                        Log.d(TAG, "✅ Tournoi mis à jour avec succès")
                        Log.d(TAG, "Tournoi mis à jour: ${updatedTournoi?.nom}")
                        onSuccess()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = errorBody ?: "Erreur lors de la mise à jour (code: ${response.code()})"
                        Log.e(TAG, "❌ Erreur mise à jour: ${response.code()} - $errorMessage")
                        Log.e(TAG, "Message: ${response.message()}")
                        onError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception lors de la mise à jour", e)
                e.printStackTrace()
                onError(e.message ?: "Erreur inconnue")
            }
        }
    }
    
    fun deleteTournoi(
        id: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = apiService.deleteTournoi(id)
                if (response.isSuccessful) {
                    Log.d(TAG, "Tournoi supprimé avec succès")
                    onSuccess()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Erreur lors de la suppression"
                    Log.e(TAG, "Erreur suppression: ${response.code()} - $errorMessage")
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception lors de la suppression", e)
                onError(e.message ?: "Erreur inconnue")
            }
        }
    }
}

class CoachTournoisViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CoachTournoisViewModel::class.java)) {
            return CoachTournoisViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

