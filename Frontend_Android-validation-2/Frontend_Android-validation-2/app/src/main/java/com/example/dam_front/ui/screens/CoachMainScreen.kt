package com.example.dam_front.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dam_front.models.Tournoi
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.repository.InscriptionRepository
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.components.SidebarScreen
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.TokenManager

import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class CoachScreen {
    TOURNOIS, PROGRAMMES, ACTIVITES, ABONNEMENTS, MON_ENFANT
}

@Composable
fun CoachMainScreen(
    onLogout: () -> Unit = {},
    onChatClick: () -> Unit = {},
    navController: androidx.navigation.NavHostController? = null
) {
    val systemUiController = rememberSystemUiController()
    val statusBarColor = HeaderBlue
    
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false
        )
    }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var coachPrenom by remember { mutableStateOf<String?>(null) }
    var coachEmail by remember { mutableStateOf<String?>(null) }
    var coachId by remember { mutableStateOf<String?>(null) }
    var currentSidebarScreen by remember { mutableStateOf(SidebarScreen.ACCUEIL) }
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Récupérer le prénom et l'email du coach
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            coachPrenom = tokenManager.getUserPrenom().first()
            coachEmail = tokenManager.getUserEmail().first()
            coachId = tokenManager.getUserId().first()
        }
    }
    
    val tournoisViewModel: TournoisViewModel = viewModel(factory = TournoisViewModelFactory())
    val coachViewModel: CoachTournoisViewModel = viewModel(factory = CoachTournoisViewModelFactory(context))
    val programsViewModel: com.example.dam_front.viewmodels.ProgramsViewModel = viewModel(factory = com.example.dam_front.viewmodels.ProgramsViewModelFactory(context))
    val activitiesViewModel: com.example.dam_front.viewmodels.ActivitiesViewModel = viewModel(factory = com.example.dam_front.viewmodels.ActivitiesViewModelFactory(context))
    val inscriptionRepository = remember { InscriptionRepository(context) }
    val participantsCountMap = remember { mutableStateMapOf<String, Int>() }
    
    val tournois by tournoisViewModel.tournois.collectAsState(initial = emptyList())
    val isLoading by tournoisViewModel.isLoading.collectAsState(initial = false)
    
    var currentScreen by remember { mutableStateOf(CoachScreen.PROGRAMMES) }
    
    // Tournois States
    var showCreateEditScreen by remember { mutableStateOf(false) }
    var tournoiToEdit by remember { mutableStateOf<Tournoi?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Tournoi?>(null) }
    var selectedTournoiForParticipants by remember { mutableStateOf<Tournoi?>(null) }
    
    // Programs States
    val programsUiState by programsViewModel.uiState.collectAsState()
    var programToEdit by remember { mutableStateOf<com.example.dam_front.models.Program?>(null) }
    var showAddProgram by remember { mutableStateOf(false) }
    var showEditProgram by remember { mutableStateOf(false) }
    var selectedProgram by remember { mutableStateOf<com.example.dam_front.models.Program?>(null) }
    var showDeleteProgramDialog by remember { mutableStateOf<com.example.dam_front.models.Program?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Activities States
    var showAddActivity by remember { mutableStateOf(false) }
    var showEditActivity by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<com.example.dam_front.models.Activity?>(null) }
    var selectedActivity by remember { mutableStateOf<com.example.dam_front.models.Activity?>(null) }
    var showDeleteActivityDialog by remember { mutableStateOf<com.example.dam_front.models.Activity?>(null) }
    
    // Charger les tournois au démarrage
    LaunchedEffect(Unit) {
        tournoisViewModel.loadTournois()
    }
    
    LaunchedEffect(tournois) {
        val currentIds = tournois.mapNotNull { it.id }.toSet()
        val idsToRemove = participantsCountMap.keys.filter { it !in currentIds }
        idsToRemove.forEach { participantsCountMap.remove(it) }
        
        currentIds.forEach { id ->
            if (!participantsCountMap.containsKey(id)) {
                val result = inscriptionRepository.getParticipants(id)
                if (result.isSuccess) {
                    participantsCountMap[id] = result.getOrNull()?.size ?: 0
                } else {
                    Log.w(
                        "CoachMainScreen",
                        "Impossible de charger les participants du tournoi $id: ${result.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }
    
    // Map SidebarScreen to CoachScreen for navigation
    val onSidebarScreenSelected: (SidebarScreen) -> Unit = { sidebarScreen ->
        currentSidebarScreen = sidebarScreen
        scope.launch {
            drawerState.close()
        }
        when (sidebarScreen) {
            SidebarScreen.ACCUEIL -> currentScreen = CoachScreen.PROGRAMMES
            SidebarScreen.TOURNOIS -> currentScreen = CoachScreen.TOURNOIS
            SidebarScreen.ACTIVITES -> currentScreen = CoachScreen.ACTIVITES
            SidebarScreen.MON_ENFANT -> currentScreen = CoachScreen.MON_ENFANT
            else -> { /* Handle other screens */ }
        }
    }
    
    if (showCreateEditScreen) {
        CreateEditTournoiScreen(
            tournoi = tournoiToEdit,
            onSave = { newTournoi, imageUri ->
                if (tournoiToEdit != null) {
                    coachViewModel.updateTournoi(
                        newTournoi,
                        imageUri = imageUri,
                        onSuccess = {
                            android.widget.Toast.makeText(context, "Tournoi mis à jour!", android.widget.Toast.LENGTH_SHORT).show()
                            showCreateEditScreen = false
                            tournoiToEdit = null
                            tournoisViewModel.loadTournois()
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Erreur: $error", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                } else {
                    coachViewModel.createTournoi(
                        newTournoi,
                        imageUri = imageUri,
                        onSuccess = {
                            android.widget.Toast.makeText(context, "Tournoi créé!", android.widget.Toast.LENGTH_SHORT).show()
                            showCreateEditScreen = false
                            tournoisViewModel.loadTournois()
                        },
                        onError = { error ->
                            android.widget.Toast.makeText(context, "Erreur: $error", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            },
            onCancel = {
                showCreateEditScreen = false
                tournoiToEdit = null
            }
        )
    } else if (showAddProgram) {
         val availableForCreation = programsUiState.availableActivities.filter { activity ->
            val assignedProgram = activity.programmeId
            assignedProgram == null || assignedProgram.isBlank()
        }
        com.example.dam_front.ui.screens.AddProgramScreen(
            availableActivities = availableForCreation,
            currentUserId = coachId ?: "",
            onBackClick = { showAddProgram = false },
            onSaveClick = { formData ->
                 val request = com.example.dam_front.models.CreateProgramRequest(
                    nomProgramme = formData.nomProgramme,
                    description = formData.description,
                    objectif = formData.objectif,
                    niveau = formData.niveau,
                    prix = formData.prix,
                    statut = formData.statut,
                    activites = formData.activites
                )
                
                if (formData.imageUri != null) {
                    val file = com.example.dam_front.utils.FileUtils.getFileFromUri(context, formData.imageUri!!)
                    if (file != null) {
                        programsViewModel.createProgramWithImage(
                            request,
                            file,
                            onSuccess = {
                                showAddProgram = false
                                android.widget.Toast.makeText(context, "Programme créé avec image!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                errorMessage = msg
                                android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        programsViewModel.createProgram(
                            request, 
                            onSuccess = { 
                                showAddProgram = false
                                android.widget.Toast.makeText(context, "Programme créé (Erreur image)!", android.widget.Toast.LENGTH_SHORT).show() 
                            }, 
                            onError = { msg -> 
                                errorMessage = msg
                                android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show() 
                            }
                        )
                    }
                } else {
                    programsViewModel.createProgram(
                        request, 
                        onSuccess = { 
                            showAddProgram = false
                            android.widget.Toast.makeText(context, "Programme créé!", android.widget.Toast.LENGTH_SHORT).show() 
                        }, 
                        onError = { msg -> 
                            errorMessage = msg
                            android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show() 
                        }
                    )
                }
            }
        )
    } else if (showEditProgram && programToEdit != null) {
        val availableForEdit = programsUiState.availableActivities.filter { activity ->
             val assignedProgram = activity.programmeId
             // Allow activities that are unassigned OR assigned to THIS program
             assignedProgram == null || assignedProgram.isBlank() || assignedProgram == programToEdit!!.id
        }
        com.example.dam_front.ui.screens.UpdateProgramScreen(
            program = programToEdit!!,
            availableActivities = availableForEdit,
            currentUserId = coachId,
            onBackClick = { 
                showEditProgram = false 
                programToEdit = null
            },
            onSaveClick = { formData ->
                 val request = com.example.dam_front.models.UpdateProgramRequest(
                    nomProgramme = formData.nomProgramme,
                    description = formData.description,
                    objectif = formData.objectif,
                    niveau = formData.niveau,
                    prix = formData.prix,
                    statut = formData.statut,
                    activites = formData.activites,
                    image = if (formData.imageUri == null) programToEdit!!.image else null // Preserve old image if no new one
                )
                
                if (formData.imageUri != null) {
                    val file = com.example.dam_front.utils.FileUtils.getFileFromUri(context, formData.imageUri!!)
                    if (file != null) {
                        programsViewModel.updateProgramImageWorkaround(
                            programToEdit!!.id,
                            request,
                            file,
                            onSuccess = {
                                showEditProgram = false
                                programToEdit = null
                                android.widget.Toast.makeText(context, "Programme mis à jour avec image!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        // Fallback to normal update if file conversion fails
                         programsViewModel.updateProgram(
                            programToEdit!!.id,
                            request,
                            onSuccess = {
                                showEditProgram = false
                                programToEdit = null
                                android.widget.Toast.makeText(context, "Programme mis à jour (Erreur image)!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    programsViewModel.updateProgram(
                        programToEdit!!.id,
                        request,
                        onSuccess = {
                            showEditProgram = false
                            programToEdit = null
                            android.widget.Toast.makeText(context, "Programme mis à jour!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onError = { msg ->
                            android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    } else if (selectedProgram != null) {
         val latestProgram = programsUiState.programs.find { it.id == selectedProgram!!.id } ?: selectedProgram!!
         com.example.dam_front.ui.screens.ProgramDetailScreen(
                program = latestProgram,
                enrollments = programsUiState.enrollments,
                onLoadEnrollments = { programsViewModel.loadEnrollments(latestProgram.id) },
                onBackClick = { selectedProgram = null },
                onEditClick = { 
                    programToEdit = latestProgram
                    showEditProgram = true
                    selectedProgram = null
                },
                onDeleteClick = { 
                    showDeleteProgramDialog = latestProgram
                },
                onMenuClick = { scope.launch { drawerState.open() } },
                onActivityClick = { /* Activity detail logic if needed */ }
         )
    } else if (showAddActivity) {
         com.example.dam_front.ui.screens.AddActivityScreen(
             onBackClick = { showAddActivity = false },
             onSaveClick = { formData -> 
                 val request = com.example.dam_front.models.CreateActivityRequest(
                    nomActivite = formData.nomActivite,
                    description = formData.description,
                    categorie = formData.categorie,
                    duree = formData.duree,
                    capaciteMax = formData.capaciteMax,
                    prix = formData.prix,
                    date = formData.date,
                    heure = formData.heure,
                    coach = coachId
                )
                
                if (formData.imageUri != null) {
                    val file = com.example.dam_front.utils.FileUtils.getFileFromUri(context, formData.imageUri!!)
                    if (file != null) {
                        activitiesViewModel.createActivityWithImage(
                            request,
                            file, // Use the file directly
                            onSuccess = {
                                showAddActivity = false
                                activitiesViewModel.refreshActivities()
                                programsViewModel.refreshActivities()
                                android.widget.Toast.makeText(context, "Activité créée avec image!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                         activitiesViewModel.createActivity(
                            request,
                            onSuccess = {
                                showAddActivity = false
                                activitiesViewModel.refreshActivities()
                                programsViewModel.refreshActivities()
                                android.widget.Toast.makeText(context, "Activité créée (Erreur image)!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    activitiesViewModel.createActivity(
                        request,
                        onSuccess = {
                            showAddActivity = false
                            activitiesViewModel.refreshActivities()
                            programsViewModel.refreshActivities()
                            android.widget.Toast.makeText(context, "Activité créée!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onError = { msg ->
                            android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
             }
         )
    } else if (showEditActivity && activityToEdit != null) {
        com.example.dam_front.ui.screens.UpdateActivityScreen(
            activity = activityToEdit!!,
            onBackClick = { 
                showEditActivity = false
                activityToEdit = null
            },
            onSaveClick = { formData ->
                 val request = com.example.dam_front.models.CreateActivityRequest(
                    nomActivite = formData.nomActivite,
                    description = formData.description,
                    categorie = formData.categorie,
                    duree = formData.duree,
                    capaciteMax = formData.capaciteMax,
                    prix = formData.prix,
                    date = formData.date,
                    heure = formData.heure,
                    image = if (formData.imageUri == null) formData.image else null // Important: keep old image if no new one
                )
                
                if (formData.imageUri != null) {
                    val file = com.example.dam_front.utils.FileUtils.getFileFromUri(context, formData.imageUri!!)
                     if (file != null) {
                        activitiesViewModel.updateActivityImageWorkaround(
                            activityToEdit!!.id,
                            request,
                            file,
                            onSuccess = {
                                 showEditActivity = false
                                 activityToEdit = null
                                 android.widget.Toast.makeText(context, "Activité mise à jour avec image!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                 android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    } else {
                        activitiesViewModel.updateActivity(
                            activityToEdit!!.id,
                            request,
                            onSuccess = {
                                 showEditActivity = false
                                 activityToEdit = null
                                 android.widget.Toast.makeText(context, "Activité mise à jour (Erreur image)!", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onError = { msg ->
                                 android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    activitiesViewModel.updateActivity(
                        activityToEdit!!.id,
                        request,
                        onSuccess = {
                             showEditActivity = false
                             activityToEdit = null
                             android.widget.Toast.makeText(context, "Activité mise à jour!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onError = { msg ->
                             android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        )
    } else if (selectedActivity != null) {
         com.example.dam_front.ui.screens.ActivityDetailScreen(
             activity = selectedActivity!!,
             currentUserId = coachId,
             onBackClick = { selectedActivity = null },
             onEditClick = {
                 activityToEdit = selectedActivity
                 showEditActivity = true
                 selectedActivity = null
             },
             onDeleteClick = {
                 showDeleteActivityDialog = selectedActivity
             },
             onMenuClick = { scope.launch { drawerState.open() } }
         )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Sidebar(
                    currentScreen = currentSidebarScreen,
                    onScreenSelected = onSidebarScreenSelected,
                    userName = coachPrenom ?: "Coach",
                    userEmail = coachEmail ?: "",
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                        }
                        coroutineScope.launch {
                            authRepository.logout()
                            onLogout()
                        }
                    },
                    isCoach = true
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Header avec nom du coach et image de profil
                    CoachHeaderSection(
                        coachName = coachPrenom ?: "Coach",
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                    
                    // Afficher l'écran des participants si un tournoi est sélectionné
                    if (selectedTournoiForParticipants != null) {
                        val tournoiParticipants = selectedTournoiForParticipants!!
                        ParticipantsScreenWrapper(
                            tournoi = tournoiParticipants,
                            onBackClick = { selectedTournoiForParticipants = null },
                            onParticipantsCountChanged = { count ->
                                tournoiParticipants.id?.let { participantsCountMap[it] = count }
                            },
                            onMenuClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        )
                    } else {
                        // Contenu selon l'écran sélectionné
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                            CoachScreen.TOURNOIS -> {
                                CoachTournoisScreen(
                                    tournois = tournois,
                                    isLoading = isLoading,
                                    participantsCount = participantsCountMap,
                                    onEditClick = { tournoi ->
                                        tournoiToEdit = tournoi
                                        showCreateEditScreen = true
                                    },
                                    onDeleteClick = { tournoi ->
                                        showDeleteDialog = tournoi
                                    },
                                    onViewParticipantsClick = { tournoi ->
                                        selectedTournoiForParticipants = tournoi
                                    },
                                    onRefresh = {
                                        tournoisViewModel.loadTournois()
                                    },
                                    onCreateClick = {
                                        tournoiToEdit = null
                                        showCreateEditScreen = true
                                    }
                                )
                            }
                            CoachScreen.PROGRAMMES -> {
                                com.example.dam_front.ui.screens.ProgramsListScreen(
                                    viewModel = programsViewModel,
                                    onProgramClick = { selectedProgram = it },
                                    onAddClick = { showAddProgram = true },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onEditClick = { program -> 
                                        programToEdit = program
                                        showEditProgram = true
                                    },
                                    onDeleteClick = { program -> 
                                        showDeleteProgramDialog = program
                                    },
                                    isCoach = true
                                )
                            }
                            CoachScreen.ACTIVITES -> {
                                    com.example.dam_front.ui.screens.ActivitiesListScreen(
                                    viewModel = activitiesViewModel,
                                    isCoach = true,
                                    onActivityClick = { selectedActivity = it },
                                    onAddClick = { showAddActivity = true },
                                    onMenuClick = { scope.launch { drawerState.open() } },
                                    onEditClick = { activity ->
                                        activityToEdit = activity
                                        showEditActivity = true
                                    },
                                    onDeleteClick = { activity ->
                                        showDeleteActivityDialog = activity
                                    }
                                    )
                            }
                            CoachScreen.ABONNEMENTS -> {
                                DefaultCoachContentCard("Abonnements")
                            }
                            CoachScreen.MON_ENFANT -> {
                                CoachSuiviNavigationWrapper(
                                    onChatClick = { navController?.navigate("chat_list") ?: onChatClick() }
                                )
                            }
                        }
                    }
                    }
                    
                    // Bottom Navigation Bar
                    CoachBottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { 
                            currentScreen = it
                        }
                    )
                }

                // Draggable Chat Bubble
                var offsetX by remember { mutableStateOf(0f) }
                var offsetY by remember { mutableStateOf(0f) }

                FloatingActionButton(
                    onClick = { navController?.navigate("chat_list") ?: onChatClick() },
                    containerColor = HeaderBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                        .align(Alignment.CenterEnd) // Start from middle-right
                        .padding(16.dp)
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubble,
                        contentDescription = "Chat",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        // Delete Tournoi Dialog
        showDeleteDialog?.let { tournoi ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Supprimer le tournoi") },
                text = { Text("Êtes-vous sûr de vouloir supprimer \"${tournoi.nom}\" ? Cette action est irréversible.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            tournoi.id?.let { id ->
                                coachViewModel.deleteTournoi(
                                    id,
                                    onSuccess = {
                                        showDeleteDialog = null
                                        tournoisViewModel.loadTournois()
                                        android.widget.Toast.makeText(context, "Tournoi supprimé", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        showDeleteDialog = null
                                        android.widget.Toast.makeText(context, "Erreur: $error", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
        
        // Delete Program Dialog
        showDeleteProgramDialog?.let { program ->
             AlertDialog(
                onDismissRequest = { showDeleteProgramDialog = null },
                title = { Text("Supprimer le programme") },
                text = { Text("Êtes-vous sûr de vouloir supprimer \"${program.nomProgramme}\" ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            programsViewModel.deleteProgram(
                                program.id,
                                onSuccess = {
                                    showDeleteProgramDialog = null
                                    android.widget.Toast.makeText(context, "Programme supprimé", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    showDeleteProgramDialog = null
                                    android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteProgramDialog = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
        
        // Delete Activity Dialog
        showDeleteActivityDialog?.let { activity ->
             AlertDialog(
                onDismissRequest = { showDeleteActivityDialog = null },
                title = { Text("Supprimer l'activité") },
                text = { Text("Êtes-vous sûr de vouloir supprimer \"${activity.nomActivite}\" ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            activitiesViewModel.deleteActivity(
                                activity.id,
                                onSuccess = {
                                    showDeleteActivityDialog = null
                                    android.widget.Toast.makeText(context, "Activité supprimée", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onError = { msg ->
                                    showDeleteActivityDialog = null
                                    android.widget.Toast.makeText(context, "Erreur: $msg", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("Supprimer")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteActivityDialog = null }) {
                        Text("Annuler")
                    }
                }
            )
        }
    }
}

@Composable
fun CoachHeaderSection(
    coachName: String,
    onMenuClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(HeaderBlue, HeaderBlueLight)
                )
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Section gauche : Profil + Bienvenue Coach + Nom
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Profile Picture - Clic pour ouvrir le menu
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    IconOrange.copy(alpha = 0.8f),
                                    IconOrangeAccent.copy(alpha = 0.6f)
                                )
                            )
                        )
                        .clickable { 
                            onMenuClick() 
                        }
                        .padding(2.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            modifier = Modifier.size(24.dp),
                            tint = HeaderBlue
                        )
                    }
                }
                
                // Bienvenue Coach et Nom
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Bienvenue Coach",
                        color = TextWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                    Text(
                        text = coachName,
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            // Icons à droite (comme le parent)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notification Badge
                com.example.dam_front.ui.components.NotificationBadge(
                    isCoach = true
                )
                
                // Settings Icon
                SettingsIcon(
                    tint = TextWhite
                )
            }
        }
    }
}

@Composable
fun CoachBottomNavigationBar(
    currentScreen: CoachScreen,
    onScreenSelected: (CoachScreen) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(16.dp, RoundedCornerShape(32.dp), spotColor = HeaderBlue.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(HeaderBlue.copy(alpha=0.3f), SportyKidsGreen.copy(alpha=0.3f))))
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFE8F5E9), // Light Green 50
                            Color(0xFFE0F7FA), // Cyan 50
                            Color(0xFFEEF9FA)  // Very light cyan
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoachNavItem(
                    icon = { color -> ListIcon(tint = color) },
                    label = "Programmes",
                    isSelected = currentScreen == CoachScreen.PROGRAMMES,
                    activeColor = IconTeal,
                    onClick = { onScreenSelected(CoachScreen.PROGRAMMES) }
                )
                CoachNavItem(
                    icon = { color -> TrophyIcon(tint = color) },
                    label = "Tournois",
                    isSelected = currentScreen == CoachScreen.TOURNOIS,
                    activeColor = IconGreen,
                    onClick = { onScreenSelected(CoachScreen.TOURNOIS) }
                )
                CoachNavItem(
                    icon = { color -> PeopleIcon(tint = color) },
                    label = "Enfant",
                    isSelected = currentScreen == CoachScreen.MON_ENFANT,
                    activeColor = IconBlue,
                    onClick = { onScreenSelected(CoachScreen.MON_ENFANT) }
                )
                CoachNavItem(
                    icon = { color -> CardIcon(tint = color) },
                    label = "Abonnements",
                    isSelected = currentScreen == CoachScreen.ABONNEMENTS,
                    activeColor = IconOrange,
                    onClick = { onScreenSelected(CoachScreen.ABONNEMENTS) }
                )
            }
        }
    }
}

@Composable
fun RowScope.CoachNavItem(
    icon: @Composable (Color) -> Unit,
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent, label = "bg")
    val contentColor by animateColorAsState(if (isSelected) activeColor else Color.Gray, label = "content")

    Box(
        modifier = Modifier
            .weight(1f)
            .height(60.dp)
            .padding(4.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
             icon(contentColor)
             AnimatedVisibility(visible = isSelected) {
                 Text(
                     text = label, 
                     fontSize = 10.sp, 
                     fontWeight = FontWeight.Bold, 
                     color = contentColor,
                     maxLines = 1,
                     modifier = Modifier.padding(top = 2.dp)
                 )
             }
        }
    }
}

@Composable
fun DefaultCoachContentCard(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkGray
        )
    }
}
