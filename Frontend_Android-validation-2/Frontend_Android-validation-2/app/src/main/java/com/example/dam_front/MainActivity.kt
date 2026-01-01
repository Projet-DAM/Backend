package com.example.dam_front

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dam_front.models.ChildResponse
import com.example.dam_front.repository.ChildRepository
import com.example.dam_front.ui.components.*
import com.example.dam_front.ui.navigation.AuthNavigation
import com.example.dam_front.ui.navigation.CoachNavigation
import com.example.dam_front.ui.screens.AddChildDialog
import com.example.dam_front.ui.screens.MainScreen
import com.example.dam_front.ui.screens.SplashScreen
import com.example.dam_front.ui.theme.CardWhite
import com.example.dam_front.ui.theme.DAM_frontTheme
import com.example.dam_front.ui.theme.*
import com.example.dam_front.utils.FirebaseTokenManager
import com.example.dam_front.utils.TokenManager
import com.example.dam_front.viewmodels.CoachHomeViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MainActivity", "Permission de notification accordée")
            initializeFirebase()
        } else {
            Log.w("MainActivity", "Permission de notification refusée")
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "Permission déjà accordée")
                    initializeFirebase()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Pour les versions antérieures à Android 13, pas besoin de permission
            initializeFirebase()
        }
    }
    
    private fun initializeFirebase() {
        val tokenManager = TokenManager(this)
        val firebaseTokenManager = FirebaseTokenManager(this)
        val userRepository = com.example.dam_front.repository.UserRepository(this)
        val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        
        coroutineScope.launch {
            try {
                val userId = tokenManager.getUserId().first()
                val userRole = tokenManager.getUserRole().first()
                
                if (userId != null && userRole != null) {
                    Log.d("MainActivity", "🔔 Initialisation Firebase pour userId=$userId, role=$userRole")
                    
                    val fcmToken = try {
                        firebaseTokenManager.getFCMToken()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ Impossible de récupérer le jeton FCM", e)
                        null
                    }

                    if (fcmToken != null) {
                        Log.d("MainActivity", "✅ Token FCM local : ${fcmToken.take(10)}...")
                        
                        // 1. MISE À JOUR DU TOKEN AU BACKEND (Crucial pour les deux rôles)
                        Log.d("MainActivity", "📤 Mise à jour du token pour l'utilisateur $userId...")
                        val updateResult = userRepository.updateFcmToken(userId, fcmToken)
                        
                        if (updateResult.isSuccess) {
                            Log.d("MainActivity", "✅ Token FCM synchronisé avec succès sur le serveur")
                        } else {
                            val error = updateResult.exceptionOrNull()
                            Log.e("MainActivity", "⚠️ Échec de synchronisation du token : ${error?.message}")
                            // On continue quand même pour les abonnements, mais c'est un mauvais signe
                        }

                        // 2. LOGIQUE PAR RÔLE
                        if (userRole.lowercase().contains("coach")) {
                            Log.d("MainActivity", "👨‍🏫 Role Coach: Abonnement au topic 'coaches'")
                            firebaseTokenManager.subscribeToTopic("coaches")
                        } else {
                            Log.d("MainActivity", "👨‍👩‍👧‍👦 Role Parent: Nettoyage et abonnements spécifiques")
                            firebaseTokenManager.unsubscribeFromTopic("coaches")
                            
                            // Même si le backend n'utilise pas encore les topics pour les programmes, 
                            // on les garde pour de futurs besoins ou si le backend est mis à jour
                            try {
                                val apiService = com.example.dam_front.api.RetrofitClient.getApiService(this@MainActivity)
                                val response = apiService.getEnrollments()
                                
                                if (response.isSuccessful && response.body() != null) {
                                    val enrollmentList = response.body()!!
                                    val programIds = enrollmentList.mapNotNull { it.program?.id }.distinct()
                                    
                                    Log.d("MainActivity", "📌 Abonnement à ${programIds.size} topics de programmes")
                                    programIds.forEach { id ->
                                        if (id.isNotEmpty()) firebaseTokenManager.subscribeToTopic("program_$id")
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MainActivity", "⚠️ Erreur abonnements topics parent", e)
                            }
                        }
                    } else {
                        Log.e("MainActivity", "❌ Token FCM null, les notifications ne fonctionneront pas")
                    }
                } else {
                    Log.d("MainActivity", "ℹ️ Utilisateur non connecté, skipping Firebase init")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "💥 Crash lors de l'initialisation Firebase", e)
            }
        }
    }
    
    // Reactive state for deep link URI
    private val deepLinkUri = mutableStateOf<android.net.Uri?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri.value = intent.data
        Log.d("MainActivity", "onNewIntent: Capture Deep Link ${intent.data}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Capture initial intent data
        deepLinkUri.value = intent.data
        
        // Demander la permission de notification
        requestNotificationPermission()
        
        setContent {
            DAM_frontTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CardWhite
                ) {
                    var showSplash by remember { mutableStateOf(true) }
                    var isAuthenticated by remember { mutableStateOf(false) }
                    var userRole by remember { mutableStateOf<String?>(null) }
                    var isLoadingRole by remember { mutableStateOf(false) }
                    
                    // Deep Link State derived from the reactive URI
                    val initialProgramId = remember(deepLinkUri.value) {
                        val uri = deepLinkUri.value
                        if (uri?.scheme == "sporty" && uri?.host == "program") {
                            uri.lastPathSegment
                        } else null
                    }
                    
                    val tokenManager = remember { TokenManager(this@MainActivity) }
                    val coroutineScope = rememberCoroutineScope()
                    
                    LaunchedEffect(initialProgramId) {
                        if (initialProgramId != null) {
                            Log.d("MainActivity", "Active Deep Link for Program: $initialProgramId")
                        }
                    }
                                        
                    // Afficher la splash screen d'abord
                    if (showSplash) {
                        SplashScreen(
                            onSplashFinished = {
                                showSplash = false
                                // Vérifier si l'utilisateur est déjà connecté après la splash
                                coroutineScope.launch {
                                    val token = tokenManager.getToken().first()
                                    if (token != null) {
                                        isLoadingRole = true
                                        userRole = tokenManager.getUserRole().first()
                                        isLoadingRole = false
                                        isAuthenticated = true
                                        Log.d("MainActivity", "Utilisateur déjà connecté, rôle: $userRole")
                                        
                                        // Initialiser Firebase pour les notifications
                                        initializeFirebase()
                                    }
                                }
                            }
                        )
                    } else if (isAuthenticated && !isLoadingRole) {
                        // Afficher l'interface selon le rôle
                        Log.d("MainActivity", "Affichage interface - Rôle: '$userRole'")
                        val roleLower = userRole?.lowercase()?.trim()
                        
                        when {
                            roleLower == "coach" || roleLower == "entraineur" || roleLower?.contains("coach") == true -> {
                                Log.d("MainActivity", "✅ Affichage interface COACH")
                                CoachNavigation(
                                    onLogout = {
                                        isAuthenticated = false
                                        userRole = null
                                    }
                                )
                            }
                            else -> {
                                Log.d("MainActivity", "✅ Affichage interface PARENT (rôle: '$userRole')")
                                MainScreen(
                                    initialProgramId = initialProgramId,
                                    onLogout = {
                                        isAuthenticated = false
                                        userRole = null
                                    }
                                )
                            }
                        }
                    } else if (isLoadingRole) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AuthNavigation(
                            onNavigateToMain = {
                                coroutineScope.launch {
                                    isLoadingRole = true
                                    userRole = tokenManager.getUserRole().first()
                                    Log.d("MainActivity", "Rôle récupéré après login: $userRole")
                                    isLoadingRole = false
                                    isAuthenticated = true
                                    
                                    // Initialiser Firebase pour les notifications
                                    initializeFirebase()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
