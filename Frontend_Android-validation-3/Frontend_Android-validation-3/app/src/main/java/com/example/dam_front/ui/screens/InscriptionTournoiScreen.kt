package com.example.dam_front.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Tournoi
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.repository.InscriptionRepository
import com.example.dam_front.repository.ChildRepository
import com.example.dam_front.ui.components.HeaderSection
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.TokenManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InscriptionTournoiScreen(
    tournoi: Tournoi,
    onCancel: () -> Unit,
    onInscriptionSuccess: () -> Unit,
    isTournoiComplet: Boolean = false,
    onMenuClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val inscriptionRepository = remember { InscriptionRepository(context) }
    val childRepository = remember { ChildRepository(context) }
    val prefs = context.getSharedPreferences("SportyKidsPrefs", android.content.Context.MODE_PRIVATE)
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var isLoading by remember { mutableStateOf(false) }
    
    // Récupérer les informations du parent depuis TokenManager
    var parentPrenom by remember { mutableStateOf<String?>(null) }
    var parentNom by remember { mutableStateOf<String?>(null) }
    
    // États du formulaire
    var enfantPrenom by remember { mutableStateOf("") }
    var enfantNom by remember { mutableStateOf("") }
    var enfantDateNaissance by remember { mutableStateOf("") }
    var parentTelephone by remember { mutableStateOf("") }
    var besoinsParticuliers by remember { mutableStateOf("") }
    
    // Charger les informations du parent
    LaunchedEffect(Unit) {
        parentPrenom = tokenManager.getUserPrenom().first()
        parentNom = tokenManager.getUserNom().first()
        android.util.Log.d("InscriptionTournoiScreen", "✅ Informations du parent chargées: $parentPrenom $parentNom")
    }
    
    // Gestion des enfants
    var children by remember { mutableStateOf<List<ChildResponse>>(emptyList()) }
    var selectedChildId by remember { mutableStateOf<String?>(null) }
    
    // Fonction helper pour pré-remplir les champs avec les données d'un enfant
    fun fillChildData(child: ChildResponse) {
        enfantPrenom = child.prenom
        enfantNom = child.nom
        
        // Convertir la date de naissance au format attendu (YYYY-MM-DD -> dd MMM yyyy)
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
            val date = inputFormat.parse(child.dateNaissance)
            if (date != null) {
                enfantDateNaissance = outputFormat.format(date)
            } else {
                enfantDateNaissance = child.dateNaissance
            }
        } catch (e: Exception) {
            enfantDateNaissance = child.dateNaissance
        }
        
        android.util.Log.d("InscriptionTournoiScreen", "✅ Champs pré-remplis: ${child.prenom} ${child.nom}, né(e) le $enfantDateNaissance")
    }
    
    // Charger les enfants au démarrage
    LaunchedEffect(Unit) {
        childRepository.getChildren().onSuccess { childrenList ->
            children = childrenList
            android.util.Log.d("InscriptionTournoiScreen", "✅ ${childrenList.size} enfant(s) chargé(s)")
            
            // Sélectionner l'enfant sauvegardé ou le premier de la liste
            val savedChildId = prefs.getString("selected_child_id", null)
            val childToSelect = if (savedChildId != null && childrenList.any { it.id == savedChildId }) {
                android.util.Log.d("InscriptionTournoiScreen", "✅ Enfant sauvegardé sélectionné: $savedChildId")
                childrenList.find { it.id == savedChildId }
            } else if (childrenList.isNotEmpty()) {
                android.util.Log.d("InscriptionTournoiScreen", "✅ Premier enfant sélectionné: ${childrenList[0].id}")
                childrenList[0]
            } else {
                null
            }
            
            // Pré-remplir immédiatement les données de l'enfant sélectionné
            if (childToSelect != null) {
                selectedChildId = childToSelect.id
                prefs.edit().putString("selected_child_id", childToSelect.id).apply()
                fillChildData(childToSelect)
            }
        }.onFailure { exception ->
            android.util.Log.e("InscriptionTournoiScreen", "❌ Erreur lors du chargement des enfants: ${exception.message}")
        }
    }
    
    // Charger et pré-remplir les informations de l'enfant sélectionné
    // Se déclenche à chaque changement de selectedChildId (quand l'utilisateur change d'enfant)
    LaunchedEffect(selectedChildId, children) {
        val childId = selectedChildId
        if (childId != null && children.isNotEmpty()) {
            // Trouver l'enfant dans la liste au lieu de faire un appel API
            val child = children.find { it.id == childId }
            if (child != null) {
                android.util.Log.d("InscriptionTournoiScreen", "🔄 Changement d'enfant détecté: ${child.prenom} ${child.nom}")
                fillChildData(child)
            } else {
                android.util.Log.w("InscriptionTournoiScreen", "⚠️ Enfant non trouvé dans la liste: $childId")
            }
        }
    }
    
    val montantInscription = tournoi.fraisParticipation ?: 0.0
    
    // Date picker pour la date de naissance
    val calendar = Calendar.getInstance()
    var showDatePicker by remember { mutableStateOf(false) }
    
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
            enfantDateNaissance = dateFormat.format(selectedDate.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    // Fonction pour valider l'inscription
    fun validerInscription() {
        // VÉRIFICATION STRICTE - Bloquer si le tournoi est complet
        if (isTournoiComplet) {
            android.util.Log.d("InscriptionTournoiScreen", "❌ Inscription BLOQUÉE - Tournoi complet")
            Toast.makeText(context, "Ce tournoi est complet. Les inscriptions sont fermées.", Toast.LENGTH_LONG).show()
            return
        }
        
        // Vérification supplémentaire de la capacité
        val max = tournoi.nombreParticipantsMax
        // Utiliser directement les propriétés du tournoi pour obtenir le compteur
        val current = tournoi.participantsCount
            ?: tournoi.nombreParticipantsActuels
            ?: tournoi.inscriptionsCount
            ?: tournoi.participants?.size
        if (max != null && current != null && current >= max) {
            android.util.Log.d("InscriptionTournoiScreen", "❌ Inscription BLOQUÉE - Capacité atteinte ($current/$max)")
            Toast.makeText(context, "Ce tournoi est complet. Les inscriptions sont fermées.", Toast.LENGTH_LONG).show()
            return
        }
        if (enfantPrenom.isBlank() || enfantNom.isBlank() || enfantDateNaissance.isBlank() || parentTelephone.isBlank()) {
            Toast.makeText(context, "Veuillez remplir tous les champs obligatoires", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Convertir la date au format ISO
        val dateFormatInput1 = SimpleDateFormat("dd MMM yyyy", Locale.FRENCH)
        val dateFormatInput2 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateFormatOutput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateNaissanceISO = try {
            // Essayer d'abord le format "dd MMM yyyy"
            val date = try {
                dateFormatInput1.parse(enfantDateNaissance)
            } catch (e: Exception) {
                // Si ça échoue, essayer le format "yyyy-MM-dd"
                try {
                    dateFormatInput2.parse(enfantDateNaissance)
                } catch (e2: Exception) {
                    null
                }
            }
            if (date != null) {
                dateFormatOutput.format(date)
            } else {
                enfantDateNaissance // Utiliser la valeur telle quelle si le parsing échoue
            }
        } catch (e: Exception) {
            enfantDateNaissance
        }
        
        isLoading = true
        coroutineScope.launch {
            val result = inscriptionRepository.inscrireAuTournoi(
                tournoiId = tournoi.id ?: "",
                tournoiNom = tournoi.nom ?: "Tournoi",
                enfantPrenom = enfantPrenom,
                enfantNom = enfantNom,
                enfantDateNaissance = dateNaissanceISO,
                parentTelephone = parentTelephone,
                montantInscription = montantInscription,
                besoinsParticuliers = besoinsParticuliers.takeIf { it.isNotBlank() }
            )
            
            isLoading = false
            
            if (result.isSuccess) {
                Toast.makeText(context, "Inscription validée avec succès!", Toast.LENGTH_SHORT).show()
                onInscriptionSuccess()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                Toast.makeText(context, "Erreur: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header avec app bar unifiée, bouton retour et sélecteur d'enfant
        HeaderSection(
            title = "Inscription",
            onMenuClick = onMenuClick,
            children = children,
            selectedChildId = selectedChildId,
            onChildSelected = { childId ->
                selectedChildId = childId
                prefs.edit().putString("selected_child_id", childId).apply()
            },
            showBackButton = true,
            onBackClick = onCancel
        )
        
        // Contenu avec scroll
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(scrollState)
                .background(Color(0xFFF5F5F5))
        ) {
            // Titre
            Text(
                text = "Inscription au tournoi",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = HeaderBlue,
                modifier = Modifier.padding(20.dp)
            )
            
            Text(
                text = tournoi.nom ?: "",
                fontSize = 18.sp,
                color = TextDarkGray,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            
            Divider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = Color(0xFFE0E0E0)
            )
            
            // Informations de l'enfant
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    
                    // Prénom enfant
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
                    
                    // Nom enfant
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
                    
                    // Date de naissance
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    
                    // Prénom parent (pré-rempli)
                    OutlinedTextField(
                        value = parentPrenom ?: "",
                        onValueChange = { },
                        label = { Text("Prénom", color = TextBlueLight) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = IconGreen)
                        },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = TextBlueLight.copy(alpha = 0.5f),
                            disabledContainerColor = CardWhite,
                            disabledTextColor = TextDarkGray,
                            disabledLabelColor = TextBlueLight,
                            disabledLeadingIconColor = IconGreen
                        )
                    )
                    
                    // Nom parent (pré-rempli)
                    OutlinedTextField(
                        value = parentNom ?: "",
                        onValueChange = { },
                        label = { Text("Nom", color = TextBlueLight) },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = IconGreen)
                        },
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledBorderColor = TextBlueLight.copy(alpha = 0.5f),
                            disabledContainerColor = CardWhite,
                            disabledTextColor = TextDarkGray,
                            disabledLabelColor = TextBlueLight,
                            disabledLeadingIconColor = IconGreen
                        )
                    )
                    
                    // Téléphone
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = HeaderBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Options supplémentaires",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeaderBlue
                        )
                    }
                    
                    // Montant d'inscription (affiché seulement)
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
                    
                    // Besoins particuliers
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
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Bouton Valider
            val actionButtonColor = if (isTournoiComplet) IconGreen else IconOrange
            Button(
                onClick = { if (!isTournoiComplet) validerInscription() },
                enabled = !isLoading && !isTournoiComplet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionButtonColor,
                    disabledContainerColor = actionButtonColor,
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                val (buttonIcon, buttonLabel) = if (isTournoiComplet) {
                    Icons.Default.Block to "Complet"
                } else {
                    Icons.Default.Check to "Valider l'inscription"
                }
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonLabel,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    if (showDatePicker) {
        datePicker.show()
        showDatePicker = false
    }
}

