package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Participant
import com.example.dam_front.models.Tournoi
import com.example.dam_front.ui.components.TournoiImage
import com.example.dam_front.ui.theme.*
import androidx.compose.ui.layout.ContentScale
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ParticipantsScreen(
    tournoi: Tournoi,
    participants: List<Participant>,
    isLoading: Boolean,
    onBackClick: () -> Unit,
    onEditParticipant: (Participant) -> Unit = {},
    onDeleteParticipant: (Participant) -> Unit = {},
    onCreateEquipesClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Image du tournoi avec bouton retour (style TournoiDetailScreen)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Image du tournoi
                TournoiImage(
                    imagePath = tournoi.image?.takeIf { it.isNotEmpty() } 
                        ?: tournoi.imageUrl?.takeIf { it.isNotEmpty() },
                    contentDescription = tournoi.nom,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop,
                    showSportName = false,
                    sportName = null
                )
                
                // Bouton retour raffiné en haut à gauche
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = IconOrange,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Retour",
                        tint = HeaderBlue
                    )
                }
            }
            
            // En-tête avec nom du tournoi et bouton créer équipes
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
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = tournoi.nom ?: "Tournoi",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                        Text(
                            text = "${participants.size} participant(s)",
                            fontSize = 14.sp,
                            color = TextBlueLight,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // Bouton "Créer équipes"
                    FloatingActionButton(
                        onClick = onCreateEquipesClick,
                        modifier = Modifier.size(48.dp),
                        containerColor = IconGreen,
                        contentColor = Color.White
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = "Créer équipes",
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
            } else if (participants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "👥",
                            fontSize = 64.sp
                        )
                        Text(
                            text = "Aucun participant",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkGray
                        )
                        Text(
                            text = "Aucun enfant n'est encore inscrit à ce tournoi",
                            fontSize = 14.sp,
                            color = TextBlueLight
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(participants) { participant ->
                        ParticipantCard(
                            participant = participant,
                            onEdit = { onEditParticipant(participant) },
                            onDelete = { onDeleteParticipant(participant) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantCard(
    participant: Participant,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
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
            // Enfant avec boutons d'action
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = IconGreenLight,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = IconGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${participant.enfantPrenom} ${participant.enfantNom}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderBlue
                    )
                    if (participant.enfantDateNaissance != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = IconOrange,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = formatDateNaissance(participant.enfantDateNaissance),
                                fontSize = 12.sp,
                                color = TextBlueLight
                            )
                        }
                    }
                }
                // Boutons d'action (même style que les tournois)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bouton modifier (vert comme les tournois)
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
                    // Bouton supprimer (orange comme les tournois)
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
            
            Divider(color = Color(0xFFE0E0E0))
            
            // Parent
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Parent / Tuteur",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkGray
                )
                Text(
                    text = "${participant.parentPrenom} ${participant.parentNom}",
                    fontSize = 16.sp,
                    color = TextDarkGray
                )
                if (participant.parentTelephone != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = IconGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = participant.parentTelephone,
                            fontSize = 14.sp,
                            color = TextBlueLight
                        )
                    }
                }
            }
            
            // Besoins particuliers si présent
            if (!participant.besoinsParticuliers.isNullOrEmpty()) {
                Divider(color = Color(0xFFE0E0E0), modifier = Modifier.padding(vertical = 4.dp))
                Column {
                    Text(
                        text = "Besoins particuliers",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextBlueLight
                    )
                    Text(
                        text = participant.besoinsParticuliers,
                        fontSize = 14.sp,
                        color = TextDarkGray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

fun formatDateNaissance(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}


