package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.first
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.repository.ChildRepository
import android.content.SharedPreferences
import com.example.dam_front.ui.screens.AddChildDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dam_front.viewmodels.ActivitiesViewModel
import com.example.dam_front.viewmodels.ActivitiesViewModelFactory
import com.example.dam_front.viewmodels.ProgramsViewModel
import com.example.dam_front.viewmodels.ProgramsViewModelFactory
import com.example.dam_front.viewmodels.CoachHomeViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory
import com.example.dam_front.ui.screens.CoachSuiviListScreen
import com.example.dam_front.models.Activity
import com.example.dam_front.models.Program
import com.example.dam_front.models.CreateProgramRequest
import com.example.dam_front.models.UpdateProgramRequest
import com.example.dam_front.models.CreateActivityRequest
import com.example.dam_front.ui.screens.ActivitiesListScreen
import com.example.dam_front.ui.screens.ProgramsListScreen
import com.example.dam_front.ui.screens.ActivityDetailScreen
import com.example.dam_front.ui.screens.ProgramDetailScreen
import com.example.dam_front.ui.screens.AddActivityScreen
import com.example.dam_front.ui.screens.UpdateActivityScreen
import com.example.dam_front.ui.screens.AddProgramScreen
import com.example.dam_front.ui.screens.UpdateProgramScreen
import com.example.dam_front.viewmodels.NotificationViewModel
import com.example.dam_front.viewmodels.NotificationViewModelFactory


enum class Screen {
    ACTIVITES, PROGRAMMES, TOURNOIS, MON_ENFANT, ABONNEMENTS, NOTIFICATIONS
}

// Colors
val IconOrange = Color(0xFFF16A2D)
val IconGreen = Color(0xFF9AD93A)
val IconBlue = Color(0xFF13818A)
val IconBlueLight = Color(0xFF3FB7C9)
val IconYellow = Color(0xFFFFC107)
val IconTeal = Color(0xFF009688)

val NavOrangeBg = Color(0xFFFFF3EE)
val NavGreenBg = Color(0xFFF4FBEB)
val NavBlueBg = Color(0xFFE6F7F8)
val NavTealBg = Color(0xFFE0F2F1)

val IconBgCream = Color(0xFFFDF6F3)
val IconBgLightBlue = Color(0xFFE0F7FA)
val IconBgTealLight = Color(0xFFE0F2F1)

val TextBlue = Color(0xFF0E5C75)
val TextBlueLight = Color(0xFF13818A)
val TextDarkGray = Color(0xFF333333)

val CardWhite = Color.White
val BottomNavBg = Color.White

val DotOrange = Color(0xFFF16A2D)
val DotGreen = Color(0xFF9AD93A)
val DotBlue = Color(0xFF3FB7C9)
val DotYellow = Color(0xFFFFC107)

val IconInactiveGray = Color.Gray
val IconInactiveGreen = Color(0xFFCFE8B3)
val IconInactiveBlue = Color(0xFFBFEBED)

