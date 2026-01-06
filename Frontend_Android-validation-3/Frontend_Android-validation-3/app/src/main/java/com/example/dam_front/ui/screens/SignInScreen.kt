package com.example.dam_front.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dam_front.models.LoginRequest
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.ui.components.SportyKidsLogo
import com.example.dam_front.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.ui.window.Dialog

@Composable
fun SignInScreen(
    onSignInClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    val prefs = context.getSharedPreferences("SportyKidsPrefs", Context.MODE_PRIVATE)
    
    // Charger les informations sauvegardées
    var email by remember { 
        mutableStateOf(prefs.getString("saved_email", "") ?: "") 
    }
    var password by remember { 
        mutableStateOf(prefs.getString("saved_password", "") ?: "") 
    }
    var rememberMe by remember { 
        mutableStateOf(prefs.getBoolean("remember_me", false)) 
    }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    // Configurer la status bar pour qu'elle soit blanche
    val systemUiController = rememberSystemUiController()
    
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = CardWhite,
            darkIcons = true // Icônes sombres sur fond blanc
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite)
            .padding(horizontal = 32.dp, vertical = 40.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        // Sporty Kids Logo
        SportyKidsLogo(
            modifier = Modifier.padding(bottom = 8.dp),
            logoSize = 200.dp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Email Input
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { 
                Text(
                    "Email",
                    color = SportyKidsTeal.copy(alpha = 0.7f)
                ) 
            },
            placeholder = { Text("parent@example.com", color = Color.Gray.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SportyKidsOrange,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedLabelColor = SportyKidsOrange,
                focusedTextColor = TextDarkGray,
                unfocusedTextColor = TextDarkGray
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Password Input
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { 
                Text(
                    "Mot de passe",
                    color = SportyKidsTeal.copy(alpha = 0.7f)
                ) 
            },
            placeholder = { Text("••••••••", color = Color.Gray.copy(alpha = 0.5f)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Masquer le mot de passe" else "Afficher le mot de passe",
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

        // Remember Me Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = SportyKidsOrange,
                    uncheckedColor = Color.Gray.copy(alpha = 0.5f)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Se souvenir de moi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDarkGray.copy(alpha = 0.7f),
                modifier = Modifier.clickable { rememberMe = !rememberMe }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot Password Link
        Text(
            text = "Mot de passe oublié ?",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = SportyKidsOrange,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showForgotPasswordDialog = true },
            textAlign = TextAlign.End
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error Message
        errorMessage?.let { error ->
            Text(
                text = error,
                color = Color(0xFFD32F2F), // Red error color
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        // Sign In Button
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
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Veuillez remplir tous les champs"
                        return@clickable
                    }
                    
                    isLoading = true
                    errorMessage = null
                    
                    coroutineScope.launch {
                        val result = authRepository.login(LoginRequest(email, password))
                        isLoading = false
                        
                        result.onSuccess {
                            // Sauvegarder les informations si "Remember me" est coché
                            with(prefs.edit()) {
                                if (rememberMe) {
                                    putString("saved_email", email)
                                    putString("saved_password", password)
                                    putBoolean("remember_me", true)
                                } else {
                                    remove("saved_email")
                                    remove("saved_password")
                                    putBoolean("remember_me", false)
                                }
                                apply()
                            }
                            onSignInClick()
                        }.onFailure { exception ->
                            errorMessage = exception.message ?: "Erreur de connexion"
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
                    text = "Se connecter",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        
        // Sign Up Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pas encore de compte ? ",
                fontSize = 14.sp,
                color = TextDarkGray.copy(alpha = 0.7f)
            )
            Text(
                text = "Créer un compte",
                fontSize = 14.sp,
                color = SportyKidsOrange,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Dialog mot de passe oublié
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                onSendEmail = { email ->
                    onForgotPasswordClick()
                    // Ici vous pouvez ajouter l'appel API pour réinitialiser le mot de passe
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SignInScreenPreview() {
    DAM_frontTheme {
        SignInScreen()
    }
}

