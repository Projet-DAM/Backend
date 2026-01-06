package com.example.dam_front.ui.screens

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.dam_front.models.Participant
import com.example.dam_front.models.Tournoi
import com.example.dam_front.repository.InscriptionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@Composable
fun ParticipantsScreenWrapper(
    tournoi: Tournoi,
    onBackClick: () -> Unit,
    onParticipantsCountChanged: (Int) -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val inscriptionRepository = remember { InscriptionRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var participants by remember { mutableStateOf<List<Participant>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf<Participant?>(null) }
    var showEquipeCreation by remember { mutableStateOf(false) }
    var showMatchManagement by remember { mutableStateOf<String?>(null) }
    
    fun loadParticipants() {
        tournoi.id?.let { tournoiId ->
            isLoading = true
            error = null
            coroutineScope.launch {
                val result = inscriptionRepository.getParticipants(tournoiId)
                if (result.isSuccess) {
                    val loadedParticipants = result.getOrNull() ?: emptyList()
                    participants = loadedParticipants
                    onParticipantsCountChanged(loadedParticipants.size)
                    isLoading = false
                } else {
                    // Si l'endpoint n'existe pas, utiliser des données mockées pour tester l'affichage
                    val exception = result.exceptionOrNull()
                    if (exception?.message?.contains("404") == true || exception?.message?.contains("Not Found") == true) {
                        android.util.Log.d("ParticipantsScreenWrapper", "Endpoint non trouvé, utilisation de données mockées pour test")
                        // Données mockées pour tester l'affichage
                        val mockParticipants = getMockParticipants(tournoiId)
                        participants = mockParticipants
                        onParticipantsCountChanged(mockParticipants.size)
                        isLoading = false
                    } else {
                        error = exception?.message
                        isLoading = false
                    }
                }
            }
        }
    }
    
    LaunchedEffect(tournoi.id) {
        loadParticipants()
    }
    
    fun onEditParticipant(participant: Participant) {
        showEditDialog = participant
    }
    
    fun onDeleteParticipant(participant: Participant) {
        participant.id?.let { id ->
            coroutineScope.launch {
                val result = inscriptionRepository.deleteInscription(id)
                if (result.isSuccess) {
                    android.widget.Toast.makeText(
                        context,
                        "Participant supprimé avec succès",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    loadParticipants()
                } else {
                    val errorMessage = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                    android.widget.Toast.makeText(
                        context,
                        "Erreur: $errorMessage",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    // Navigation vers la gestion des matchs
    if (showMatchManagement != null) {
        MatchManagementScreen(
            tournoiId = showMatchManagement!!,
            tournoiNom = tournoi.nom,
            onBackClick = { showMatchManagement = null },
            onMenuClick = onMenuClick
        )
    }
    // Navigation vers la création d'équipes
    else if (showEquipeCreation) {
        EquipeCreationScreen(
            tournoi = tournoi,
            participants = participants,
            onBackClick = { showEquipeCreation = false },
            onEquipesCreated = {
                showEquipeCreation = false
                // Recharger les participants si nécessaire
                loadParticipants()
            },
            onManageMatchesClick = { tournoiId ->
                showMatchManagement = tournoiId
            }
        )
    } else {
        ParticipantsScreen(
            tournoi = tournoi,
            participants = participants,
            isLoading = isLoading,
            onBackClick = onBackClick,
            onEditParticipant = ::onEditParticipant,
            onDeleteParticipant = ::onDeleteParticipant,
            onCreateEquipesClick = { showEquipeCreation = true }
        )
    }
    
    // Dialog d'édition
    showEditDialog?.let { participant ->
        EditParticipantDialog(
            participant = participant,
            tournoi = tournoi,
            onDismiss = { showEditDialog = null },
            onSave = { updatedParticipant ->
                coroutineScope.launch {
                    val result = inscriptionRepository.updateInscription(
                        inscriptionId = participant.id ?: return@launch,
                        enfantPrenom = updatedParticipant.enfantPrenom,
                        enfantNom = updatedParticipant.enfantNom,
                        enfantDateNaissance = updatedParticipant.enfantDateNaissance,
                        parentPrenom = updatedParticipant.parentPrenom,
                        parentNom = updatedParticipant.parentNom,
                        parentTelephone = updatedParticipant.parentTelephone,
                        montantInscription = updatedParticipant.montantInscription,
                        besoinsParticuliers = updatedParticipant.besoinsParticuliers
                    )
                    if (result.isSuccess) {
                        android.widget.Toast.makeText(
                            context,
                            "Participant modifié avec succès",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        showEditDialog = null
                        loadParticipants()
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                        android.widget.Toast.makeText(
                            context,
                            "Erreur: $errorMessage",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

// Fonction pour générer des données mockées pour tester l'affichage
fun getMockParticipants(tournoiId: String): List<Participant> {
    return listOf(
        Participant(
            id = "mock1",
            enfantPrenom = "Ahmed",
            enfantNom = "Ben Ali",
            enfantDateNaissance = "2010-05-15",
            parentPrenom = "Mohamed",
            parentNom = "Ben Ali",
            parentTelephone = "+216 12 345 678",
            montantInscription = 20.0,
            besoinsParticuliers = null,
            tournoiId = tournoiId
        ),
        Participant(
            id = "mock2",
            enfantPrenom = "Salma",
            enfantNom = "Trabelsi",
            enfantDateNaissance = "2011-08-22",
            parentPrenom = "Fatma",
            parentNom = "Trabelsi",
            parentTelephone = "+216 98 765 432",
            montantInscription = 20.0,
            besoinsParticuliers = "Allergie aux arachides",
            tournoiId = tournoiId
        ),
        Participant(
            id = "mock3",
            enfantPrenom = "Youssef",
            enfantNom = "Ammar",
            enfantDateNaissance = "2009-12-10",
            parentPrenom = "Karim",
            parentNom = "Ammar",
            parentTelephone = "+216 55 123 456",
            montantInscription = 20.0,
            besoinsParticuliers = null,
            tournoiId = tournoiId
        )
    )
}