@Composable
fun MainScreen(
    initialProgramId: String? = null,
    onLogout: () -> Unit = {}
) {
    // Configurer la status bar pour qu'elle soit bleue (comme le header) au lieu de violet
    val systemUiController = rememberSystemUiController()
    val statusBarColor = HeaderBlue
    
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = statusBarColor,
            darkIcons = false // Icônes blanches sur fond bleu
        )
    }
    
    val context = LocalContext.current
    val authRepository = remember { com.example.dam_front.repository.AuthRepository(context) }
    val tokenManager = remember { com.example.dam_front.utils.TokenManager(context) }
    val childRepository = remember { ChildRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("SportyKidsPrefs", android.content.Context.MODE_PRIVATE)

    // Integration of Programs & Activities Logic
    val activitiesViewModel: ActivitiesViewModel = viewModel(
        factory = ActivitiesViewModelFactory(context)
    )
    val activitiesUiState by activitiesViewModel.uiState.collectAsState()
    val programsViewModel: ProgramsViewModel = viewModel(
        factory = ProgramsViewModelFactory(context)
    )
    val programsUiState by programsViewModel.uiState.collectAsState()
    
    val notificationViewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory(context)
    )
    val unreadNotificationsCount by notificationViewModel.unreadCount.collectAsState()
    
    var selectedActivity by remember { mutableStateOf<Activity?>(null) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var showAddActivity by remember { mutableStateOf(false) }
    var selectedProgram by remember { mutableStateOf<Program?>(null) }
    var programToEdit by remember { mutableStateOf<Program?>(null) }
    var showAddProgram by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var debugFcmToken by remember { mutableStateOf<String?>(null) }
    
    // Error Dialog Handler
    errorMessage?.let { error ->
        ErrorDialog(
            message = error,
            onDismiss = { errorMessage = null }
        )
    }

    if (debugFcmToken != null) {
        AlertDialog(
            onDismissRequest = { debugFcmToken = null },
            title = { Text("Token FCM Debug (Copiez-le)") },
            text = { 
                SelectionContainer {
                    Text(debugFcmToken!!)
                }
            },
            confirmButton = {
                TextButton(onClick = { debugFcmToken = null }) { Text("Fermer") }
            }
        )
    }
    
    var currentScreen by remember { mutableStateOf(Screen.PROGRAMMES) }
    var currentSidebarScreen by remember { mutableStateOf(SidebarScreen.ACCUEIL) }
    var drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedTournoi by remember { mutableStateOf<com.example.dam_front.models.Tournoi?>(null) }
    var programToEnroll by remember { mutableStateOf<com.example.dam_front.models.Program?>(null) }
    val scope = rememberCoroutineScope()
    
    // Gestion des enfants
    var children by remember { mutableStateOf<List<ChildResponse>>(emptyList()) }
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    var showAddChildDialog by remember { mutableStateOf(false) }
    
    // Récupérer le prénom et l'email de l'utilisateur
    var userPrenom by remember { mutableStateOf<String?>(null) }
    var userEmail by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var currentUserId by remember { mutableStateOf<String?>(null) }
    
    // Charger les informations utilisateur en continu
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            tokenManager.getUserPrenom().collect { userPrenom = it }
        }
        coroutineScope.launch {
            tokenManager.getUserEmail().collect { userEmail = it }
        }
        coroutineScope.launch {
            tokenManager.getUserId().collect { currentUserId = it }
        }
        coroutineScope.launch {
            tokenManager.getUserRole().collect { 
                userRole = it 
                // Log pour debug
                android.util.Log.d("MainScreen", "Role mis à jour: $it")
            }
        }
        
        // Charger les enfants une seule fois ou sur demande
        coroutineScope.launch {
            childRepository.getChildren().onSuccess { childrenList ->
                children = childrenList
                // Charger l'enfant sélectionné depuis SharedPreferences
                val savedChildId = prefs.getString("selected_child_id", null)
                if (savedChildId != null && childrenList.any { it.id == savedChildId }) {
                    selectedChildId = savedChildId
                } else if (childrenList.isNotEmpty()) {
                    // Sélectionner le premier enfant par défaut
                    selectedChildId = childrenList[0].id
                    prefs.edit().putString("selected_child_id", selectedChildId).apply()
                }
            }
        }
    }
    
    // Handle Deep Link navigation
    var hasHandledDeepLink by remember { mutableStateOf(false) }
    LaunchedEffect(programsUiState.programs, initialProgramId) {
        if (!hasHandledDeepLink && initialProgramId != null && programsUiState.programs.isNotEmpty()) {
            val program = programsUiState.programs.find { it.id == initialProgramId }
            if (program != null) {
                selectedProgram = program
                hasHandledDeepLink = true
                android.util.Log.d("MainScreen", "Deep link processed: navigating to program ${program.nomProgramme}")
            }
        }
    }
    
    // Map SidebarScreen to Screen for navigation
    val onSidebarScreenSelected: (SidebarScreen) -> Unit = { sidebarScreen ->
        currentSidebarScreen = sidebarScreen
        scope.launch {
            drawerState.close()
        }
        when (sidebarScreen) {
            SidebarScreen.ACCUEIL -> currentScreen = Screen.ACTIVITES
            SidebarScreen.TOURNOIS -> currentScreen = Screen.TOURNOIS
            SidebarScreen.ACTIVITES -> currentScreen = Screen.ACTIVITES
            SidebarScreen.MON_ENFANT -> currentScreen = Screen.MON_ENFANT
            SidebarScreen.AJOUTER_ENFANT -> {
                android.util.Log.d("MainScreen", "Sidebar: AJOUTER_ENFANT selected")
                showAddChildDialog = true
            }
            else -> { /* Handle other screens */ }
        }
        // Réinitialiser le tournoi sélectionné quand on change d'écran
        selectedTournoi = null
    }
    
    // Recharger les enfants après ajout
    val reloadChildren: () -> Unit = {
        coroutineScope.launch {
            childRepository.getChildren().onSuccess { childrenList ->
                children = childrenList
                if (selectedChildId == null && childrenList.isNotEmpty()) {
                    selectedChildId = childrenList[0].id
                    prefs.edit().putString("selected_child_id", selectedChildId).apply()
                }
            }
        }
    }
    
    // Show Update Activity Screen
    activityToEdit?.let { activity ->
        UpdateActivityScreen(
            activity = activity,
            onBackClick = { activityToEdit = null },
            onSaveClick = { formData ->
                val request = com.example.dam_front.models.CreateActivityRequest(
                    nomActivite = formData.nomActivite,
                    description = formData.description.ifBlank { null },
                    categorie = formData.categorie,
                    date = formData.date,
                    heure = formData.heure,
                    duree = formData.duree,
                    capaciteMax = formData.capaciteMax,
                    prix = formData.prix,
                    statut = activity.statut
                )
                
                scope.launch {
                    if (formData.imageUri != null) {
                        val imageFile = uriToFile(context, formData.imageUri)
                        
                        if (imageFile != null) {
                            activitiesViewModel.updateActivityImageWorkaround(
                                activityId = activity.id,
                                request = request,
                                imageFile = imageFile,
                                onSuccess = {
                                    activityToEdit = null
                                    selectedActivity = null
                                },
                                onError = { errorMsg -> errorMessage = errorMsg }
                            )
                        } else {
                            errorMessage = "Erreur lors de la conversion de l'image."
                        }
                    } else {
                        activitiesViewModel.updateActivity(
                            activityId = activity.id,
                            request = request,
                            onSuccess = {
                                activityToEdit = null
                                selectedActivity = null
                            },
                            onError = { errorMsg -> errorMessage = errorMsg }
                        )
                    }
                }
            }
        )
        return
    }
    
    // Show Add Activity Screen
    if (showAddActivity) {
        AddActivityScreen(
            onBackClick = { showAddActivity = false },
            onSaveClick = { formData ->
                val request = com.example.dam_front.models.CreateActivityRequest(
                    nomActivite = formData.nomActivite,
                    description = formData.description.ifBlank { null },
                    categorie = formData.categorie,
                    date = formData.date,
                    heure = formData.heure,
                    duree = formData.duree,
                    capaciteMax = formData.capaciteMax,
                    prix = formData.prix,
                    statut = "ACTIVE"
                )
                
                // Correction: imageFile can be null if uri is null, which createActivityWithImage handles?
                // createActivityWithImage(request, imageFile, ...)
                // But if imageFile is null, does it default to createActivity?
                // ViewModel.createActivityWithImage takes imageFile: java.io.File? (nullable)
                // So it's fine.
                
                val imageFile = formData.imageUri?.let { uri -> uriToFile(context, uri) }
                
                scope.launch {
                    activitiesViewModel.createActivityWithImage(
                        request = request,
                        imageFile = imageFile,
                        onSuccess = { 
                            showAddActivity = false
                            programsViewModel.refreshActivities()
                        },
                        onError = { message -> errorMessage = message }
                    )
                }
            }
        )
        return
    }

    // Show Activity Detail Screen
    selectedActivity?.let { activity ->
        val isCoach = userRole?.lowercase()?.contains("coach") == true
        
        if (isCoach) {
            ActivityDetailScreen(
                activity = activity,
                currentUserId = currentUserId,
                onBackClick = { selectedActivity = null },
                onEditClick = {
                    activityToEdit = activity
                    selectedActivity = null
                },
                onDeleteClick = {
                    scope.launch {
                        activitiesViewModel.deleteActivity(
                            activityId = activity.id,
                            onSuccess = { selectedActivity = null },
                            onError = { message -> errorMessage = message }
                        )
                    }
                },
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )
        } else {
            ParentActivityDetailScreen(
                activity = activity,
                onBackClick = { selectedActivity = null },
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )
        }
        return
    }

    // Show Update Program Screen
    programToEdit?.let { program ->
        val latestProgram = programsUiState.programs.find { it.id == program.id } ?: program
        val editableActivities = programsUiState.availableActivities.filter { activity ->
            val assignedProgram = activity.programmeId
            assignedProgram == null || assignedProgram.isBlank() || assignedProgram == latestProgram.id
        }
        UpdateProgramScreen(
            program = latestProgram,
            availableActivities = editableActivities,
            currentUserId = currentUserId,
            onBackClick = { programToEdit = null },
            onSaveClick = { formData ->
                val request = com.example.dam_front.models.UpdateProgramRequest(
                    nomProgramme = formData.nomProgramme,
                    description = formData.description,
                    objectif = formData.objectif,
                    niveau = formData.niveau,
                    prix = formData.prix,
                    statut = formData.statut,
                    activites = formData.activites
                )

                scope.launch {
                    if (formData.imageUri != null) {
                        val imageFile = uriToFile(context, formData.imageUri)
                        if (imageFile != null) {
                            programsViewModel.updateProgramImageWorkaround(
                                programId = latestProgram.id,
                                request = request,
                                imageFile = imageFile,
                                onSuccess = {
                                    programToEdit = null
                                    selectedProgram = null
                                },
                                onError = { message -> errorMessage = message }
                            )
                        } else {
                            errorMessage = "Erreur lors de la conversion de l'image"
                        }
                    } else {
                        programsViewModel.updateProgram(
                            programId = latestProgram.id,
                            request = request,
                            onSuccess = {
                                programToEdit = null
                                selectedProgram = null
                            },
                            onError = { message -> errorMessage = message }
                        )
                    }
                }
            }
        )
        return
    }

    if (showAddProgram) {
        val availableForCreation = programsUiState.availableActivities.filter { activity ->
            val assignedProgram = activity.programmeId
            assignedProgram == null || assignedProgram.isBlank()
        }
        AddProgramScreen(
            availableActivities = availableForCreation,
            currentUserId = currentUserId,
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

                scope.launch {
                    if (formData.imageUri != null) {
                        val imageFile = uriToFile(context, formData.imageUri)
                        if (imageFile != null) {
                            programsViewModel.createProgramWithImage(
                                request = request,
                                imageFile = imageFile,
                                onSuccess = { showAddProgram = false },
                                onError = { message -> errorMessage = message }
                            )
                        } else {
                            errorMessage = "Erreur lors de la conversion de l'image"
                        }
                    } else {
                        programsViewModel.createProgram(
                            request = request,
                            onSuccess = { showAddProgram = false },
                            onError = { message -> errorMessage = message }
                        )
                    }
                }
            }
        )
        return
    }

    if (programToEnroll != null) {
        ChildSelectionScreen(
            program = programToEnroll!!,
            onBackClick = { programToEnroll = null },
            onRegistrationComplete = {
                programToEnroll = null
                selectedProgram = null
                // Plus besoin de mettre d'erreur ici, le Toast est déjà géré par ChildSelectionScreen
            }
        )
        return
    }

    selectedProgram?.let { program ->
        val latestProgram = programsUiState.programs.find { it.id == program.id } ?: program
        val isCoach = userRole?.lowercase()?.contains("coach") == true
        
        if (isCoach) {
            ProgramDetailScreen(
                program = latestProgram,
                enrollments = programsUiState.enrollments,
                onLoadEnrollments = {
                    programsViewModel.loadEnrollments(latestProgram.id)
                },
                onBackClick = { selectedProgram = null },
                onEditClick = {
                    programToEdit = latestProgram
                    selectedProgram = null
                },
                onDeleteClick = {
                    programsViewModel.deleteProgram(
                        programId = latestProgram.id,
                        onSuccess = { selectedProgram = null },
                        onError = { message -> errorMessage = message }
                    )
                },
                onMenuClick = {
                    scope.launch { drawerState.open() }
                },
                onActivityClick = { programActivity ->
                    val fullActivity = activitiesUiState.activities.find { it.id == programActivity.id }
                    if (fullActivity != null) {
                        selectedActivity = fullActivity
                    } else {
                        errorMessage = "Activité non trouvée"
                    }
                }
            )
        } else {
            ParentProgramDetailScreen(
                program = latestProgram,
                onBackClick = { selectedProgram = null },
                onMenuClick = {
                     scope.launch { drawerState.open() }
                },
                onActivityClick = { programActivity ->
                    val fullActivity = activitiesUiState.activities.find { it.id == programActivity.id }
                    if (fullActivity != null) {
                        selectedActivity = fullActivity
                    } else {
                        errorMessage = "Activité non trouvée"
                    }
                },
                onSubscribeClick = {
                    programToEnroll = latestProgram
                }
            )
        }
        return
    }

    // État pour la navigation vers l'arbre
    var showBracketScreen by remember { mutableStateOf<String?>(null) }
    
    // Si on est sur l'écran de l'arbre
    if (showBracketScreen != null) {
        BracketParentScreen(
            tournoiId = showBracketScreen!!,
            tournoiNom = selectedTournoi?.nom,
            onBackClick = { showBracketScreen = null },
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            }
        )
    }
    // Si on est sur la page de détail d'un tournoi, afficher seulement cette page
    else if (currentScreen == Screen.NOTIFICATIONS) {
        NotificationsScreen(
            viewModel = notificationViewModel,
            onBackClick = { currentScreen = Screen.PROGRAMMES }
        )
    } else if (selectedTournoi != null && currentScreen == Screen.TOURNOIS) {
        TournoiDetailScreen(
            tournoi = selectedTournoi!!,
            onBackClick = { selectedTournoi = null },
            onInscriptionClick = { tournoi ->
                // L'inscription a été validée
                selectedTournoi = null
            },
            onBracketClick = { tournoiId ->
                showBracketScreen = tournoiId
            },
            onMenuClick = {
                scope.launch {
                    drawerState.open()
                }
            }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Sidebar(
                    currentScreen = currentSidebarScreen,
                    onScreenSelected = onSidebarScreenSelected,
                    userName = userPrenom ?: "Utilisateur",
                    userEmail = userEmail ?: "",
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                        }
                        coroutineScope.launch {
                            authRepository.logout()
                            onLogout()
                        }
                    }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Header
                    HeaderSection(
                        title = when (currentScreen) {
                            Screen.ACTIVITES -> "Activités Sportives"
                            Screen.PROGRAMMES -> "Programmes"
                            Screen.ABONNEMENTS -> "Abonnements & Offres"
                            Screen.TOURNOIS -> "Tournois"
                            Screen.MON_ENFANT -> if (userRole?.equals("Coach", true) == true) "Espace Coach" else "Suivre Mon Enfant"
                            Screen.NOTIFICATIONS -> "Notifications"
                        },
                        onMenuClick = { 
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        children = children,
                        selectedChildId = selectedChildId,
                        onChildSelected = { childId ->
                            selectedChildId = childId
                            prefs.edit().putString("selected_child_id", childId).apply()
                        },
                        unreadNotificationsCount = unreadNotificationsCount,
                        onNotificationsClick = {
                            currentScreen = Screen.NOTIFICATIONS
                        },
                        onDebugFcmClick = {
                            coroutineScope.launch {
                                // On utilise une instance fraîche pour être sûr
                                val manager = com.example.dam_front.utils.FirebaseTokenManager(context)
                                val token = manager.getFCMToken()
                                debugFcmToken = token ?: "Erreur : Token non généré"
                                android.util.Log.d("MainScreen", "Debug FCM Token: $token")
                            }
                        }
                    )
                    
                    // Main Content Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        MainContentCard(
                            currentScreen = currentScreen,
                            selectedTournoi = selectedTournoi,
                            onTournoiSelected = { tournoi -> selectedTournoi = tournoi },
                            onTournoiDeselected = { selectedTournoi = null },
                            selectedChildId = selectedChildId,
                            userRole = userRole,
                            activitiesViewModel = activitiesViewModel,
                            programsViewModel = programsViewModel,
                            onAddActivityClick = { showAddActivity = true },
                            onActivityClick = { activity -> selectedActivity = activity },
                            onAddProgramClick = { showAddProgram = true },
                            onProgramClick = { program -> selectedProgram = program },
                            onLogout = onLogout
                        )
                    }
                    
                    // Bottom Navigation
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { 
                            currentScreen = it
                            selectedTournoi = null // Réinitialiser quand on change d'écran
                        }
                    )
                }
                
                // Dialog d'ajout d'enfant
                if (showAddChildDialog) {
                    android.util.Log.d("MainScreen", "Rendering AddChildDialog")
                    AddChildDialog(
                        onDismiss = { showAddChildDialog = false },
                        onChildAdded = {
                            reloadChildren()
                            showAddChildDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderSection(
    title: String,
    onMenuClick: () -> Unit = {},
    children: List<ChildResponse> = emptyList(),
    selectedChildId: String? = null,
    onChildSelected: (String) -> Unit = {},
    unreadNotificationsCount: Int = 0,
    onNotificationsClick: () -> Unit = {},
    onDebugFcmClick: () -> Unit = {}
) {
    var showChildDropdown by remember { mutableStateOf(false) }
    val selectedChild = children.find { it.id == selectedChildId }
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
            // Section gauche : Profil + Titre
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Profile Picture - Clic pour ouvrir le menu (design moderne et compact)
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
                        // Icône de profil moderne
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            modifier = Modifier.size(24.dp),
                            tint = HeaderBlue
                        )
                    }
                }
                
                // Titre et sélecteur d'enfant
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    // Sélecteur d'enfant
                    if (children.isNotEmpty()) {
                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showChildDropdown = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = selectedChild?.let { "${it.prenom} ${it.nom}" } ?: "Sélectionner un enfant",
                                    color = TextWhite.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Sélectionner un enfant",
                                    tint = TextWhite.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            DropdownMenu(
                                expanded = showChildDropdown,
                                onDismissRequest = { showChildDropdown = false }
                            ) {
                                children.forEach { child ->
                                    DropdownMenuItem(
                                        text = { Text("${child.prenom} ${child.nom}") },
                                        onClick = {
                                            onChildSelected(child.id)
                                            showChildDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Sporty ",
                                color = IconOrange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "KIDS",
                                color = IconGreen,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            
            // Icons à droite
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Notification Icon avec badge clickable
                IconButton(onClick = onNotificationsClick) {
                    Box {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContentCard(
    currentScreen: Screen,
    selectedTournoi: com.example.dam_front.models.Tournoi?,
    onTournoiSelected: (com.example.dam_front.models.Tournoi) -> Unit,
    onTournoiDeselected: () -> Unit,
    selectedChildId: String? = null,
    userRole: String? = null,
    activitiesViewModel: ActivitiesViewModel? = null,
    programsViewModel: ProgramsViewModel? = null,
    onAddActivityClick: () -> Unit = {},
    onActivityClick: (com.example.dam_front.models.Activity) -> Unit = {},
    onAddProgramClick: () -> Unit = {},
    onProgramClick: (com.example.dam_front.models.Program) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // Log pour débogage
    android.util.Log.d("MainScreen", "MainContentCard appelé avec screen: $currentScreen")
    
    when (currentScreen) {
        Screen.ACTIVITES -> {
            if (activitiesViewModel != null) {
                // Check if user is coach
                val isCoach = userRole?.lowercase()?.contains("coach") == true
                ActivitiesListScreen(
                    viewModel = activitiesViewModel,
                    isCoach = isCoach,
                    onMenuClick = {}, 
                    onAddClick = onAddActivityClick,
                    onActivityClick = onActivityClick
                )
            } else {
                DefaultContentCard(currentScreen = currentScreen)
            }
        }
        Screen.PROGRAMMES -> {
            if (programsViewModel != null) {
                // Logic based on role
                val isCoach = userRole?.lowercase()?.contains("coach") == true
                
                if (isCoach) {
                    ProgramsListScreen(
                        viewModel = programsViewModel,
                        onMenuClick = {},
                        onAddClick = onAddProgramClick,
                        onProgramClick = onProgramClick
                    )
                } else {
                    ParentDashboardScreen(
                        viewModel = programsViewModel,
                        onMenuClick = {},
                        onProgramClick = onProgramClick
                    )
                }
            } else {
                DefaultContentCard(currentScreen = currentScreen)
            }
        }
        Screen.TOURNOIS -> {
            // Afficher la liste des tournois
            android.util.Log.d("MainScreen", "Affichage de TournoisListScreen")
            TournoisListScreen(
                onTournoiClick = onTournoiSelected,
                selectedChildId = selectedChildId
            )
        }
// CORRECTION HERE
        Screen.MON_ENFANT -> {
            android.util.Log.d("MainScreen", "Affichage de MonEnfantScreen")
            MonEnfantScreen(onLogout = onLogout)
        }
        else -> {
            // Afficher la carte par défaut pour les autres écrans
            android.util.Log.d("MainScreen", "Affichage de DefaultContentCard pour $currentScreen")
            DefaultContentCard(currentScreen = currentScreen)
        }
    }
}

@Composable
fun DefaultContentCard(currentScreen: Screen) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(4.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            val iconColor = when (currentScreen) {
                Screen.ACTIVITES -> IconOrange
                Screen.PROGRAMMES -> IconTeal
                Screen.ABONNEMENTS -> IconGreen
                Screen.TOURNOIS -> IconGreen
                Screen.MON_ENFANT -> IconBlueLight
                Screen.NOTIFICATIONS -> HeaderBlue
            }
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconColor),
                contentAlignment = Alignment.Center
            ) {
                when (currentScreen) {
                    Screen.ACTIVITES -> TargetIcon(tint = Color.White)
                    Screen.PROGRAMMES -> ListIcon(tint = Color.White)
                    Screen.ABONNEMENTS -> CardIcon(tint = Color.White)
                    Screen.TOURNOIS -> TrophyIcon(tint = Color.White)
                    Screen.MON_ENFANT -> PeopleIcon(tint = Color.White)
                    Screen.NOTIFICATIONS -> Icon(Icons.Default.Notifications, null, tint = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = when (currentScreen) {
                    Screen.ACTIVITES -> "Activités Sportives"
                    Screen.PROGRAMMES -> "Programmes"
                    Screen.ABONNEMENTS -> "Abonnements & Offres"
                    Screen.TOURNOIS -> "Tournois"
                    Screen.MON_ENFANT -> "Suivre Mon Enfant"
                    Screen.NOTIFICATIONS -> "Notifications"
                },
                color = TextBlue,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Cette section sera bientôt disponible\navec toutes les informations dont\nvous avez besoin.",
                color = TextBlueLight,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Pagination dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaginationDot(color = DotOrange, isSelected = currentScreen == Screen.ACTIVITES)
                PaginationDot(color = DotBlue, isSelected = currentScreen == Screen.PROGRAMMES)
                PaginationDot(color = DotGreen, isSelected = currentScreen == Screen.ABONNEMENTS)
                PaginationDot(color = DotYellow, isSelected = currentScreen == Screen.TOURNOIS)
                PaginationDot(color = DotBlue, isSelected = currentScreen == Screen.MON_ENFANT)
            }
        }
    }
}

// ... Rest of the file identical to previous step ...
@Composable
fun PaginationDot(color: Color, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(if (isSelected) 10.dp else 8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
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
                            Color(0xFFE3F2FD), // Light Blue 100
                            Color(0xFFF3E5F5), // Light Purple 50
                            Color(0xFFFCE4EC)  // Pink 50
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    icon = { color -> ListIcon(tint = color) },
                    label = "Programmes",
                    isSelected = currentScreen == Screen.PROGRAMMES,
                    activeColor = IconTeal,
                    onClick = { onScreenSelected(Screen.PROGRAMMES) }
                )
                NavItem(
                    icon = { color -> TrophyIcon(tint = color) },
                    label = "Tournois",
                    isSelected = currentScreen == Screen.TOURNOIS,
                    activeColor = IconGreen,
                    onClick = { onScreenSelected(Screen.TOURNOIS) }
                )
                NavItem(
                    icon = { color -> PeopleIcon(tint = color) },
                    label = "Mon Enfant",
                    isSelected = currentScreen == Screen.MON_ENFANT,
                    activeColor = IconBlue,
                    onClick = { onScreenSelected(Screen.MON_ENFANT) }
                )
                NavItem(
                    icon = { color -> CardIcon(tint = color) },
                    label = "Abonnements",
                    isSelected = currentScreen == Screen.ABONNEMENTS,
                    activeColor = IconOrange,
                    onClick = { onScreenSelected(Screen.ABONNEMENTS) }
                )
            }
        }
    }
}

@Composable
fun RowScope.NavItem(
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

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun MainScreenPreview() {
    DAM_frontTheme {
        MainScreen()
    }
}

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Erreur", color = Color.Red) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = SportyOrange)
            }
        },
        containerColor = Color.White,
        titleContentColor = Color.Red,
        textContentColor = TextDarkGray
    )
}

