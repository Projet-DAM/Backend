package com.example.dam_front.ui.screens

import android.content.Context
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.SignUpRequest
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.ui.components.SportyKidsLogo
import com.example.dam_front.ui.theme.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch

enum class UserRole(val displayName: String, val icon: ImageVector, val color: Color, val gradientColors: List<Color>) {
    PARENT("Parent", Icons.Default.Person, SportyKidsBlue, listOf(SportyKidsBlue, SportyKidsLightBlue)),
    COACH("Coach", Icons.Default.SportsSoccer, SportyKidsGreen, listOf(SportyKidsGreen, SportyKidsLightGreen))
}

@Composable
fun SignUpScreen(
    onSignUpClick: (String?) -> Unit = {}, // Passer le rôle
    onSignInClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Champs communs
    var nom by remember { mutableStateOf("") }
    var prenom by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Champs Coach
    var certifications by remember { mutableStateOf<List<String>>(emptyList()) }
    var newCertification by remember { mutableStateOf("") }
    var specialite by remember { mutableStateOf("") }
    var experience by remember { mutableStateOf("") }
    
    // Configurer la status bar
    val systemUiController = rememberSystemUiController()
    
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = CardWhite,
            darkIcons = true
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite)
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        
        // Sporty Kids Logo
        SportyKidsLogo(
            modifier = Modifier.padding(bottom = 8.dp),
            logoSize = 200.dp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Sélection de rôle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserRole.values().forEach { role ->
                RoleSelectionCard(
                    role = role,
                    isSelected = selectedRole == role,
                    onClick = { selectedRole = role },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        // Afficher le formulaire seulement si un rôle est sélectionné
        selectedRole?.let { role ->
            Spacer(modifier = Modifier.height(32.dp))
            
            // Formulaire commun
            CommonSignUpFields(
                nom = nom,
                onNomChange = { nom = it },
                prenom = prenom,
                onPrenomChange = { prenom = it },
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                passwordVisible = passwordVisible,
                onPasswordVisibleChange = { passwordVisible = it },
                confirmPasswordVisible = confirmPasswordVisible,
                onConfirmPasswordVisibleChange = { confirmPasswordVisible = it }
            )
            
            // Formulaire spécifique selon le rôle
            when (role) {
                UserRole.COACH -> {
                    Spacer(modifier = Modifier.height(24.dp))
                    CoachSpecificFields(
                        certifications = certifications,
                        onCertificationsChange = { certifications = it },
                        newCertification = newCertification,
                        onNewCertificationChange = { newCertification = it },
                        specialite = specialite,
                        onSpecialiteChange = { specialite = it },
                        experience = experience,
                        onExperienceChange = { experience = it }
                    )
                }
                UserRole.PARENT -> {
                    // Pas de champs supplémentaires pour parent
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Message d'erreur
            errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color(0xFFD32F2F),
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            
            // Bouton d'inscription
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                SportyKidsOrange,
                                SportyKidsBlue,
                                SportyKidsGreen.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        enabled = !isLoading,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Validation
                        if (nom.isBlank() || prenom.isBlank() || email.isBlank() || password.isBlank()) {
                            errorMessage = "Veuillez remplir tous les champs obligatoires"
                            return@clickable
                        }
                        
                        if (password.length < 6) {
                            errorMessage = "Le mot de passe doit contenir au moins 6 caractères"
                            return@clickable
                        }
                        
                        if (password != confirmPassword) {
                            errorMessage = "Les mots de passe ne correspondent pas"
                            return@clickable
                        }
                        
                        // Validation spécifique selon le rôle
                        when (role) {
                            UserRole.COACH -> {
                                if (specialite.isBlank() || experience.isBlank()) {
                                    errorMessage = "Veuillez remplir tous les champs du coach"
                                    return@clickable
                                }
                            }
                            UserRole.PARENT -> {
                                // Pas de validation supplémentaire
                            }
                        }
                        
                        isLoading = true
                        errorMessage = null
                        
                        coroutineScope.launch {
                            // Convertir le rôle au format attendu par le backend
                            val roleString = when (role) {
                                UserRole.PARENT -> "parent"
                                UserRole.COACH -> "coach"
                            }
                            
                            android.util.Log.d("SignUpScreen", "📝 Création SignUpRequest - Rôle: $roleString")
                            android.util.Log.d("SignUpScreen", "  Nom: $nom, Prénom: $prenom, Email: $email")
                            
                            val signUpRequest = SignUpRequest(
                                nom = nom,
                                prenom = prenom,
                                email = email,
                                motDePasse = password,
                                role = roleString,
                                certification = if (role == UserRole.COACH && certifications.isNotEmpty()) certifications else null,
                                specialite = if (role == UserRole.COACH) specialite else null,
                                experience = if (role == UserRole.COACH && experience.isNotBlank()) experience.toIntOrNull() else null
                            )
                            
                            android.util.Log.d("SignUpScreen", "🚀 Envoi de la requête d'inscription...")
                            val result = authRepository.signup(signUpRequest)
                            isLoading = false
                            
                            result.onSuccess { pair ->
                                val token = pair.first
                                if (token.isNotEmpty()) {
                                    Toast.makeText(context, "Inscription réussie !", Toast.LENGTH_SHORT).show()
                                    // Passer le rôle pour la navigation
                                    onSignUpClick(role.name.lowercase())
                                } else {
                                    Toast.makeText(context, "Inscription réussie ! Veuillez vérifier votre email et vous connecter.", Toast.LENGTH_LONG).show()
                                    onSignInClick()
                                }
                            }.onFailure { exception ->
                                errorMessage = exception.message ?: "Erreur lors de l'inscription"
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Créer un compte",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sign In Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Déjà un compte ? ",
                fontSize = 14.sp,
                color = TextDarkGray.copy(alpha = 0.7f)
            )
            Text(
                text = "Se connecter",
                fontSize = 14.sp,
                color = SportyKidsOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignInClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun RoleSelectionCard(
    role: UserRole,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) role.color else Color.Gray.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isSelected) {
                        Brush.verticalGradient(role.gradientColors)
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                CardWhite,
                                role.color.copy(alpha = 0.05f)
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else role.color.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = role.icon,
                        contentDescription = role.displayName,
                        modifier = Modifier.size(28.dp),
                        tint = if (isSelected) Color.White else role.color
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = role.displayName,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else role.color,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = if (isSelected) androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = 0.2f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                            blurRadius = 2f
                        ) else null
                    )
                )
            }
        }
    }
}

@Composable
fun CommonSignUpFields(
    nom: String,
    onNomChange: (String) -> Unit,
    prenom: String,
    onPrenomChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleChange: (Boolean) -> Unit
) {
    // Nom
    OutlinedTextField(
        value = nom,
        onValueChange = onNomChange,
        label = { Text("Nom", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("Votre nom", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = SportyKidsOrange)
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsOrange,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsOrange,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Prénom
    OutlinedTextField(
        value = prenom,
        onValueChange = onPrenomChange,
        label = { Text("Prénom", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("Votre prénom", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.Person, contentDescription = null, tint = SportyKidsOrange)
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsOrange,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsOrange,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Email
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("email@example.com", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        leadingIcon = {
            Icon(Icons.Default.Email, contentDescription = null, tint = SportyKidsOrange)
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsOrange,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsOrange,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Mot de passe
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text("Mot de passe", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("••••••••", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = SportyKidsOrange)
        },
        trailingIcon = {
            IconButton(onClick = { onPasswordVisibleChange(!passwordVisible) }) {
                Icon(
                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = SportyKidsOrange
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsOrange,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsOrange,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Confirmer mot de passe
    OutlinedTextField(
        value = confirmPassword,
        onValueChange = onConfirmPasswordChange,
        label = { Text("Confirmer le mot de passe", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("••••••••", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        leadingIcon = {
            Icon(Icons.Default.Lock, contentDescription = null, tint = SportyKidsOrange)
        },
        trailingIcon = {
            IconButton(onClick = { onConfirmPasswordVisibleChange(!confirmPasswordVisible) }) {
                Icon(
                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null,
                    tint = SportyKidsOrange
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsOrange,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsOrange,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
}

@Composable
fun CoachSpecificFields(
    certifications: List<String>,
    onCertificationsChange: (List<String>) -> Unit,
    newCertification: String,
    onNewCertificationChange: (String) -> Unit,
    specialite: String,
    onSpecialiteChange: (String) -> Unit,
    experience: String,
    onExperienceChange: (String) -> Unit
) {
    Text(
        text = "Informations Coach",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = SportyKidsTeal,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Spécialité
    OutlinedTextField(
        value = specialite,
        onValueChange = onSpecialiteChange,
        label = { Text("Spécialité", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("Ex: Football, Basketball...", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = SportyKidsGreen)
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsGreen,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsGreen,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Expérience
    OutlinedTextField(
        value = experience,
        onValueChange = { if (it.all { char -> char.isDigit() }) onExperienceChange(it) },
        label = { Text("Années d'expérience", color = SportyKidsTeal.copy(alpha = 0.7f)) },
        placeholder = { Text("Ex: 5", color = Color.Gray.copy(alpha = 0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        leadingIcon = {
            Icon(Icons.Default.Work, contentDescription = null, tint = SportyKidsGreen)
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SportyKidsGreen,
            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
            focusedLabelColor = SportyKidsGreen,
            focusedTextColor = TextDarkGray,
            unfocusedTextColor = TextDarkGray
        )
    )
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Certifications
    Text(
        text = "Certifications",
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = TextDarkGray,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(modifier = Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = newCertification,
            onValueChange = onNewCertificationChange,
            placeholder = { Text("Ajouter une certification", color = Color.Gray.copy(alpha = 0.5f)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SportyKidsGreen,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
        IconButton(
            onClick = {
                if (newCertification.isNotBlank()) {
                    onCertificationsChange(certifications + newCertification)
                    onNewCertificationChange("")
                }
            },
            modifier = Modifier
                .size(48.dp)
                .background(SportyKidsGreen, RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ajouter", tint = Color.White)
        }
    }
    
    // Liste des certifications
    if (certifications.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            certifications.chunked(2).forEach { rowCerts ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCerts.forEach { cert ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = SportyKidsGreen.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = cert,
                                    fontSize = 14.sp,
                                    color = SportyKidsGreen,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        onCertificationsChange(certifications.filter { it != cert })
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Supprimer",
                                        modifier = Modifier.size(16.dp),
                                        tint = SportyKidsGreen
                                    )
                                }
                            }
                        }
                    }
                    // Remplir avec un espace vide si impair
                    if (rowCerts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignUpScreenPreview() {
    DAM_frontTheme {
        SignUpScreen()
    }
}
