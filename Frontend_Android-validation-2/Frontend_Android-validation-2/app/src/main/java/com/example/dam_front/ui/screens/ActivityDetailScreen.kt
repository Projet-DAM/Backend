package com.example.dam_front.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Activity
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.components.SportyGradientButton
import com.example.dam_front.ui.theme.*
import com.example.dam_front.config.ApiConfig
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning

@Composable
fun ActivityDetailScreen(
    activity: Activity,
    currentUserId: String?,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    var showPermissionAlert by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var coachName by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val userRepository = remember { com.example.dam_front.repository.UserRepository(context) }

    LaunchedEffect(activity.coach) {
        if (!activity.coach.isNullOrBlank()) {
            userRepository.getUserById(activity.coach).onSuccess { coach ->
                coachName = "${coach.prenom} ${coach.nom}"
            }.onFailure {
                coachName = "Non renseigné"
            }
        } else {
            coachName = "Non renseigné"
        }
    }

    val isOwner = currentUserId != null && activity.coach != null && currentUserId == activity.coach
    
    val handleDeleteClick = {
        if (isOwner) {
            showDeleteConfirmation = true
        } else {
            showPermissionAlert = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Header (Match ProgramDetail style)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Transparent)
                        ) {
                            ArrowBackIcon(tint = SportyDarkBlue, modifier = Modifier.size(24.dp))
                        }
                        Text(
                            text = "Détails de l'activité",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = SportyDarkBlue
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Activity Image and Main Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        // Image
                        Box(modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()) {
                            if (!activity.image.isNullOrBlank()) {
                                val baseUrl = ApiConfig.BASE_URL.removeSuffix("/")
                                val imagePath = if (activity.image.startsWith("/")) activity.image else "/${activity.image}"
                                val fullUrl = if (activity.image.startsWith("http")) activity.image else "$baseUrl$imagePath"

                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = "Activity Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TargetIcon(modifier = Modifier.size(64.dp), tint = Color.White)
                                }
                            }
                            
                            // Status Badge
                            val statusToDisplay = activity.statut?.uppercase()
                            if (statusToDisplay != null && statusToDisplay != "BROUILLON" && statusToDisplay != "INCONNU") {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (statusToDisplay) {
                                                "CONFIRME", "ACTIVE" -> SportyLime
                                                "ANNULE" -> Color(0xFFE53935)
                                                else -> IconOrange
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = statusToDisplay,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Info
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activity.nomActivite,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkGray
                                    )
                                    if (!activity.categorie.isNullOrBlank()) {
                                        Text(
                                            text = activity.categorie,
                                            fontSize = 14.sp,
                                            color = SportyMutedText
                                        )
                                    }
                                }
                                Text(
                                    text = String.format("%.2f TND", activity.prix ?: 0.0),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SportyDarkBlue
                                )
                            }
                            
                            SportySectionDivider()
                            
                            // Info Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                InfoItem(
                                    icon = { CalendarIcon(tint = SportyOrange, modifier = Modifier.size(20.dp)) },
                                    label = "Date",
                                    value = formatDate(activity.date)
                                )
                                InfoItem(
                                    icon = { ClockIcon(tint = SportyOrange, modifier = Modifier.size(20.dp)) },
                                    label = "Heure",
                                    value = formatTime(activity.heure)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                InfoItem(
                                    icon = { TimerIcon(tint = SportyOrange, modifier = Modifier.size(20.dp)) },
                                    label = "Durée",
                                    value = "${activity.duree ?: 60} min"
                                )
                                InfoItem(
                                    icon = { PeopleIcon(tint = SportyOrange, modifier = Modifier.size(20.dp)) },
                                    label = "Capacité",
                                    value = "${activity.capaciteMax ?: 0} pers."
                                )
                            }

                            if (!activity.description.isNullOrBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "À propos",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDarkGray
                                    )
                                    Text(
                                        text = activity.description,
                                        fontSize = 14.sp,
                                        color = TextLightGray,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Academy Card (Newly added info)
                if (!activity.academie.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(SportyTeal.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                TrophyIcon(tint = SportyTeal, modifier = Modifier.size(24.dp))
                            }
                            Column {
                                Text(
                                    text = "Académie",
                                    fontSize = 12.sp,
                                    color = SportyMutedText
                                )
                                Text(
                                    text = activity.academie,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDarkGray
                                )
                            }
                        }
                    }
                }

                // Coach Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SportyTeal.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SportyTeal,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Coach Responsable",
                                fontSize = 12.sp,
                                color = SportyMutedText
                            )
                            Text(
                                text = coachName ?: "Chargement...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkGray
                            )
                        }
                    }
                }

                // Associated Program (Newly added info)
                if (!activity.programmeId.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ListIcon(tint = IconOrange)
                                Text(
                                    text = "Programme associé",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkGray
                                )
                            }
                            Text(
                                text = "ID: ${activity.programmeId}",
                                fontSize = 14.sp,
                                color = SportyMutedText
                            )
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SportyGradientButton(
                        text = "Modifier l'activité",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onEditClick,
                        leadingIcon = { EditIcon(tint = Color.White) }
                    )

                    Button(
                        onClick = handleDeleteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFFE53935)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFE53935)
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DeleteIcon(tint = Color(0xFFE53935))
                            Text(
                                text = "Supprimer",
                                color = Color(0xFFE53935),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
        
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = {
                    Text(
                        text = "Confirmer la suppression",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDarkGray
                    )
                },
                text = {
                    Text(
                        text = "Êtes-vous sûr de vouloir supprimer cette activité ? Cette action est définitive.",
                        fontSize = 16.sp,
                        color = TextDarkGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation = false
                            onDeleteClick()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Supprimer",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showDeleteConfirmation = false },
                        colors = ButtonDefaults.buttonColors(containerColor = IconOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Annuler",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (showPermissionAlert) {
            AlertDialog(
                onDismissRequest = { showPermissionAlert = false },
                title = {
                    Text(
                        text = "Accès refusé",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDarkGray
                    )
                },
                text = {
                    Text(
                        text = "Vous n'êtes pas autorisé à supprimer cette activité. Seul le coach propriétaire peut la supprimer.",
                        fontSize = 16.sp,
                        color = TextDarkGray
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showPermissionAlert = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IconOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Compris",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                icon = {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = SportyOrange)
                }
            )
        }
    }
}

// Helpers
private fun formatDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "Non renseignée"
    return try {
        val inputDate = if (dateString.contains("T")) dateString.split("T")[0] else dateString
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        val date = inputFormat.parse(inputDate)
        if (date != null) outputFormat.format(date) else dateString
    } catch (e: Exception) {
        dateString
    }
}

private fun formatTime(timeString: String?): String {
    if (timeString.isNullOrBlank()) return "Non définie"
    return try {
        timeString.substringBeforeLast(":") // Remove seconds if HH:mm:ss
    } catch (e: Exception) {
        timeString
    }
}

@Composable
fun SportySectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = SportyDivider,
        thickness = 1.dp
    )
}

@Composable
private fun InfoItem(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = SportyMutedText
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            color = TextDarkGray,
            fontWeight = FontWeight.Bold
        )
    }
}
