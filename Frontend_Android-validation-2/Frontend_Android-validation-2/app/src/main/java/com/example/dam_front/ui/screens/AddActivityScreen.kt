package com.example.dam_front.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.dam_front.repository.GeminiRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AutoAwesome
import android.widget.Toast
import android.util.Log
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityScreen(
    onBackClick: () -> Unit,
    onSaveClick: (ActivityFormData) -> Unit = {}
) {
    var nomActivite by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var categorie by remember { mutableStateOf("") }
    var duree by remember { mutableStateOf(60) } // en minutes
    var capaciteMax by remember { mutableStateOf(20) }
    var prix by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedTime by remember { mutableStateOf<Calendar?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }
    
    val scope = rememberCoroutineScope()
    var isGeneratingAI by remember { mutableStateOf(false) }
    val geminiRepository = remember { GeminiRepository() }
    val context = LocalContext.current
    
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    val dateString = selectedDate?.let { dateFormatter.format(it.time) } ?: ""
    val timeString = selectedTime?.let { timeFormatter.format(it.time) } ?: ""
    
    val categories = listOf(
        "Football",
        "Basketball",
        "Tennis",
        "Natation",
        "Athlétisme",
        "Volleyball",
        "Handball",
        "Judo",
        "Karate",
        "Gymnastique",
        "Autre"
    )
    
    var expanded by remember { mutableStateOf(false) }
    
    var nomActiviteError by remember { mutableStateOf<String?>(null) }
    var categorieError by remember { mutableStateOf<String?>(null) }
    var prixError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var isValid = true
        if (nomActivite.isBlank()) {
            nomActiviteError = "Le nom de l'activité est requis"
            isValid = false
        } else {
            nomActiviteError = null
        }

        if (categorie.isBlank()) {
            categorieError = "La catégorie est requise"
            isValid = false
        } else {
            categorieError = null
        }

        if (prix.isBlank()) {
            prixError = "Le prix est requis"
            isValid = false
        } else if (prix.toDoubleOrNull() == null) {
            prixError = "Le prix doit être un nombre valide"
            isValid = false
        } else {
            prixError = null
        }

        if (selectedDate == null) {
            dateError = "La date est requise"
            isValid = false
        } else {
            dateError = null
        }

        if (selectedTime == null) {
            timeError = "L'heure est requise"
            isValid = false
        } else {
            timeError = null
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
                                text = "Ajouter une activité",
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
                        text = "Nouveau • Formulaire",
                        backgroundColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    )
                }
            }
        }
        
        // Form Content
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

            // Nom de l'activité
            OutlinedTextField(
                value = nomActivite,
                onValueChange = { 
                    nomActivite = it
                    if (it.isNotBlank()) nomActiviteError = null
                },
                label = { Text("Nom de l'activité") },
                isError = nomActiviteError != null,
                supportingText = nomActiviteError?.let { { Text(it) } },
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
            
            // Description with AI Button
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Description",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkGray
                    )
                    
                    if (nomActivite.isNotBlank() && categorie.isNotBlank()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isGeneratingAI = true
                                    try {
                                        val generated = geminiRepository.generateActivityDescription(nomActivite, categorie)
                                        description = generated
                                        Toast.makeText(context, "Description générée avec succès !", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Log.e("AddActivity", "AI Generation Error", e)
                                        Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isGeneratingAI = false
                                    }
                                }
                            },
                            enabled = !isGeneratingAI,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SportyTeal.copy(alpha = 0.1f),
                                contentColor = SportyTeal
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            if (isGeneratingAI) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SportyTeal
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Générer avec IA", fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Décrivez l'activité ou utilisez l'IA...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        focusedLabelColor = SportyDarkBlue
                    )
                )
            }
            
            // Catégorie - Menu déroulant
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = categorie,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Catégorie") },
                    isError = categorieError != null,
                    supportingText = categorieError?.let { { Text(it) } },
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
                        errorBorderColor = Color.Red,
                        errorLabelColor = Color.Red,
                        errorSupportingTextColor = Color.Red
                    )
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(CardWhite)
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                categorie = category
                                categorieError = null
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextDarkGray
                            )
                        )
                    }
                }
            }
            
            // Durée - Stepper
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
                        text = "Durée (minutes)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bouton -
                        IconButton(
                            onClick = { if (duree > 15) duree -= 15 },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IconOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Diminuer",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Valeur
                        Text(
                            text = "$duree min",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkGray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        // Bouton +
                        IconButton(
                            onClick = { duree += 15 },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IconOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Augmenter",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Capacité - Stepper
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
                        text = "Capacité maximale",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDarkGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bouton -
                        IconButton(
                            onClick = { if (capaciteMax > 1) capaciteMax-- },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IconOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Diminuer",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Valeur
                        Text(
                            text = "$capaciteMax personnes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkGray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        // Bouton +
                        IconButton(
                            onClick = { capaciteMax++ },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IconOrange)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Augmenter",
                                tint = TextWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Prix
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
            
            // Date - DatePicker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                OutlinedTextField(
                    value = dateString,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Date") },
                    isError = dateError != null,
                    supportingText = dateError?.let { { Text(it) } },
                    placeholder = { Text("Sélectionner une date") },
                    trailingIcon = {
                        CalendarIcon(tint = SportyOrange, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        disabledBorderColor = if (dateError != null) Color.Red else SportyDivider,
                        disabledTextColor = TextDarkGray,
                        disabledLabelColor = if (dateError != null) Color.Red else TextLightGray
                    )
                )
            }
            
            // Heure - TimePicker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTimePicker = true }
            ) {
                OutlinedTextField(
                    value = timeString,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Heure") },
                    isError = timeError != null,
                    supportingText = timeError?.let { { Text(it) } },
                    placeholder = { Text("Sélectionner une heure") },
                    trailingIcon = {
                        ClockIcon(tint = SportyOrange, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        disabledBorderColor = if (timeError != null) Color.Red else SportyDivider,
                        disabledTextColor = TextDarkGray,
                        disabledLabelColor = if (timeError != null) Color.Red else TextLightGray
                    )
                )
            }
            
            // DatePicker Dialog
            if (showDatePicker) {
                key(showDatePicker, selectedDate) {
                    val initialDateMillis = selectedDate?.timeInMillis ?: System.currentTimeMillis()
                    val dialogDatePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = initialDateMillis,
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                                // Only allow dates from today (ignoring time) onwards
                                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                                calendar.set(Calendar.HOUR_OF_DAY, 0)
                                calendar.set(Calendar.MINUTE, 0)
                                calendar.set(Calendar.SECOND, 0)
                                calendar.set(Calendar.MILLISECOND, 0)
                                return utcTimeMillis >= calendar.timeInMillis
                            }
                        }
                    )
                    
                    AlertDialog(
                        onDismissRequest = { showDatePicker = false },
                        title = { 
                            Text(
                                "Sélectionner la date",
                                color = TextDarkGray,
                                fontWeight = FontWeight.Bold
                              ) 
                        },
                        text = {
                            DatePicker(state = dialogDatePickerState)
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    dialogDatePickerState.selectedDateMillis?.let { millis ->
                                        val calendar = Calendar.getInstance()
                                        calendar.timeInMillis = millis
                                        selectedDate = calendar
                                        dateError = null
                                    }
                                    showDatePicker = false
                                }
                            ) {
                                Text("OK", color = HeaderBlue, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) {
                                Text("Annuler", color = TextLightGray)
                            }
                        },
                        containerColor = CardWhite
                    )
                }
            }
            
            // TimePicker Dialog
            if (showTimePicker) {
                key(showTimePicker, selectedTime) {
                    val currentTime = selectedTime ?: Calendar.getInstance()
                    val dialogTimePickerState = rememberTimePickerState(
                        initialHour = currentTime.get(Calendar.HOUR_OF_DAY),
                        initialMinute = currentTime.get(Calendar.MINUTE)
                    )
                    
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { 
                            Text(
                                "Sélectionner l'heure",
                                color = TextDarkGray,
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        text = {
                            TimePicker(state = dialogTimePickerState)
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val calendar = Calendar.getInstance()
                                    calendar.set(Calendar.HOUR_OF_DAY, dialogTimePickerState.hour)
                                    calendar.set(Calendar.MINUTE, dialogTimePickerState.minute)
                                    calendar.set(Calendar.SECOND, 0)
                                    calendar.set(Calendar.MILLISECOND, 0)
                                    selectedTime = calendar
                                    timeError = null
                                    showTimePicker = false
                                }
                            ) {
                                Text("OK", color = HeaderBlue, fontWeight = FontWeight.SemiBold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) {
                                Text("Annuler", color = TextLightGray)
                            }
                        },
                        containerColor = CardWhite
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bouton Enregistrer - Modern Gradient
            SportyGradientButton(
                text = "Enregistrer l'activité",
                onClick = {
                    if (validate()) {
                        onSaveClick(
                            ActivityFormData(
                                nomActivite = nomActivite,
                                description = description,
                                categorie = categorie,
                                duree = duree,
                                capaciteMax = capaciteMax,
                                prix = prix.toDoubleOrNull() ?: 0.0,
                                date = dateString,
                                heure = timeString,
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

data class ActivityFormData(
    val nomActivite: String,
    val description: String,
    val categorie: String,
    val duree: Int,
    val capaciteMax: Int,
    val prix: Double,
    val date: String,
    val heure: String,
    val imageUri: Uri? = null,
    val image: String? = null
)

