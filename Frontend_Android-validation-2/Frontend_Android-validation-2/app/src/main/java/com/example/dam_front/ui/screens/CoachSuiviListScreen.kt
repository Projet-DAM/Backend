package com.example.dam_front.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
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
import coil.compose.AsyncImage
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.utils.ImageUtils
import com.example.dam_front.models.User
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.CoachHomeViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.dam_front.utils.TokenManager

/**
 * Écran de liste des suivis pour le coach
 * Version simplifiée de CoachHomeScreen SANS la bottom navigation
 * (la bottom nav est gérée par CoachMainScreen)
 */
@Composable
fun CoachSuiviListScreen(
    coachViewModel: CoachHomeViewModel,
    sharedViewModel: SuiviSharedViewModel,
    onCreateSuivi: () -> Unit,
    onChildDetail: (String) -> Unit,
    onChatClick: () -> Unit,
    onAiSummaryClick: () -> Unit
) {
    val uiState by coachViewModel.uiState.collectAsState()
    val suivisMap by sharedViewModel.suivisByChild.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isDateDescending by remember { mutableStateOf(true) } // Tri par date (décroissant par défaut)
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Auth & Token
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    // Combine nom + prénom
    val prenom by remember(tokenManager) { 
        kotlinx.coroutines.flow.flow { emit(tokenManager.getUserPrenom().first()) } 
    }.collectAsState(initial = null)
    
    val nom by remember(tokenManager) { 
        kotlinx.coroutines.flow.flow { emit(tokenManager.getUserNom().first()) } 
    }.collectAsState(initial = null)
    
    val coachName = if (prenom != null || nom != null) "${prenom ?: ""} ${nom ?: ""}".trim() else "Coach"
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var suiviToDelete by remember { mutableStateOf<SuiviEnfant?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }
    
    // Trigger children refresh when screen loads
    LaunchedEffect(Unit) {
        coachViewModel.refreshChildren()
    }
    
    // Create children list with real photos
    var derivedChildren by remember { mutableStateOf<List<User>>(emptyList()) }
    
    LaunchedEffect(suivisMap) {
        val childIds = suivisMap.values.flatten()
            .mapNotNull { suivi -> suivi.enfant?.id ?: suivi.enfantId }
            .distinct()
        
        val fetchedChildren = withContext(Dispatchers.IO) {
            childIds.mapNotNull { childId ->
                try {
                    val apiService = com.example.dam_front.api.RetrofitClient.createAuthenticatedService(context, com.example.dam_front.api.ApiService::class.java)
                    apiService.getUser(childId)
                } catch (e: Exception) {
                    suivisMap.values.flatten()
                        .firstOrNull { it.enfant?.id == childId }
                        ?.enfant?.let { enfantRef ->
                            User(
                                id = enfantRef.id,
                                email = "child@example.local",
                                prenom = enfantRef.prenom ?: "Unknown",
                                nom = enfantRef.nom ?: "Child",
                                role = "ENFANT",
                                photoProfil = null
                            )
                        }
                }
            }
        }.sortedBy { "${it.prenom} ${it.nom}" }
        
        derivedChildren = fetchedChildren
    }
    
    val effectiveChildren = if (derivedChildren.isNotEmpty()) derivedChildren else uiState.allChildren
    
    // Refresh suivis for all children
    LaunchedEffect(effectiveChildren) {
        effectiveChildren.forEach { child ->
            child.id?.let { sharedViewModel.refreshForChild(it) }
        }
    }
    
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = HeaderBlue)
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Erreur", color = Color.Red)
            }
        }
        else -> {
            // Get all suivis and sort by date
            val allSuivis = remember(suivisMap, isDateDescending) {
                val allList = suivisMap.values.flatten().distinctBy { it.id }
                
                if (isDateDescending) {
                    allList.sortedByDescending { it.dateSuivi }
                } else {
                    allList.sortedBy { it.dateSuivi }
                }
            }
            
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
            
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                // No topBar, derived from CoachMainScreen
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                floatingActionButton = {
                        // Floating buttons removed from here. 
                        // Chat is now global in CoachMainScreen.
                        // Add is now in the top Action Section.
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Action Section
                    CoachActionSection(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        isDateDescending = isDateDescending,
                        onSortChange = { isDateDescending = it },
                        onAddClick = onCreateSuivi,
                        onAiSummaryClick = onAiSummaryClick
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
                                    onClick = onCreateSuivi,
                                    colors = ButtonDefaults.buttonColors(containerColor = IconOrange)
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
                                val childId = suivi.enfant?.id ?: suivi.enfantId
                                // val child = effectiveChildren.find { it.id == childId }
                                
                                CoachSuiviCard(
                                    suivi = suivi,
                                    onDetails = {
                                        childId?.let { onChildDetail(it) }
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
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Supprimer le suivi",
                        fontWeight = FontWeight.Bold,
                        color = HeaderBlue
                    )
                }
            },
            text = {
                Column {
                    Text("Êtes-vous sûr de vouloir supprimer ce suivi ?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cette action est irréversible.",
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        suiviToDelete?.let { suivi ->
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val repo = com.example.dam_front.repository.SuiviRepository(context)
                                    val result = repo.deleteSuivi(suivi.id)
                                    withContext(Dispatchers.Main) {
                                        if (result.isSuccess) {
                                            snackbarMessage = "✅ Suivi supprimé avec succès"
                                            sharedViewModel.loadAllSuivis()
                                        } else {
                                            // Handle error
                                            val ex = result.exceptionOrNull()
                                            snackbarMessage = "❌ Erreur: ${ex?.message}"
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        snackbarMessage = "❌ Erreur: ${e.message}"
                                    }
                                }
                            }
                        }
                        showDeleteDialog = false
                        suiviToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDeleteDialog = false
                        suiviToDelete = null
                    },
                    border = BorderStroke(1.dp, IconOrange),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IconOrange),
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

@Composable
private fun CoachActionSection(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isDateDescending: Boolean,
    onSortChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onAiSummaryClick: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(16.dp)
    ) {
        // Row combining Search, Sort Icon, and Add Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            // Search Bar (Flexible weight)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Rechercher...", fontSize = 14.sp) },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Search, 
                        contentDescription = null,
                        tint = TextBlueLight
                    ) 
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HeaderBlue,
                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFF8F9FA)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = TextDarkGray
                ),
            )

            // Sort Icon with Dropdown
            Box {
                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(1.dp, HeaderBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        Icons.Default.FilterList, // Using FilterList icon for Sort/Filter action
                        contentDescription = "Trier",
                        tint = HeaderBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Date : Plus récent -> Plus ancien") },
                        onClick = {
                            onSortChange(true)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (isDateDescending) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = HeaderBlue)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date : Plus ancien -> Plus récent") },
                        onClick = {
                            onSortChange(false)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (!isDateDescending) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = HeaderBlue)
                            }
                        }
                    )
                }
            }

            // AI Summary Icon
            IconButton(
                onClick = onAiSummaryClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = HeaderBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = "Résumé IA",
                    tint = HeaderBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Add Button
            FloatingActionButton(
                onClick = onAddClick,
                modifier = Modifier.size(48.dp),
                containerColor = IconOrange,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Default.Add, 
                    contentDescription = "Créer un suivi",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun CoachSuiviCard(
    suivi: SuiviEnfant,
    onDetails: (String) -> Unit,
    onDelete: () -> Unit,
    allChildren: List<User> = emptyList(),
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val childName = suivi.enfant?.let { "${it.prenom} ${it.nom}" } ?: suivi.enfantName ?: "Enfant"
    val presencePercent = if (suivi.presence) 100 else 0
    val performancePercent = suivi.performance * 10
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = IconOrange.copy(alpha = 0.5f))
            .border(2.dp, if (suivi.presence) IconGreen.copy(alpha = 0.5f) else IconOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .background(Color.White, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Photo, Name, Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val childId = suivi.enfant?.id ?: suivi.enfantId
                
                val childPhotoUrl = run {
                    val childFromList = allChildren.find { it.id == childId }
                    ImageUtils.getPhotoUrl(childFromList?.photoProfil)
                        ?: "https://ui-avatars.com/api/?name=${childName.replace(" ", "+")}&background=4CAF50&color=fff&size=128&bold=true&format=png"
                }
                
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(IconOrange, IconGreen)
                            ),
                            shape = CircleShape
                        )
                        .padding(3.dp)
                ) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(childPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Photo de $childName",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = childName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderBlue
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp), 
                            tint = IconOrange
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = sdf.format(suivi.dateSuivi),
                            fontSize = 14.sp,
                            color = TextDarkGray.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { childId?.let { onDetails(it) } },
                        modifier = Modifier.background(HeaderBlue.copy(alpha = 0.1f), CircleShape).size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Détails",
                            tint = HeaderBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.background(IconGreen.copy(alpha = 0.1f), CircleShape).size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = IconGreen, // User requested Green Trash Can
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp), 
                color = Color(0xFFEEEEEE), 
                thickness = 1.dp
            )
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Présence", presencePercent, if (presencePercent > 0) IconGreen else IconOrange)
                StatItem("Performance", performancePercent, if (performancePercent >= 60) IconOrange else Color.Gray)
            }
            
            // Comment
            if (!suivi.commentaire.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)), // Light Yellow/Orange bg
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "💬 ${suivi.commentaire}",
                        fontSize = 14.sp,
                        color = Color(0xFF5D4037),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${value/10}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun SimpleTopBar(
    title: String,
    subtitle: String?,
    profilePhoto: String?,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Photo
            if (profilePhoto != null) {
                AsyncImage(
                    model = profilePhoto,
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(HeaderBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.take(1),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
    }
}
