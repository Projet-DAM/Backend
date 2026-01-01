package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Match
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.repository.MatchRepository
import com.example.dam_front.repository.ChildRepository
import com.example.dam_front.ui.components.TournamentBracket
import com.example.dam_front.ui.components.HeaderSection
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.launch
import android.content.SharedPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BracketParentScreen(
    tournoiId: String,
    tournoiNom: String? = null,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val matchRepository = remember { MatchRepository(context) }
    val childRepository = remember { ChildRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("SportyKidsPrefs", android.content.Context.MODE_PRIVATE)

    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Gestion des enfants
    var children by remember { mutableStateOf<List<ChildResponse>>(emptyList()) }
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    
    // Charger les enfants
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            childRepository.getChildren().onSuccess { childrenList ->
                children = childrenList
                val savedChildId = prefs.getString("selected_child_id", null)
                if (savedChildId != null && childrenList.any { it.id == savedChildId }) {
                    selectedChildId = savedChildId
                } else if (childrenList.isNotEmpty()) {
                    selectedChildId = childrenList[0].id
                    prefs.edit().putString("selected_child_id", selectedChildId).apply()
                }
            }
        }
    }

    // Charger les matchs (inclure la finale même si elle est à venir)
    LaunchedEffect(tournoiId) {
        isLoading = true
        error = null

        coroutineScope.launch {
            // Charger tous les matchs (y compris la finale à venir)
            val result = matchRepository.getTournamentMatches(tournoiId, showFuture = true)
            
            if (result.isSuccess) {
                val allMatches = result.getOrNull() ?: emptyList()
                // Filtrer pour garder seulement les matchs terminés/en cours + la finale
                matches = allMatches.filter { match ->
                    val isTermine = match.statut?.lowercase() == "termine"
                    val isEnCours = match.statut?.lowercase() == "en_cours"
                    val isFinale = match.phase?.lowercase() == "finale"
                    // Garder les matchs terminés/en cours + toujours afficher la finale
                    (isTermine || isEnCours) || isFinale
                }
                android.util.Log.d("BracketParentScreen", "✅ ${matches.size} match(s) chargé(s) (incluant la finale)")
            } else {
                error = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                android.util.Log.e("BracketParentScreen", "❌ Erreur: $error")
            }

            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header avec sélecteur d'enfant
        HeaderSection(
            title = tournoiNom ?: "Arbre du tournoi",
            onMenuClick = onMenuClick,
            children = children,
            selectedChildId = selectedChildId,
            onChildSelected = { childId ->
                selectedChildId = childId
                prefs.edit().putString("selected_child_id", childId).apply()
            }
        )
        
        // Contenu avec scroll
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
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
                                text = "Chargement de l'arbre...",
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
                                onClick = onBackClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IconOrange
                                )
                            ) {
                                Text("Retour", color = Color.White)
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
                                text = "L'arbre du tournoi n'a pas encore été généré ou aucun match n'a été joué.",
                                color = TextBlueLight,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                else -> {
                    // Afficher le bracket visuel avec scroll
                    TournamentBracket(
                        matches = matches,
                        modifier = Modifier.fillMaxSize(),
                        onBackClick = onBackClick
                    )
                }
            }
        }
    }
}