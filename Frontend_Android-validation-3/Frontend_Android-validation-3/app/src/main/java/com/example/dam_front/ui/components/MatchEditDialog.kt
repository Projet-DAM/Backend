package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.dam_front.models.Match
import com.example.dam_front.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchEditDialog(
    match: Match,
    onDismiss: () -> Unit,
    onSave: (Int?, Int?, String) -> Unit
) {
    var scoreEquipe1 by remember { mutableStateOf(match.scoreEquipe1?.toString() ?: "") }
    var scoreEquipe2 by remember { mutableStateOf(match.scoreEquipe2?.toString() ?: "") }
    var selectedStatut by remember { mutableStateOf(match.statut ?: "en_attente") }
    var expandedStatut by remember { mutableStateOf(false) }
    
    val equipe1Nom = match.equipe1?.nom ?: "Équipe 1"
    val equipe2Nom = match.equipe2?.nom ?: "Équipe 2"
    
    val statuts = listOf("en_attente", "a_venir", "en_cours", "termine")
    val statutsDisplay = mapOf(
        "en_attente" to "En attente",
        "a_venir" to "À venir",
        "en_cours" to "En cours",
        "termine" to "Terminé"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = IconOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Modifier le match",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextDarkGray
                        )
                    }
                }
                
                Divider()
                
                // Équipes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        match.equipe1?.couleur?.let { couleur ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(couleur)))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = equipe1Nom,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                    }
                    
                    Text(
                        text = "VS",
                        fontSize = 18.sp,
                        color = TextBlueLight,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        match.equipe2?.couleur?.let { couleur ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(android.graphics.Color.parseColor(couleur)))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = equipe2Nom,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue,
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
                
                Divider()
                
                // Scores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Score Équipe 1
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Score $equipe1Nom",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDarkGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = scoreEquipe1,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() }) {
                                    scoreEquipe1 = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IconOrange,
                                unfocusedBorderColor = TextBlueLight
                            )
                        )
                    }
                    
                    Text(
                        text = "-",
                        fontSize = 24.sp,
                        color = TextBlueLight,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    
                    // Score Équipe 2
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Score $equipe2Nom",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDarkGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = scoreEquipe2,
                            onValueChange = { 
                                if (it.all { char -> char.isDigit() }) {
                                    scoreEquipe2 = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IconOrange,
                                unfocusedBorderColor = TextBlueLight
                            )
                        )
                    }
                }
                
                // Statut
                Column {
                    Text(
                        text = "Statut",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    ExposedDropdownMenuBox(
                        expanded = expandedStatut,
                        onExpandedChange = { expandedStatut = it }
                    ) {
                        OutlinedTextField(
                            value = statutsDisplay[selectedStatut] ?: selectedStatut,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStatut)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IconOrange,
                                unfocusedBorderColor = TextBlueLight
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedStatut,
                            onDismissRequest = { expandedStatut = false }
                        ) {
                            statuts.forEach { statut ->
                                DropdownMenuItem(
                                    text = { Text(statutsDisplay[statut] ?: statut) },
                                    onClick = {
                                        selectedStatut = statut
                                        expandedStatut = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Boutons (réduits pour tenir sur une ligne)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Annuler",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    
                    Button(
                        onClick = {
                            val score1 = scoreEquipe1.toIntOrNull()
                            val score2 = scoreEquipe2.toIntOrNull()
                            onSave(score1, score2, selectedStatut)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IconGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Enregistrer",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

