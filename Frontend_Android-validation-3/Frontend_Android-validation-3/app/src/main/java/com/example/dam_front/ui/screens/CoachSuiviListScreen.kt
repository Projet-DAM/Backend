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
import androidx.compose.ui.graphics.Brush
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
    onAiSummaryClick: (String) -> Unit = {}
) {
    val uiState by coachViewModel.uiState.collectAsState()
    val suivisMap by sharedViewModel.suivisByChild.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedChildId by remember { mutableStateOf("") }
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

    // Ensure the selected child's suivis are refreshed
    LaunchedEffect(selectedChildId) {
        if (selectedChildId.isNotBlank()) {
            sharedViewModel.refreshForChild(selectedChildId)
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
            // Get all suivis and filter
            val allSuivis = remember(suivisMap, selectedChildId) {
                val allList = suivisMap.values.flatten().distinctBy { it.id }

                if (selectedChildId.isBlank()) {
                    allList
                } else {
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
                    // Action Section
                    CoachActionSection(
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        children = uiState.allChildren,
                        selectedChildId = selectedChildId,
                        onChildSelect = { selectedChildId = it },
                        onAddClick = onCreateSuivi,
                        onAiSummaryClick = {
                            if (selectedChildId.isNotBlank()) {
                                onAiSummaryClick(selectedChildId)
                            }
                        }
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
                                    onAiSummary = {
                                        childId?.let { onAiSummaryClick(it) }
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
    children: List<User>,
    selectedChildId: String,
    onChildSelect: (String) -> Unit,
    onAddClick: () -> Unit,
    onAiSummaryClick: () -> Unit
) {
    var showFilterDialog by remember { mutableStateOf(false) }

    // Filter Dialog
    if (showFilterDialog) {
        ChildFilterDialog(
            children = children,
            selectedChildId = selectedChildId,
            onChildSelect = {
                onChildSelect(it)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(16.dp)
    ) {
        // Row combining Search, Filter Icon, and Add Icon
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

            // Filter Icon
            IconButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (selectedChildId.isNotEmpty()) SportyKidsGreen.copy(alpha = 0.2f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filtrer",
                    tint = if (selectedChildId.isNotEmpty()) SportyKidsGreen else SportyKidsBlue,
                    modifier = Modifier.size(24.dp)
                )
            }


            // AI Summary Icon (Only when child selected)
            if (selectedChildId.isNotEmpty()) {
                IconButton(
                    onClick = onAiSummaryClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = SportyKidsGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Résumé IA",
                        tint = SportyKidsGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChildFilterDialog(
    children: List<User>,
    selectedChildId: String,
    onChildSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrer par enfant", color = HeaderBlue, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedChildId.isEmpty(),
                        onClick = { onChildSelect("") },
                        label = { Text("Tous les enfants") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IconOrange.copy(alpha = 0.2f),
                            selectedLabelColor = IconOrange
                        )
                    )
                }
                items(children) { child ->
                    FilterChip(
                        selected = selectedChildId == child.id,
                        onClick = { onChildSelect(child.id ?: "") },
                        label = { Text("${child.prenom} ${child.nom}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IconOrange.copy(alpha = 0.2f),
                            selectedLabelColor = IconOrange
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = TextDarkGray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CoachSuiviCard(
    suivi: SuiviEnfant,
    onDetails: (String) -> Unit,
    onDelete: () -> Unit,
    onAiSummary: () -> Unit,
    allChildren: List<User> = emptyList(),
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.FRANCE) }
    val childName = suivi.enfant?.let { "${it.prenom} ${it.nom}" } ?: suivi.enfantName ?: "Élève"
    val presencePercent = if (suivi.presence) 100 else 0
    val performance = suivi.performance.toFloat().coerceIn(0f, 10f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = HeaderBlue.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val childId = suivi.enfant?.id ?: suivi.enfantId
                val childPhotoUrl = run {
                    val childFromList = allChildren.find { it.id == childId }
                    ImageUtils.getPhotoUrl(childFromList?.photoProfil)
                        ?: "https://ui-avatars.com/api/?name=${childName.replace(" ", "+")}&background=4A90E2&color=fff&size=128&bold=true"
                }

                // Enhanced Avatar with Gradient Border
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(HeaderBlue, IconOrange, SportyKidsGreen)
                            ),
                            shape = CircleShape
                        )
                        .padding(2.dp)
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

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = childName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Surface(
                        color = IconOrange.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = sdf.format(suivi.dateSuivi),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    color = (if (suivi.presence) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (suivi.presence) "Présent" else "Absent",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = (if (suivi.presence) Color(0xFF2E7D32) else Color(0xFFC62828)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Performance Metric
                MetricCard(
                    label = "Performance",
                    value = "${suivi.performance}/10",
                    progress = performance / 10f,
                    icon = Icons.Default.Star,
                    color = IconOrange,
                    modifier = Modifier.weight(1f)
                )
                
                // Effort Metric (Calculated or defaulted)
                MetricCard(
                    label = "Progression",
                    value = if (suivi.performance > 7) "+12%" else "+5%",
                    progress = (suivi.performance.toFloat() / 10f) * 0.8f,
                    icon = Icons.Default.TrendingUp,
                    color = SportyKidsGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Comment Insight
            if (!suivi.commentaire.isNullOrBlank()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(HeaderBlue.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FormatQuote,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = HeaderBlue
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = suivi.commentaire ?: "",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action Buttons Group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Action: Details
                Button(
                    onClick = { suivi.enfant?.id?.let { onDetails(it) } ?: suivi.enfantId?.let { onDetails(it) } },
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Détails", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Secondary Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(
                        icon = Icons.Default.AutoAwesome,
                        color = SportyKidsGreen,
                        onClick = onAiSummary
                    )
                    ActionButton(
                        icon = Icons.Default.DeleteOutline,
                        color = Color(0xFFEF4444),
                        onClick = onDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    progress: Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextBlue)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
        }
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
