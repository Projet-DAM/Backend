package com.example.dam_front.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Program
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.components.SportyGradientButton
import com.example.dam_front.ui.components.SportyInfoChip
import com.example.dam_front.ui.theme.CardWhite
import com.example.dam_front.ui.theme.HeaderBlue
import com.example.dam_front.ui.theme.HeaderBlueLight
import com.example.dam_front.ui.theme.IconOrange
import com.example.dam_front.ui.theme.InactiveStatusGray
import com.example.dam_front.ui.theme.SportyBackgroundBottom
import com.example.dam_front.ui.theme.SportyBackgroundTop
import com.example.dam_front.ui.theme.SportyCardTint
import com.example.dam_front.ui.theme.SportyDarkBlue
import com.example.dam_front.ui.theme.SportyDivider
import com.example.dam_front.ui.theme.SportyLime
import com.example.dam_front.ui.theme.SportyMutedText
import com.example.dam_front.ui.theme.SportyOrange
import com.example.dam_front.ui.theme.SportyTeal
import com.example.dam_front.ui.theme.TextDarkGray
import com.example.dam_front.ui.theme.TextLightGray
import com.example.dam_front.ui.theme.TextWhite
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.ProgramEnrollment
import com.example.dam_front.models.ProgramActivity

@Composable
fun ProgramDetailScreen(
    program: Program,
    enrollments: List<ProgramEnrollment> = emptyList(),
    onLoadEnrollments: () -> Unit = {},
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMenuClick: () -> Unit,
    onActivityClick: (ProgramActivity) -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEnrolledChildrenDialog by remember { mutableStateOf(false) }
    
    // Load enrollments when program changes
    LaunchedEffect(program.id) {
        onLoadEnrollments()
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
            // Simple Light Header
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
                            text = "Détails du programme",
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
                // Program Image and Info Card
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
                            if (!program.image.isNullOrBlank()) {
                                val imageUrl = if (program.image.startsWith("http")) {
                                    program.image
                                } else {
                                    val cleanPath = if (program.image.startsWith("/")) program.image.substring(1) else program.image
                                    "${ApiConfig.BASE_URL}/$cleanPath"
                                }
                                
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Program Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when (program.statut) {
                                            "ACTIF" -> SportyLime
                                            "ARCHIVE" -> InactiveStatusGray
                                            else -> IconOrange
                                        }
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = program.statut.uppercase(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
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
                                        text = program.nomProgramme,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkGray
                                    )
                                    if (!program.objectif.isNullOrBlank()) {
                                        Text(
                                            text = program.objectif,
                                            fontSize = 14.sp,
                                            color = SportyMutedText,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                Text(
                                    text = String.format("%.2f TND", program.prix ?: 0.0),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SportyDarkBlue
                                )
                            }

                            SportySectionDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                InfoItem(
                                    icon = { TrophyIcon(tint = SportyOrange, modifier = Modifier.size(20.dp)) },
                                    label = "Niveau",
                                    value = program.niveau ?: "Non défini"
                                )
                                InfoItem(
                                    icon = { PeopleIcon(tint = SportyOrange, modifier = Modifier.size(20.dp)) },
                                    label = "Inscrits",
                                    value = if (program.capaciteMaximale != null) {
                                        "${program.nombreInscrits ?: 0}/${program.capaciteMaximale}"
                                    } else {
                                        "${program.nombreInscrits ?: 0}"
                                    }
                                )
                            }
                            
                            if (!program.description.isNullOrBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "À propos",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDarkGray
                                    )
                                    Text(
                                        text = program.description,
                                        fontSize = 14.sp,
                                        color = TextLightGray,
                                        lineHeight = 22.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Enrollments Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PeopleIcon(tint = IconOrange)
                                Text(
                                    text = "Enfants inscrits (${enrollments.size})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkGray
                                )
                            }
                            
                            if (enrollments.isNotEmpty()) {
                                Button(
                                    onClick = { showEnrolledChildrenDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SportyOrange
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = "Voir tout",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (enrollments.isEmpty()) {
                            Text(
                                text = "Aucun enfant inscrit pour le moment",
                                color = SportyMutedText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Activities Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ListIcon(tint = IconOrange)
                            Text(
                                text = "Activités (${program.activites.size})",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkGray
                            )
                        }

                        if (program.activites.isEmpty()) {
                            Text(
                                text = "Aucune activité associée",
                                color = SportyMutedText,
                                fontSize = 14.sp
                            )
                        } else {
                            program.activites.forEach { activity ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onActivityClick(activity) }
                                        .background(Color(0xFFF8F9FA))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = activity.nomActivite ?: "Activité ${activity.id.take(6)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDarkGray
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CalendarIcon(tint = TextLightGray)
                                        Text(
                                            text = activity.categorie ?: "Catégorie non renseignée",
                                            fontSize = 13.sp,
                                            color = TextLightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button Modifier (Green to match edit icon in Tournois?) or stick to SportyGradientButton?
                    // Tournois uses small icon for edit. Here we need big buttons.
                    // Let's use the HeaderBlue style or keep SportyGradientButton but maybe lighter?
                    // Keeping SportyGradientButton is fine as it's a primary action.
                    SportyGradientButton(
                        text = "Modifier le programme",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onEditClick,
                        leadingIcon = { EditIcon(tint = Color.White) }
                    )

                    Button(
                        onClick = { showDeleteConfirmation = true },
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
                        text = "Êtes-vous sûr de vouloir supprimer ce programme ? Cette action est définitive.",
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
        
        // Enrolled Children Dialog
        if (showEnrolledChildrenDialog) {
            EnrolledChildrenDialog(
                enrollments = enrollments,
                onDismiss = { showEnrolledChildrenDialog = false }
            )
        }
    }
}

@Composable
fun EnrolledChildrenDialog(
    enrollments: List<ProgramEnrollment>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeopleIcon(tint = SportyOrange)
                Text(
                    text = "Enfants inscrits (${enrollments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = SportyDarkBlue
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                enrollments.forEach { enrollment ->
                    val child = enrollment.child
                    val parent = enrollment.parent
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, SportyDivider),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Child Info with Photo
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!child?.photoProfil.isNullOrBlank()) {
                                    val imageUrl = if (child?.photoProfil?.startsWith("http") == true) {
                                        child.photoProfil
                                    } else {
                                        "${ApiConfig.BASE_URL}/${child?.photoProfil?.removePrefix("/")}"
                                    }
                                    
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = "Photo de ${child?.prenom}",
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, SportyOrange, CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(SportyTeal.copy(alpha = 0.2f))
                                            .border(2.dp, SportyTeal.copy(alpha = 0.5f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = child?.prenom?.firstOrNull()?.toString()?.uppercase() ?: "?",
                                            color = SportyTeal,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${child?.prenom} ${child?.nom}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = SportyDarkBlue
                                    )
                                    child?.dateNaissance?.let { dob ->
                                        Text(
                                            text = "Né(e) le ${com.example.dam_front.utils.DateUtils.formatDate(dob)}",
                                            fontSize = 13.sp,
                                            color = SportyMutedText
                                        )
                                    }
                                }
                            }
                            
                            SportySectionDivider()
                            
                            // Parent Info
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Informations du parent",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SportyMutedText
                                )
                                
                                Text(
                                    text = "${parent?.prenom} ${parent?.nom}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDarkGray
                                )
                                
                                parent?.email?.let { email ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        EmailIcon(tint = SportyMutedText, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = email,
                                            fontSize = 13.sp,
                                            color = TextLightGray
                                        )
                                    }
                                }
                                
                                parent?.telephone?.let { phone ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PhoneIcon(tint = SportyMutedText, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = phone,
                                            fontSize = 13.sp,
                                            color = TextLightGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SportyOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Fermer",
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

@Composable
private fun InfoItem(icon: @Composable () -> Unit, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        icon()
        Column {
            Text(text = label, fontSize = 12.sp, color = SportyMutedText)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDarkGray)
        }
    }
}
