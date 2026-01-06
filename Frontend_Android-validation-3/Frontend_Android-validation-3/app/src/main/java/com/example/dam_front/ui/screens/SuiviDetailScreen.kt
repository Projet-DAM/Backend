package com.example.dam_front.ui.screens

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuiviDetailScreen(
    navController: NavController,
    childId: String
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    
    // Use SharedViewModel to get data
    val activity = context as androidx.activity.ComponentActivity
    val sharedVm: SuiviSharedViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = SuiviSharedViewModelFactory(app)
    )
    
    // Refresh data for this child
    LaunchedEffect(childId) { 
        sharedVm.refreshForChild(childId) 
    }
    
    val suivisMap by sharedVm.suivisByChild.collectAsState()
    val childSuivis = suivisMap[childId] ?: emptyList()
    
    // We need child info. Since we don't have a dedicated ChildViewModel here,
    // we can infer it from the first suivi OR we might need to fetch it.
    // Ideally, we'd have a viewmodel or repository call.
    // For now, let's try to get it from the cached data or just display "Enfant"
    val childInfo = childSuivis.firstOrNull()?.enfant
    val childName = childInfo?.let { "${it.prenom} ${it.nom}" } ?: "Détails de l'enfant"
    val childPhoto = childInfo?.let { 
         // Assuming childInfo has photoUrl, but SuiviEnfant.enfant is EnfantRef which might not have photo.
         // Let's use a placeholder if null.
         null // EnfantRef usually doesn't have photo
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(childName, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBlue
                )
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (childSuivis.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun suivi trouvé pour cet enfant", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Header Stats
                item {
                    val presenceCount = childSuivis.count { it.presence }
                    val avgPerf = childSuivis.map { it.performance }.average().takeIf { !it.isNaN() }?.toInt() ?: 0
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            label = "Taux de présence",
                            value = "${(presenceCount * 100 / childSuivis.size)}%",
                            color = SportyKidsGreen,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Performance Moy.",
                            value = "$avgPerf/10",
                            color = AccentOrange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // History List
                items(childSuivis.size) { index ->
                    val suivi = childSuivis[index]
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = suivi.dateSuivi?.let { sdf.format(it) } ?: "Date inconnue",
                                    fontWeight = FontWeight.Bold,
                                    color = HeaderBlue,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (suivi.presence) "Présent" else "Absent",
                                    color = if (suivi.presence) SportyKidsGreen else SportyKidsRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Badge(text = "Perf: ${suivi.performance}/10", color = AccentOrange)
                                suivi.effortLevel?.let {
                                    Badge(text = "Effort: $it", color = SportyKidsBlue)
                                }
                            }
                            
                            if (!suivi.commentaire.isNullOrBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = suivi.commentaire,
                                    color = TextDarkGray,
                                    fontSize = 14.sp
                                )
                            }
                            
                            // Focus Areas
                            if (!suivi.focusAreas.isNullOrEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Focus: " + suivi.focusAreas.joinToString(", "), fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}
