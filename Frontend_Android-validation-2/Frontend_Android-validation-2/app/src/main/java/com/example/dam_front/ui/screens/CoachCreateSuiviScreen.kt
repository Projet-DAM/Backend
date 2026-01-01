package com.example.dam_front.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.dam_front.viewmodels.CoachCreateSuiviViewModel
import com.example.dam_front.viewmodels.CoachCreateSuiviViewModelFactory
import androidx.navigation.NavController
import com.example.dam_front.repository.SuiviRepository
import com.example.dam_front.repository.CoachRepository
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.example.dam_front.ui.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import com.example.dam_front.data.CreateSuiviEnfantDto
import com.example.dam_front.ui.screens.COACH_CHILD_DETAIL_ROUTE
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch


const val CREATE_SUIVI_ROUTE = "coach_create_suivi"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachCreateSuiviScreen(navController: NavController, enfantId: String, suiviId: String? = null) {
    // Use ViewModel to hold form state and perform network operations
    val application = LocalContext.current.applicationContext as android.app.Application
    val vm: CoachCreateSuiviViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = CoachCreateSuiviViewModelFactory(application, enfantId, suiviId)
    )
    val uiState by vm.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val sharedVm: com.example.dam_front.viewmodels.SuiviSharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        viewModelStoreOwner = activity,
        factory = com.example.dam_front.viewmodels.SuiviSharedViewModelFactory(application)
    )

    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())) }
    var dateMillis by remember { mutableStateOf(uiState.dateMillis) }
    var dateDisplayStr by remember { mutableStateOf(if (uiState.dateDisplay.isBlank()) SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date()) else uiState.dateDisplay) }
    var presence by remember { mutableStateOf(uiState.presence) }
    var performanceText by remember { mutableStateOf(uiState.performance.toString()) }
    var commentaire by remember { mutableStateOf(TextFieldValue(uiState.commentaire ?: "")) }
    var activityType by remember { mutableStateOf(uiState.activityType ?: "Training") }
    var focusAreas by remember { mutableStateOf(TextFieldValue(uiState.focusAreas.joinToString(", "))) }
    var nextGoals by remember { mutableStateOf(TextFieldValue(uiState.nextSessionGoals.joinToString(", "))) }
    var effortLevel by remember { mutableStateOf(uiState.effortLevel) }
    var emotionalState by remember { mutableStateOf(uiState.emotionalState ?: "Motivé") }
    val context = LocalContext.current
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = SportyKidsBlue,
        unfocusedBorderColor = IconBlue,
        cursorColor = SportyKidsBlue,
        focusedLabelColor = SportyKidsBlue,
        unfocusedLabelColor = TextDarkGray,
        focusedContainerColor = CardWhite,
        unfocusedContainerColor = CardWhite
    )
    var performanceExpanded by remember { mutableStateOf(false) }
    var activityExpanded by remember { mutableStateOf(false) }
    var effortExpanded by remember { mutableStateOf(false) }
    var emotionalExpanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(uiState.isLoading) }
    var snackbarHostState = remember { SnackbarHostState() }
    var isEditing by remember { mutableStateOf(!suiviId.isNullOrBlank()) }
    var selectedEnfantId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(enfantId) }
    var availablePlayers by remember { mutableStateOf(uiState.availablePlayers) }
    var expandedPlayers by remember { mutableStateOf(false) }
    var childQuery by remember { mutableStateOf("") }

    val openDatePicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(context, { _, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            TimePickerDialog(context, { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                dateMillis = cal.timeInMillis
                val isoSdfLocal = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                dateStr = isoSdfLocal.format(Date(dateMillis))
                dateDisplayStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(dateMillis))
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    println("📋 CoachCreateSuiviScreen: enfantId='$enfantId', selectedEnfantId='$selectedEnfantId', availablePlayers.size=${availablePlayers.size}")

    // Keep ViewModel state in sync when it changes
    LaunchedEffect(uiState) {
        println("🔄 LaunchedEffect triggered: availablePlayers.size=${uiState.availablePlayers.size}")
        uiState.availablePlayers.forEachIndexed { idx, player ->
            println("  🔍 uiState player[$idx]: ${player.prenom} ${player.nom} - ID: ${player.id}")
        }
        // reflect loading and available players
        isLoading = uiState.isLoading
        availablePlayers = uiState.availablePlayers
        availablePlayers.forEachIndexed { idx, player ->
            println("  📦 local player[$idx] after copy: \${player.prenom} \${player.nom} - ID: \${player.id}")
        }
        // Set initial selectedEnfantId only if it's blank or invalid
        if (selectedEnfantId.isBlank() && availablePlayers.isNotEmpty() && enfantId.isBlank()) {
            val firstPlayerId = availablePlayers.firstOrNull()?.id
            if (!firstPlayerId.isNullOrBlank()) {
                println("🆔 Setting initial selectedEnfantId to: $firstPlayerId")
                selectedEnfantId = firstPlayerId
            }
        } else if (enfantId.isNotBlank() && selectedEnfantId != enfantId) {
            println("🆔 Using passed enfantId: $enfantId")
            selectedEnfantId = enfantId
        }
        // if ViewModel populated suivi data, copy to local inputs
        // (this keeps the existing form state behavior while allowing ViewModel to hydrate values)
        if (uiState.dateMillis != dateMillis) dateMillis = uiState.dateMillis
        uiState.commentaire?.let { if (commentaire.text.isBlank()) commentaire = TextFieldValue(it) }
        uiState.activityType?.let { if (activityType.isBlank()) activityType = it }
    }

    // Show UI errors (e.g. no players / permissions) in a snackbar so coach understands why
    LaunchedEffect(uiState.error) {
        uiState.error?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.White, CircleShape)
                            .shadow(2.dp, CircleShape)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Retour", 
                            tint = HeaderBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = HeaderBlue
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = NavGrayBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            
            // Header: Child Name
            val headerTitle = availablePlayers.find { it.id == selectedEnfantId }?.let { "${it.prenom} ${it.nom}" } 
                ?: if (isEditing) "Modifier le Suivi" else "Nouveau Suivi"
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = headerTitle,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeaderBlue,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = if (isEditing) "Modification des progrès" else "Remplissez les informations ci-dessous",
                fontSize = 16.sp,
                color = TextDarkGray.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Form Content
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Informations Section - ORANGE Theme (Playful/Identity)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.border(1.dp, IconOrange.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(IconOrange.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Face, contentDescription = null, tint = IconOrange)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "L'Enfant",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = IconOrange
                            )
                        }
                        
                        Spacer(Modifier.height(20.dp))

                        // Child Selector
                        if (availablePlayers.isEmpty()) {
                            OutlinedTextField(
                                value = "Aucun joueur disponible",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Joueur") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = IconOrange) },
                                shape = fieldShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconOrange,
                                    unfocusedBorderColor = IconOrange.copy(alpha = 0.3f),
                                    cursorColor = IconOrange,
                                    focusedLabelColor = IconOrange,
                                    unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                        } else {
                            val selectedPlayer = availablePlayers.find { it.id == selectedEnfantId }
                            val displayText = selectedPlayer?.let { "${it.prenom} ${it.nom}" } ?: "Choisir un joueur"
                            
                            ExposedDropdownMenuBox(
                                expanded = expandedPlayers, 
                                onExpandedChange = { expandedPlayers = !expandedPlayers }
                            ) {
                                OutlinedTextField(
                                    value = displayText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Joueur") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPlayers) },
                                    shape = fieldShape,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = IconOrange,
                                        unfocusedBorderColor = IconOrange.copy(alpha = 0.3f),
                                        cursorColor = IconOrange,
                                        focusedLabelColor = IconOrange,
                                        unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f),
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedPlayers, 
                                    onDismissRequest = { expandedPlayers = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        OutlinedTextField(
                                            value = childQuery,
                                            onValueChange = { childQuery = it },
                                            singleLine = true,
                                            placeholder = { Text("Rechercher un élève...") },
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                unfocusedBorderColor = Color(0xFFEEEEEE),
                                                focusedBorderColor = IconOrange
                                            )
                                        )
                                        
                                        val filtered = if (childQuery.isBlank()) availablePlayers else availablePlayers.filter {
                                            "${it.prenom} ${it.nom}".contains(childQuery, ignoreCase = true)
                                        }
                                        
                                        filtered.forEach { p ->
                                            DropdownMenuItem(
                                                text = { Text("${p.prenom} ${p.nom}", color = TextDarkGray) }, 
                                                onClick = {
                                                    selectedEnfantId = p.id ?: ""
                                                    expandedPlayers = false
                                                    childQuery = ""
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Date Picker
                        OutlinedTextField(
                            value = dateDisplayStr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date et heure") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openDatePicker() },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IconOrange,
                                unfocusedBorderColor = IconOrange.copy(alpha = 0.3f),
                                cursorColor = IconOrange,
                                focusedLabelColor = IconOrange,
                                unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            trailingIcon = {
                                IconButton(onClick = { openDatePicker() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Choisir date", tint = IconOrange)
                                }
                            },
                            shape = fieldShape
                        )
                    }
                }

                // Performance Section - GREEN Theme (Growth/Success)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.border(1.dp, IconGreen.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(IconGreen.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = IconGreen)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Performance",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = IconGreen
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Performance
                        ExposedDropdownMenuBox(
                            expanded = performanceExpanded, 
                            onExpandedChange = { performanceExpanded = !performanceExpanded }
                        ) {
                            OutlinedTextField(
                                value = performanceText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Note (0-10)") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = performanceExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IconGreen,
                                    unfocusedBorderColor = IconGreen.copy(alpha = 0.3f),
                                    cursorColor = IconGreen,
                                    focusedLabelColor = IconGreen,
                                    unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                shape = fieldShape
                            )
                            ExposedDropdownMenu(expanded = performanceExpanded, onDismissRequest = { performanceExpanded = false }) {
                                (0..10).forEach { v ->
                                    DropdownMenuItem(text = { Text(v.toString()) }, onClick = { performanceText = v.toString(); performanceExpanded = false })
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Comment
                        OutlinedTextField(
                            value = commentaire,
                            onValueChange = { commentaire = it },
                            label = { Text("Commentaire du Coach") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = IconGreen,
                                unfocusedBorderColor = IconGreen.copy(alpha = 0.3f),
                                cursorColor = IconGreen,
                                focusedLabelColor = IconGreen,
                                unfocusedLabelColor = TextDarkGray.copy(alpha = 0.7f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            shape = fieldShape
                        )
                    }
                }

                // Details Section - BLUE Theme (Focus/Calm)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.border(1.dp, SportyKidsBlue.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(SportyKidsBlue.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sports, contentDescription = null, tint = SportyKidsBlue)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Détails Séance",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = SportyKidsBlue
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Activity Type
                        ExposedDropdownMenuBox(
                            expanded = activityExpanded, 
                            onExpandedChange = { activityExpanded = !activityExpanded }
                        ) {
                            OutlinedTextField(
                                value = activityType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Type d'activité") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = activityExpanded) },
                                colors = fieldColors,
                                shape = fieldShape
                            )
                            ExposedDropdownMenu(expanded = activityExpanded, onDismissRequest = { activityExpanded = false }) {
                                listOf("Training", "Match", "Recovery", "Technique", "Other").forEach { opt ->
                                    DropdownMenuItem(text = { Text(opt) }, onClick = { activityType = opt; activityExpanded = false })
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = focusAreas,
                            onValueChange = { focusAreas = it },
                            label = { Text("Zones de focus") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            shape = fieldShape
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = nextGoals,
                            onValueChange = { nextGoals = it },
                            label = { Text("Objectifs prochaine séance") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = fieldColors,
                            shape = fieldShape
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Effort & Emotional Row
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Effort
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = effortExpanded, 
                                    onExpandedChange = { effortExpanded = !effortExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = effortLevel.toString(),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Effort") },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = effortExpanded) },
                                        colors = fieldColors,
                                        shape = fieldShape
                                    )
                                    ExposedDropdownMenu(expanded = effortExpanded, onDismissRequest = { effortExpanded = false }) {
                                        (1..10).forEach { v ->
                                            DropdownMenuItem(text = { Text(v.toString()) }, onClick = { effortLevel = v; effortExpanded = false })
                                        }
                                    }
                                }
                            }
                            
                            // Emotional
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = emotionalExpanded, 
                                    onExpandedChange = { emotionalExpanded = !emotionalExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = emotionalState,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Humeur") },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = emotionalExpanded) },
                                        colors = fieldColors,
                                        shape = fieldShape
                                    )
                                    ExposedDropdownMenu(expanded = emotionalExpanded, onDismissRequest = { emotionalExpanded = false }) {
                                        listOf("Motivé", "Stressé", "Fatigué", "Neutre").forEach { opt ->
                                            DropdownMenuItem(text = { Text(opt) }, onClick = { emotionalState = opt; emotionalExpanded = false })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))

                // Submit Buttons
                val canSubmit = (!isLoading) && (isEditing || selectedEnfantId.isNotBlank())
                Button(
                    onClick = {
                        isLoading = true
                        scope.launch {
                            try {
                                val date = Date(dateMillis)
                                val perf = performanceText.toIntOrNull() ?: 0
                                if (isEditing && !suiviId.isNullOrBlank()) {
                                    val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                    val updateDto = com.example.dam_front.data.UpdateSuiviEnfantDto(
                                        dateSuivi = isoSdf.format(date),
                                        presence = presence,
                                        performance = perf,
                                        commentaire = commentaire.text.ifBlank { null }
                                    )
                                    vm.updateSuivi(suiviId, updateDto) { res ->
                                        isLoading = false
                                        res.onSuccess { suivi ->
                                            try { sharedVm.addOrUpdateLocal(suivi) } catch (_: Exception) {}
                                            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_suivis", true)
                                            scope.launch { snackbarHostState.showSnackbar("Suivi mis à jour") }
                                            val cid = suivi.enfant?.id ?: selectedEnfantId
                                            navController.navigate("$COACH_CHILD_DETAIL_ROUTE/$cid")
                                        }.onFailure { ex -> scope.launch { snackbarHostState.showSnackbar(ex.message ?: "Erreur") } }
                                    }
                                } else {
                                    val isoSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }
                                    val dto = CreateSuiviEnfantDto(
                                        dateSuivi = isoSdf.format(date),
                                        presence = presence,
                                        performance = perf,
                                        commentaire = commentaire.text.ifBlank { null },
                                        enfantId = selectedEnfantId.ifBlank { enfantId },
                                        activityType = activityType.ifBlank { null },
                                        focusAreas = if (focusAreas.text.isBlank()) null else focusAreas.text.split(',').map { it.trim() },
                                        nextSessionGoals = if (nextGoals.text.isBlank()) null else nextGoals.text.split(',').map { it.trim() },
                                        effortLevel = effortLevel,
                                        emotionalState = emotionalState.ifBlank { null }
                                    )
                                    vm.createSuivi(dto) { res ->
                                        isLoading = false
                                        res.onSuccess { suivi ->
                                            try { sharedVm.addOrUpdateLocal(suivi) } catch (_: Exception) {}
                                            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_suivis", true)
                                            scope.launch { snackbarHostState.showSnackbar("Suivi créé") }
                                            navController.navigate(COACH_HOME_ROUTE) {
                                                popUpTo(COACH_HOME_ROUTE) { inclusive = true }
                                            }
                                        }.onFailure { ex -> scope.launch { snackbarHostState.showSnackbar(ex.message ?: "Erreur") } }
                                    }
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                snackbarHostState.showSnackbar(e.message ?: "Erreur inconnue")
                            }
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeaderBlue,
                        disabledContainerColor = IconInactiveGray
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextWhite,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isLoading) "Envoi..." else if (isEditing) "Mettre à jour" else "Enregistrer le suivi",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(Modifier.height(30.dp))
            }
        }
    }
}
