package com.example.dam_front.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dam_front.models.Participant
import com.example.dam_front.models.Tournoi
import com.example.dam_front.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditParticipantDialog(
    participant: Participant,
    tournoi: Tournoi,
    onDismiss: () -> Unit,
    onSave: (Participant) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // États du formulaire pré-remplis avec les données du participant
    var enfantPrenom by remember { mutableStateOf(participant.enfantPrenom) }
    var enfantNom by remember { mutableStateOf(participant.enfantNom) }
    var enfantDateNaissance by remember { 
        mutableStateOf(
            participant.enfantDateNaissance?.let { formatDateForDisplay(it) } ?: ""
        )
    }
    var parentPrenom by remember { mutableStateOf(participant.parentPrenom) }
    var parentNom by remember { mutableStateOf(participant.parentNom) }
    var parentTelephone by remember { mutableStateOf(participant.parentTelephone ?: "") }
    var besoinsParticuliers by remember { mutableStateOf(participant.besoinsParticuliers ?: "") }
    
    val montantInscription = participant.montantInscription ?: tournoi.fraisParticipation ?: 0.0
    
    // Date picker pour la date de naissance
    val calendar = Calendar.getInstance()
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Parser la date existante pour initialiser le DatePicker
    val initialDate = participant.enfantDateNaissance?.let { dateStr ->
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            format.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    } ?: Calendar.getInstance().time
    
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
            enfantDateNaissance = dateFormat.format(selectedDate.time)
        },
        Calendar.getInstance().apply { time = initialDate }.get(Calendar.YEAR),
        Calendar.getInstance().apply { time = initialDate }.get(Calendar.MONTH),
        Calendar.getInstance().apply { time = initialDate }.get(Calendar.DAY_OF_MONTH)
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HeaderBlue)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifier le participant",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = Color.White
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Informations de l'enfant
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Informations de l'enfant",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue
                            )
                            
                            OutlinedTextField(
                                value = enfantPrenom,
                                onValueChange = { enfantPrenom = it },
                                label = { Text("Prénom") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = IconGreen)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconGreen,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                )
                            )
                            
                            OutlinedTextField(
                                value = enfantNom,
                                onValueChange = { enfantNom = it },
                                label = { Text("Nom") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = IconGreen)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconGreen,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                )
                            )
                            
                            OutlinedTextField(
                                value = enfantDateNaissance,
                                onValueChange = { },
                                label = { Text("Date de naissance") },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = IconOrange)
                                },
                                readOnly = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconOrange,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.CalendarToday, contentDescription = "Sélectionner date", tint = IconOrange)
                                    }
                                }
                            )
                        }
                    }
                    
                    // Informations du parent
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Informations du parent / tuteur",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue
                            )
                            
                            OutlinedTextField(
                                value = parentPrenom,
                                onValueChange = { parentPrenom = it },
                                label = { Text("Prénom") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = IconGreen)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconGreen,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                )
                            )
                            
                            OutlinedTextField(
                                value = parentNom,
                                onValueChange = { parentNom = it },
                                label = { Text("Nom") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = IconGreen)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconGreen,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                )
                            )
                            
                            OutlinedTextField(
                                value = parentTelephone,
                                onValueChange = { parentTelephone = it },
                                label = { Text("Numéro de téléphone") },
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = IconGreen)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconGreen,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    // Options supplémentaires
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Options supplémentaires",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeaderBlue
                            )
                            
                            OutlinedTextField(
                                value = "$montantInscription TND",
                                onValueChange = { },
                                label = { Text("Montant d'inscription (TND)") },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IconGreen)
                                },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = IconGreen.copy(alpha = 0.5f),
                                    disabledContainerColor = IconGreenLight.copy(alpha = 0.3f)
                                )
                            )
                            
                            OutlinedTextField(
                                value = besoinsParticuliers,
                                onValueChange = { besoinsParticuliers = it },
                                label = { Text("Besoins particuliers") },
                                leadingIcon = {
                                    Icon(Icons.Default.Description, contentDescription = null, tint = IconOrange)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconOrange,
                                    unfocusedBorderColor = TextBlueLight.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Boutons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(
                                    width = 2.dp,
                                    color = HeaderBlue,
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HeaderBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Annuler",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (enfantPrenom.isBlank() || enfantNom.isBlank() || enfantDateNaissance.isBlank() || parentTelephone.isBlank()) {
                                    Toast.makeText(context, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                
                                // Convertir la date au format ISO
                                val dateFormatInput = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
                                val dateFormatOutput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val dateNaissanceISO = try {
                                    val date = dateFormatInput.parse(enfantDateNaissance)
                                    dateFormatOutput.format(date ?: Date())
                                } catch (e: Exception) {
                                    participant.enfantDateNaissance ?: ""
                                }
                                
                                val updatedParticipant = Participant(
                                    id = participant.id,
                                    enfantPrenom = enfantPrenom,
                                    enfantNom = enfantNom,
                                    enfantDateNaissance = dateNaissanceISO,
                                    parentPrenom = parentPrenom,
                                    parentNom = parentNom,
                                    parentTelephone = parentTelephone,
                                    montantInscription = montantInscription,
                                    besoinsParticuliers = besoinsParticuliers.takeIf { it.isNotBlank() },
                                    tournoiId = participant.tournoiId
                                )
                                
                                onSave(updatedParticipant)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IconOrange
                            ),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enregistrer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        datePicker.show()
        showDatePicker = false
    }
}

fun formatDateForDisplay(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        val date = inputFormat.parse(dateString)
        date?.let { outputFormat.format(it) } ?: dateString
    } catch (e: Exception) {
        dateString
    }
}

