package com.example.dam_front.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dam_front.services.SuiviGeminiService
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory
import android.app.Activity
import com.example.dam_front.data.SuiviEnfant
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.repository.ChildRepository
import kotlinx.coroutines.launch
import com.example.dam_front.utils.PdfUtils
import com.example.dam_front.utils.TokenManager
import com.example.dam_front.email.EmailRetrofitClient
import com.example.dam_front.email.EmailRequest
import com.example.dam_front.email.AiSummaryParams
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.dam_front.email.EmailConfig
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuiviAiSummaryScreen(
    navController: NavController,
    childId: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // ViewModel to access suivis
    val activity = context as? Activity
    val application = activity?.application ?: return
    
    val sharedVm: SuiviSharedViewModel = viewModel(
        viewModelStoreOwner = activity as androidx.activity.ComponentActivity,
        factory = SuiviSharedViewModelFactory(application)
    )
    
    val suivisMap by sharedVm.suivisByChild.collectAsState()
    
    // State
    var isLoading by remember { mutableStateOf(false) }
    var generatedSummary by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var childName by remember { mutableStateOf("Chargement...") }
    var isSendingEmail by remember { mutableStateOf(false) }

    // Helper functions
    fun savePdf() {
        if (generatedSummary == null) return
        val file = PdfUtils.generateSummaryPdf(context, childName, generatedSummary!!)
        if (file != null) {
            Toast.makeText(context, "PDF sauvegardé: ${file.path}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Erreur lors de la sauvegarde du PDF", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail() {
        if (generatedSummary == null) {
             Toast.makeText(context, "Aucun résumé à envoyer", Toast.LENGTH_SHORT).show()
             return
        }
        
        isSendingEmail = true
        scope.launch {
            try {
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                
                // Clean up summary for email
                val cleanSummary = generatedSummary!!
                    .replace(Regex("\\[SECTION_START:.*?\\]"), "")
                    .trim()

                val testEmail = "hadilaroua52@gmail.com"
                
                // Robust Regex Parsing
                fun extractSection(summary: String, tag: String): String {
                    // Use a lookahead to stop at the next marker or end of string
                    val pattern = Regex("\\[SECTION_START:$tag\\](.*?)(?=\\[SECTION_START:|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                    val match = pattern.find(summary)
                    val content = match?.groupValues?.get(1)?.trim() ?: ""
                    // Remove any trailing "]" if the regex was too greedy with brackets
                    return content.replace(Regex("^.*?\\]"), "").trim()
                }

                val resumeBody = extractSection(generatedSummary!!, "RESUME")
                val fortsBody = extractSection(generatedSummary!!, "FORTS")
                val axesBody = extractSection(generatedSummary!!, "AXES")
                val conseilsBody = extractSection(generatedSummary!!, "CONSEILS")
                val objectifsBody = extractSection(generatedSummary!!, "OBJECTIF")

                android.util.Log.d("AiSummary", "Parsed Sections: RESUME=${resumeBody.take(20)}, FORTS=${fortsBody.take(20)}")

                val params = AiSummaryParams(
                    userEmail = testEmail,
                    parentName = "Parent SportyKids",
                    childName = childName,
                    message = resumeBody,
                    summaryContent = resumeBody,
                    resumeGeneral = resumeBody,
                    strengths = fortsBody,
                    pointsForts = fortsBody,
                    improvements = axesBody,
                    axesAmelioration = axesBody,
                    axes = axesBody,
                    counsel = conseilsBody,
                    conseils = conseilsBody,
                    conseilsParents = conseilsBody,
                    goals = objectifsBody,
                    objectifs = objectifsBody,
                    prochainDefi = objectifsBody,
                    summaryText = cleanSummary,
                    date = dateStr,
                    replyTo = "hadil.aroua@esprit.tn"
                )
                
                if (EmailConfig.SERVICE_ID.contains("YOUR_SERVICE")) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                         Toast.makeText(context, "Configuration Email requise", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val request = EmailRequest(
                    serviceId = EmailConfig.SERVICE_ID,
                    templateId = EmailConfig.TEMPLATE_ID, 
                    userId = EmailConfig.USER_ID,
                    templateParams = params
                )
                
                val response = EmailRetrofitClient.api.sendEmail(request)
                if (response.isSuccessful) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Résumé envoyé par email !", Toast.LENGTH_LONG).show()
                    }
                } else {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        Toast.makeText(context, "Erreur envoi email: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    Toast.makeText(context, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                isSendingEmail = false
            }
        }
    }

    LaunchedEffect(childId, suivisMap) {
        if (childId.isNotBlank()) {
            val trimmedId = childId.trim()
            
            // Priority 1: Search in ANY suivi in the system (more robust for coaches)
            val allSuivisList = suivisMap.values.flatten()
            val match = allSuivisList.find { 
                (it.enfant?.id == trimmedId || it.enfantId == trimmedId) && it.enfant?.prenom != null 
            }
            
            if (match != null) {
                childName = "${match.childNameFromRef()}"
            } else {
                // Try from the map directly if group found but no enfant ref populated
                val simpleMatch = suivisMap[trimmedId]?.firstOrNull { it.enfantName != null }
                if (simpleMatch != null) {
                    childName = simpleMatch.enfantName!!
                }
            }

            // Priority 2: Repository check (Parents)
            val repo = ChildRepository(context)
            repo.getChildren().onSuccess { list ->
                val child = list.find { it.id == trimmedId }
                if (child != null) {
                    childName = "${child.prenom} ${child.nom}"
                } else if (childName == "Chargement..." || childName == "Enfant") {
                    // Priority 3: Fallback for coaches (Direct API User fetch)
                    try {
                        val apiService = com.example.dam_front.api.RetrofitClient.createAuthenticatedApiService(context)
                        val user = apiService.getUser(trimmedId)
                        childName = "${user.prenom} ${user.nom}"
                    } catch (e: Exception) {
                        android.util.Log.e("AiSummary", "Failed fallback fetch for $trimmedId", e)
                    }
                }
            }
            sharedVm.refreshForChild(trimmedId)
        }
    }

    val childSuivis = remember(suivisMap, childId) {
        suivisMap[childId] ?: emptyList()
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                // Child Info Header
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(50.dp),
                            color = IconOrange.copy(alpha = 0.1f),
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = childName.take(1).uppercase(),
                                    color = IconOrange,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(text = childName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextDarkGray)
                            Text(text = "${childSuivis.size} suivis disponibles", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Main Action Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = SportyKidsOrange, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Résumé Intelligent", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlue)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (generatedSummary == null && !isLoading) {
                            Text(
                                text = "Générez un bilan complet basé sur les suivis récents de l'enfant.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (childSuivis.isEmpty()) {
                                        errorMessage = "Aucun suivi disponible pour générer un résumé."
                                        return@Button
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    
                                    val recentSuivis = childSuivis
                                        .sortedByDescending { it.dateSuivi }
                                        .take(10)
                                        .map { 
                                            val date = it.dateSuivi?.toString()?.take(10) ?: ""
                                            val perf = "Performance: ${it.performance}/10"
                                            val text = it.commentaire ?: ""
                                            "[$date] $perf - $text"
                                        }
                                    
                                    SuiviGeminiService.generateSummary(
                                        childName = childName,
                                        suivis = recentSuivis,
                                        onSuccess = { result ->
                                            scope.launch { generatedSummary = result; isLoading = false }
                                        },
                                        onError = { error ->
                                            scope.launch { errorMessage = error; isLoading = false }
                                        }
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SportyKidsGreen),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Générer le résumé avec l'IA")
                            }
                        } else if (isLoading) {
                            CircularProgressIndicator(color = SportyKidsGreen)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Analyse en cours...", color = Color.Gray)
                        } else {
                            val summary = generatedSummary ?: ""
                            if (!summary.contains("[SECTION_START:")) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(text = summary, modifier = Modifier.padding(16.dp), fontSize = 15.sp, lineHeight = 24.sp, color = TextDarkGray)
                                }
                            } else {
                                val sections = summary.split("[SECTION_START:")
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    sections.forEach { section ->
                                        if (section.isBlank()) return@forEach
                                        val parts = section.split("]", limit = 2)
                                        if (parts.size >= 2) {
                                            val sectionType = parts[0]
                                            val sectionContent = parts[1].trim()
                                            val (title, color) = when(sectionType.uppercase()) {
                                                "RESUME" -> "RÉSUMÉ GLOBAL" to HeaderBlue
                                                "FORTS" -> "POINTS FORTS" to SportyKidsGreen
                                                "AXES" -> "AXES D'AMÉLIORATION" to IconOrange
                                                "CONSEILS" -> "CONSEILS AUX PARENTS" to TextBlue
                                                "OBJECTIF" -> "PROCHAIN DÉFI" to SportyKidsGreen
                                                else -> sectionType to Color.Gray
                                            }
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
                                                shape = RoundedCornerShape(12.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(text = title, fontWeight = FontWeight.Bold, color = color, fontSize = 13.sp)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = sectionContent, fontSize = 14.sp, lineHeight = 20.sp, color = TextDarkGray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ActionIconButton(
                                    text = "PDF",
                                    icon = Icons.Default.PictureAsPdf,
                                    color = Color(0xFFE74C3C),
                                    onClick = { savePdf() },
                                    modifier = Modifier.weight(1f)
                                )

                                ActionIconButton(
                                    text = "Email",
                                    icon = Icons.Default.Email,
                                    color = Color(0xFF3498DB),
                                    isLoading = isSendingEmail,
                                    onClick = { sendEmail() },
                                    modifier = Modifier.weight(1f)
                                )

                                val clipboardManager = LocalClipboardManager.current
                                ActionIconButton(
                                    text = "Copier",
                                    icon = Icons.Default.ContentCopy,
                                    color = Color(0xFF95A5A6),
                                    onClick = { 
                                        generatedSummary?.let { 
                                            clipboardManager.setText(AnnotatedString(it)) 
                                            Toast.makeText(context, "Résumé copié !", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = errorMessage ?: "", color = Color.Red, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Back Button - Finally on top!
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour", tint = TextDarkGray)
            }
        }
    }
}

@Composable
fun ActionIconButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.elevatedButtonColors(containerColor = Color.White, contentColor = color),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 4.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = color, strokeWidth = 2.dp)
            } else {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Extension helper for name
fun SuiviEnfant.childNameFromRef(): String {
    return if (this.enfant?.prenom != null) "${this.enfant.prenom} ${this.enfant.nom}" 
           else this.enfantName ?: "Enfant"
}