private fun uriToFile(context: android.content.Context, uri: android.net.Uri): java.io.File? {
    return try {
        // Detect the MIME type from the URI
        val mimeType = context.contentResolver.getType(uri)
        println("DEBUG uriToFile: MIME type detected: $mimeType")
        
        // Determine the file extension based on MIME type
        val extension = when (mimeType) {
            "image/jpeg", "image/jpg" -> ".jpg"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            "image/bmp" -> ".bmp"
            else -> {
                // Try to get extension from URI path
                val fileName = uri.path?.substringAfterLast('/') ?: ""
                val ext = fileName.substringAfterLast('.', "")
                if (ext.isNotEmpty() && ext.length <= 4) {
                    ".$ext"
                } else {
                    ".jpg" // Default fallback
                }
            }
        }
        
        println("DEBUG uriToFile: Using extension: $extension")
        
        val inputStream = context.contentResolver.openInputStream(uri)
        val tempFile = java.io.File.createTempFile("image", extension, context.cacheDir)
        println("DEBUG uriToFile: Created temp file: ${tempFile.absolutePath}")
        
        inputStream?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        
        println("DEBUG uriToFile: File size: ${tempFile.length()} bytes")
        tempFile
    } catch (e: Exception) {
        println("DEBUG uriToFile: Error - ${e.message}")
        e.printStackTrace()
        null
    }
}
