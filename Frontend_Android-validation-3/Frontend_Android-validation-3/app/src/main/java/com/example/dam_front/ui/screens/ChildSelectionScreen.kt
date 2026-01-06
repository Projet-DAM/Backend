package com.example.dam_front.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.Child
import com.example.dam_front.models.Program
import com.example.dam_front.repository.EnrollmentRepository
import com.example.dam_front.repository.UserRepository
import com.example.dam_front.repository.PaymentRepository
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ChildSelectionScreen(
    program: Program,
    onBackClick: () -> Unit,
    onRegistrationComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userRepository = remember { UserRepository(context) }
    val enrollmentRepository = remember { EnrollmentRepository(context) }
    val paymentRepository = remember { PaymentRepository(context) }
    
    var children by remember { mutableStateOf<List<Child>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Multi-selection state
    var selectedChildIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var enrolledChildIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isRegistering by remember { mutableStateOf(false) }
    
    // Store current payment intent ID for confirmation
    val currentPaymentIntentId = remember { mutableStateOf<String?>(null) }

    // Dialog states
    var showAddChildDialog by remember { mutableStateOf(false) }
    var selectedChildForDetails by remember { mutableStateOf<Child?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // Dynamic Price Calculation
    val unitPrice = program.prix ?: 0.0
    val totalPrice = unitPrice * selectedChildIds.size

    // Payment Sheet Callback
    val paymentSheet = rememberPaymentSheet { paymentSheetResult ->
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                // Payment successful, proceed with enrollment AND confirmation
                scope.launch {
                    try {
                        // 1. Confirm payment with backend
                        val paymentId = currentPaymentIntentId.value
                        if (paymentId != null) {
                            paymentRepository.confirmPayment(paymentId)
                                .onSuccess { 
                                    android.util.Log.d("PaymentFlow", "Payment confirmed successfully")
                                }
                                .onFailure { e ->
                                    android.util.Log.e("PaymentFlow", "Failed to confirm payment", e)
                                    // Non-blocking error for UI
                                }
                        } else {
                             android.util.Log.e("PaymentFlow", "Payment ID is null, cannot confirm")
                        }

                        // 2. Proceed with enrollment
                        val selectedChildren = children.filter { selectedChildIds.contains(it.id) }
                        val childNamesMap = selectedChildren.associate { it.id to "${it.prenom} ${it.nom}" }
                        
                        enrollmentRepository.enrollChildren(
                            programId = program.id,
                            programName = program.nomProgramme,
                            childIds = selectedChildIds.toList(),
                            childNames = childNamesMap,
                            amountPerChild = unitPrice
                        ).onSuccess { enrollments ->
                            val childNames = enrollments.joinToString(", ") { it.childName }
                            successMessage = if (enrollments.size == 1) {
                                "L'inscription de ${childNames} a été effectuée avec succès.\n\nUn email de confirmation contenant le planning a été envoyé à votre adresse."
                            } else {
                                "L'inscription de ${enrollments.size} enfants (${childNames}) a été effectuée avec succès.\n\nLes emails de confirmation ont été envoyés."
                            }
                            isRegistering = false
                            showSuccessDialog = true
                            
                            // 🔔 S'abonner au topic du programme immédiatement pour recevoir les futures notifs
                            try {
                                val topic = "program_${program.id}"
                                com.example.dam_front.utils.FirebaseTokenManager(context).subscribeToTopic(topic)
                                android.util.Log.d("ChildSelection", "📌 Abonnement au topic réussi: $topic")
                            } catch (e: Exception) {
                                android.util.Log.e("ChildSelection", "❌ Échec de l'abonnement au topic", e)
                            }
                        }.onFailure { exception ->
                            isRegistering = false
                            successMessage = "Inscription validée, mais une erreur technique a empêché la mise à jour immédiate de l'affichage.\n\nPas d'inquiétude, un email de confirmation arrive."
                            showSuccessDialog = true
                        }
                    } catch (e: Exception) {
                        isRegistering = false
                        Toast.makeText(context, "Erreur inattendue: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            is PaymentSheetResult.Canceled -> {
                isRegistering = false
                Toast.makeText(context, "Paiement annulé", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                isRegistering = false
                Toast.makeText(context, "Erreur de paiement: ${paymentSheetResult.error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Function to reload children
    fun reloadChildren() {
        isLoading = true
        scope.launch {
            userRepository.getChildren()
                .onSuccess { fetchedChildren ->
                    children = fetchedChildren
                    
                    // Check enrollment status for each child
                    val enrolled = mutableSetOf<String>()
                    fetchedChildren.forEach { child ->
                        if (enrollmentRepository.isChildEnrolled(child.id, program.id)) {
                            enrolled.add(child.id)
                        }
                    }
                    enrolledChildIds = enrolled
                    isLoading = false
                }
                .onFailure { 
                    error = it.message
                    isLoading = false
                }
        }
    }

    LaunchedEffect(Unit) {
        reloadChildren()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(SportyBackgroundTop, SportyBackgroundBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Modern Header
            ChildSelectionHeader(
                programName = program.nomProgramme,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {


                // Program Summary Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Programme",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SportyMutedText
                                )
                                Text(
                                    text = program.nomProgramme,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SportyDarkBlue
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Prix unitaire",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SportyMutedText
                                )
                                Text(
                                    text = String.format("%.2f TND", unitPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SportyOrange,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Capacity Information
                        if (program.capaciteMaximale != null) {
                            val nombreInscrits = program.nombreInscrits ?: 0
                            val placesDisponibles = program.capaciteMaximale - nombreInscrits
                            val isFull = placesDisponibles <= 0
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isFull) Color(0xFFFFEBEE) 
                                        else if (placesDisponibles <= 3) Color(0xFFFFF3E0)
                                        else Color(0xFFE8F5E9)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    com.example.dam_front.ui.components.PeopleIcon(
                                        tint = if (isFull) Color(0xFFD32F2F) 
                                               else if (placesDisponibles <= 3) Color(0xFFF57C00)
                                               else Color(0xFF388E3C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isFull) "Programme complet" 
                                                   else "Places disponibles",
                                            fontSize = 12.sp,
                                            color = SportyMutedText
                                        )
                                        Text(
                                            text = if (isFull) "Aucune place disponible"
                                                   else "$placesDisponibles place(s) restante(s)",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFull) Color(0xFFD32F2F) 
                                                    else if (placesDisponibles <= 3) Color(0xFFF57C00)
                                                    else Color(0xFF388E3C)
                                        )
                                    }
                                }
                                Text(
                                    text = "$nombreInscrits/${program.capaciteMaximale}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SportyDarkBlue
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Sélectionnez les enfants à inscrire",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SportyOrange)
                    }
                } else if (error != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = error ?: "Une erreur est survenue", color = Color.Red)
                    }
                } else if (children.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Aucun enfant trouvé",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Cliquez sur 'Ajouter un enfant' pour commencer",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(children) { child ->
                            val isEnrolled = enrolledChildIds.contains(child.id)
                            ChildSelectionItem(
                                child = child,
                                isSelected = selectedChildIds.contains(child.id),
                                isEnrolled = isEnrolled,
                                onClick = {
                                    if (!isEnrolled) {
                                        selectedChildIds = if (selectedChildIds.contains(child.id)) {
                                            selectedChildIds - child.id
                                        } else {
                                            selectedChildIds + child.id
                                        }
                                    }
                                },
                                onDetailsClick = {
                                    selectedChildForDetails = child
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total Price and Pay Button
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total à payer",
                                    fontSize = 14.sp,
                                    color = SportyMutedText
                                )
                                Text(
                                    text = "${selectedChildIds.size} enfant(s)",
                                    fontSize = 12.sp,
                                    color = SportyDarkBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = String.format("%.2f TND", totalPrice),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SportyOrange
                            )
                        }

                        // Check capacity
                        val nombreInscrits = program.nombreInscrits ?: 0
                        val capaciteMaximale = program.capaciteMaximale
                        val placesDisponibles = if (capaciteMaximale != null) capaciteMaximale - nombreInscrits else Int.MAX_VALUE
                        val isProgramFull = capaciteMaximale != null && placesDisponibles <= 0
                        val canEnroll = !isProgramFull && (capaciteMaximale == null || selectedChildIds.size <= placesDisponibles)
                        
                        // Warning message if trying to select more than available
                        if (capaciteMaximale != null && selectedChildIds.size > placesDisponibles && placesDisponibles > 0) {
                            Text(
                                text = "⚠️ Seulement $placesDisponibles place(s) disponible(s)",
                                fontSize = 13.sp,
                                color = Color(0xFFF57C00),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        // Full program message
                        if (isProgramFull) {
                            Text(
                                text = "❌ Ce programme a atteint sa capacité maximale",
                                fontSize = 13.sp,
                                color = Color(0xFFD32F2F),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedChildIds.isNotEmpty()) {
                                    // Check capacity before enrollment
                                    if (isProgramFull) {
                                        Toast.makeText(
                                            context,
                                            "Ce programme a atteint sa capacité maximale. Aucune nouvelle inscription n'est possible pour le moment.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@Button
                                    }
                                    
                                    if (!canEnroll) {
                                        Toast.makeText(
                                            context,
                                            "Ce programme n'a que $placesDisponibles place(s) disponible(s), mais vous essayez d'inscrire ${selectedChildIds.size} enfant(s).",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@Button
                                    }
                                    
                                    android.util.Log.d("PaymentFlow", "Starting payment process for amount: $totalPrice")
                                    isRegistering = true
                                    scope.launch {
                                        try {
                                            android.util.Log.d("PaymentFlow", "Creating payment intent...")
                                            // 1. Create Payment Intent with phone number
                                            // TODO: Get real user phone number. Using hardcoded for testing as requested.
                                            val userPhoneNumber = "+21627863334" 
                                            
                                            paymentRepository.createPaymentIntent(totalPrice, phoneNumber = userPhoneNumber)
                                                .onSuccess { clientSecret ->
                                                    android.util.Log.d("PaymentFlow", "Payment intent created successfully: $clientSecret")
                                                    
                                                    // Store paymentIntentId for later confirmation
                                                    val paymentIntentId = clientSecret.split("_secret_")[0]
                                                    
                                                    try {
                                                        // 2. Initialize Payment Configuration
                                                        android.util.Log.d("PaymentFlow", "Initializing Stripe configuration...")
                                                        PaymentConfiguration.init(
                                                            context, 
                                                            "pk_test_51SYrsQ9Ze2kfHWLpISczxhH1OFnBHFICnCVAomayaVIeacdMurBBhQGq5nSSR1SjLR6c0CDRtBdv7VIvuw6N1xga00IaSFXKag"
                                                        )
                                                        
                                                        // 3. Present Payment Sheet with Custom Styling
                                                        android.util.Log.d("PaymentFlow", "Presenting payment sheet...")
                                                        val configuration = PaymentSheet.Configuration(
                                                            merchantDisplayName = "SportyConnect",
                                                            appearance = PaymentSheet.Appearance(
                                                                colorsLight = PaymentSheet.Colors(
                                                                    primary = android.graphics.Color.parseColor("#F16A2D"), // SportyOrange
                                                                    surface = android.graphics.Color.parseColor("#FFFFFF"),
                                                                    component = android.graphics.Color.parseColor("#F5FBFF"), // SportyCardTint
                                                                    componentBorder = android.graphics.Color.parseColor("#E1E8F0"), // SportyDivider
                                                                    componentDivider = android.graphics.Color.parseColor("#E1E8F0"),
                                                                    onComponent = android.graphics.Color.parseColor("#0E5C75"), // SportyDarkBlue
                                                                    onSurface = android.graphics.Color.parseColor("#0E5C75"),
                                                                    subtitle = android.graphics.Color.parseColor("#6F7B91"), // SportyMutedText
                                                                    placeholderText = android.graphics.Color.parseColor("#6F7B91"),
                                                                    appBarIcon = android.graphics.Color.parseColor("#0E5C75"),
                                                                    error = android.graphics.Color.parseColor("#D32F2F")
                                                                ),
                                                                shapes = PaymentSheet.Shapes(
                                                                    cornerRadiusDp = 16.0f,
                                                                    borderStrokeWidthDp = 1.0f
                                                                ),
                                                                typography = PaymentSheet.Typography.default.copy(
                                                                    sizeScaleFactor = 1.0f,
                                                                    fontResId = null // Use system font
                                                                ),
                                                                primaryButton = PaymentSheet.PrimaryButton(
                                                                    colorsLight = PaymentSheet.PrimaryButtonColors(
                                                                        background = android.graphics.Color.parseColor("#F16A2D"), // SportyOrange
                                                                        onBackground = android.graphics.Color.parseColor("#FFFFFF"),
                                                                        border = android.graphics.Color.parseColor("#F16A2D")
                                                                    ),
                                                                    shape = PaymentSheet.PrimaryButtonShape(
                                                                        cornerRadiusDp = 16.0f,
                                                                        borderStrokeWidthDp = 0.0f
                                                                    ),
                                                                    typography = PaymentSheet.PrimaryButtonTypography(
                                                                        fontResId = null
                                                                    )
                                                                )
                                                            )
                                                        )
                                                        paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
                                                        
                                                        // We need to pass the paymentIntentId to the callback somehow, 
                                                        // or store it in a state that the callback can access.
                                                        // Since rememberPaymentSheet callback is defined outside, we'll use a side effect or a mutable state.
                                                        // But here we are inside a coroutine.
                                                        // Let's use a mutable state variable defined at the top level of the composable.
                                                        currentPaymentIntentId.value = paymentIntentId
                                                        
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("PaymentFlow", "Error in payment configuration", e)
                                                        isRegistering = false
                                                        Toast.makeText(context, "Erreur configuration paiement: ${e.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                                .onFailure { e ->
                                                    android.util.Log.e("PaymentFlow", "Error creating payment intent", e)
                                                    isRegistering = false
                                                    Toast.makeText(context, "Erreur initialisation paiement: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        } catch (e: Exception) {
                                            android.util.Log.e("PaymentFlow", "Unexpected error in payment flow", e)
                                            isRegistering = false
                                            Toast.makeText(context, "Erreur inattendue: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = selectedChildIds.isNotEmpty() && !isRegistering && !isProgramFull && canEnroll,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            ),
                            contentPadding = PaddingValues(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (selectedChildIds.isNotEmpty() && !isRegistering && !isProgramFull && canEnroll)
                                            Brush.horizontalGradient(listOf(SportyOrange, SportyLime))
                                        else
                                            Brush.horizontalGradient(listOf(Color.Gray, Color.Gray))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isRegistering) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = if (isProgramFull) "Programme Complet" else "Payer et Terminer",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for Adding Child
        FloatingActionButton(
            onClick = { showAddChildDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 180.dp, end = 24.dp),
            containerColor = SportyTeal,
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Ajouter un enfant",
                modifier = Modifier.size(32.dp)
            )
        }

        // Add Child Dialog
        if (showAddChildDialog) {
            AddChildDialog(
                onDismiss = { showAddChildDialog = false },
                onConfirm = { nom, prenom, dateNaissance, photoUri ->
                    scope.launch {
                        val request = com.example.dam_front.models.CreateChildRequest(
                            nom = nom,
                            prenom = prenom,
                            dateNaissance = dateNaissance
                        )
                        userRepository.createChild(request)
                            .onSuccess { createdChild ->
                                // Upload photo if selected
                                if (photoUri != null) {
                                    try {
                                        val inputStream = context.contentResolver.openInputStream(photoUri)
                                        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                                        inputStream?.close()
                                        
                                        if (bitmap != null) {
                                            val file = java.io.File(context.cacheDir, "temp_child_photo.jpg")
                                            val outputStream = java.io.FileOutputStream(file)
                                            // Compress to JPEG with 80% quality
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                                            outputStream.close()

                                            userRepository.uploadChildPhoto(createdChild.id, file)
                                                .onSuccess {
                                                    Toast.makeText(context, "Enfant créé avec succès", Toast.LENGTH_SHORT).show()
                                                }
                                                .onFailure { e ->
                                                    Toast.makeText(context, "Erreur upload: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        } else {
                                            Toast.makeText(context, "Erreur: Impossible de lire l'image", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Erreur photo: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Enfant créé avec succès", Toast.LENGTH_SHORT).show()
                                }
                                
                                showAddChildDialog = false
                                reloadChildren()
                            }
                            .onFailure { exception ->
                                Toast.makeText(
                                    context,
                                    "Erreur création: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            )
        }

        // Child Details Dialog
        selectedChildForDetails?.let { child ->
            ChildDetailsDialog(
                child = child,
                onDismiss = { selectedChildForDetails = null }
            )
        }

        // Professional Success Dialog
        if (showSuccessDialog) {
            EnrollmentSuccessDialog(
                message = successMessage,
                onConfirm = {
                    showSuccessDialog = false
                    onRegistrationComplete()
                }
            )
        }
    }
}

@Composable
fun EnrollmentSuccessDialog(
    message: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onConfirm,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF388E3C),
                    modifier = Modifier.size(40.dp)
                )
            }
        },
        title = {
            Text(
                text = "Inscription Confirmée !",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SportyDarkBlue,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SportyMutedText,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SportyOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Génial !",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun ChildSelectionItem(
    child: Child,
    isSelected: Boolean,
    isEnrolled: Boolean,
    onClick: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isEnrolled) { onClick() }
            .then(
                if (isSelected) Modifier.border(2.dp, SportyOrange, RoundedCornerShape(20.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnrolled) Color.LightGray.copy(alpha = 0.3f) 
                             else if (isSelected) Color.White 
                             else Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Photo
            if (!child.photoProfil.isNullOrBlank()) {
                val photo = child.photoProfil!!
                val imageUrl = if (photo.startsWith("http")) {
                    photo
                } else {
                    "${com.example.dam_front.config.ApiConfig.BASE_URL}${photo.removePrefix("/")}"
                }
                
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = "Photo de ${child.prenom}",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isEnrolled) Color.Gray.copy(alpha = 0.2f)
                            else if (isSelected) SportyOrange.copy(alpha = 0.1f) 
                            else Color.LightGray.copy(alpha = 0.2f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isEnrolled) Color.Gray else if (isSelected) SportyOrange else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${child.prenom} ${child.nom}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnrolled) Color.Gray else SportyDarkBlue
                )
                
                // Date de naissance
                if (!child.dateNaissance.isNullOrBlank()) {
                    Text(
                        text = "Né(e) le ${com.example.dam_front.utils.DateUtils.formatDate(child.dateNaissance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnrolled) Color.Gray else SportyMutedText,
                        fontSize = 12.sp
                    )
                }
                
                Text(
                    text = if (isEnrolled) "Déjà inscrit" 
                           else if (isSelected) "Sélectionné" 
                           else "Appuyer pour sélectionner",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnrolled) Color.Red.copy(alpha = 0.7f) 
                            else if (isSelected) SportyOrange 
                            else SportyMutedText
                )
            }
            
            // Details Button
            IconButton(
                onClick = onDetailsClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Voir détails",
                    tint = if (isEnrolled) Color.Gray else SportyTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            if (isEnrolled) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Déjà inscrit",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            } else if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Sélectionné",
                    tint = SportyOrange,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, Color.LightGray, CircleShape)
                )
            }
        }
    }
}

@Composable
fun AddChildDialog(
    onDismiss: () -> Unit,
    onConfirm: (nom: String, prenom: String, dateNaissance: String, photoUri: android.net.Uri?) -> Unit
) {
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var dateNaissance by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        selectedPhotoUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Ajouter un enfant",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SportyDarkBlue
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Photo de profil
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { photoLauncher.launch("image/*") }
                ) {
                    if (selectedPhotoUri != null) {
                        coil.compose.AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = "Photo de profil",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .border(3.dp, SportyOrange, CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(SportyTeal.copy(alpha = 0.2f))
                                .border(2.dp, SportyTeal.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = SportyTeal,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                    
                    // Camera icon button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(SportyOrange, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ajouter photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Text(
                    text = "Appuyez pour ajouter une photo",
                    style = MaterialTheme.typography.bodySmall,
                    color = SportyMutedText,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = prenom,
                    onValueChange = { prenom = it },
                    label = { Text("Prénom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyOrange,
                        focusedLabelColor = SportyOrange
                    )
                )
                
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyOrange,
                        focusedLabelColor = SportyOrange
                    )
                )
                
                OutlinedTextField(
                    value = dateNaissance,
                    onValueChange = { dateNaissance = it },
                    label = { Text("Date de naissance (AAAA-MM-JJ)") },
                    placeholder = { Text("2015-01-15") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SportyOrange,
                        focusedLabelColor = SportyOrange
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nom.isNotBlank() && prenom.isNotBlank()) {
                        onConfirm(nom, prenom, dateNaissance, selectedPhotoUri)
                    }
                },
                enabled = nom.isNotBlank() && prenom.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SportyOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Créer",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Annuler",
                    color = SportyMutedText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun ChildDetailsDialog(
    child: Child,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Détails de l'enfant",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = SportyDarkBlue
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Photo Profile
                if (!child.photoProfil.isNullOrBlank()) {
                    val imageUrl = if (child.photoProfil.startsWith("http")) {
                        child.photoProfil
                    } else {
                        "${com.example.dam_front.config.ApiConfig.BASE_URL}${child.photoProfil.removePrefix("/")}"
                    }

                    coil.compose.AsyncImage(
                        model = imageUrl,
                        contentDescription = "Photo de ${child.prenom}",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SportyTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = SportyTeal,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${child.prenom} ${child.nom}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SportyDarkBlue
                    )
                    
                    child.dateNaissance?.let { dob ->
                        Text(
                            text = "Né(e) le ${com.example.dam_front.utils.DateUtils.formatDate(dob)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SportyMutedText
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SportyOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Fermer")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = SportyMutedText,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            fontSize = 15.sp,
            color = TextDarkGray,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ChildSelectionHeader(
    programName: String,
    onBackClick: () -> Unit
) {
    val headerBrush = remember {
        Brush.linearGradient(listOf(SportyDarkBlue, SportyTeal))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(headerBrush)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INSCRIPTION",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = programName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
