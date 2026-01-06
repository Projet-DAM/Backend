package com.example.dam_front.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import com.example.dam_front.models.EquipeMatch
import com.example.dam_front.models.Match
import com.example.dam_front.ui.theme.*

// Helper function to safely parse color string
private fun parseColorSafely(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (_: Exception) {
        HeaderBlue
    }
}

// Animation de félicitation festive pour l'équipe gagnante
@Composable
private fun CelebrationAnimation(
    teamName: String,
    teamColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    
    // Animation de scale (pulsation plus prononcée)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Animation de rotation plus dynamique
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    // Animation d'opacité pour les confettis
    val confettiAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confetti"
    )
    
    // Animation de translation verticale pour les confettis
    val confettiY by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "confettiY"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Confettis animés en haut (plus nombreux et colorés)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(7) { index ->
                    Icon(
                        imageVector = when (index % 4) {
                            0 -> Icons.Default.Star
                            1 -> Icons.Default.Favorite
                            2 -> Icons.Default.Whatshot
                            else -> Icons.Default.EmojiEvents
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size((24 + index * 3).dp)
                            .graphicsLayer {
                                rotationZ = rotation + (index * 25f)
                                alpha = confettiAlpha
                                scaleX = scale * (0.7f + index * 0.08f)
                                scaleY = scale * (0.7f + index * 0.08f)
                                translationY = confettiY * (if (index % 2 == 0) 1f else -1f)
                            },
                        tint = when (index % 5) {
                            0 -> IconOrange
                            1 -> SportyKidsGreen
                            2 -> HeaderBlue
                            3 -> Color(0xFFFFD700) // Or
                            else -> Color(0xFFFF1493) // Rose vif
                        }
                    )
                }
            }
            
            // Message de félicitation avec animation festive
            Card(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = rotation * 0.05f
                    },
                colors = CardDefaults.cardColors(
                    containerColor = teamColor.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    teamColor.copy(alpha = 0.3f),
                                    IconOrange.copy(alpha = 0.2f),
                                    SportyKidsGreen.copy(alpha = 0.2f)
                                )
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Trophée animé
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    rotationZ = rotation
                                    scaleX = scale * 1.1f
                                    scaleY = scale * 1.1f
                                },
                            tint = Color(0xFFFFD700) // Or brillant
                        )
                        Column {
                            Text(
                                text = "🎉 CHAMPION ! 🎉",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = teamColor,
                                modifier = Modifier.graphicsLayer {
                                    scaleX = scale * 0.95f
                                    scaleY = scale * 0.95f
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$teamName remporte le tournoi ! 🏆",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkGray
                            )
                        }
                    }
                }
            }
            
            // Confettis animés en bas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(6) { index ->
                    Icon(
                        imageVector = when (index % 3) {
                            0 -> Icons.Default.Star
                            1 -> Icons.Default.Favorite
                            else -> Icons.Default.Whatshot
                        },
                        contentDescription = null,
                        modifier = Modifier
                            .size((22 + index * 2).dp)
                            .graphicsLayer {
                                rotationZ = -rotation + (index * 30f)
                                alpha = confettiAlpha * 0.8f
                                scaleX = scale * (0.6f + index * 0.1f)
                                scaleY = scale * (0.6f + index * 0.1f)
                                translationY = -confettiY * (if (index % 2 == 0) 1f else -1f)
                            },
                        tint = when (index % 4) {
                            0 -> Color(0xFFFF1493) // Rose vif
                            1 -> Color(0xFF00CED1) // Turquoise
                            2 -> Color(0xFFFFD700) // Or
                            else -> IconOrange
                        }
                    )
                }
            }
        }
    }
}

// Helper function to convert phase to round
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

// Organize matches by round (from highest to lowest)
private fun organizeMatchesByRound(matches: List<Match>): Map<Int, List<Match>> {
    return matches
        .filter { it.phase != null }
        .groupBy { getRoundFromPhase(it.phase) }
        .toSortedMap(compareByDescending { it })
}

