package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.isComplet
import com.example.dam_front.models.remainingSlots
import com.example.dam_front.ui.components.TournoiImage
import com.example.dam_front.ui.components.FilterDialog
import com.example.dam_front.ui.components.FilterOptions
import com.example.dam_front.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CoachTournoisScreen(
    tournois: List<Tournoi>,
    isLoading: Boolean,
    participantsCount: Map<String, Int> = emptyMap(),
    onEditClick: (Tournoi) -> Unit,
    onDeleteClick: (Tournoi) -> Unit,
    onViewParticipantsClick: (Tournoi) -> Unit,
    onRefresh: () -> Unit,
    onCreateClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: CoachTournoisViewModel = viewModel(factory = CoachTournoisViewModelFactory(context))
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOptions by remember { mutableStateOf<FilterOptions>(FilterOptions()) }
    
    // Filtrer et trier les tournois
    val filteredAndSortedTournois = remember(tournois, searchQuery, filterOptions) {
        val filtered = tournois.filter { tournoi ->
            // Filtre par recherche
            val matchesSearch = searchQuery.isBlank() || 
                tournoi.nom?.contains(searchQuery, ignoreCase = true) == true
            
            // Filtre par état
            val matchesEtat = filterOptions.etat == null || 
                tournoi.etat?.lowercase() == filterOptions.etat?.lowercase()
            
            // Filtre par niveau
            val matchesNiveau = filterOptions.niveau == null || 
                tournoi.niveau?.lowercase() == filterOptions.niveau?.lowercase()
            
            matchesSearch && matchesEtat && matchesNiveau
        }
        
        // Trier par date (plus proche au plus loin)
        filtered.sortedBy { tournoi ->
            tournoi.dateDebut?.let { dateStr ->
                try {
                    val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    format.parse(dateStr)?.time ?: Long.MAX_VALUE
                } catch (e: Exception) {
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        format.parse(dateStr)?.time ?: Long.MAX_VALUE
                    } catch (e2: Exception) {
                        Long.MAX_VALUE
                    }
                }
            } ?: Long.MAX_VALUE
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Search Bar avec bouton +
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
                    placeholder = { Text("Rechercher un tournoi...", fontSize = 14.sp) },
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
                
                // Icône de filtre (au milieu)
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
                
                // Bouton + à côté de la recherche
                FloatingActionButton(
                    onClick = onCreateClick,
                    modifier = Modifier.size(48.dp),
                    containerColor = IconOrange,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = "Créer un tournoi",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Dialog de filtre
        if (showFilterDialog) {
            FilterDialog(
                onDismiss = { showFilterDialog = false },
                onFilterApply = { options ->
                    filterOptions = options
                },
                currentFilters = filterOptions
            )
        }
        
        // Tournament List
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading && tournois.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = IconOrange)
                            Text("Chargement des tournois...", color = TextBlueLight, fontSize = 16.sp)
                        }
                    }
                }
                filteredAndSortedTournois.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("🏆", fontSize = 64.sp)
                            Text("Aucun tournoi trouvé", color = TextDarkGray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Créez votre premier tournoi", color = TextBlueLight, fontSize = 14.sp)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isLoading && tournois.isNotEmpty()) {
                            item {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = IconOrange)
                            }
                        }
                        items(filteredAndSortedTournois) { tournoi ->
                            val count = tournoi.id?.let { participantsCount[it] }
                            CoachTournoiCard(
                                tournoi = tournoi,
                                participantsCount = count,
                                onEditClick = { 
                                    android.util.Log.d("CoachTournoisScreen", "✏️ Clic sur Modifier - Tournoi: ${tournoi.id}, ${tournoi.nom}")
                                    onEditClick(tournoi) 
                                },
                                onDeleteClick = { onDeleteClick(tournoi) },
                                onViewParticipantsClick = { onViewParticipantsClick(tournoi) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachTournoiCard(
    tournoi: Tournoi,
    participantsCount: Int? = null,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewParticipantsClick: () -> Unit
) {
    val maxParticipants = tournoi.nombreParticipantsMax
    // Corriger le compteur pour qu'il ne dépasse pas le maximum
    val correctedCount = maxParticipants?.let { max ->
        participantsCount?.let { if (it > max) max else it }
    } ?: participantsCount
    val isFull = tournoi.isComplet(correctedCount)
    val remainingSlots = tournoi.remainingSlots(correctedCount)
    Card(
        modifier = Modifier
            .fillMaxWidth()
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
                TournoiImage(
                    imagePath = tournoi.image?.takeIf { it.isNotEmpty() } ?: tournoi.imageUrl?.takeIf { it.isNotEmpty() },
                    contentDescription = tournoi.nom,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    showSportName = true,
                    sportName = tournoi.sport
                )
                if (isFull) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(IconGreen, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "COMPLET",
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
                // Nom du tournoi et date sur la même ligne
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nom du tournoi
                    Text(
                        text = tournoi.nom ?: "Tournoi sans nom",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextBlue,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Date à côté du nom
                    if (tournoi.dateDebut != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(IconOrangeLight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = formatDateShortCoach(tournoi.dateDebut),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = IconOrangeDark
                            )
                        }
                    }
                }
                
                // Ne plus afficher le bloc places disponibles
                
                // Action Icons et bouton participants sur la même ligne
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bouton "Voir les participants" à gauche
                    OutlinedButton(
                        onClick = onViewParticipantsClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = HeaderBlue,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = HeaderBlue
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.People, 
                            contentDescription = null, 
                            modifier = Modifier.size(14.dp),
                            tint = HeaderBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Voir les participants", fontSize = 10.sp, color = HeaderBlue)
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Icône Edit (vert)
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IconGreenLight)
                    ) {
                        Icon(
                            Icons.Default.Edit, 
                            contentDescription = "Modifier",
                            modifier = Modifier.size(20.dp),
                            tint = IconGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Icône Delete (orange)
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IconOrangeLight)
                    ) {
                        Icon(
                            Icons.Default.Delete, 
                            contentDescription = "Supprimer",
                            modifier = Modifier.size(20.dp),
                            tint = IconOrange
                        )
                    }
                }
            }
        }
    }
}

private fun formatDateShortCoach(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e2: Exception) {
            dateString
        }
    }
}

