package com.example.dam_front.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlin.math.roundToInt
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.isComplet
import com.example.dam_front.models.remainingSlots
import com.example.dam_front.models.currentParticipantsCount
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.repository.InscriptionRepository
import com.example.dam_front.repository.ChildRepository
import com.example.dam_front.ui.components.TournoiImage
import com.example.dam_front.ui.components.HeaderSection
import com.example.dam_front.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Fonction helper pour formater les dates
private fun buildDateRange(dateDebut: String?, dateFin: String?): String {
    val formatDate = { dateStr: String ->
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
            val date = inputFormat.parse(dateStr)
            date?.let { outputFormat.format(it) } ?: dateStr
        } catch (e: Exception) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("d MMM yyyy", Locale.FRENCH)
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) } ?: dateStr
            } catch (e2: Exception) {
                dateStr
            }
        }
    }

    return when {
        dateDebut != null && dateFin != null -> {
            "${formatDate(dateDebut)} - ${formatDate(dateFin)}"
        }
        dateDebut != null -> formatDate(dateDebut)
        dateFin != null -> formatDate(dateFin)
        else -> ""
    }
}

@Composable
fun TournoiDetailScreen(
    tournoi: Tournoi,
    onBackClick: () -> Unit,
    onInscriptionClick: (Tournoi) -> Unit = {},
    onBracketClick: (String) -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showInscriptionForm by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val inscriptionRepository = remember { InscriptionRepository(context) }
    val childRepository = remember { ChildRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("SportyKidsPrefs", android.content.Context.MODE_PRIVATE)

    // État pour le geste de glissement vers le bas (style iOS)
    var dragOffset by remember { mutableStateOf(0f) }
    val dragThreshold = 150f // Seuil pour déclencher le retour
    var localParticipantsCount by remember(tournoi.id) {
        mutableStateOf<Int?>(null)
    }
    var isLoadingParticipants by remember(tournoi.id) { mutableStateOf(false) }
    val nombreParticipantsMax = tournoi.nombreParticipantsMax

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

    // Charger le nombre réel de participants depuis l'API si nécessaire
    LaunchedEffect(tournoi.id) {
        if (tournoi.id != null && nombreParticipantsMax != null) {
            // Vérifier d'abord si le compteur est disponible dans le modèle
            val countFromModel = tournoi.participantsCount
                ?: tournoi.nombreParticipantsActuels
                ?: tournoi.inscriptionsCount
                ?: tournoi.participants?.size

            if (countFromModel == null) {
                // Le compteur n'est pas dans le modèle, le récupérer depuis l'API
                isLoadingParticipants = true
                android.util.Log.d("TournoiDetailScreen", "📡 Chargement des participants depuis l'API pour ${tournoi.nom}")
                val result = inscriptionRepository.getParticipants(tournoi.id)
                if (result.isSuccess) {
                    val participants = result.getOrNull() ?: emptyList()
                    localParticipantsCount = participants.size
                    android.util.Log.d("TournoiDetailScreen", "✅ Participants chargés: ${participants.size}")
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = exception?.message ?: "Erreur inconnue"
                    android.util.Log.w("TournoiDetailScreen", "⚠️ Impossible de charger les participants: $errorMessage")

                    // Si erreur 403 (accès refusé) après avoir essayé les deux endpoints,
                    // on ne peut pas vérifier le nombre de participants
                    // Par sécurité, on considère que le tournoi pourrait être complet
                    // On laisse localParticipantsCount = null pour bloquer l'inscription
                    if (errorMessage.contains("403") || errorMessage.contains("Forbidden") || errorMessage.contains("Accès refusé")) {
                        android.util.Log.w("TournoiDetailScreen", "🚫 Accès refusé (403) - Impossible de vérifier le nombre de participants. Inscription bloquée par sécurité.")
                        localParticipantsCount = null // null = on ne sait pas, donc on bloque par sécurité
                    } else {
                        // Pour les autres erreurs (404, etc.), on permet l'inscription
                        android.util.Log.w("TournoiDetailScreen", "⚠️ Erreur lors du chargement - L'inscription sera permise, le backend vérifiera si le tournoi est complet.")
                        localParticipantsCount = 0
                    }
                }
                isLoadingParticipants = false
            } else {
                // Le compteur est disponible dans le modèle
                localParticipantsCount = countFromModel
                android.util.Log.d("TournoiDetailScreen", "✅ Compteur depuis modèle: $countFromModel")
            }
        }
    }

    // Calculer l'état complet de manière stricte
    val isTournoiComplet = remember(localParticipantsCount, tournoi, isLoadingParticipants) {
        // Vérifier d'abord le statut/état du tournoi
        val statusComplet = listOf(tournoi.statut, tournoi.etat).any {
            it?.equals("complet", ignoreCase = true) == true
        }

        // Vérifier ensuite la capacité - utiliser le compteur local (chargé depuis l'API) OU le compteur du modèle Tournoi
        val max = nombreParticipantsMax
        val currentLocal = localParticipantsCount
        // Utiliser directement les propriétés du tournoi au lieu de la fonction d'extension
        val currentFromModel = tournoi.participantsCount
            ?: tournoi.nombreParticipantsActuels
            ?: tournoi.inscriptionsCount
            ?: tournoi.participants?.size
        // Utiliser le compteur local (chargé depuis l'API) s'il est disponible, sinon utiliser celui du modèle
        val current = currentLocal ?: currentFromModel

        // Vérifier si la capacité est atteinte
        val capacityComplet = max != null && current != null && current >= max

        val isComplete = statusComplet || capacityComplet

        android.util.Log.d("TournoiDetailScreen", "🔍 Tournoi ${tournoi.nom}: localCount=$currentLocal (chargé depuis API), modelCount=$currentFromModel, current=$current, max=$max, isLoading=$isLoadingParticipants, statut=${tournoi.statut}, etat=${tournoi.etat}, statusComplet=$statusComplet, capacityComplet=$capacityComplet, complet=$isComplete")
        isComplete
    }
    val placesRestantes = tournoi.remainingSlots(localParticipantsCount)

    // BLOQUER COMPLÈTEMENT l'ouverture du formulaire si le tournoi est complet
    LaunchedEffect(isTournoiComplet) {
        if (isTournoiComplet) {
            showInscriptionForm = false
            android.util.Log.d("TournoiDetailScreen", "Tournoi complet - formulaire fermé automatiquement")
        }
    }

    // Vérifier si le formulaire peut être ouvert - LOGIQUE STRICTE
    fun canOpenInscriptionForm(): Boolean {
        // PRIORITÉ 0: Si on charge les participants, BLOQUER temporairement
        if (isLoadingParticipants) {
            android.util.Log.d("TournoiDetailScreen", "⏳ Chargement des participants en cours - INSCRIPTION BLOQUÉE temporairement")
            return false
        }

        // PRIORITÉ 1: Si le tournoi est marqué comme complet (statut ou capacité), BLOQUER
        if (isTournoiComplet) {
            android.util.Log.d("TournoiDetailScreen", "❌ Tournoi complet - INSCRIPTION BLOQUÉE")
            return false
        }

        // PRIORITÉ 2: Vérifier la capacité si elle est définie
        val max = nombreParticipantsMax
        if (max != null) {
            // Utiliser le compteur local (chargé depuis l'API) OU le compteur du modèle Tournoi
            val currentLocal = localParticipantsCount
            // Utiliser directement les propriétés du tournoi
            val currentFromModel = tournoi.participantsCount
                ?: tournoi.nombreParticipantsActuels
                ?: tournoi.inscriptionsCount
                ?: tournoi.participants?.size
            val current = currentLocal ?: currentFromModel

            // Si on ne peut pas vérifier le nombre de participants, permettre l'inscription
            // Le backend vérifiera la capacité réelle
            if (current == null) {
                android.util.Log.d("TournoiDetailScreen", "⚠️ Impossible de vérifier le nombre de participants - INSCRIPTION PERMISE (backend vérifiera)")
                return true
            }

            // Si on connaît le nombre actuel et qu'il est >= max, BLOQUER
            if (current >= max) {
                android.util.Log.d("TournoiDetailScreen", "❌ Capacité atteinte ($current/$max) - INSCRIPTION BLOQUÉE")
                return false
            }
        }

        // Si on arrive ici, l'inscription est possible
        android.util.Log.d("TournoiDetailScreen", "✅ Inscription possible")
        return true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header avec app bar unifiée
            HeaderSection(
                title = tournoi.nom ?: "Tournoi",
                onMenuClick = onMenuClick,
                children = children,
                selectedChildId = selectedChildId,
                onChildSelected = { childId ->
                    selectedChildId = childId
                    prefs.edit().putString("selected_child_id", childId).apply()
                }
            )

            // Contenu avec scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .background(Color(0xFFF5F5F5))
            ) {
                // Image du tournoi avec bordures arrondies et geste de glissement (seule l'image se déplace)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .offset { IntOffset(0, dragOffset.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (dragOffset > dragThreshold) {
                                        onBackClick()
                                    } else {
                                        dragOffset = 0f
                                    }
                                }
                            ) { change, dragAmount ->
                                // Détecter uniquement le glissement vers le bas quand on est en haut
                                if (dragAmount.y > 0 && scrollState.value == 0) {
                                    dragOffset = (dragOffset + dragAmount.y).coerceAtLeast(0f)
                                } else if (dragOffset > 0 && dragAmount.y < 0) {
                                    // Si on glisse vers le haut, réduire l'offset
                                    dragOffset = (dragOffset + dragAmount.y).coerceAtLeast(0f)
                                }
                            }
                        }
                ) {
                    TournoiImage(
                        imagePath = tournoi.image?.takeIf { it.isNotEmpty() }
                            ?: tournoi.imageUrl?.takeIf { it.isNotEmpty() },
                        contentDescription = tournoi.nom,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                        showSportName = false,
                        sportName = null
                    )
                }

                // Contenu scrollable (sans le geste de glissement)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(scrollState)
                ) {

                    // Contenu détaillé avec card amélioré
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Titre du tournoi avec bouton Bracket
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tournoi.nom ?: "Tournoi sans nom",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SportyKidsGreen,
                                    modifier = Modifier.weight(1f)
                                )

                                // Bouton Bracket
                                TextButton(
                                    onClick = {
                                        tournoi.id?.let { tournoiId ->
                                            onBracketClick(tournoiId)
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = IconOrange
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = "Bracket",
                                        modifier = Modifier.size(20.dp),
                                        tint = IconOrange
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Bracket",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = IconOrange
                                    )
                                }
                            }

                            // Sport • Catégorie • Niveau • État
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                if (tournoi.sport != null) {
                                    Text(
                                        text = tournoi.sport,
                                        fontSize = 16.sp,
                                        color = SportyKidsGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (tournoi.sport != null && (tournoi.categorieAge != null || tournoi.niveau != null || tournoi.etat != null)) {
                                    Text(
                                        text = "•",
                                        fontSize = 16.sp,
                                        color = SportyKidsGreen
                                    )
                                }
                                if (tournoi.categorieAge != null) {
                                    Text(
                                        text = tournoi.categorieAge,
                                        fontSize = 16.sp,
                                        color = SportyKidsGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                if (tournoi.categorieAge != null && (tournoi.niveau != null || tournoi.etat != null)) {
                                    Text(
                                        text = "•",
                                        fontSize = 16.sp,
                                        color = SportyKidsGreen
                                    )
                                }
                                if (tournoi.niveau != null) {
                                    Text(
                                        text = tournoi.niveau,
                                        fontSize = 16.sp,
                                        color = SportyKidsGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                // Badge État à droite
                                if (tournoi.etat != null) {
                                    if (tournoi.niveau != null) {
                                        Text(
                                            text = "•",
                                            fontSize = 16.sp,
                                            color = SportyKidsGreen
                                        )
                                    }
                                    val etatColor = when (tournoi.etat.lowercase()) {
                                        "ouvert" -> IconGreen
                                        "fermé" -> Color.Red
                                        "terminé" -> SportyKidsGreen
                                        else -> SportyKidsGreen
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = etatColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tournoi.etat.replaceFirstChar {
                                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                            },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = etatColor
                                        )
                                    }
                                }
                            }

                            // Dates avec calendrier
                            if (tournoi.dateDebut != null || tournoi.dateFin != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = IconOrange,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = buildDateRange(tournoi.dateDebut, tournoi.dateFin),
                                        fontSize = 16.sp,
                                        color = TextDarkGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Lieu avec pin
                            if (tournoi.lieu != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = IconGreen,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = tournoi.lieu,
                                        fontSize = 16.sp,
                                        color = TextDarkGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Prix
                            if (tournoi.fraisParticipation != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "💰",
                                        fontSize = 22.sp
                                    )
                                    Text(
                                        text = "${tournoi.fraisParticipation} TND",
                                        fontSize = 16.sp,
                                        color = TextDarkGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Divider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color(0xFFE0E0E0)
                            )

                            // Description améliorée
                            if (!tournoi.description.isNullOrEmpty()) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    ) {
                                        Text(
                                            text = "📝",
                                            fontSize = 20.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "Description",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SportyKidsGreen
                                        )
                                    }
                                    Text(
                                        text = tournoi.description,
                                        fontSize = 16.sp,
                                        color = TextDarkGray,
                                        lineHeight = 26.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            // Récompense améliorée
                            if (!tournoi.recompense.isNullOrEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = IconOrange.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        ) {
                                            Text(
                                                text = "🏆",
                                                fontSize = 24.sp,
                                                modifier = Modifier.padding(end = 8.dp)
                                            )
                                            Text(
                                                text = "Récompense",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = IconOrange
                                            )
                                        }
                                        Text(
                                            text = tournoi.recompense,
                                            fontSize = 16.sp,
                                            color = TextDarkGray,
                                            lineHeight = 24.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Barre en bas - Bande "Complet" ou Bouton "S'inscrire"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    // Vérifier si le tournoi est complet - LOGIQUE ULTRA STRICTE
                    val max = nombreParticipantsMax
                    val currentLocal = localParticipantsCount
                    val currentFromModel = tournoi.participantsCount
                        ?: tournoi.nombreParticipantsActuels
                        ?: tournoi.inscriptionsCount
                        ?: tournoi.participants?.size
                    val current = currentLocal ?: currentFromModel

                    // Vérification stricte de la capacité - UNIQUEMENT si on connaît le nombre exact
                    val capacityReached = max != null && current != null && current >= max

                    // Vérification du statut
                    val statusComplet = listOf(tournoi.statut, tournoi.etat).any {
                        it?.equals("complet", ignoreCase = true) == true
                    }

                    // Le tournoi est complet si : statut complet OU capacité atteinte OU isTournoiComplet
                    val isReallyComplet = statusComplet || capacityReached || isTournoiComplet

                    // Si on charge les participants, afficher un indicateur de chargement
                    if (isLoadingParticipants) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(
                                    color = Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = IconOrange
                            )
                        }
                    }
                    // Si le tournoi est complet, afficher UNIQUEMENT une BANDE "Complet"
                    else if (isReallyComplet) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            IconGreen,
                                            IconGreen.copy(alpha = 0.8f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    spotColor = IconGreen.copy(alpha = 0.3f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "COMPLET",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    } else {
                        // Bouton "S'inscrire" - S'AFFICHE si le tournoi n'est PAS complet
                        val canShowButton = !isReallyComplet && canOpenInscriptionForm()

                        if (canShowButton) {
                            Button(
                                onClick = {
                                    val canOpen = canOpenInscriptionForm()
                                    val notComplete = !isTournoiComplet && !isReallyComplet && !capacityReached

                                    if (canOpen && notComplete) {
                                        showInscriptionForm = true
                                    }
                                },
                                enabled = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IconOrange
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = Color.White,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = IconOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "S'inscrire",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            // Si le bouton ne peut pas s'afficher, afficher la bande "Complet" par sécurité
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                IconGreen,
                                                IconGreen.copy(alpha = 0.8f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .shadow(
                                        elevation = 8.dp,
                                        shape = RoundedCornerShape(20.dp),
                                        spotColor = IconGreen.copy(alpha = 0.3f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(
                                                color = Color.White.copy(alpha = 0.2f),
                                                shape = CircleShape
                                            )
                                            .padding(4.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "COMPLET",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Recalculer isReallyComplet pour la vérification du formulaire (même logique que pour le bouton)
    val maxForForm = nombreParticipantsMax
    val currentLocalForForm = localParticipantsCount
    val currentFromModelForForm = tournoi.participantsCount
        ?: tournoi.nombreParticipantsActuels
        ?: tournoi.inscriptionsCount
        ?: tournoi.participants?.size
    val currentForForm = currentLocalForForm ?: currentFromModelForForm
    val capacityReachedForForm = maxForForm != null && currentForForm != null && currentForForm >= maxForForm
    val statusCompletForForm = listOf(tournoi.statut, tournoi.etat).any {
        it?.equals("complet", ignoreCase = true) == true
    }
    val isReallyCompletForForm = statusCompletForForm || capacityReachedForForm || isTournoiComplet

    android.util.Log.d("TournoiDetailScreen", "🎯 FORMULAIRE: current=$currentForForm, max=$maxForForm, capacityReached=$capacityReachedForForm, statusComplet=$statusCompletForForm, isReallyCompletForForm=$isReallyCompletForForm")

    // BLOQUER COMPLÈTEMENT le formulaire si le tournoi est complet
    // Vérification ULTRA STRICTE avant d'afficher le formulaire
    val canShowForm = showInscriptionForm && !isTournoiComplet && !isReallyCompletForForm && canOpenInscriptionForm()

    // Si le formulaire essaie de s'ouvrir mais le tournoi est complet, le fermer IMMÉDIATEMENT
    LaunchedEffect(showInscriptionForm, isTournoiComplet, isReallyCompletForForm) {
        if (showInscriptionForm && (isTournoiComplet || isReallyCompletForForm)) {
            android.util.Log.d("TournoiDetailScreen", "❌ FORMULAIRE FERMÉ - Tournoi complet détecté (isTournoiComplet=$isTournoiComplet, isReallyCompletForForm=$isReallyCompletForForm)")
            showInscriptionForm = false
        }
    }

    // Afficher le formulaire UNIQUEMENT si le tournoi n'est pas complet
    if (canShowForm && !isReallyCompletForForm) {
        android.util.Log.d("TournoiDetailScreen", "✅ AFFICHAGE FORMULAIRE AUTORISÉ")
        // Stocker les valeurs dans des variables locales pour éviter les problèmes de smart cast
        val currentParticipants = localParticipantsCount
        val maxParticipants = tournoi.nombreParticipantsMax

        // DERNIÈRE VÉRIFICATION avant d'afficher le formulaire
        val finalCheck = maxParticipants == null || currentParticipants == null || currentParticipants < maxParticipants
        if (finalCheck && !isTournoiComplet && !isReallyCompletForForm) {
            InscriptionTournoiScreen(
                tournoi = tournoi,
                onCancel = {
                    showInscriptionForm = false
                },
                onInscriptionSuccess = {
                    showInscriptionForm = false
                    // Recharger les participants depuis l'API pour avoir le compteur exact
                    if (tournoi.id != null) {
                        coroutineScope.launch {
                            try {
                                android.util.Log.d("TournoiDetailScreen", "🔄 Rechargement des participants après inscription réussie")
                                isLoadingParticipants = true
                                val result = inscriptionRepository.getParticipants(tournoi.id)
                                if (result.isSuccess) {
                                    val participants = result.getOrNull() ?: emptyList()
                                    localParticipantsCount = participants.size
                                    android.util.Log.d("TournoiDetailScreen", "✅ Participants rechargés: ${participants.size}")
                                    // Vérifier si le tournoi est maintenant complet
                                    val newMax = tournoi.nombreParticipantsMax
                                    if (newMax != null && participants.size >= newMax) {
                                        android.util.Log.d("TournoiDetailScreen", "✅ Tournoi ${tournoi.nom} est maintenant complet: ${participants.size} / $newMax")
                                    }
                                } else {
                                    // En cas d'erreur, incrémenter le compteur local
                                    val currentCount = localParticipantsCount ?: 0
                                    localParticipantsCount = currentCount + 1
                                    android.util.Log.w("TournoiDetailScreen", "⚠️ Impossible de recharger les participants, utilisation du compteur local")
                                }
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                // Ignorer l'exception d'annulation - c'est normal quand on quitte l'écran
                                android.util.Log.d("TournoiDetailScreen", "🔄 Rechargement annulé (écran quitté)")
                            } finally {
                                isLoadingParticipants = false
                            }
                        }
                    }
                    onInscriptionClick(tournoi)
                },
                isTournoiComplet = isReallyCompletForForm,
                onMenuClick = onMenuClick
            )
        } else {
            // Si le tournoi devient complet pendant que le formulaire est ouvert, le fermer
            android.util.Log.d("TournoiDetailScreen", "❌ Formulaire ne peut pas s'afficher - conditions non remplies")
            showInscriptionForm = false
        }
    } else {
        // Si le formulaire essaie de s'ouvrir mais le tournoi est complet, le fermer
        if (showInscriptionForm) {
            android.util.Log.d("TournoiDetailScreen", "❌ Formulaire BLOQUÉ - Tournoi complet")
            showInscriptionForm = false
        }
    }
}

@Composable
fun AvailabilitySection(
    isFull: Boolean,
    remainingSlots: Int?,
    maxSlots: Int?
) {
    val backgroundColor = if (isFull) IconGreen.copy(alpha = 0.1f) else SportyKidsGreen.copy(alpha = 0.08f)
    val highlightColor = if (isFull) IconGreen else SportyKidsGreen
    val label = when {
        isFull -> "Tournoi complet"
        remainingSlots != null && maxSlots != null -> "$remainingSlots place(s) restante(s) / $maxSlots"
        maxSlots != null -> "Capacité: $maxSlots"
        else -> "Capacité non précisée"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isFull) "Plus de places disponibles" else "Places disponibles",
                fontSize = 14.sp,
                color = SportyKidsGreen,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextDarkGray
            )
        }
        Box(
            modifier = Modifier
                .background(highlightColor, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isFull) "COMPLET" else "OUVERT",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
