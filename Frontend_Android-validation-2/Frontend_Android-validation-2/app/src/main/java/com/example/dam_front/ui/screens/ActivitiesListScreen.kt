package com.example.dam_front.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.Activity
import com.example.dam_front.ui.components.FilterDialog
import com.example.dam_front.ui.components.FilterOptions
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.DateUtils
import com.example.dam_front.viewmodels.ActivitiesViewModel
import com.example.dam_front.viewmodels.ActivitiesViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ActivitiesListScreen(
    viewModel: ActivitiesViewModel = viewModel(factory = ActivitiesViewModelFactory(LocalContext.current)),
    isCoach: Boolean = true,
    onActivityClick: (Activity) -> Unit = {},
    onAddClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onEditClick: (Activity) -> Unit = {},
    onDeleteClick: (Activity) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOptions by remember { mutableStateOf<FilterOptions>(FilterOptions()) }

    // Filter logic
    val filteredActivities = remember(uiState.activities, searchQuery, filterOptions) {
        uiState.activities.filter { activity ->
            // Search
            val matchesSearch = searchQuery.isBlank() || 
                activity.nomActivite.contains(searchQuery, ignoreCase = true)
            
            // Filter by State (Statut)
            val matchesEtat = filterOptions.etat == null || 
                activity.statut?.lowercase() == filterOptions.etat?.lowercase()
                
            matchesSearch && matchesEtat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Search Bar Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher une activité...", fontSize = 14.sp) },
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
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextDarkGray
                    )
                )
                
                // Filter Icon
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (filterOptions.etat != null) 
                                SportyKidsGreen.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filtrer",
                        tint = if (filterOptions.etat != null) 
                            SportyKidsGreen else SportyKidsBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Add Button (Only if Coach)
                if (isCoach) {
                    FloatingActionButton(
                        onClick = onAddClick,
                        modifier = Modifier.size(48.dp),
                        containerColor = IconOrange,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, 
                            contentDescription = "Créer une activité",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        
        // Filter Dialog
        if (showFilterDialog) {
            FilterDialog(
                onDismiss = { showFilterDialog = false },
                onFilterApply = { options ->
                    filterOptions = options
                },
                currentFilters = filterOptions
            )
        }
        
        // Content List
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = IconOrange)
                            Text("Chargement des activités...", color = TextBlueLight, fontSize = 16.sp)
                        }
                    }
                }
                uiState.error != null -> {
                     Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Erreur",
                            color = Color.Red,
                            fontSize = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refreshActivities() }) {
                            Text("Réessayer")
                        }
                    }
                }
                filteredActivities.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("🎯", fontSize = 64.sp)
                            Text("Aucune activité trouvée", color = TextDarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            if (isCoach) {
                                Text("Créez votre première activité", color = TextBlueLight, fontSize = 14.sp)
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredActivities) { activity ->
                            CoachActivityCard(
                                activity = activity,
                                isCoach = isCoach,
                                onClick = { onActivityClick(activity) },
                                onEditClick = { onEditClick(activity) },
                                onDeleteClick = { onDeleteClick(activity) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachActivityCard(
    activity: Activity,
    isCoach: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // Image
                if (!activity.image.isNullOrBlank()) {
                    val imageUrl = if (activity.image.startsWith("http")) {
                        activity.image
                    } else {
                        val cleanPath = if (activity.image.startsWith("/")) activity.image.substring(1) else activity.image
                        "${ApiConfig.BASE_URL}/$cleanPath"
                    }
                    
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = activity.nomActivite,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(SportyDarkBlue, SportyTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SportsSoccer,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Status Badge (Top Right)
                val statusToDisplay = activity.statut?.uppercase()
                if (statusToDisplay != null && statusToDisplay != "BROUILLON" && statusToDisplay != "INCONNU") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(
                                if (statusToDisplay == "ACTIVE" || statusToDisplay == "CONFIRME") IconGreen else Color.Gray, 
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = statusToDisplay,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name and Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = activity.nomActivite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(IconOrangeLight, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = DateUtils.formatDate(activity.date, longFormat = false),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = IconOrangeDark
                        )
                    }
                }

                // Price & Time Info Line
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         Icon(Icons.Default.AccessTime, contentDescription = null, tint = TextLightGray, modifier = Modifier.size(16.dp))
                         Text(text = activity.heure ?: "--:--", fontSize = 13.sp, color = TextLightGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         Icon(Icons.Default.EuroSymbol, contentDescription = null, tint = TextLightGray, modifier = Modifier.size(16.dp))
                         Text(text = "${activity.prix ?: 0} TND", fontSize = 13.sp, color = TextLightGray)
                    }
                }
                
                // Action Buttons
                if (isCoach) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View Details Button
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HeaderBlue,
                                containerColor = Color.Transparent
                            ),
                            border = BorderStroke(1.5.dp, HeaderBlue),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Voir les détails", fontSize = 12.sp, color = HeaderBlue)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Edit
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IconGreenLight)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(20.dp), tint = IconGreen)
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Delete
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IconOrangeLight)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", modifier = Modifier.size(20.dp), tint = IconOrange)
                        }
                    }
                }
            }
        }
    }
}
