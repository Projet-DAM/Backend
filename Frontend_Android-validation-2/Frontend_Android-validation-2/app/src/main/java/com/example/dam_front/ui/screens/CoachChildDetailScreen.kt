@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.dam_front.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.CoachChildDetailViewModel
import com.example.dam_front.viewmodels.CoachChildDetailViewModelFactory
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.dam_front.ui.components.*
import com.example.dam_front.viewmodels.AiSummaryViewModel
import com.example.dam_front.utils.PdfService
import com.example.dam_front.models.User

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CoachChildDetailScreen(
    navController: NavController,
    childId: String
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val viewModel: CoachChildDetailViewModel = viewModel(
        factory = CoachChildDetailViewModelFactory(app, childId)
    )
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Vérifier rôle coach
    val authRepo = AuthRepository(context)
    var isCoach by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isCoach = authRepo.getUserRole()?.equals("coach", ignoreCase = true) == true
    }

    // Shared VM pour cache suivis
    val activity = context as androidx.activity.ComponentActivity
    val sharedVm: SuiviSharedViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = SuiviSharedViewModelFactory(app)
    )
    LaunchedEffect(childId) { sharedVm.refreshForChild(childId) }

    // State for delete confirmation
    var showDeleteDialog by remember { mutableStateOf(false) }
    var suiviToDelete by remember { mutableStateOf<SuiviEnfant?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // AI Summary state
    val aiViewModel: AiSummaryViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )
    val isGeneratingAi by aiViewModel.isGenerating.collectAsState()
    val aiSummaryResult by aiViewModel.summaryResult.collectAsState()
    val aiError by aiViewModel.error.collectAsState()

    // Show snackbar when message changes
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.White, CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = AccentOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color(0xFFF8F9FA),
        content = { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                // Enhanced Child Info Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                        colors = listOf(HeaderBlue, HeaderBlueLight)
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                val photoUrl = uiState.childPhoto?.let { p ->
                                    if (p.startsWith("http")) p else ApiConfig.BASE_URL.trimEnd('/') + (if (p.startsWith("/")) p else "/$p")
                                } ?: "https://placehold.co/80"

                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(Color.White, CircleShape)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = uiState.childName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = uiState.category,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )

                                    // Add stats summary
                                    val suiviCount = (sharedVm.suivisByChild.collectAsState().value[childId] ?: uiState.suivis).size
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        StatChip("$suiviCount", "Suivis", SportyKidsGreen)
                                        val avgPerf = (sharedVm.suivisByChild.collectAsState().value[childId] ?: uiState.suivis)
                                            .map { it.performance }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
                                        StatChip("$avgPerf%", "Moy.", AccentOrange)
                                    }
                                }
                            }
                        }
                    }
                }

                // AI Summary Section for Coach
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Analyse de performance IA",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlue
                        )

                        if (aiSummaryResult != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    SummaryContent(aiSummaryResult!!)
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Export PDF Button
                                        Button(
                                            onClick = {
                                                val childUser = User(
                                                    id = childId,
                                                    prenom = uiState.childName.split(" ").firstOrNull() ?: "",
                                                    nom = uiState.childName.split(" ").lastOrNull() ?: "",
                                                    email = "",
                                                    role = "enfant"
                                                )
                                                val file = PdfService.generateSummaryPdf(context, uiState.childName, aiSummaryResult!!)
                                                if (file != null) {
                                                    PdfService.sharePdf(context, file)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = ExportGreen),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("PDF", fontSize = 12.sp)
                                        }

                                        // Email Button (Coach specific)
                                        Button(
                                            onClick = {
                                                val file = PdfService.generateSummaryPdf(context, uiState.childName, aiSummaryResult!!)
                                                if (file != null) {
                                                    // Note: We don't have the parent email easily here, 
                                                    // but we can let the coach choose the recipient in their email app
                                                    PdfService.sendEmailWithPdf(context, file, uiState.childName)
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Email", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Generate Button
                            Button(
                                onClick = {
                                    val childUser = User(
                                        id = childId,
                                        prenom = uiState.childName.split(" ").firstOrNull() ?: "",
                                        nom = uiState.childName.split(" ").lastOrNull() ?: "",
                                        email = "",
                                        role = "enfant"
                                    )
                                    aiViewModel.generateSummary(childUser)
                                },
                                enabled = !isGeneratingAi,
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HeaderBlue)
                            ) {
                                if (isGeneratingAi) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Générer l'analyse IA")
                                }
                            }
                        }
                        
                        if (aiError != null) {
                            Text(aiError!!, color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }

                // Enhanced Suivis Section
                item {
                    val suivisMap by sharedVm.suivisByChild.collectAsState()
                    val shownSuivis = suivisMap[childId] ?: uiState.suivis

                    if (shownSuivis.isNotEmpty()) {
                        Text(
                            text = "Historique des suivis",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlue,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    shownSuivis.sortedByDescending { it.dateSuivi }.forEach { suivi ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(6.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Date Header with colored background
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color(0xFFF0F8FF),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "📅 ${suivi.dateSuivi}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HeaderBlue
                                    )
                                }

                                // Stats Row with colorful indicators
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatusCard(
                                        title = "Présence",
                                        value = if (suivi.presence) "✓ Présent" else "✗ Absent",
                                        color = if (suivi.presence) SportyKidsGreen else SportyKidsRed,
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatusCard(
                                        title = "Performance",
                                        value = "${suivi.performance}%",
                                        color = when {
                                            suivi.performance >= 80 -> SportyKidsGreen
                                            suivi.performance >= 60 -> AccentOrange
                                            else -> SportyKidsRed
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Additional info with icons
                                suivi.activityType?.let {
                                    InfoRow("🏃", "Activité", it, HeaderBlue)
                                }
                                suivi.effortLevel?.let {
                                    InfoRow("💪", "Effort", "$it/10", AccentOrange)
                                }
                                suivi.emotionalState?.let {
                                    InfoRow("😊", "État émotionnel", it, SportyKidsGreen)
                                }

                                // Comment section
                                suivi.commentaire?.takeIf { it.isNotBlank() }?.let {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Text(
                                                text = "💬 Commentaire",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFE65100)
                                            )
                                            Spacer(Modifier.height(8.dp))
                                            Text(
                                                text = it,
                                                fontSize = 14.sp,
                                                color = Color(0xFF5D4037),
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }

                                // Focus Areas Section
                                suivi.focusAreas?.takeIf { it.isNotEmpty() }?.let { list: List<String> ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "🎯",
                                                    fontSize = 18.sp
                                                )
                                                Text(
                                                    text = "Zones de focus",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SportyKidsGreen
                                                )
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                list.forEach { area ->
                                                    CoachTagCard(area, SportyKidsGreen)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Next Session Goals Section
                                suivi.nextSessionGoals?.takeIf { it.isNotEmpty() }?.let { list: List<String> ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "🚀",
                                                    fontSize = 18.sp
                                                )
                                                Text(
                                                    text = "Objectifs prochaine séance",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentOrange
                                                )
                                            }
                                            Spacer(Modifier.height(12.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                list.forEach { goal ->
                                                    CoachTagCard(goal, AccentOrange)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Enhanced Action Buttons
                                if (isCoach) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 16.dp),
                                        color = Color.Gray.copy(alpha = 0.3f)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Edit Button
                                        Button(
                                            onClick = { navController.navigate("suivi_edit/${childId}/${suivi.id}") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SportyKidsGreen
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = ButtonDefaults.buttonElevation(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "✏️",
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Modifier",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Delete Button
                                        OutlinedButton(
                                            onClick = {
                                                suiviToDelete = suivi
                                                showDeleteDialog = true
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = SportyKidsRed
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                SportyKidsRed
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "🗑️",
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "Supprimer",
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // Delete confirmation dialog
    if (showDeleteDialog && suiviToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                suiviToDelete = null
            },
            title = {
                Text(
                    text = "Confirmer la suppression",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Êtes-vous sûr de vouloir supprimer ce suivi ? Cette action est irréversible.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        suiviToDelete?.let { suivi ->
                            viewModel.deleteSuivi(suivi.id) { result ->
                                result.onSuccess {
                                    snackbarMessage = "Suivi supprimé avec succès"
                                }.onFailure { error ->
                                    snackbarMessage = "Erreur lors de la suppression: ${error.message}"
                                }
                            }
                        }
                        showDeleteDialog = false
                        suiviToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = SportyKidsRed
                    )
                ) {
                    Text("Supprimer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        suiviToDelete = null
                    }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
private fun CoachTagCard(tag: String, color: Color) {
    Box(
        modifier = Modifier
            .background(
                brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                    colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.1f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = tag,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun StatChip(value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: String,
    label: String,
    value: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// SimpleFlowRow removed; replaced with Column for tag display
