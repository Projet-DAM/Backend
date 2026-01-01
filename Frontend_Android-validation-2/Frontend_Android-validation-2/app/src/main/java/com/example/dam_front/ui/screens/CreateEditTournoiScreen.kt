package com.example.dam_front.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.SportType
import com.example.dam_front.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTournoiScreen(
    tournoi: Tournoi? = null,
    onSave: (Tournoi, android.net.Uri?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    
    // Initialiser les valeurs depuis le tournoi existant ou avec des valeurs par défaut
    var nom by remember { mutableStateOf(tournoi?.nom ?: "") }
    var sport by remember { mutableStateOf(tournoi?.sport ?: "") }
    var selectedSportType by remember { mutableStateOf<SportType?>(null) }
    var lieu by remember { mutableStateOf(tournoi?.lieu ?: "") }
    
    // Initialiser le sport sélectionné depuis le tournoi existant
    LaunchedEffect(tournoi?.sport) {
        if (tournoi?.sport != null) {
            selectedSportType = SportType.fromValue(tournoi.sport!!)
        }
    }
    
    // Formater les dates pour l'affichage (extraire juste la date si c'est un format ISO)
    val initialDateDebut = remember(tournoi?.dateDebut) {
        tournoi?.dateDebut?.let { dateStr ->
            try {
                // Si c'est un format ISO avec timezone, extraire juste la date
                if (dateStr.contains("T")) {
                    dateStr.substring(0, 10) // Prendre yyyy-MM-dd
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        } ?: ""
    }
    var dateDebut by remember { mutableStateOf(initialDateDebut) }
    
    // Debug: Log pour vérifier l'état du bouton
    LaunchedEffect(nom, dateDebut) {
        val isEnabled = nom.isNotBlank() && dateDebut.isNotBlank()
        android.util.Log.d("CreateEditTournoi", "🔘 État bouton: enabled=$isEnabled, nom='$nom', dateDebut='$dateDebut'")
    }
    
    val initialDateFin = remember(tournoi?.dateFin) {
        tournoi?.dateFin?.let { dateStr ->
            try {
                if (dateStr.contains("T")) {
                    dateStr.substring(0, 10)
                } else {
                    dateStr
                }
            } catch (e: Exception) {
                dateStr
            }
        } ?: ""
    }
    var dateFin by remember { mutableStateOf(initialDateFin) }
    
    var description by remember { mutableStateOf(tournoi?.description ?: "") }
    var categorieAge by remember { mutableStateOf(tournoi?.categorieAge ?: "") }
    var nombreParticipantsMax by remember { mutableStateOf(tournoi?.nombreParticipantsMax?.toString() ?: "") }
    var fraisParticipation by remember { mutableStateOf(tournoi?.fraisParticipation?.toString() ?: "") }
    var etat by remember { mutableStateOf(tournoi?.etat ?: "") }
    var niveau by remember { mutableStateOf(tournoi?.niveau ?: "") }
    
    // Initialiser l'image : si c'est un URI local, le parser
    var imageUri by remember(tournoi?.image) { 
        mutableStateOf<android.net.Uri?>(
            tournoi?.image?.takeIf { it.startsWith("content://") }?.let { 
                android.net.Uri.parse(it) 
            }
        )
    }
    var imagePath by remember(tournoi?.image) { 
        mutableStateOf<String?>(tournoi?.image) 
    }
    
    var showDateDebutPicker by remember { mutableStateOf(false) }
    var showDateFinPicker by remember { mutableStateOf(false) }
    var showEtatDropdown by remember { mutableStateOf(false) }
    var showNiveauDropdown by remember { mutableStateOf(false) }
    var showSportDropdown by remember { mutableStateOf(false) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Valeurs exactes attendues par le backend (en minuscules avec accents)
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
    
    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            imageUri = it
            imagePath = it.toString()
        }
    }
    
    // Map Picker Launcher
    val mapPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("address")
            address?.let {
                lieu = it
            }
        }
    }
    
    // Date Picker pour date de début
    if (showDateDebutPicker) {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                dateDebut = dateFormat.format(calendar.time)
                showDateDebutPicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    
    // Date Picker pour date de fin
    if (showDateFinPicker) {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                dateFin = dateFormat.format(calendar.time)
                showDateFinPicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header avec titre coloré
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (tournoi != null) "Modifier le tournoi" else "Créer un tournoi",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = HeaderBlue
            )
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Fermer", tint = TextDarkGray)
            }
        }
        
        // Image Picker
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    imageUri != null -> {
                        // Nouvelle image sélectionnée
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Image du tournoi",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    imagePath != null && imagePath!!.startsWith("content://") -> {
                        // Image existante avec URI local
                        AsyncImage(
                            model = android.net.Uri.parse(imagePath!!),
                            contentDescription = "Image du tournoi",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    imagePath != null && (imagePath!!.startsWith("http://") || imagePath!!.startsWith("https://")) -> {
                        // Image existante avec URL HTTP
                        AsyncImage(
                            model = imagePath,
                            contentDescription = "Image du tournoi",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    imagePath != null -> {
                        // Image existante avec chemin relatif
                        AsyncImage(
                            model = com.example.dam_front.utils.ImageUtils.buildImageUrl(imagePath),
                            contentDescription = "Image du tournoi",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        // Pas d'image
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = IconGreen
                            )
                            Text(
                                text = "Cliquez pour ajouter une image",
                                color = TextBlueLight,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Champs du formulaire
        InfoField(
            label = "Nom du tournoi",
            value = nom,
            onValueChange = { nom = it },
            leadingIcon = Icons.Default.EmojiEvents,
            iconColor = IconOrange
        )
        
        // Champ Sport avec dropdown
        SportDropdownField(
            label = "Sport",
            selectedSport = selectedSportType,
            onSportSelected = { sportType ->
                selectedSportType = sportType
                sport = sportType.value
            },
            expanded = showSportDropdown,
            onExpandedChange = { showSportDropdown = it },
            iconColor = IconGreen
        )
        
        LocationField(
            label = "Lieu",
            value = lieu,
            onValueChange = { lieu = it },
            onMapClick = {
                val intent = Intent(context, com.example.dam_front.ui.map.MapPickerActivity::class.java)
                mapPickerLauncher.launch(intent)
            },
            iconColor = IconOrange
        )
        
        // Date de début avec DatePicker
        DateField(
            label = "Date de début",
            value = dateDebut,
            onValueChange = { dateDebut = it },
            onClick = { showDateDebutPicker = true },
            iconColor = IconGreen
        )
        
        // Date de fin avec DatePicker
        DateField(
            label = "Date de fin",
            value = dateFin,
            onValueChange = { dateFin = it },
            onClick = { showDateFinPicker = true },
            iconColor = IconOrange
        )
        
        // Dropdown État
        DropdownField(
            label = "État",
            value = etatsDisplay[etat] ?: etat,
            options = etats.map { etatsDisplay[it] ?: it },
            expanded = showEtatDropdown,
            onExpandedChange = { showEtatDropdown = it },
            onValueChange = { displayValue ->
                // Trouver la clé correspondante à la valeur d'affichage
                val key = etatsDisplay.entries.find { it.value == displayValue }?.key ?: displayValue
                etat = key
            },
            iconColor = IconGreen
        )
        
        // Dropdown Niveau
        DropdownField(
            label = "Niveau",
            value = niveauxDisplay[niveau] ?: niveau,
            options = niveaux.map { niveauxDisplay[it] ?: it },
            expanded = showNiveauDropdown,
            onExpandedChange = { showNiveauDropdown = it },
            onValueChange = { displayValue ->
                // Trouver la clé correspondante à la valeur d'affichage
                val key = niveauxDisplay.entries.find { it.value == displayValue }?.key ?: displayValue
                niveau = key
            },
            iconColor = IconOrange
        )
        
        InfoField(
            label = "Description",
            value = description,
            onValueChange = { description = it },
            leadingIcon = Icons.Default.Description,
            maxLines = 3,
            iconColor = IconGreen
        )
        
        InfoField(
            label = "Catégorie d'âge",
            value = categorieAge,
            onValueChange = { categorieAge = it },
            leadingIcon = Icons.Default.People,
            iconColor = IconOrange
        )
        
        InfoField(
            label = "Nombre de participants max",
            value = nombreParticipantsMax,
            onValueChange = { nombreParticipantsMax = it },
            leadingIcon = Icons.Default.Group,
            keyboardType = KeyboardType.Number,
            iconColor = IconGreen
        )
        
        InfoField(
            label = "Frais de participation",
            value = fraisParticipation,
            onValueChange = { fraisParticipation = it },
            leadingIcon = Icons.Default.AttachMoney,
            keyboardType = KeyboardType.Decimal,
            iconColor = IconOrange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Boutons d'action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    android.util.Log.d("CreateEditTournoi", "🔵 BOUTON CLIQUE - Début traitement")
                    
                    // Formater les dates au format ISO si nécessaire
                    val formattedDateDebut = if (dateDebut.isNotBlank()) {
                        if (dateDebut.contains("T")) {
                            dateDebut // Déjà au format ISO
                        } else {
                            // Convertir yyyy-MM-dd en ISO format
                            "${dateDebut}T00:00:00.000Z"
                        }
                    } else null
                    
                    val formattedDateFin = if (dateFin.isNotBlank()) {
                        if (dateFin.contains("T")) {
                            dateFin // Déjà au format ISO
                        } else {
                            "${dateFin}T00:00:00.000Z"
                        }
                    } else null
                    
                    // Utiliser l'URI si une nouvelle image a été sélectionnée, sinon garder l'ancien chemin
                    // IMPORTANT: Ne pas envoyer les URIs content:// au backend car ils ne sont pas accessibles
                    // Si une nouvelle image est sélectionnée, on devrait l'uploader au serveur
                    // Pour l'instant, on envoie null si c'est un URI content:// (sera uploadé séparément plus tard)
                    val finalImagePath = when {
                        imageUri != null -> {
                            val uriString = imageUri.toString()
                            if (uriString.startsWith("content://")) {
                                // URI local - ne pas l'envoyer au backend pour l'instant
                                // TODO: Implémenter l'upload d'image vers le serveur
                                android.util.Log.w("CreateEditTournoi", "⚠️ URI content:// détecté, ne sera pas envoyé au backend")
                                null // Ne pas envoyer l'URI content://
                            } else {
                                uriString
                            }
                        }
                        else -> {
                            val currentImagePath = imagePath
                            if (currentImagePath != null && !currentImagePath.startsWith("content://")) {
                                currentImagePath
                            } else {
                                null
                            }
                        }
                    }
                    
                    // Convertir les nombres - TOUJOURS inclure les valeurs existantes lors de l'édition
                    // Si le champ est vide lors de l'édition, garder la valeur existante pour qu'elle soit envoyée au backend
                    val nombreParticipantsMaxInt = if (nombreParticipantsMax.isBlank() && tournoi != null) {
                        // Si le champ est vide mais qu'on édite un tournoi existant, garder la valeur existante
                        tournoi.nombreParticipantsMax
                    } else if (nombreParticipantsMax.isNotBlank()) {
                        // Si une nouvelle valeur est entrée, la convertir
                        nombreParticipantsMax.toIntOrNull()?.takeIf { it > 0 }
                    } else {
                        // Si vide et pas de tournoi existant, null
                        null
                    }
                    
                    val fraisParticipationDouble = if (fraisParticipation.isBlank() && tournoi != null) {
                        // Si le champ est vide mais qu'on édite un tournoi existant, garder la valeur existante
                        tournoi.fraisParticipation
                    } else if (fraisParticipation.isNotBlank()) {
                        // Si une nouvelle valeur est entrée, la convertir
                        fraisParticipation.toDoubleOrNull()?.takeIf { it >= 0 }
                    } else {
                        // Si vide et pas de tournoi existant, null
                        null
                    }
                    
                    android.util.Log.d("CreateEditTournoi", "Conversion des nombres:")
                    android.util.Log.d("CreateEditTournoi", "  nombreParticipantsMax: '$nombreParticipantsMax' -> $nombreParticipantsMaxInt (existant: ${tournoi?.nombreParticipantsMax})")
                    android.util.Log.d("CreateEditTournoi", "  fraisParticipation: '$fraisParticipation' -> $fraisParticipationDouble (existant: ${tournoi?.fraisParticipation})")
                    
                    val newTournoi = Tournoi(
                        id = tournoi?.id,
                        nom = nom,
                        sport = sport,
                        lieu = lieu,
                        dateDebut = formattedDateDebut,
                        dateFin = formattedDateFin,
                        description = description,
                        categorieAge = categorieAge,
                        nombreParticipantsMax = nombreParticipantsMaxInt,
                        fraisParticipation = fraisParticipationDouble,
                        etat = etat.takeIf { it.isNotBlank() },
                        niveau = niveau.takeIf { it.isNotBlank() },
                        image = finalImagePath
                    )
                    
                    android.util.Log.d("CreateEditTournoi", "📦 Données préparées:")
                    android.util.Log.d("CreateEditTournoi", "  - ID: ${newTournoi.id}")
                    android.util.Log.d("CreateEditTournoi", "  - Nom: ${newTournoi.nom}")
                    android.util.Log.d("CreateEditTournoi", "  - Sport: ${newTournoi.sport}")
                    android.util.Log.d("CreateEditTournoi", "  - Image: ${newTournoi.image}")
                    android.util.Log.d("CreateEditTournoi", "  - État: ${newTournoi.etat}")
                    android.util.Log.d("CreateEditTournoi", "  - Niveau: ${newTournoi.niveau}")
                    android.util.Log.d("CreateEditTournoi", "  - Mode: ${if (tournoi != null) "UPDATE" else "CREATE"}")
                    
                    android.util.Log.d("CreateEditTournoi", "🚀 Appel de onSave...")
                    // Passer l'imageUri si c'est une nouvelle image (content://)
                    val imageUriToPass = if (imageUri != null && imageUri.toString().startsWith("content://")) {
                        imageUri
                    } else {
                        null
                    }
                    onSave(newTournoi, imageUriToPass)
                    android.util.Log.d("CreateEditTournoi", "✅ onSave appelé")
                },
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (!nom.isNotBlank() || !dateDebut.isNotBlank()) {
                            Modifier
                        } else {
                            Modifier
                        }
                    ),
                enabled = nom.isNotBlank() && dateDebut.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IconGreen,
                    disabledContainerColor = IconGreen.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.7f)
                )
            ) {
                if (nom.isNotBlank() && dateDebut.isNotBlank()) {
                    Text(
                        if (tournoi != null) "Mettre à jour" else "Créer",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        if (nom.isBlank()) "Nom requis" else "Date requise",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = IconOrange
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, IconOrange)
            ) {
                Text("Annuler", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxLines: Int = 1,
    iconColor: Color = HeaderBlue
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { 
            Icon(
                leadingIcon, 
                contentDescription = null,
                tint = iconColor
            ) 
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor,
            unfocusedBorderColor = iconColor.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8F9FA),
            focusedLabelColor = iconColor,
            unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f)
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = TextDarkGray
        )
    )
}

@Composable
fun LocationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onMapClick: () -> Unit,
    iconColor: Color = IconOrange
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { 
            Icon(
                Icons.Default.LocationOn, 
                contentDescription = null,
                tint = iconColor
            ) 
        },
        trailingIcon = {
            IconButton(onClick = onMapClick) {
                Icon(
                    Icons.Default.Explore,
                    contentDescription = "Ouvrir la carte",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        maxLines = 2,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor,
            unfocusedBorderColor = iconColor.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8F9FA),
            focusedLabelColor = iconColor,
            unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f)
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = TextDarkGray
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onClick: () -> Unit,
    iconColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 14.sp) },
        leadingIcon = { 
            Icon(
                Icons.Default.CalendarToday, 
                contentDescription = null,
                tint = iconColor
            ) 
        },
        trailingIcon = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Ouvrir le calendrier",
                tint = iconColor,
                modifier = Modifier.clickable { onClick() }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        enabled = false,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor,
            unfocusedBorderColor = iconColor.copy(alpha = 0.5f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF8F9FA),
            focusedLabelColor = iconColor,
            unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f),
            disabledBorderColor = iconColor.copy(alpha = 0.5f),
            disabledContainerColor = Color(0xFFF8F9FA)
        ),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 14.sp,
            color = TextDarkGray
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    iconColor: Color
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 14.sp) },
            leadingIcon = { 
                Icon(
                    Icons.Default.ArrowDropDown, 
                    contentDescription = null,
                    tint = iconColor
                ) 
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = iconColor,
                unfocusedBorderColor = iconColor.copy(alpha = 0.5f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8F9FA),
                focusedLabelColor = iconColor,
                unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = TextDarkGray
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(Color.White)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        onExpandedChange(false)
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = TextDarkGray
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportDropdownField(
    label: String,
    selectedSport: SportType?,
    onSportSelected: (SportType) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    iconColor: Color
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedSport?.let { SportType.getDisplayName(it.value) } ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 14.sp) },
            leadingIcon = { 
                Icon(
                    Icons.Default.SportsSoccer, 
                    contentDescription = null,
                    tint = iconColor
                ) 
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            placeholder = { Text("Sélectionner un sport", fontSize = 14.sp, color = TextDarkGray.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = iconColor,
                unfocusedBorderColor = iconColor.copy(alpha = 0.5f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color(0xFFF8F9FA),
                focusedLabelColor = iconColor,
                unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                color = TextDarkGray
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(Color.White)
        ) {
            SportType.entries.forEach { sportType ->
                DropdownMenuItem(
                    text = { Text(SportType.getDisplayName(sportType.value)) },
                    onClick = {
                        onSportSelected(sportType)
                        onExpandedChange(false)
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = TextDarkGray
                    )
                )
            }
        }
    }
}
