package com.example.dam_front.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.utils.ImageUtils
import com.example.dam_front.models.User
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.CoachHomeViewModel
import com.example.dam_front.viewmodels.CoachHomeViewModelFactory
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory
import com.example.dam_front.utils.TokenManager
import java.text.SimpleDateFormat
import java.util.Locale

const val COACH_HOME_ROUTE = "suivi_list"
const val COACH_CHILD_DETAIL_ROUTE = "suivi_detail"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachHomeScreen(
    navController: NavController,
    onMenuClick: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    println("🎯 CoachHomeScreen COMPOSABLE CALLED")
    
    // ViewModel and state
    val context = LocalContext.current
    val application = context.applicationContext as Application
    println("🏭 CoachHomeScreen: Creating ViewModel...")
    val viewModel: CoachHomeViewModel = viewModel(
        factory = CoachHomeViewModelFactory(application)
    )
    println("✅ CoachHomeScreen: ViewModel created successfully")
    val uiState by viewModel.uiState.collectAsState()
    
    // ADAPTATION FOR TARGET: Use TokenManager class instead of singleton
    val tokenManager = remember { TokenManager(context) }
    val prenom by tokenManager.getUserPrenom().collectAsState(initial = null)
    val nom by tokenManager.getUserNom().collectAsState(initial = null)
    val persistedName = if (prenom != null || nom != null) "${prenom ?: ""} ${nom ?: ""}".trim() else null
    
    var localCoachName by remember { mutableStateOf(persistedName?.ifBlank { uiState.coachName } ?: uiState.coachName) }

    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val sharedVm: SuiviSharedViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = SuiviSharedViewModelFactory(application)
    )
    val suivisMap by sharedVm.suivisByChild.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedChildId by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var suiviToDelete by remember { mutableStateOf<SuiviEnfant?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(persistedName, uiState.coachName) {
        localCoachName = persistedName?.ifBlank { uiState.coachName } ?: uiState.coachName
    }
    
    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }

    // Trigger children refresh when screen loads
    LaunchedEffect(Unit) {
        println("🚀 CoachHomeScreen: LaunchedEffect triggered - calling viewModel.refreshChildren()")
        viewModel.refreshChildren()
        println("🔄 CoachHomeScreen: refreshChildren() call completed")
    }

    // Create children list with real photos by fetching full user data
    var derivedChildren by remember { mutableStateOf<List<User>>(emptyList()) }
    
    LaunchedEffect(suivisMap) {
        val childIds = suivisMap.values.flatten()
            .mapNotNull { suivi -> suivi.enfant?.id ?: suivi.enfantId }
            .distinct()
        
        println("🔧 CoachHomeScreen: Fetching real user data for ${childIds.size} children")
        
        // Fetch actual User objects with photos from the API
        val fetchedChildren = withContext(Dispatchers.IO) {
            childIds.mapNotNull { childId ->
                try {
                    println("🔍 Fetching full user data for: $childId")
                    // ADAPTATION: Using RetrofitClient.createAuthenticatedService
                    val apiService = com.example.dam_front.api.RetrofitClient.createAuthenticatedService(context, com.example.dam_front.api.ApiService::class.java)
                    val user = apiService.getUser(childId)
                    println("✅ Loaded: ${user.prenom} ${user.nom} - Photo: '${user.photoProfil}'")
                    user
                } catch (e: Exception) {
                    println("❌ Failed to fetch user $childId: ${e.message}")
                    // Fallback to creating User from EnfantRef data
                    suivisMap.values.flatten()
                        .firstOrNull { it.enfant?.id == childId }
                        ?.enfant?.let { enfantRef ->
                            User(
                                id = enfantRef.id,
                                email = "",
                                nom = enfantRef.nom ?: "Child",
                                prenom = enfantRef.prenom ?: "Unknown",
                                role = "ENFANT"
                            )
                        }
                }
            }
        }.sortedBy { "${it.prenom} ${it.nom}" }
        
        derivedChildren = fetchedChildren
        println("🎉 Loaded ${fetchedChildren.size} children with real data")
    }    // Use derived children instead of ViewModel children
    val effectiveChildren = if (derivedChildren.isNotEmpty()) derivedChildren else uiState.allChildren
    
    // Refresh suivis for all children and debug children data
    LaunchedEffect(effectiveChildren) {
        println("CoachHomeScreen: Children data changed - Count: ${effectiveChildren.size}")
        effectiveChildren.forEachIndexed { index, child ->
            child.id?.let { sharedVm.refreshForChild(it) }
        }
    }

    // Ensure the selected child's suivis are refreshed when selection changes
    LaunchedEffect(selectedChildId) {
        if (selectedChildId.isNotBlank()) {
            sharedVm.refreshForChild(selectedChildId)
        }
    }

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Erreur", color = Color.Red)
            }
        }
        else -> {
            // Get all suivis and filter
            val allSuivis = remember(suivisMap, selectedChildId) {
                // Flatten all lists and remove duplicates based on suivi ID
                val allList = suivisMap.values.flatten().distinctBy { it.id }
                
                if (selectedChildId.isBlank()) {
                    allList
                } else {
                    // Filter by selected child ID
                    allList.filter { 
                        val childId = it.enfant?.id ?: it.enfantId
                        childId == selectedChildId 
                    }
                }
            }.sortedByDescending { it.dateSuivi }

            val filteredSuivis = remember(allSuivis, searchQuery) {
                if (searchQuery.isBlank()) {
                    allSuivis
                } else {
                    allSuivis.filter { suivi ->
                        val childName = suivi.enfant?.let { "${it.prenom} ${it.nom}" } ?: suivi.enfantName ?: ""
                        childName.contains(searchQuery, ignoreCase = true)
                    }
                }
            }

            var selectedNavItem by remember { mutableStateOf(0) } // Default to first item for coach
            
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    BottomAppBar(
                        containerColor = Color.White,
                        contentColor = IconInactiveGray,
                        modifier = Modifier.height(80.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Activités
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedNavItem = 0 }
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (selectedNavItem == 0) HeaderBlue else Color.White,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.DirectionsRun,
                                        contentDescription = "Activités",
                                        tint = if (selectedNavItem == 0) Color.White else HeaderBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Activités",
                                    fontSize = 10.sp,
                                    color = if (selectedNavItem == 0) HeaderBlue else Color.Gray,
                                    fontWeight = if (selectedNavItem == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            // Abonnements
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedNavItem = 1 }
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (selectedNavItem == 1) SportyKidsBlue else Color.White,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.CreditCard,
                                        contentDescription = "Abonnements",
                                        tint = if (selectedNavItem == 1) Color.White else SportyKidsBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Abonnements",
                                    fontSize = 10.sp,
                                    color = if (selectedNavItem == 1) SportyKidsBlue else Color.Gray,
                                    fontWeight = if (selectedNavItem == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            // Tournois
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedNavItem = 2 }
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            SportyKidsGreen,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = "Tournois",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Tournois",
                                    fontSize = 10.sp,
                                    color = if (selectedNavItem == 2) SportyKidsGreen else Color.Gray,
                                    fontWeight = if (selectedNavItem == 2) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            // Coaching (for coach)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedNavItem = 3 }
                                    .padding(vertical = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (selectedNavItem == 3) AccentOrange else IconOrangeLight,
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Sports,
                                        contentDescription = "Coaching",
                                        tint = if (selectedNavItem == 3) Color.White else AccentOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Coaching",
                                    fontSize = 10.sp,
                                    color = if (selectedNavItem == 3) AccentOrange else Color.Gray,
                                    fontWeight = if (selectedNavItem == 3) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { navController.navigate("coach_create_suivi") },
                        containerColor = AccentOrange,
                        contentColor = Color.White,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Créer un suivi",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Simple Top Bar
                    // Note: SimpleTopBar must be available in Target
                    SimpleTopBar(
                        title = "Coach",
                        subtitle = localCoachName,
                        profilePhoto = uiState.coachPhoto,
                        onLogout = onLogout
                    )
                    
                    // Action Section
                    CoachActionSection(
                        onChatClick = { navController.navigate("chat_list") },
                        searchQuery = searchQuery,
                        onSearchChange = { query: String -> searchQuery = query },
                        children = effectiveChildren,
                        selectedChildId = selectedChildId,
                        onChildSelect = { childId: String -> selectedChildId = childId }
                    )
                    // Suivis List
                    if (filteredSuivis.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Aucun suivi disponible",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(16.dp))
                                Button(
                                    onClick = { navController.navigate("coach_create_suivi") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SportyKidsGreen)
                                ) {
                                    Text("Créer un suivi", color = Color.White)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                        ) {
                            items(filteredSuivis) { suivi ->
                                // Find the child from the children list to get their photo
                                val childId = suivi.enfant?.id ?: suivi.enfantId
                                val child = effectiveChildren.find { it.id == childId }
                                
                                CoachSuiviCard(
                                    suivi = suivi,
                                    onDetails = { 
                                        childId?.let { navController.navigate("$COACH_CHILD_DETAIL_ROUTE/$it") }
                                    },
                                    onDelete = {
                                        suiviToDelete = suivi
                                        showDeleteDialog = true
                                    },
                                    allChildren = effectiveChildren
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && suiviToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                suiviToDelete = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = SportyKidsRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Supprimer le suivi",
                        fontWeight = FontWeight.Bold,
                        color = TextBlue
                    )
                }
            },
            text = {
                Column {
                    Text("Êtes-vous sûr de vouloir supprimer ce suivi ?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cette action est irréversible.",
                        color = SportyKidsRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        suiviToDelete?.let { suivi ->
                            // Use coroutine scope to perform the deletion
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    println("🗑️ Attempting to delete suivi: ${suivi.id}")
                                    
                                    // ADAPTATION: Use context for repo
                                    val repo = com.example.dam_front.repository.SuiviRepository(context)
                                    val result = repo.deleteSuivi(suivi.id)
                                    withContext(Dispatchers.Main) {
                                        if (result.isSuccess) {
                                            snackbarMessage = "✅ Suivi supprimé avec succès"
                                            // Refresh the shared ViewModel to update the UI  
                                            sharedVm.loadAllSuivis()
                                        } else {
                                            val error = result.exceptionOrNull()
                                            snackbarMessage = "❌ Erreur: ${error?.message ?: "Erreur inconnue"}"
                                            println("🚨 Delete error: ${error?.message}")
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        snackbarMessage = "❌ Erreur de connexion: ${e.message}"
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        showDeleteDialog = false
                        suiviToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SportyKidsRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Supprimer", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        suiviToDelete = null
                    },
                    border = BorderStroke(1.dp, AccentOrange),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AccentOrange
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Annuler", fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// SimpleTopBar is defined in ParentScreen.kt - shared component

@Composable
private fun CoachActionSection(
    onChatClick: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    children: List<com.example.dam_front.models.User>,
    selectedChildId: String,
    onChildSelect: (String) -> Unit
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(16.dp)
    ) {
        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chat Button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onChatClick() },
                shape = CircleShape,
                color = HeaderBlue,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.ChatBubble,
                        contentDescription = "Chat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Dropdown Child Selector
            Box(modifier = Modifier.weight(1f)) {
                
                val selectedName = children.find { it.id == selectedChildId }?.let { "${it.prenom} ${it.nom}" }
                    ?: "Tous les enfants"

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { dropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    color = NavGrayBg,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Enfants",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                selectedName,
                                fontSize = 14.sp,
                                color = Color.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Tous les enfants") },
                        onClick = {
                            onChildSelect("")
                            dropdownExpanded = false
                        }
                    )
                    children.forEach { child ->
                        DropdownMenuItem(
                            text = { Text("${child.prenom} ${child.nom}") },
                            onClick = {
                                onChildSelect(child.id ?: "")
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF5F5F5),
            shadowElevation = 1.dp
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher un enfant...", color = Color.Gray.copy(alpha = 0.6f)) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CoachSuiviCard(
    suivi: SuiviEnfant,
    onDetails: (String) -> Unit,
    onDelete: () -> Unit,
    allChildren: List<com.example.dam_front.models.User> = emptyList(),
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val childName = suivi.enfant?.let { "${it.prenom} ${it.nom}" } ?: suivi.enfantName ?: "Enfant"
    val presencePercent = if (suivi.presence) 100 else 0
    val performancePercent = (suivi.performance) * 10
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .border(2.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Photo, Name, Trophy
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Get child photo from allChildren list
                val childId = suivi.enfant?.id ?: suivi.enfantId
                val child = allChildren.find { it.id == childId }
                
                // Photo loading simplifiée avec ImageUtils
                val childPhotoUrl = run {
                    ImageUtils.getPhotoUrl(child?.photoProfil)
                        ?: "https://ui-avatars.com/api/?name=${childName.replace(" ", "+")}&background=FF9800&color=fff&size=128&bold=true&format=png"
                }
                
                AsyncImage(
                    model = coil.request.ImageRequest.Builder(LocalContext.current)
                        .data(childPhotoUrl)
                        .crossfade(true)
                        .allowHardware(false)
                        .build(),
                    contentDescription = "Photo de $childName",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .border(3.dp, AccentOrange, CircleShape)
                        .background(AccentOrange.copy(alpha = 0.1f), CircleShape),
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        println("❌ Failed to load real photo for $childName: ${error.result.throwable.message}")
                    },
                    onSuccess = {
                        println("✅ Successfully loaded real photo for $childName")
                    },
                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(NavGrayBg),
                    error = coil.compose.rememberAsyncImagePainter(
                        model = "https://ui-avatars.com/api/?name=${childName.replace(" ", "+")}&background=FF9800&color=fff&size=128&bold=true&format=png"
                    )
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        childName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextBlue
                    )
                    Text(
                        "enfant",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = SportyKidsRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = AccentOrange,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = AccentOrange,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Dernière suivi: ${sdf.format(suivi.dateSuivi)}",
                    color = AccentOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    text = "Présence: $presencePercent%",
                    color = SportyKidsGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Performance: $performancePercent%",
                    color = AccentOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Performance Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val perf = suivi.performance
                val (bgColor, textContent) = when {
                    perf >= 8 -> SportyKidsGreen to "B"
                    perf >= 5 -> HeaderBlue to "3"
                    else -> SportyKidsRed to perf.toString()
                }

                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                if (index == 0) bgColor else Color(0xFFEFEFEF),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (index == 0) {
                            Text(
                                textContent,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Details Button
            OutlinedButton(
                onClick = { onDetails(suivi.enfant?.id ?: suivi.enfantId ?: "") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, AccentOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentOrange),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    "Voir les détails",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
