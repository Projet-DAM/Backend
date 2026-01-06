package com.example.dam_front.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
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
import com.example.dam_front.models.Program
import com.example.dam_front.ui.components.FilterDialog
import com.example.dam_front.ui.components.FilterOptions
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.ProgramsViewModel
import com.example.dam_front.viewmodels.ProgramsViewModelFactory

@Composable
fun ProgramsListScreen(
    viewModel: ProgramsViewModel = viewModel(factory = ProgramsViewModelFactory(LocalContext.current)),
    onProgramClick: (Program) -> Unit = {},
    onAddClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onEditClick: (Program) -> Unit = {},
    onDeleteClick: (Program) -> Unit = {},
    isCoach: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Filter active programs (remove temp uploads)
    val activePrograms = remember(uiState.programs) {
        uiState.programs.filter { !it.nomProgramme.startsWith("temp_upload_") }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOptions by remember { mutableStateOf(FilterOptions()) }

    // Filter Logic
    val filteredPrograms = remember(activePrograms, searchQuery, filterOptions, uiState.coachNames) {
        activePrograms.filter { program ->
            // Search
            val query = searchQuery.lowercase()
            val matchesSearch = searchQuery.isBlank() || 
                program.nomProgramme.lowercase().contains(query) ||
                uiState.coachNames[program.coach]?.lowercase()?.contains(query) == true

            // Filter by State (Statut)
            val matchesEtat = filterOptions.etat == null || 
                program.statut.lowercase() == filterOptions.etat?.lowercase()
                
            // Filter by Level (Niveau)
            val matchesNiveau = filterOptions.niveau == null || 
                program.niveau?.lowercase() == filterOptions.niveau?.lowercase()

            matchesSearch && matchesEtat && matchesNiveau
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
                    placeholder = { Text("Rechercher un programme...", fontSize = 14.sp) },
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
                            color = if (filterOptions.etat != null || filterOptions.niveau != null) 
                                SportyKidsGreen.copy(alpha = 0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filtrer",
                        tint = if (filterOptions.etat != null || filterOptions.niveau != null) 
                            SportyKidsGreen else SportyKidsBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Add Button (Only for Coach)
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
                            contentDescription = "Créer un programme",
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
                            Text("Chargement des programmes...", color = TextBlueLight, fontSize = 16.sp)
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
                        Button(onClick = { viewModel.refreshData() }) {
                            Text("Réessayer")
                        }
                    }
                }
                filteredPrograms.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("📚", fontSize = 64.sp)
                            Text("Aucun programme trouvé", color = TextDarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Créez votre premier programme", color = TextBlueLight, fontSize = 14.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredPrograms) { program ->
                             val displayCoachName = program.coachName
                                    ?: program.coachNameUnderscore
                                    ?: uiState.coachNames[program.coach]
                                    ?: "Coach"

                            CoachProgramCard(
                                program = program,
                                coachName = displayCoachName,
                                onClick = { onProgramClick(program) },
                                onEditClick = { onEditClick(program) },
                                onDeleteClick = { onDeleteClick(program) },
                                isCoach = isCoach
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachProgramCard(
    program: Program,
    coachName: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    isCoach: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = if(program.statut == "ACTIF") IconGreen.copy(alpha=0.4f) else IconOrange.copy(alpha=0.4f))
            .border(2.dp, if(program.statut == "ACTIF") IconGreen.copy(alpha=0.3f) else IconOrange.copy(alpha=0.3f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
             Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // Image
                if (!program.image.isNullOrBlank()) {
                    val imageUrl = if (program.image.startsWith("http")) {
                        program.image
                    } else {
                         val cleanPath = if (program.image.startsWith("/")) program.image.substring(1) else program.image
                        "${ApiConfig.BASE_URL}/$cleanPath"
                    }
                    
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = program.nomProgramme,
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
                            Icons.Default.School, // Generic icon for program
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Status Badge (Top Right)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(
                           if (program.statut == "ACTIF") IconGreen else IconOrange,
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = program.statut.uppercase(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Name and Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = program.nomProgramme,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${program.prix ?: 0} TND",
                         fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = IconOrangeDark
                    )
                }

                // Level & Coach Info Line
                 Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         Icon(Icons.Default.Star, contentDescription = null, tint = TextLightGray, modifier = Modifier.size(16.dp))
                         Text(text = program.niveau ?: "Niveau ?", fontSize = 13.sp, color = TextLightGray)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                         Icon(Icons.Default.Person, contentDescription = null, tint = TextLightGray, modifier = Modifier.size(16.dp))
                         Text(text = coachName, fontSize = 13.sp, color = TextLightGray)
                    }
                }
                
                // Action Buttons
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
                        Text("Voir le programme", fontSize = 12.sp, color = HeaderBlue)
                    }

                    // Admin Actions (Only for Coach)
                    if (isCoach) {
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
