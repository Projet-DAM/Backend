package com.example.dam_front.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dam_front.ui.theme.*

data class FilterOptions(
    val etat: String? = null, // "ouvert", "fermé", "terminé"
    val niveau: String? = null, // "débutant", "intermédiaire", "avancé"
    val showAllSports: Boolean? = null // null = par défaut (sport de l'enfant), true = tous les sports, false = sport de l'enfant uniquement
)

@Composable
fun FilterDialog(
    onDismiss: () -> Unit,
    onFilterApply: (FilterOptions) -> Unit,
    currentFilters: FilterOptions = FilterOptions()
) {
    var selectedEtat by remember { mutableStateOf(currentFilters.etat) }
    var selectedNiveau by remember { mutableStateOf(currentFilters.niveau) }
    var showAllSports by remember { mutableStateOf(currentFilters.showAllSports) }
    
    var expandedEtat by remember { mutableStateOf(false) }
    var expandedNiveau by remember { mutableStateOf(false) }
    var expandedSport by remember { mutableStateOf(false) }
    
    val etats = listOf("ouvert", "fermé", "terminé")
    val etatsDisplay = mapOf(
        "ouvert" to "Ouvert",
        "fermé" to "Fermé",
        "terminé" to "Terminé"
    )
    
    val niveaux = listOf("débutant", "intermédiaire", "avancé")
    val niveauxDisplay = mapOf(
        "débutant" to "Débutant",
        "intermédiaire" to "Intermédiaire",
        "avancé" to "Avancé"
    )
    
    val sportsOptions = listOf(
        "Tous les sports" to true,
        "Sport de mon enfant uniquement" to false
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header amélioré
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
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = IconOrange,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Filtrer",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(
                                color = Color(0xFFF5F5F5),
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = TextDarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                // Dropdown État
                FilterDropdownSection(
                    label = "État",
                    icon = Icons.Default.Info,
                    selectedValue = selectedEtat?.let { etatsDisplay[it] } ?: "Sélectionner un état",
                    expanded = expandedEtat,
                    onExpandedChange = { expandedEtat = it },
                    options = etats.map { etatsDisplay[it] ?: it },
                    onOptionSelected = { option ->
                        val key = etatsDisplay.entries.find { it.value == option }?.key
                        selectedEtat = if (selectedEtat == key) null else key
                        expandedEtat = false
                    },
                    selectedKey = selectedEtat,
                    displayMap = etatsDisplay,
                    iconColor = SportyKidsGreen
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Dropdown Sport
                FilterDropdownSection(
                    label = "Sport",
                    icon = Icons.Default.SportsSoccer,
                    selectedValue = when (showAllSports) {
                        true -> "Tous les sports"
                        false -> "Sport de mon enfant uniquement"
                        null -> "Sélectionner une option"
                    },
                    expanded = expandedSport,
                    onExpandedChange = { expandedSport = it },
                    options = sportsOptions.map { it.first },
                    onOptionSelected = { option ->
                        val newValue = sportsOptions.find { it.first == option }?.second
                        showAllSports = if (showAllSports == newValue) null else newValue
                        expandedSport = false
                    },
                    selectedKey = showAllSports,
                    displayMap = null,
                    iconColor = IconOrange
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Dropdown Niveau
                FilterDropdownSection(
                    label = "Niveau",
                    icon = Icons.Default.Star,
                    selectedValue = selectedNiveau?.let { niveauxDisplay[it] } ?: "Sélectionner un niveau",
                    expanded = expandedNiveau,
                    onExpandedChange = { expandedNiveau = it },
                    options = niveaux.map { niveauxDisplay[it] ?: it },
                    onOptionSelected = { option ->
                        val key = niveauxDisplay.entries.find { it.value == option }?.key
                        selectedNiveau = if (selectedNiveau == key) null else key
                        expandedNiveau = false
                    },
                    selectedKey = selectedNiveau,
                    displayMap = niveauxDisplay,
                    iconColor = IconOrange
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Boutons améliorés (réduits pour tenir sur une ligne)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bouton Annuler
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextDarkGray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = Color(0xFFE0E0E0)
                        )
                    ) {
                        Text(
                            text = "Annuler",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                    
                    // Bouton Appliquer amélioré
                    Button(
                        onClick = {
                            onFilterApply(FilterOptions(selectedEtat, selectedNiveau, showAllSports))
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IconOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 1.dp
                        )
                    ) {
                        Text(
                            text = "Appliquer",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdownSection(
    label: String,
    icon: ImageVector,
    selectedValue: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    selectedKey: Any?,
    displayMap: Map<String, String>?,
    iconColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Label avec icône
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkGray
            )
        }
        
        // Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                leadingIcon = {
                    if (selectedKey != null) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = iconColor,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    focusedTextColor = TextDarkGray,
                    unfocusedTextColor = if (selectedKey != null) TextDarkGray else Color(0xFF9E9E9E)
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    fontWeight = if (selectedKey != null) FontWeight.Medium else FontWeight.Normal
                )
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.background(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            ) {
                options.forEach { option ->
                    val isSelected = when {
                        displayMap != null -> {
                            val key = displayMap.entries.find { it.value == option }?.key
                            selectedKey == key
                        }
                        else -> {
                            when (option) {
                                "Tous les sports" -> selectedKey == true
                                "Sport de mon enfant uniquement" -> selectedKey == false
                                else -> false
                            }
                        }
                    }
                    
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) iconColor else TextDarkGray
                                )
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onOptionSelected(option)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = if (isSelected) iconColor else TextDarkGray
                        ),
                        modifier = Modifier.background(
                            color = if (isSelected) iconColor.copy(alpha = 0.1f) else Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
