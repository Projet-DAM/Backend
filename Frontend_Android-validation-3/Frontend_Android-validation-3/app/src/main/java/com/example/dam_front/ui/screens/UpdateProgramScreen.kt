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
import androidx.compose.material.icons.filled.Edit
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
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.Activity
import com.example.dam_front.models.Program
import com.example.dam_front.models.ProgramStatus
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProgramScreen(
    program: Program,
    availableActivities: List<Activity>,
    currentUserId: String?,
    onBackClick: () -> Unit,
    onSaveClick: (ProgramFormData) -> Unit = {}
) {
    var nomProgramme by remember { mutableStateOf(program.nomProgramme) }
    var description by remember { mutableStateOf(program.description ?: "") }
    var objectif by remember { mutableStateOf(program.objectif ?: "") }
    var niveau by remember { mutableStateOf(program.niveau ?: "") }
    var prix by remember { mutableStateOf(program.prix?.toString() ?: "") }
    var statut by remember { mutableStateOf(program.statut) }
    var expanded by remember { mutableStateOf(false) }
    val selectedActivities = remember {
        mutableStateListOf<String>().apply { addAll(program.activites.map { it.id }) }
    }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
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
                        text = "Modifier le programme",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SportyDarkBlue
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        // New image selected
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else if (!program.image.isNullOrBlank()) {
                        // Existing image
                        val imageUrl = if (program.image.startsWith("http")) {
                            program.image
                        } else {
                            val cleanPath = if (program.image.startsWith("/")) program.image.substring(1) else program.image
                            "${ApiConfig.BASE_URL}$cleanPath"
                        }
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Existing Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Placeholder
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
                        }
                    }

                    // Change/Edit Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SportyOrange)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (selectedImageUri != null || !program.image.isNullOrBlank()) "Modifier" else "Ajouter",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
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
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
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
                    focusedLabelColor = SportyDarkBlue,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
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
                    focusedLabelColor = SportyDarkBlue,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
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
                        focusedLabelColor = SportyDarkBlue,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
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
                        focusedLabelColor = SportyDarkBlue,
                         focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Activités associées",
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
                    if (availableActivities.isEmpty()) {
                        Text(
                            text = "Aucune activité supplémentaire n'est disponible. Libérez une activité d'un autre programme ou créez-en une nouvelle pour l'ajouter ici.",
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
                                focusedLabelColor = SportyDarkBlue,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
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
                                    focusedLabelColor = SportyDarkBlue,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
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
                text = "Mettre à jour le programme",
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
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Update",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}


