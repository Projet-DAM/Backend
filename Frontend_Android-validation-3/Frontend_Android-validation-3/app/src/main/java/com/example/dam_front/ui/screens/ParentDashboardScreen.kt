package com.example.dam_front.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.Program
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.ProgramsViewModel
import com.example.dam_front.viewmodels.ProgramsViewModelFactory

@Composable
fun ParentDashboardScreen(
    viewModel: ProgramsViewModel = viewModel(factory = ProgramsViewModelFactory(LocalContext.current)),
    onMenuClick: () -> Unit = {},
    onProgramClick: (Program) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(SportyBackgroundTop, SportyBackgroundBottom))
    }
    
    // Search and sort state
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf("name_asc") }
    
    // Filter for ACTIVE programs only
    val activePrograms = remember(uiState.programs) {
        uiState.programs.filter { it.statut.equals("ACTIF", ignoreCase = true) }
    }
    
    // Apply search filter
    val filteredPrograms = remember(activePrograms, searchQuery, uiState.coachNames) {
        if (searchQuery.isBlank()) {
            activePrograms
        } else {
            activePrograms.filter { program ->
                val query = searchQuery.lowercase()
                program.nomProgramme.lowercase().contains(query) ||
                program.description?.lowercase()?.contains(query) == true ||
                program.niveau?.lowercase()?.contains(query) == true ||
                uiState.coachNames[program.coach]?.lowercase()?.contains(query) == true
            }
        }
    }
    
    // Apply sorting
    val sortedPrograms = remember(filteredPrograms, sortOption) {
        when (sortOption) {
            "name_asc" -> filteredPrograms.sortedBy { it.nomProgramme.lowercase() }
            "name_desc" -> filteredPrograms.sortedByDescending { it.nomProgramme.lowercase() }
            "price_asc" -> filteredPrograms.sortedBy { it.prix ?: 0.0 }
            "price_desc" -> filteredPrograms.sortedByDescending { it.prix ?: 0.0 }
            "activities_asc" -> filteredPrograms.sortedBy { it.activites.size }
            "activities_desc" -> filteredPrograms.sortedByDescending { it.activites.size }
            "level" -> filteredPrograms.sortedBy { 
                when (it.niveau?.lowercase()) {
                    "débutant" -> 0
                    "intermédiaire" -> 1
                    "avancé" -> 2
                    else -> 3
                }
            }
            else -> filteredPrograms
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ParentDashboardHeader(
                onMenuClick = onMenuClick,
                activeProgramsCount = activePrograms.size
            )

            // Search and Sort Section
            var expanded by remember { mutableStateOf(false) }
            val sortOptions = mapOf(
                "name_asc" to "Nom (A-Z)",
                "name_desc" to "Nom (Z-A)",
                "price_asc" to "Prix croissant",
                "price_desc" to "Prix décroissant",
                "activities_asc" to "Moins d'activités",
                "activities_desc" to "Plus d'activités",
                "level" to "Par niveau"
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Rechercher un programme...") },
                    leadingIcon = {
                        SearchIcon(tint = SportyMutedText, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                CloseIcon(tint = SportyMutedText, modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = Color.White
                    ),
                    singleLine = true
                )

                // Sort Options
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = TextDarkGray
                        ),
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SortIcon(tint = SportyOrange, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Trier: ${sortOptions[sortOption]}",
                                    fontSize = 14.sp
                                )
                            }
                            ArrowDropDownIcon(tint = SportyMutedText, modifier = Modifier.size(20.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        sortOptions.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    sortOption = key
                                    expanded = false
                                },
                                leadingIcon = {
                                    if (sortOption == key) {
                                        CheckIcon(tint = SportyOrange, modifier = Modifier.size(20.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = SportyDarkBlue)
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = uiState.error ?: "Une erreur est survenue",
                                color = TextDarkGray,
                                fontSize = 16.sp
                            )
                            SportyGradientButton(
                                text = "Réessayer",
                                onClick = { viewModel.refreshData() },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                    }
                }

                else -> {
                    if (sortedPrograms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        "Aucun programme ne correspond à votre recherche"
                                    } else {
                                        "Aucun programme disponible pour le moment"
                                    },
                                    color = SportyMutedText,
                                    fontSize = 16.sp
                                )
                                if (searchQuery.isNotEmpty()) {
                                    SportyGradientButton(
                                        text = "Effacer la recherche",
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    )
                                } else {
                                    SportyGradientButton(
                                        text = "Actualiser",
                                        onClick = { viewModel.refreshData() },
                                        modifier = Modifier.fillMaxWidth(0.6f)
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(sortedPrograms) { program ->
                                ParentProgramItem(
                                    program = program,
                                    coachName = uiState.coachNames[program.coach],
                                    onClick = { onProgramClick(program) }
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
fun ParentDashboardHeader(
    onMenuClick: () -> Unit,
    activeProgramsCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardWhite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Espace Parents",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SportyDarkBlue
                )
                Text(
                    text = "$activeProgramsCount programmes actifs",
                    fontSize = 14.sp,
                    color = SportyMutedText
                )
            }
            
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Transparent)
            ) {
                MenuIcon(tint = SportyDarkBlue)
            }
        }
    }
}

@Composable
fun ParentProgramItem(
    program: Program,
    coachName: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (!program.image.isNullOrBlank()) {
                    val imageUrl = if (program.image.startsWith("http")) {
                        program.image
                    } else {
                        val cleanPath = if (program.image.startsWith("/")) program.image.substring(1) else program.image
                        "${ApiConfig.BASE_URL}/$cleanPath"
                    }
                    
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Image du programme",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(SportyDarkBlue, SportyTeal)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        TargetIcon(tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    }
                }

                // Level Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SportyDarkBlue)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = (program.niveau ?: "NIVEAU ?").uppercase(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                
                // Price Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = String.format("%.2f TND", program.prix ?: 0.0),
                        color = SportyDarkBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Content Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = program.nomProgramme,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkGray
                )

                if (!program.description.isNullOrBlank()) {
                    Text(
                        text = program.description,
                        fontSize = 14.sp,
                        color = SportyMutedText,
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Coach info
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PeopleIcon(tint = SportyOrange, modifier = Modifier.size(18.dp))
                        val displayCoach = program.coachName 
                            ?: program.coachNameUnderscore 
                            ?: coachName 
                            ?: "Coach"
                        Text(
                            text = displayCoach,
                            fontSize = 13.sp,
                            color = TextLightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Activities count
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${program.activites.size} activités",
                            fontSize = 13.sp,
                            color = TextLightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Details Button
                    OutlinedButton(
                        onClick = onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SportyDarkBlue),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SportyDarkBlue
                        )
                    ) {
                        Text(
                            text = "Voir détails",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Subscribe Button
                    val nombreInscrits = program.nombreInscrits ?: 0
                    val capaciteMaximale = program.capaciteMaximale
                    val isFull = capaciteMaximale != null && (nombreInscrits >= capaciteMaximale)

                    Button(
                        onClick = onClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues(),
                        enabled = true
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (isFull)
                                        Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                                    else
                                        Brush.horizontalGradient(listOf(SportyOrange, SportyLime))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFull) "Complet" else "S'inscrire",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

            }
        }
    }
}
