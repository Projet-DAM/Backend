package com.example.dam_front.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import coil.compose.AsyncImage
import com.example.dam_front.models.Activity
import com.example.dam_front.models.ProgramStatus
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProgramScreen(
    availableActivities: List<Activity>,
    currentUserId: String?,
    onBackClick: () -> Unit,
    onSaveClick: (ProgramFormData) -> Unit = {}
) {
    var nomProgramme by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var objectif by remember { mutableStateOf("") }
    var niveau by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("") }
    var statut by remember { mutableStateOf(ProgramStatus.BROUILLON.name) }
    var expanded by remember { mutableStateOf(false) }
    val selectedActivities = remember { mutableStateListOf<String>() }

    // Update price when selected activities change
    LaunchedEffect(selectedActivities.toList()) {
        val total = selectedActivities.sumOf { activityId ->
            availableActivities.find { it.id == activityId }?.prix ?: 0.0
        }
        prix = if (total == 0.0) "" else if (total % 1.0 == 0.0) {
            total.toInt().toString()
        } else {
            total.toString()
        }
    }

    var activitySearchQuery by remember { mutableStateOf("") }
    var activitySortExpanded by remember { mutableStateOf(false) }
    var activitySortOption by remember { mutableStateOf(ActivitySortOption.NAME_ASC) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    BackHandler(onBack = onBackClick)

    var nomProgrammeError by remember { mutableStateOf<String?>(null) }
    var prixError by remember { mutableStateOf<String?>(null) }
    var activitiesError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (nomProgramme.isBlank()) {
            nomProgrammeError = "Le nom du programme est requis"
            isValid = false
        } else {
            nomProgrammeError = null
        }

        if (prix.isNotBlank() && prix.toDoubleOrNull() == null) {
            prixError = "Le prix doit être un nombre valide"
            isValid = false
        } else {
            prixError = null
        }

        if (selectedActivities.isEmpty()) {
            activitiesError = "Veuillez sélectionner au moins une activité"
            isValid = false
        } else {
            activitiesError = null
        }

        return isValid
    }

    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(SportyBackgroundTop, SportyBackgroundBottom))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Modern Header Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(SportyDarkBlue, SportyTeal)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SPORTY KIDS",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Créer un programme",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Académie Sportive",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 15.sp
                            )
                        }
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            ArrowBackIcon(tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    
                    SportyInfoChip(
                        text = "Nouveau • Programme",
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Image Picker - Modern Design
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clickable {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Change badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SportyOrange)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Changer",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(SportyOrange.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Image",
                                    tint = SportyOrange,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Text(
                                text = "Ajouter une image",
                                color = TextDarkGray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Cliquez pour sélectionner",
                                color = TextLightGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = nomProgramme,
                onValueChange = { 
                    nomProgramme = it
                    if (it.isNotBlank()) nomProgrammeError = null
                },
                label = { Text("Nom du programme") },
                isError = nomProgrammeError != null,
                supportingText = nomProgrammeError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportyDarkBlue,
                    unfocusedBorderColor = SportyDivider,
                    focusedLabelColor = SportyDarkBlue,
                    errorBorderColor = Color.Red,
                    errorLabelColor = Color.Red,
                    errorSupportingTextColor = Color.Red
                )
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(16.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportyDarkBlue,
                    unfocusedBorderColor = SportyDivider,
                    focusedLabelColor = SportyDarkBlue
                )
            )

            OutlinedTextField(
                value = objectif,
                onValueChange = { objectif = it },
                label = { Text("Objectif principal") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportyDarkBlue,
                    unfocusedBorderColor = SportyDivider,
                    focusedLabelColor = SportyDarkBlue
                )
            )

            var niveauExpanded by remember { mutableStateOf(false) }
            val niveaux = listOf("Débutant", "Intermédiaire", "Avancé", "Tous niveaux")

            ExposedDropdownMenuBox(
                expanded = niveauExpanded,
                onExpandedChange = { niveauExpanded = !niveauExpanded }
            ) {
                OutlinedTextField(
                    value = niveau,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Niveau visé") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = niveauExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        focusedLabelColor = SportyDarkBlue
                    )
                )
                DropdownMenu(
                    expanded = niveauExpanded,
                    onDismissRequest = { niveauExpanded = false },
                    modifier = Modifier.background(CardWhite)
                ) {
                    niveaux.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                niveau = selectionOption
                                niveauExpanded = false
                            },
                            colors = MenuDefaults.itemColors(textColor = TextDarkGray)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = prix,
                onValueChange = { 
                    prix = it
                    if (it.isNotBlank() && it.toDoubleOrNull() != null) prixError = null
                },
                label = { Text("Prix (TND)") },
                isError = prixError != null,
                supportingText = prixError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportyDarkBlue,
                    unfocusedBorderColor = SportyDivider,
                    focusedLabelColor = SportyDarkBlue,
                    errorBorderColor = Color.Red,
                    errorLabelColor = Color.Red,
                    errorSupportingTextColor = Color.Red
                )
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = statut,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Statut") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        focusedLabelColor = SportyDarkBlue
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(CardWhite)
                ) {
                    ProgramStatus.values().forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name) },
                            onClick = {
                                statut = status.name
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(textColor = TextDarkGray)
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Sélectionner les activités",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (activitiesError != null) Color.Red else TextDarkGray
                    )
                    if (activitiesError != null) {
                        Text(
                            text = activitiesError!!,
                            fontSize = 12.sp,
                            color = Color.Red
                        )
                    }
                    Text(
                        text = "Important : vous pouvez uniquement ajouter au programme les activités que vous avez créées.",
                        fontSize = 13.sp,
                        color = SportyOrange
                    )
                    if (availableActivities.isEmpty()) {
                        Text(
                            text = "Aucune activité n'est disponible pour être associée. Créez une nouvelle activité ou libérez une activité déjà liée à un autre programme.",
                            color = TextLightGray,
                            fontSize = 14.sp
                        )
                    } else {
                        OutlinedTextField(
                            value = activitySearchQuery,
                            onValueChange = { activitySearchQuery = it },
                            label = { Text("Rechercher une activité") },
                            placeholder = { Text("Nom, catégorie...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SportyDarkBlue,
                                unfocusedBorderColor = SportyDivider,
                                focusedLabelColor = SportyDarkBlue
                            )
                        )

                        ExposedDropdownMenuBox(
                            expanded = activitySortExpanded,
                            onExpandedChange = { activitySortExpanded = !activitySortExpanded }
                        ) {
                            OutlinedTextField(
                                value = activitySortOption.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Trier les activités") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = activitySortExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SportyDarkBlue,
                                    unfocusedBorderColor = SportyDivider,
                                    focusedLabelColor = SportyDarkBlue
                                )
                            )
                            DropdownMenu(
                                expanded = activitySortExpanded,
                                onDismissRequest = { activitySortExpanded = false },
                                modifier = Modifier.background(CardWhite)
                            ) {
                                ActivitySortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            activitySortOption = option
                                            activitySortExpanded = false
                                        },
                                        colors = MenuDefaults.itemColors(textColor = TextDarkGray)
                                    )
                                }
                            }
                        }

                        val filteredActivities = availableActivities
                            .filter { activity ->
                                val query = activitySearchQuery.trim()
                                if (query.isBlank()) {
                                    true
                                } else {
                                    activity.nomActivite.contains(query, ignoreCase = true) ||
                                            (activity.categorie?.contains(query, ignoreCase = true) ?: false)
                                }
                            }
                            .sortedWith(
                                when (activitySortOption) {
                                    ActivitySortOption.NAME_ASC -> compareBy { it.nomActivite.lowercase() }
                                    ActivitySortOption.NAME_DESC -> compareByDescending { it.nomActivite.lowercase() }
                                }
                            )

                        if (filteredActivities.isEmpty()) {
                            Text(
                                text = "Aucune activité ne correspond à votre recherche.",
                                color = TextLightGray,
                                fontSize = 14.sp
                            )
                        }

                        filteredActivities.forEach { activity ->
                            val isSelected = selectedActivities.contains(activity.id)
                            val isOwner = currentUserId != null && activity.coach != null && currentUserId == activity.coach
                            val isEnabled = isOwner
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            !isEnabled -> Color(0xFFF0F0F0)
                                            isSelected -> SportyDarkBlue.copy(alpha = 0.05f)
                                            else -> Color(0xFFF8F9FA)
                                        }
                                    )
                                    .clickable(enabled = isEnabled) {
                                        if (isSelected) {
                                            selectedActivities.remove(activity.id)
                                        } else {
                                            selectedActivities.add(activity.id)
                                            activitiesError = null
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = activity.nomActivite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEnabled) TextDarkGray else TextLightGray.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = activity.categorie ?: "Catégorie non renseignée",
                                        fontSize = 13.sp,
                                        color = if (isEnabled) TextLightGray else TextLightGray.copy(alpha = 0.5f)
                                    )
                                }
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedActivities.add(activity.id)
                                            activitiesError = null
                                        } else {
                                            selectedActivities.remove(activity.id)
                                        }
                                    },
                                    enabled = isEnabled,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = SportyDarkBlue,
                                        uncheckedColor = TextLightGray
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SportyGradientButton(
                text = "Enregistrer le programme",
                onClick = {
                    if (validate()) {
                        onSaveClick(
                            ProgramFormData(
                                nomProgramme = nomProgramme,
                                description = description.ifBlank { null },
                                objectif = objectif.ifBlank { null },
                                niveau = niveau.ifBlank { null },
                                prix = prix.toDoubleOrNull(),
                                statut = statut,
                                activites = selectedActivities.toList(),
                                imageUri = selectedImageUri
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

data class ProgramFormData(
    val nomProgramme: String,
    val description: String?,
    val objectif: String?,
    val niveau: String?,
    val prix: Double?,
    val statut: String,
    val activites: List<String>,
    val imageUri: Uri? = null
)

enum class ActivitySortOption(val label: String) {
    NAME_ASC("Nom (A-Z)"),
    NAME_DESC("Nom (Z-A)")
}