@Composable
fun TournamentBracket(
    matches: List<Match>,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val organizedMatches = remember(matches) {
        organizeMatchesByRound(matches)
    }
    
    if (organizedMatches.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextBlueLight
                )
                Text(
                    text = "Aucun match disponible",
                    color = TextDarkGray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "L'arbre du tournoi sera généré après la création des équipes",
                    color = TextBlueLight,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
        return
    }
    
    val density = LocalDensity.current
    val rounds = organizedMatches.keys.sortedDescending() // Du plus haut au plus bas (Finale → Demi → Quart)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bracket vertical (pyramid structure - de haut en bas)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rounds.forEach { round ->
                    val matchesInRound = organizedMatches[round] ?: emptyList()
                    
                    // Round title avec petite icône de retour à gauche (pour QUARTS DE FINALE)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // Petite icône de retour à côté de "QUARTS DE FINALE" (round 3)
                        if (round == 3 && onBackClick != null) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Retour",
                                    tint = IconOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        
                        Text(
                            text = when (round) {
                                1 -> "🏆 FINALE"
                                2 -> "🥈 DEMI-FINALES"
                                3 -> "🥉 QUARTS DE FINALE"
                                4 -> "HUITIÈMES DE FINALE"
                                else -> "ROUND $round"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue,
                            textAlign = TextAlign.Start
                        )
                    }
                    
                    // Matches in this round
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        matchesInRound.forEach { match ->
                            CompactMatchCard(
                                match = match,
                                round = round,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                            )
                            
                            // Animation de félicitation pour le gagnant de la finale
                            if (round == 1 && match.statut?.lowercase() == "termine") {
                                val winner = when {
                                    (match.scoreEquipe1 ?: 0) > (match.scoreEquipe2 ?: 0) -> match.equipe1
                                    (match.scoreEquipe2 ?: 0) > (match.scoreEquipe1 ?: 0) -> match.equipe2
                                    else -> null
                                }
                                
                                winner?.let { winningTeam ->
                                    CelebrationAnimation(
                                        teamName = winningTeam.nom ?: "Équipe gagnante",
                                        teamColor = winningTeam.couleur?.let { 
                                            parseColorSafely(it) 
                                        } ?: IconOrange
                                    )
                                }
                            }
                        }
                    }
                    
                    // Connection indicator (flèche vers le bas)
                    if (round != rounds.last()) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(vertical = 4.dp),
                            tint = IconOrange.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMatchCard(
    match: Match,
    round: Int,
    modifier: Modifier = Modifier
) {
    // Fonction helper pour obtenir le nom d'affichage d'une équipe
    fun getEquipeDisplayName(equipe: EquipeMatch?, isEquipeA: Boolean): String {
        return when {
            equipe != null && !equipe.nom.isNullOrBlank() -> equipe.nom!!
            match.phase == "demi_final" -> {
                val matchOrdre = match.ordre ?: 1
                val quartNumero = if (isEquipeA) {
                    matchOrdre * 2 - 1
                } else {
                    matchOrdre * 2
                }
                "Vainqueur Quart $quartNumero"
            }
            match.phase == "finale" -> {
                if (isEquipeA) {
                    "Vainqueur Demi 1"
                } else {
                    "Vainqueur Demi 2"
                }
            }
            else -> "À déterminer"
        }
    }
    
    val equipe1Nom = getEquipeDisplayName(match.equipe1, isEquipeA = true)
    val equipe2Nom = getEquipeDisplayName(match.equipe2, isEquipeA = false)
    val score1 = match.scoreEquipe1
    val score2 = match.scoreEquipe2
    val statut = match.statut?.lowercase() ?: "en_attente"
    
    val statutColor = when (statut) {
        "termine" -> IconGreen
        "en_cours" -> IconOrange
        "a_venir" -> TextBlueLight
        else -> TextBlueLight
    }
    
    val backgroundColor = when (statut) {
        "termine" -> Color.White
        "en_cours" -> IconOrange.copy(alpha = 0.05f)
        else -> Color.White
    }
    
    val borderColor = when (statut) {
        "termine" -> IconGreen.copy(alpha = 0.3f)
        "en_cours" -> IconOrange.copy(alpha = 0.5f)
        else -> TextBlueLight.copy(alpha = 0.3f)
    }
    
    val isFinale = round == 1
    
    Card(
        modifier = modifier
            .shadow(
                elevation = if (isFinale) 6.dp else 3.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = if (isFinale) IconOrange.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFinale) 1.5.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status badge (plus petit)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statutColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = when (statut) {
                            "termine" -> "✓ Terminé"
                            "en_cours" -> "▶ En cours"
                            "a_venir" -> "⏱ À venir"
                            else -> "⏳ En attente"
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statutColor
                    )
                }
                
                if (isFinale) {
                    Text(
                        text = "🏆",
                        fontSize = 16.sp
                    )
                }
            }
            
            // Team 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team color indicator
                    match.equipe1?.couleur?.let { couleur ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(parseColorSafely(couleur))
                                .border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(TextBlueLight)
                        )
                    }
                    
                    Text(
                        text = equipe1Nom,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeaderBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Score
                if (score1 != null) {
                    Text(
                        text = score1.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (score1 > (score2 ?: -1)) IconGreen else TextDarkGray
                    )
                }
            }
            
            // VS separator
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                thickness = 0.5.dp,
                color = TextBlueLight.copy(alpha = 0.3f)
            )
            
            // Team 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team color indicator
                    match.equipe2?.couleur?.let { couleur ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(parseColorSafely(couleur))
                                .border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(TextBlueLight)
                        )
                    }
                    
                    Text(
                        text = equipe2Nom,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeaderBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Score
                if (score2 != null) {
                    Text(
                        text = score2.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (score2 > (score1 ?: -1)) IconGreen else TextDarkGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfessionalMatchCard(
    match: Match,
    round: Int,
    modifier: Modifier = Modifier
) {
    // Fonction helper pour obtenir le nom d'affichage d'une équipe
    fun getEquipeDisplayName(equipe: EquipeMatch?, isEquipeA: Boolean): String {
        return when {
            equipe != null && !equipe.nom.isNullOrBlank() -> equipe.nom!!
            match.phase == "demi_final" -> {
                // Pour les demi-finales : équipeA = Vainqueur Quart (ordre*2-1), équipeB = Vainqueur Quart (ordre*2)
                val matchOrdre = match.ordre ?: 1
                val quartNumero = if (isEquipeA) {
                    matchOrdre * 2 - 1
                } else {
                    matchOrdre * 2
                }
                "Vainqueur Quart $quartNumero"
            }
            match.phase == "finale" -> {
                // Pour la finale : équipeA = Vainqueur Demi 1, équipeB = Vainqueur Demi 2
                if (isEquipeA) {
                    "Vainqueur Demi 1"
                } else {
                    "Vainqueur Demi 2"
                }
            }
            else -> "À déterminer"
        }
    }
    
    val equipe1Nom = getEquipeDisplayName(match.equipe1, isEquipeA = true)
    val equipe2Nom = getEquipeDisplayName(match.equipe2, isEquipeA = false)
    val score1 = match.scoreEquipe1
    val score2 = match.scoreEquipe2
    val statut = match.statut?.lowercase() ?: "en_attente"
    
    val statutColor = when (statut) {
        "termine" -> IconGreen
        "en_cours" -> IconOrange
        "a_venir" -> TextBlueLight
        else -> TextBlueLight
    }
    
    val backgroundColor = when (statut) {
        "termine" -> Color.White
        "en_cours" -> IconOrange.copy(alpha = 0.05f)
        else -> Color.White
    }
    
    val borderColor = when (statut) {
        "termine" -> IconGreen.copy(alpha = 0.4f)
        "en_cours" -> IconOrange.copy(alpha = 0.4f)
        else -> TextBlueLight.copy(alpha = 0.2f)
    }
    
    val isFinale = round == 1
    
    Card(
        modifier = modifier
            .shadow(
                elevation = if (isFinale) 8.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isFinale) IconOrange.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFinale) 2.dp else 1.5.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status badge
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
                        text = when (statut) {
                            "termine" -> "✓ Terminé"
                            "en_cours" -> "▶ En cours"
                            "a_venir" -> "⏱ À venir"
                            else -> "⏳ En attente"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statutColor
                    )
                }
                
                if (isFinale) {
                    Text(
                        text = "🏆",
                        fontSize = 20.sp
                    )
                }
            }
            
            // Team 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team color indicator
                    match.equipe1?.couleur?.let { couleur ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(parseColorSafely(couleur))
                                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(TextBlueLight)
                        )
                    }
                    
                    Text(
                        text = equipe1Nom,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeaderBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Score 1
                if (score1 != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (statut == "termine" && score1 > (score2 ?: 0)) {
                                    IconGreen.copy(alpha = 0.2f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$score1",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (statut == "termine" && score1 > (score2 ?: 0)) IconGreen else HeaderBlue
                        )
                    }
                } else {
                    Text(
                        text = "-",
                        fontSize = 14.sp,
                        color = TextBlueLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Divider
            HorizontalDivider(
                color = TextBlueLight.copy(alpha = 0.3f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            // Team 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Team color indicator
                    match.equipe2?.couleur?.let { couleur ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(parseColorSafely(couleur))
                                .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(TextBlueLight)
                        )
                    }
                    
                    Text(
                        text = equipe2Nom,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeaderBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Score 2
                if (score2 != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (statut == "termine" && score2 > (score1 ?: 0)) {
                                    IconGreen.copy(alpha = 0.2f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$score2",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (statut == "termine" && score2 > (score1 ?: 0)) IconGreen else HeaderBlue
                        )
                    }
                } else {
                    Text(
                        text = "-",
                        fontSize = 14.sp,
                        color = TextBlueLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
