package com.example.dam_front.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.ui.components.SportyKidsLogo
import com.example.dam_front.ui.theme.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendEmail: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val authRepository = remember { AuthRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icône de fermeture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fermer",
                            tint = SportyKidsTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Icône de cadenas
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    SportyKidsOrange.copy(alpha = 0.2f),
                                    SportyKidsBlue.copy(alpha = 0.1f)
                                )
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = SportyKidsOrange
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Titre
                Text(
                    text = "Mot de passe oublié ?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SportyKidsTeal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = "Entrez votre adresse email et nous vous enverrons un lien pour réinitialiser votre mot de passe.",
                    fontSize = 14.sp,
                    color = TextDarkGray.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Note temporaire
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = SportyKidsOrange.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "ℹ️ Note: Cette fonctionnalité sera bientôt disponible. L'email sera envoyé automatiquement une fois le backend configuré.",
                        fontSize = 12.sp,
                        color = SportyKidsOrange,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Champ email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { 
                        Text(
                            "Email",
                            color = SportyKidsTeal.copy(alpha = 0.7f)
                        ) 
                    },
                    placeholder = { 
                        Text(
                            "votre@email.com",
                            color = Color.Gray.copy(alpha = 0.5f)
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = SportyKidsOrange
                        )
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
                
                // Message d'erreur
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFD32F2F),
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Message de succès
                successMessage?.let { success ->
                    Text(
                        text = success,
                        color = SportyKidsGreen,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bouton envoyer
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
                            enabled = !isLoading && email.isNotBlank(),
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (email.isBlank()) {
                                errorMessage = "Veuillez entrer votre email"
                                return@clickable
                            }
                            
                            // Validation basique de l'email
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Veuillez entrer une adresse email valide"
                                return@clickable
                            }
                            
                            isLoading = true
                            errorMessage = null
                            successMessage = null
                            
                            coroutineScope.launch {
                                val result = authRepository.forgotPassword(email)
                                isLoading = false
                                
                                result.onSuccess {
                                    successMessage = "Demande enregistrée ! L'email sera envoyé une fois le backend configuré.\n\nEmail: $email"
                                    onSendEmail(email)
                                    // Fermer le dialog après 4 secondes
                                    kotlinx.coroutines.delay(4000)
                                    onDismiss()
                                }.onFailure { exception ->
                                    val errorMsg = exception.message ?: "Erreur lors de l'envoi de l'email"
                                    // Si c'est un 404, donner un message plus explicite
                                    if (errorMsg.contains("404") || errorMsg.contains("not found")) {
                                        errorMessage = "L'endpoint de réinitialisation n'est pas disponible sur le serveur. Veuillez contacter l'administrateur."
                                    } else {
                                        errorMessage = errorMsg
                                    }
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
                            text = "Envoyer",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lien retour
                Text(
                    text = "Retour à la connexion",
                    fontSize = 14.sp,
                    color = SportyKidsOrange,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onDismiss() }
                )
            }
        }
    }
}

