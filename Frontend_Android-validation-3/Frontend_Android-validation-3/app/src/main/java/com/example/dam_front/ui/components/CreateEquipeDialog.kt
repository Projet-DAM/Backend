package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dam_front.models.CreateEquipeRequest
import com.example.dam_front.models.Participant
import com.example.dam_front.models.Tournoi
import com.example.dam_front.ui.theme.*

@Composable
fun CreateEquipeDialog(
    tournoi: Tournoi,
    participants: List<Participant>,
    onDismiss: () -> Unit,
    onCreate: (CreateEquipeRequest) -> Unit
) {
    var nomEquipe by remember { mutableStateOf("") }
    var couleur by remember { mutableStateOf("#FF9800") } // Orange par défaut
    var formatEquipe by remember { mutableStateOf("5v5") }
    var selectedParticipants by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    val formatsEquipe = listOf("5v5", "7v7", "11v11")
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nouvelle équipe",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = HeaderBlue
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextDarkGray
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Formulaire avec scroll
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Nom de l'équipe
                    item {
                        Column {
                            Text(
                                text = "Nom de l'équipe *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            OutlinedTextField(
                                value = nomEquipe,
                                onValueChange = { nomEquipe = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ex: ARSENAL") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HeaderBlue,
                                    unfocusedBorderColor = TextBlueLight
                                )
                            )
                        }
                    }
                    
                    // Couleur
                    item {
                        Column {
                            Text(
                                text = "Couleur *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(android.graphics.Color.parseColor(couleur)))
                                        .border(2.dp, HeaderBlue, RoundedCornerShape(12.dp))
                                        .clickable { showColorPicker = true }
                                )
                                Text(
                                    text = couleur,
                                    fontSize = 14.sp,
                                    color = TextDarkGray
                                )
                            }
                        }
                    }
                    
                    // Format équipe
                    item {
                        Column {
                            Text(
                                text = "Format équipe *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = HeaderBlue
                                    )
                                ) {
                                    Text(
                                        text = formatEquipe,
                                        modifier = Modifier.weight(1f),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    formatsEquipe.forEach { format ->
                                        DropdownMenuItem(
                                            text = { Text(format) },
                                            onClick = {
                                                formatEquipe = format
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Sélection des enfants
                    item {
                        Column {
                            Text(
                                text = "Sélectionner les enfants *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "${selectedParticipants.size} enfant(s) sélectionné(s)",
                                fontSize = 14.sp,
                                color = TextBlueLight
                            )
                        }
                    }
                    
                    // Liste des participants
                    items(participants) { participant ->
                        val isSelected = participant.id?.let { selectedParticipants.contains(it) } ?: false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    participant.id?.let { id ->
                                        selectedParticipants = if (isSelected) {
                                            selectedParticipants - id
                                        } else {
                                            selectedParticipants + id
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) IconGreenLight else CardWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(2.dp, IconGreen)
                            } else {
                                null
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) IconGreen else TextBlueLight,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${participant.enfantPrenom} ${participant.enfantNom}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = HeaderBlue
                                        )
                                        if (participant.enfantDateNaissance != null) {
                                            Text(
                                                text = participant.enfantDateNaissance,
                                                fontSize = 12.sp,
                                                color = TextBlueLight
                                            )
                                        }
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sélectionné",
                                        tint = IconGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Boutons d'action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annuler")
                    }
                    Button(
                        onClick = {
                            if (nomEquipe.isNotBlank() && selectedParticipants.isNotEmpty()) {
                                onCreate(
                                    CreateEquipeRequest(
                                        nom = nomEquipe,
                                        couleur = couleur,
                                        tournoiId = tournoi.id ?: "",
                                        enfants = selectedParticipants.toList(),
                                        formatEquipe = formatEquipe
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = nomEquipe.isNotBlank() && selectedParticipants.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IconGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Créer")
                    }
                }
            }
        }
    }
    
    // Color Picker Dialog
    if (showColorPicker) {
        ColorPickerDialog(
            currentColor = couleur,
            onColorSelected = { newColor ->
                couleur = newColor
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
fun ColorPickerDialog(
    currentColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = listOf(
        "#FF9800", // Orange
        "#8BC34A", // Green
        "#2196F3", // Blue
        "#9C27B0", // Purple
        "#F44336", // Red
        "#00BCD4", // Cyan
        "#FFEB3B", // Yellow
        "#795548", // Brown
        "#607D8B", // Blue Grey
        "#E91E63"  // Pink
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choisir une couleur", color = HeaderBlue) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Afficher les couleurs en grille scrollable
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    colors.chunked(5).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(color)))
                                        .border(
                                            width = if (color == currentColor) 4.dp else 0.dp,
                                            color = HeaderBlue,
                                            shape = CircleShape
                                        )
                                        .clickable { onColorSelected(color) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = HeaderBlue)
            }
        }
    )
}

