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
import com.example.dam_front.models.Activity
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
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.repository.GeminiRepository
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.AutoAwesome
import android.widget.Toast
import android.util.Log
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateActivityScreen(
    activity: Activity,
    onBackClick: () -> Unit,
    onSaveClick: (ActivityFormData) -> Unit = {}
) {
    // Initialiser les champs avec les valeurs de l'activité
    var nomActivite by remember { mutableStateOf(activity.nomActivite) }
    var description by remember { mutableStateOf(activity.description ?: "") }
    var categorie by remember { mutableStateOf(activity.categorie ?: "") }
    var duree by remember { mutableStateOf(activity.duree ?: 60) }
    var capaciteMax by remember { mutableStateOf(activity.capaciteMax ?: 20) }
    var prix by remember { mutableStateOf(activity.prix?.toString() ?: "") }
    
    // Parser la date et l'heure depuis l'activité
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    // Parser la date en essayant différents formats ISO 8601
    var selectedDate by remember {
        mutableStateOf<Calendar>(
            try {
                val formats = listOf(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                    "yyyy-MM-dd'T'HH:mm:ssXXX",
                    "yyyy-MM-dd"
                )
                
                var parsedDate: Date? = null
                for (format in formats) {
                    try {
                        val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                        inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                        parsedDate = activity.date?.let { inputFormat.parse(it) }
                        if (parsedDate != null) break
                    } catch (e: Exception) {
                        // Try next format
                    }
                }
                
                if (parsedDate != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = parsedDate
                    calendar
                } else {
                    // Si le parsing échoue, utiliser la date actuelle
                    Calendar.getInstance()
                }
            } catch (e: Exception) {
                // Si le parsing échoue, utiliser la date actuelle
                Calendar.getInstance()
            }
        )
    }
    
    var selectedTime by remember {
        mutableStateOf<Calendar>(
            try {
                val calendar = Calendar.getInstance()
                val timeParts = activity.heure?.split(":")
                if (timeParts != null && timeParts.size >= 2) {
                    calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                    calendar.set(Calendar.MINUTE, timeParts[1].toInt())
                }
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar
            } catch (e: Exception) {
                // Si le parsing échoue, utiliser l'heure actuelle
                Calendar.getInstance()
            }
        )
    }
    
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
    
    val dateString = remember(selectedDate) { dateFormatter.format(selectedDate.time) }
    val timeString = remember(selectedTime) { timeFormatter.format(selectedTime.time) }
    
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
                        text = "Modifier l'activité",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SportyDarkBlue
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
            // Image Picker - Modern Design with existing image
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
                    val imageModel = if (selectedImageUri != null) {
                        selectedImageUri
                    } else if (!activity.image.isNullOrBlank()) {
                        val baseUrl = ApiConfig.BASE_URL.removeSuffix("/")
                        val imagePath = if (activity.image.startsWith("/")) activity.image else "/${activity.image}"
                        val fullUrl = if (activity.image.startsWith("http")) activity.image else "$baseUrl$imagePath"
                        fullUrl
                    } else {
                        null
                    }

                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = "Activity Image",
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
                                text = "Changer l'image",
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
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
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
                                        Log.e("UpdateActivity", "AI Generation Error", e)
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
                        focusedLabelColor = SportyDarkBlue,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
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
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
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
                    placeholder = { Text("Sélectionner une date") },
                    trailingIcon = {
                        CalendarIcon(tint = SportyOrange, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        disabledBorderColor = SportyDivider,
                        disabledTextColor = TextDarkGray,
                        disabledContainerColor = Color.White,
                        disabledLabelColor = TextLightGray
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
                    placeholder = { Text("Sélectionner une heure") },
                    trailingIcon = {
                        ClockIcon(tint = SportyOrange, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyDarkBlue,
                        unfocusedBorderColor = SportyDivider,
                        disabledBorderColor = SportyDivider,
                        disabledTextColor = TextDarkGray,
                        disabledContainerColor = Color.White,
                        disabledLabelColor = TextLightGray
                    )
                )
            }
            
            // DatePicker Dialog
            if (showDatePicker) {
                key(showDatePicker, selectedDate) {
                    val initialDateMillis = selectedDate.timeInMillis
                    val dialogDatePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = initialDateMillis
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
                    val dialogTimePickerState = rememberTimePickerState(
                        initialHour = selectedTime.get(Calendar.HOUR_OF_DAY),
                        initialMinute = selectedTime.get(Calendar.MINUTE)
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
            
            // Bouton Mettre à jour - Modern Gradient
            SportyGradientButton(
                text = "Mettre à jour l'activité",
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
                                imageUri = selectedImageUri,
                                image = activity.image // Transmettre l'ancienne image si aucune nouvelle n'est sélectionnée
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { 
                    EditIcon(
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }
    }
}

