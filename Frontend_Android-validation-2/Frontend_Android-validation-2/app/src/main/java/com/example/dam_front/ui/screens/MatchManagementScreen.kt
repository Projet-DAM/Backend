package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Match
import com.example.dam_front.repository.MatchRepository
import com.example.dam_front.ui.components.MatchEditDialog
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Helper function to format match date and time
private fun formatMatchDate(dateStr: String?, heureStr: String?): String {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateStr?.let { dateFormat.parse(it) }
        
        val formattedDate = date?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.FRENCH).format(it)
        } ?: dateStr ?: ""
        
        val heure = heureStr ?: ""
        
        if (heure.isNotEmpty()) {
            "$formattedDate à $heure"
        } else {
            formattedDate
        }
    } catch (e: Exception) {
        "$dateStr $heureStr".trim()
    }
}

// Fonction helper pour convertir phase en round
private fun getRoundFromPhase(phase: String?): Int {
    return when (phase?.lowercase()) {
        "finale" -> 1
        "demi_final", "demi-final" -> 2
        "quart_final", "quart-final" -> 3
        "huitieme_final", "huitieme-final" -> 4
        "seizieme_final", "seizieme-final" -> 5
        else -> phase?.toIntOrNull() ?: 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchManagementScreen(
    tournoiId: String,
    tournoiNom: String? = null,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val matchRepository = remember { MatchRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showEditDialog by remember { mutableStateOf<Match?>(null) }
    
    fun loadMatches() {
        isLoading = true
        error = null
        coroutineScope.launch {
            val result = matchRepository.getTournamentMatches(tournoiId, showFuture = true)
            
            if (result.isSuccess) {
                matches = result.getOrNull() ?: emptyList()
                android.util.Log.d("MatchManagementScreen", "✅ ${matches.size} match(s) chargé(s)")
            } else {
                error = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                android.util.Log.e("MatchManagementScreen", "❌ Erreur: $error")
            }
            
            isLoading = false
        }
    }
    
    // Charger les matchs au démarrage
    LaunchedEffect(tournoiId) {
        loadMatches()
    }
    
    // Grouper les matchs par round (convertir phase en round)
    // Dans un tournoi éliminatoire :
    // - Finale (round 1) : 1 match seulement (2 gagnants des demi-finales)
    // - Demi-finales (round 2) : 2 matchs seulement (4 gagnants des quarts)
    // - Quarts de finale (round 3) : 4 matchs (8 équipes)
    val matchesByRound = remember(matches) {
        val grouped = matches.groupBy { match: Match -> getRoundFromPhase(match.phase) }
            .toMutableMap()
        
        // Validation et correction pour les demi-finales (round 2) : seulement 2 matchs maximum
        grouped[2]?.let { demiFinales ->
            if (demiFinales.size > 2) {
                android.util.Log.w("MatchManagementScreen", "⚠️ ${demiFinales.size} matchs en demi-finale détectés, limitation à 2 matchs (système éliminatoire)")
                // Prendre seulement les 2 premiers matchs (triés par ordre si disponible)
                val sortedDemi = demiFinales.sortedBy { it.ordre ?: 0 }.take(2)
                grouped[2] = sortedDemi
            } else if (demiFinales.size < 2 && demiFinales.isNotEmpty()) {
                android.util.Log.d("MatchManagementScreen", "ℹ️ ${demiFinales.size} match(s) en demi-finale (attendu: 2)")
            }
        }
        
        // Validation pour la finale (round 1) : seulement 1 match maximum
        grouped[1]?.let { finales ->
            if (finales.size > 1) {
                android.util.Log.w("MatchManagementScreen", "⚠️ ${finales.size} matchs en finale détectés, limitation à 1 match (système éliminatoire)")
                // Prendre seulement le premier match (trié par ordre si disponible)
                val sortedFinale = finales.sortedBy { it.ordre ?: 0 }.take(1)
                grouped[1] = sortedFinale
            } else if (finales.size < 1 && finales.isNotEmpty()) {
                android.util.Log.d("MatchManagementScreen", "ℹ️ ${finales.size} match(s) en finale (attendu: 1)")
            }
        }
        
        grouped.toSortedMap(compareByDescending<Int> { it })
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Petite icône de retour
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Retour",
                    tint = IconOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Contenu
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .background(Color(0xFFF5F5F5))
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = IconOrange)
                            Text(
                                text = "Chargement des matchs...",
                                color = TextDarkGray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Red
                            )
                            Text(
                                text = "Erreur",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                            Text(
                                text = error ?: "Erreur inconnue",
                                color = TextDarkGray,
                                fontSize = 14.sp
                            )
                            Button(
                                onClick = { loadMatches() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IconOrange
                                )
                            ) {
                                Text("Réessayer", color = Color.White)
                            }
                        }
                    }
                }
                
                matches.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextBlueLight
                            )
                            Text(
                                text = "Aucun match disponible",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkGray
                            )
                            Text(
                                text = "Générez d'abord l'arbre de tournoi depuis l'écran des équipes.",
                                color = TextBlueLight,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        matchesByRound.forEach { (round: Int, roundMatches: List<Match>) ->
                            item {
                                Text(
                                    text = when (round) {
                                        1 -> "🏆 Finale"
                                        2 -> "🥈 Demi-finales"
                                        3 -> "🥉 Quarts de finale"
                                        4 -> "Huitièmes de finale"
                                        else -> "Round $round"
                                    },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HeaderBlue,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(roundMatches) { match ->
                                MatchManagementCard(
                                    match = match,
                                    onEditClick = { showEditDialog = match }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Dialog d'édition de match
        showEditDialog?.let { match ->
            MatchEditDialog(
                match = match,
                onDismiss = { showEditDialog = null },
                onSave = { score1, score2, statut ->
                    coroutineScope.launch {
                        match.id?.let { matchId ->
                            val result = matchRepository.updateMatch(matchId, score1, score2, statut)
                            if (result.isSuccess) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Match mis à jour avec succès",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                showEditDialog = null
                                loadMatches() // Recharger les matchs
                            } else {
                                val errorMsg = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                                android.widget.Toast.makeText(
                                    context,
                                    errorMsg,
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun MatchManagementCard(
    match: Match,
    onEditClick: () -> Unit
) {
    val equipe1Nom = match.equipe1?.nom ?: "Équipe 1"
    val equipe2Nom = match.equipe2?.nom ?: "Équipe 2"
    val score1 = match.scoreEquipe1
    val score2 = match.scoreEquipe2
    val statut = match.statut ?: "en_attente"
    
    val statutColor = when (statut.lowercase()) {
        "termine" -> IconGreen
        "en_cours" -> IconOrange
        "a_venir" -> TextBlueLight
        else -> TextBlueLight
    }
    
    val statutText = when (statut.lowercase()) {
        "termine" -> "Terminé"
        "en_cours" -> "En cours"
        "a_venir" -> "À venir"
        else -> "En attente"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header avec statut et bouton modifier
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statutColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statutText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statutColor
                    )
                }
                
                // Bouton Modifier
                TextButton(
                    onClick = onEditClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = IconOrange
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Modifier",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Modifier",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Équipes et scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Équipe 1
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    match.equipe1?.couleur?.let { couleur ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(android.graphics.Color.parseColor(couleur)))
                        )
                    }
                    Column {
                        Text(
                            text = equipe1Nom,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                        if (score1 != null) {
                            Text(
                                text = "Score: $score1",
                                fontSize = 14.sp,
                                color = TextBlueLight
                            )
                        }
                    }
                }
                
                // VS ou Scores
                if (score1 != null && score2 != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$score1",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                        Text(
                            text = "-",
                            fontSize = 18.sp,
                            color = TextBlueLight
                        )
                        Text(
                            text = "$score2",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                    }
                } else {
                    Text(
                        text = "VS",
                        fontSize = 16.sp,
                        color = TextBlueLight,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Équipe 2
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = equipe2Nom,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        if (score2 != null) {
                            Text(
                                text = "Score: $score2",
                                fontSize = 14.sp,
                                color = TextBlueLight,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                    match.equipe2?.couleur?.let { couleur ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(android.graphics.Color.parseColor(couleur)))
                        )
                    }
                }
            }
            
            // Date et heure
            if (match.dateMatch != null || match.heureMatch != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = TextBlueLight
                    )
                    Text(
                        text = formatMatchDate(match.dateMatch, match.heureMatch),
                        fontSize = 12.sp,
                        color = TextBlueLight
                    )
                }
            }
        }
    }
}

