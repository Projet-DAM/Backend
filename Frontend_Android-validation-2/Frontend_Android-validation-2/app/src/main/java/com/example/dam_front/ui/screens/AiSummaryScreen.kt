package com.example.dam_front.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.utils.PdfService
import coil.compose.AsyncImage
import com.example.dam_front.models.User
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.ImageUtils
import com.example.dam_front.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryScreen(
    navController: androidx.navigation.NavController,
    children: List<User> = emptyList()
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    val viewModel: com.example.dam_front.viewmodels.AiSummaryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )

    var selectedChild by remember { mutableStateOf<User?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }
    
    val isGenerating by viewModel.isGenerating.collectAsState()
    val summaryResult by viewModel.summaryResult.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(

        containerColor = Color(0xFFF8FBFF)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Dropdown to select child
            item {
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedChild?.let { "${it.prenom} ${it.nom}" } ?: "Sélectionner un enfant",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Choisir l'enfant") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderBlue,
                            unfocusedBorderColor = HeaderBlue.copy(alpha = 0.5f)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        children.forEach { child ->
                            DropdownMenuItem(
                                text = { Text("${child.prenom} ${child.nom}") },
                                onClick = {
                                    selectedChild = child
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            // Child info section (Visible only if child selected)
            if (selectedChild != null) {
                item {
                    ChildInfoSection(selectedChild!!)
                }
            }

            // Error Display
            if (error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red)
                            Spacer(Modifier.width(8.dp))
                            Text(error ?: "", color = Color.Red, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Big Card: Résumé intelligent
            item {
                SummaryCard(
                    isGenerating = isGenerating,
                    summaryResult = summaryResult,
                    selectedChild = selectedChild,
                    viewModel = viewModel
                )
            }

            // Bottom Generation Button
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { 
                        selectedChild?.let { viewModel.generateSummary(it) }
                    },
                    enabled = selectedChild != null && !isGenerating,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp)
                        .shadow(4.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeaderBlue,
                        disabledContainerColor = HeaderBlue.copy(alpha = 0.5f)
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Génération en cours…", color = Color.White, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Générer le résumé avec l’IA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChildInfoSection(child: User) {
    val age = remember(child) {
        try {
            if (!child.dateNaissance.isNullOrBlank()) {
                // ISO format like "2015-12-31" or "2015-12-31T00:00:00.000Z"
                val birthDateStr = child.dateNaissance.split("T")[0]
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                val birthDate = sdf.parse(birthDateStr)
                if (birthDate != null) {
                    val dob = java.util.Calendar.getInstance()
                    dob.time = birthDate
                    val today = java.util.Calendar.getInstance()
                    var ageYears = today.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR)
                    if (today.get(java.util.Calendar.DAY_OF_YEAR) < dob.get(java.util.Calendar.DAY_OF_YEAR)) {
                        ageYears--
                    }
                    "$ageYears ans"
                } else "Age inconnu"
            } else "Age inconnu"
        } catch (e: Exception) {
            "Age inconnu"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val photoUrl = ImageUtils.getPhotoUrl(child.photoProfil) ?: "https://placehold.co/128"
            AsyncImage(
                model = photoUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp, SportyKidsGreen, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "${child.prenom} ${child.nom}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextBlue
                )
                Text(
                    text = "$age • Passionné de Sport",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    isGenerating: Boolean,
    summaryResult: AiSummaryResult?,
    selectedChild: User?,
    viewModel: com.example.dam_front.viewmodels.AiSummaryViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSendingEmail by remember { mutableStateOf(false) }
    var emailSentStatus by remember { mutableStateOf<Boolean?>(null) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = IconOrange, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Résumé intelligent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextBlue
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))

            if (isGenerating) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = HeaderBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Analyse des performances...", color = Color.Gray)
                    }
                }
            } else if (summaryResult == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Aucun résumé généré pour le moment",
                        textAlign = TextAlign.Center,
                        color = Color.Gray.copy(alpha = 0.8f),
                        fontSize = 16.sp
                    )
                }
                } else {
                SummaryContent(summaryResult)
                
                // Action Buttons
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // PDF Export Button
                    Button(
                        onClick = {
                            summaryResult?.let { summary ->
                                selectedChild?.let { child ->
                                    val file = PdfService.generateSummaryPdf(context, "${child.prenom} ${child.nom}", summary)
                                    if (file != null) {
                                        PdfService.sharePdf(context, file)
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ExportGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Email Send Button
                    Button(
                        onClick = {
                            if (selectedChild != null && summaryResult != null) {
                                isSendingEmail = true
                                viewModel.sendSummaryEmail(selectedChild, summaryResult) { success ->
                                    isSendingEmail = false
                                    emailSentStatus = success
                                    scope.launch {
                                        if (success) {
                                            Toast.makeText(context, "Email envoyé avec succès !", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Échec de l'envoi de l'email.", Toast.LENGTH_LONG).show()
                                        }
                                        kotlinx.coroutines.delay(3000)
                                        emailSentStatus = null
                                    }
                                }
                            }
                        },
                        enabled = !isSendingEmail,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HeaderBlue,
                            disabledContainerColor = HeaderBlue.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 8.dp)
                    ) {
                        if (isSendingEmail) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (emailSentStatus == true) Icons.Default.CheckCircle else Icons.Default.Email, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (emailSentStatus == true) "Envoyé" else "Email", 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

