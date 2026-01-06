package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import java.util.*

@Composable
fun TournoisListScreen(
    onTournoiClick: (Tournoi) -> Unit = {},
    selectedChildId: String? = null
) {
    android.util.Log.d("TournoisListScreen", "TournoisListScreen composable appelé")
    
    val viewModel: TournoisViewModel = viewModel(factory = TournoisViewModelFactory())
    android.util.Log.d("TournoisListScreen", "ViewModel créé")
    
    val tournois by viewModel.tournois.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val error by viewModel.error.collectAsState(initial = null)
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val childRepository = remember { com.example.dam_front.repository.ChildRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    var selectedChild by remember { mutableStateOf<com.example.dam_front.models.ChildResponse?>(null) }
    
    // Charger la liste des enfants et trouver l'enfant sélectionné
    LaunchedEffect(selectedChildId) {
        if (selectedChildId != null) {
            coroutineScope.launch {
                childRepository.getChildren().onSuccess { childrenList ->
                    selectedChild = childrenList.find { it.id == selectedChildId }
                    if (selectedChild != null) {
                        android.util.Log.d("TournoisListScreen", "✅ Enfant chargé: ${selectedChild!!.prenom} ${selectedChild!!.nom} - Sport: ${selectedChild!!.sportPratique}")
                    } else {
                        android.util.Log.w("TournoisListScreen", "⚠️ Enfant non trouvé dans la liste: $selectedChildId")
                    }
                }.onFailure { exception ->
                    android.util.Log.e("TournoisListScreen", "❌ Erreur chargement enfants: ${exception.message}")
                    selectedChild = null
                }
            }
        } else {
            selectedChild = null
        }
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterOptions by remember { mutableStateOf<FilterOptions>(FilterOptions()) }
    
    // Filtrer et trier les tournois
    val filteredAndSortedTournois = remember(tournois, searchQuery, filterOptions, selectedChild) {
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
            
            // Filtre par sport - Pour les parents, afficher TOUS les tournois par défaut
            val matchesSport = when {
                // Si aucun enfant n'est sélectionné, afficher tous les tournois
                selectedChild == null -> true
                // Si showAllSports est explicitement true, afficher tous les tournois
                filterOptions.showAllSports == true -> true
                // Si l'enfant n'a pas de sport défini, afficher tous les tournois (cas parents)
                selectedChild?.sportPratique.isNullOrBlank() -> true
                // Si showAllSports est false ET l'enfant a un sport, filtrer par sport
                filterOptions.showAllSports == false -> tournoi.sport?.lowercase() == selectedChild?.sportPratique?.lowercase()
                // Par défaut, si l'enfant a un sport défini, filtrer par sport (cas coaches)
                else -> tournoi.sport?.lowercase() == selectedChild?.sportPratique?.lowercase()
            }
            
            matchesSearch && matchesEtat && matchesNiveau && matchesSport
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
    
    android.util.Log.d("TournoisListScreen", "État: isLoading=$isLoading, tournois=${tournois.size}, error=$error")
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Search Bar avec filtre
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = TextDarkGray
                        )
                    )
                    
                    // Icône de filtre
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
        
        // Contenu principal
        Box(modifier = Modifier.weight(1f)) {
            when {
                isLoading && tournois.isEmpty() && error == null -> {
                    // Écran de chargement
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = IconOrange,
                                modifier = Modifier.size(48.dp),
                                strokeWidth = 4.dp
                            )
                            Text(
                                text = "Chargement des tournois...",
                                color = TextBlueLight,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                error != null -> {
                    // Écran d'erreur
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 64.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        Text(
                            text = "Erreur",
                            color = Color.Red,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = error ?: "Erreur inconnue",
                            color = TextDarkGray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 24.dp),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Button(
                            onClick = { viewModel.loadTournois() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IconOrange
                            ),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Text("Réessayer", color = Color.White)
                        }
                    }
                }
                
                filteredAndSortedTournois.isEmpty() -> {
                    // Écran vide
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "🏆",
                                fontSize = 64.sp
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Aucun tournoi trouvé" else "Aucun tournoi disponible",
                                color = TextDarkGray,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Essayez une autre recherche" else "Les nouveaux tournois apparaîtront ici",
                                color = TextBlueLight,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                else -> {
                    // Liste des tournois
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Indicateur de rafraîchissement si on recharge avec des données
                        if (isLoading && filteredAndSortedTournois.isNotEmpty()) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = IconOrange
                                )
                            }
                        }
                        
                        items(filteredAndSortedTournois) { tournoi ->
                            TournoiCard(
                                tournoi = tournoi,
                                onClick = { onTournoiClick(tournoi) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournoiCard(
    tournoi: Tournoi,
    onClick: () -> Unit
) {
    val isFull = tournoi.isComplet()
    val remainingSlots = tournoi.remainingSlots()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                TournoiImage(
                    imagePath = tournoi.image?.takeIf { it.isNotEmpty() } 
                        ?: tournoi.imageUrl?.takeIf { it.isNotEmpty() },
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
                            .padding(10.dp)
                            .background(IconGreen, RoundedCornerShape(40))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COMPLET",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Contenu de la carte (nom et date côte à côte)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
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
                                .background(
                                    color = IconOrangeLight,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = formatDateShort(tournoi.dateDebut),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = IconOrangeDark
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun AvailabilityChip(
    isFull: Boolean,
    remainingSlots: Int?,
    maxSlots: Int
) {
    val badgeColor = if (isFull) IconGreen.copy(alpha = 0.15f) else IconOrangeLight
    val textColor = if (isFull) IconGreen else IconOrangeDark
    val label = if (isFull) {
        "Complet"
    } else {
        val remaining = remainingSlots ?: maxSlots
        "$remaining place(s) dispo"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(badgeColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Capacité: $maxSlots",
            fontSize = 11.sp,
            color = TextBlueLight
        )
    }
}

fun formatDateShort(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        try {
            // Essayer un autre format
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
            val date = inputFormat.parse(dateString)
            date?.let { outputFormat.format(it) } ?: dateString
        } catch (e2: Exception) {
            dateString
        }
    }
}
