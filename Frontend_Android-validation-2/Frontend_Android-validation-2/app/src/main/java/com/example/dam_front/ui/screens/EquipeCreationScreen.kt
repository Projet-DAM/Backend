package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.CreateEquipeRequest
import com.example.dam_front.models.Equipe
import com.example.dam_front.models.Participant
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.UpdateEquipeRequest
import com.example.dam_front.repository.EquipeRepository
import com.example.dam_front.ui.components.CreateEquipeDialog
import com.example.dam_front.ui.components.EditEquipeDialog
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun EquipeCreationScreen(
    tournoi: Tournoi,
    participants: List<Participant>,
    onBackClick: () -> Unit,
    onEquipesCreated: () -> Unit = {},
    onManageMatchesClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val equipeRepository = remember { EquipeRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var equipes by remember { mutableStateOf<List<Equipe>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Equipe?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isGeneratingBracket by remember { mutableStateOf(false) }
    var showBracketSuccessDialog by remember { mutableStateOf(false) }
    
    fun loadEquipes() {
        tournoi.id?.let { tournoiId ->
            isLoading = true
            coroutineScope.launch {
                val result = equipeRepository.getTournamentEquipes(tournoiId)
                if (result.isSuccess) {
                    equipes = result.getOrNull() ?: emptyList()
                } else {
                    error = result.exceptionOrNull()?.message
                }
                isLoading = false
            }
        }
    }
    
    // Charger les équipes existantes
    LaunchedEffect(tournoi.id) {
        loadEquipes()
    }
    
    // Filtrer les participants disponibles (non assignés à une équipe)
    val availableParticipants = remember(participants, equipes) {
        // Obtenir les IDs des participants déjà dans des équipes
        val assignedIds = equipes.flatMap { equipe ->
            equipe.enfants?.mapNotNull { enfant ->
                // L'ID de l'enfant correspond à l'ID du participant (inscription)
                enfant.id ?: enfant.participantId
            } ?: emptyList()
        }.toSet()
        
        // Filtrer pour ne garder que les participants non assignés
        participants.filter { participant ->
            participant.id?.let { id -> id !in assignedIds } ?: false
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
            // Header avec bouton retour
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(IconOrangeLight)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Retour",
                                tint = IconOrange
                            )
                        }
                        Column {
                            Text(
                                text = "Création d'équipes",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue
                            )
                            Text(
                                text = tournoi.nom ?: "Tournoi",
                                fontSize = 14.sp,
                                color = TextBlueLight
                            )
                        }
                    }
                    
                    // Bouton "Nouvelle équipe"
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.size(48.dp),
                        containerColor = IconGreen,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nouvelle équipe",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = IconOrange)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section équipes existantes
                    if (equipes.isNotEmpty()) {
                        item {
                            Text(
                                text = "Équipes créées (${equipes.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        items(equipes) { equipe ->
                            EquipeCard(
                                equipe = equipe,
                                onEdit = {
                                    showEditDialog = equipe
                                },
                                onDelete = {
                                    coroutineScope.launch {
                                        equipe.id?.let { id ->
                                            val result = equipeRepository.deleteEquipe(id)
                                            if (result.isSuccess) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Équipe supprimée",
                                                    android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                                loadEquipes() // Recharger la liste
                                            } else {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Erreur: ${result.exceptionOrNull()?.message}",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        
                        // Bouton Générer l'arbre de tournoi
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    tournoi.id?.let { tournoiId ->
                                        isGeneratingBracket = true
                                        coroutineScope.launch {
                                            val result = equipeRepository.generateBracket(tournoiId)
                                            isGeneratingBracket = false
                                            
                                            if (result.isSuccess) {
                                                showBracketSuccessDialog = true
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
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = equipes.isNotEmpty() && !isGeneratingBracket,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IconOrange
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                if (isGeneratingBracket) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Génération en cours...",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Générer l'arbre de tournoi",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Bouton Gérer les matchs
                        item {
                            Button(
                                onClick = {
                                    tournoi.id?.let { tournoiId ->
                                        onManageMatchesClick(tournoiId)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                enabled = equipes.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IconGreen
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsSoccer,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Gérer les matchs",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Section participants disponibles (pour drag & drop)
                    if (availableParticipants.isNotEmpty()) {
                        item {
                            Text(
                                text = "Participants disponibles (${availableParticipants.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        item {
                            ParticipantsDragList(
                                participants = availableParticipants,
                                equipes = equipes,
                                onParticipantDropped = { participantId, equipeId ->
                                    // TODO: Implémenter le drag & drop
                                }
                            )
                        }
                    } else if (participants.isNotEmpty() && availableParticipants.isEmpty()) {
                        item {
                            Text(
                                text = "Tous les participants sont déjà dans une équipe",
                                fontSize = 14.sp,
                                color = TextBlueLight,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Dialog de création d'équipe
        if (showCreateDialog) {
            CreateEquipeDialog(
                tournoi = tournoi,
                participants = availableParticipants,
                onDismiss = { showCreateDialog = false },
                onCreate = { createRequest ->
                    coroutineScope.launch {
                        isLoading = true
                        val result = equipeRepository.createEquipe(createRequest)
                        if (result.isSuccess) {
                            showCreateDialog = false
                            android.widget.Toast.makeText(
                                context,
                                "Équipe créée avec succès",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            loadEquipes() // Recharger la liste
                            onEquipesCreated()
                        } else {
                            error = result.exceptionOrNull()?.message
                            android.widget.Toast.makeText(
                                context,
                                "Erreur: ${result.exceptionOrNull()?.message}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                        isLoading = false
                    }
                }
            )
        }
        
        // Dialog d'édition d'équipe
        showEditDialog?.let { equipe ->
            // Pour l'édition, inclure les participants disponibles + ceux déjà dans cette équipe
            val participantsForEdit = remember(availableParticipants, equipe, participants, equipes) {
                // Obtenir les IDs des participants déjà dans d'autres équipes (pas celle en cours d'édition)
                val assignedIds = equipes
                    .filter { it.id != equipe.id } // Exclure l'équipe en cours d'édition
                    .flatMap { otherEquipe ->
                        otherEquipe.enfants?.mapNotNull { enfant ->
                            enfant.id ?: enfant.participantId
                        } ?: emptyList()
                    }.toSet()
                
                val currentEquipeParticipantIds = equipe.enfants?.mapNotNull { it.id ?: it.participantId }?.toSet() ?: emptySet()
                
                participants.filter { participant ->
                    participant.id?.let { id ->
                        // Inclure si disponible (pas dans d'autres équipes) OU si déjà dans cette équipe
                        id !in assignedIds || id in currentEquipeParticipantIds
                    } ?: false
                }
            }
            
            EditEquipeDialog(
                equipe = equipe,
                tournoi = tournoi,
                participants = participantsForEdit,
                onDismiss = { showEditDialog = null },
                onUpdate = { updateRequest ->
                    coroutineScope.launch {
                        equipe.id?.let { id ->
                            isLoading = true
                            // Passer l'équipe actuelle pour calculer les différences
                            val result = equipeRepository.updateEquipe(id, updateRequest, equipe)
                            if (result.isSuccess) {
                                showEditDialog = null
                                android.widget.Toast.makeText(
                                    context,
                                    "Équipe modifiée avec succès",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                loadEquipes() // Recharger la liste
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "Erreur: ${result.exceptionOrNull()?.message}",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                            isLoading = false
                        }
                    }
                }
            )
        }
        
        // Dialog de succès après génération de l'arbre
        if (showBracketSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showBracketSuccessDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = IconGreen,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Arbre généré !",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                    }
                },
                text = {
                    Text(
                        text = "L'arbre de tournoi a été généré avec succès. Vous pouvez maintenant visualiser les matchs.",
                        fontSize = 16.sp,
                        color = TextDarkGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBracketSuccessDialog = false
                            // TODO: Navigation vers l'écran de visualisation de l'arbre
                            // Pour l'instant, on ferme juste le dialog
                            android.widget.Toast.makeText(
                                context,
                                "Fonctionnalité de visualisation à venir",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IconGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "OK",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                containerColor = CardWhite,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun EquipeCard(
    equipe: Equipe,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Indicateur de couleur
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(android.graphics.Color.parseColor(equipe.couleur)))
                    )
                    
                    Column {
                        Text(
                            text = equipe.nom,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                        Text(
                            text = "${equipe.enfants?.size ?: 0} enfant(s) • ${equipe.formatEquipe}",
                            fontSize = 14.sp,
                            color = TextBlueLight
                        )
                    }
                }
                
                // Boutons d'action
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IconGreenLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Modifier",
                            tint = IconGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(IconOrangeLight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = IconOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
        }
    }
}

@Composable
fun ParticipantsDragList(
    participants: List<Participant>,
    equipes: List<Equipe>,
    onParticipantDropped: (String, String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        items(participants) { participant ->
            ParticipantDragItem(participant = participant)
        }
    }
}

@Composable
fun ParticipantDragItem(participant: Participant) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(containerColor = IconGreenLight),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = IconGreen,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${participant.enfantPrenom} ${participant.enfantNom}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = HeaderBlue,
                maxLines = 2
            )
        }
    }
}

