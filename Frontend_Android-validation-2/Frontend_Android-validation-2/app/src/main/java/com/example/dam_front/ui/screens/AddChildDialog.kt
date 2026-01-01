package com.example.dam_front.ui.screens

import android.app.DatePickerDialog
import android.widget.ArrayAdapter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import coil.compose.AsyncImage
import com.example.dam_front.models.SportType
import com.example.dam_front.repository.ChildRepository
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onChildAdded: () -> Unit
) {
    val context = LocalContext.current
    val childRepository = remember { ChildRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var prenom by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf<String?>(null) }
    var selectedSport by remember { mutableStateOf<SportType?>(null) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    
    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Titre
                Text(
                    text = "Ajouter un enfant",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlue,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Prénom
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it },
                    label = { Text("Prénom", color = SportyKidsTeal.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyKidsOrange,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedLabelColor = SportyKidsOrange
                    )
                )
                
                // Nom
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom", color = SportyKidsTeal.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyKidsOrange,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        focusedLabelColor = SportyKidsOrange
                    )
                )
                
                // Date de naissance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)
                                    dateNaissance = dateFormat.format(calendar.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = dateNaissance ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Date de naissance", color = SportyKidsTeal.copy(alpha = 0.7f)) },
                        placeholder = { Text("YYYY-MM-DD", color = Color.Gray.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportyKidsOrange,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedLabelColor = SportyKidsOrange,
                            disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                            disabledContainerColor = Color.White
                        )
                    )
                }
                
                // Sport
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedSport?.let { SportType.getDisplayName(it.value) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sport pratiqué", color = SportyKidsTeal.copy(alpha = 0.7f)) },
                        placeholder = { Text("Sélectionner un sport", color = Color.Gray.copy(alpha = 0.5f)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SportyKidsOrange,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                            focusedLabelColor = SportyKidsOrange
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SportType.entries.forEach { sport ->
                            DropdownMenuItem(
                                text = { Text(SportType.getDisplayName(sport.value)) },
                                onClick = {
                                    selectedSport = sport
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Image de profil (optionnel)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Photo de profil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = SportyKidsOrange
                                )
                                Text(
                                    text = "Ajouter une photo de profil (optionnel)",
                                    color = TextDarkGray.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                // Message d'erreur
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Boutons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextDarkGray
                        )
                    ) {
                        Text("Annuler", fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = {
                            if (prenom.isBlank() || nom.isBlank() || dateNaissance == null || selectedSport == null) {
                                errorMessage = "Veuillez remplir tous les champs"
                            } else {
                                isLoading = true
                                errorMessage = null
                                
                                coroutineScope.launch {
                                val result = childRepository.createChild(
                                    com.example.dam_front.models.CreateChildRequest(
                                        prenom = prenom,
                                        nom = nom,
                                        dateNaissance = dateNaissance!!,
                                        // sportPratique removed from model
                                    ),
                                    imageUri = imageUri
                                )
                                
                                isLoading = false
                                
                                    result.onSuccess {
                                        onChildAdded()
                                        onDismiss()
                                    }.onFailure { exception ->
                                        val errorMsg = exception.message ?: "Erreur lors de l'ajout de l'enfant"
                                        // Vérifier si c'est une erreur 401 (token expiré)
                                        if (errorMsg.contains("401") || errorMsg.contains("Unauthorized")) {
                                            errorMessage = "Votre session a expiré. Veuillez vous reconnecter."
                                        } else {
                                            errorMessage = errorMsg
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SportyKidsOrange
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Ajouter", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

