package com.example.dam_front

import android.Manifest
import android.content.Context
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
import com.example.dam_front.ui.screens.CoachMainScreen
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
            Log.d("MainActivity", "✅ Permission de notification accordée")
            initializeFirebase()
        } else {
            Log.w("MainActivity", "❌ Permission de notification refusée")
            // On tente quand même d'initialiser le token mais les notifications 
            // visuelles seront bloquées par le système
            initializeFirebase()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("MainActivity", "🚀 OnCreate started")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        } else {
            initializeFirebase()
        }

        setContent {
            val systemUiController = rememberSystemUiController()
            
            SideEffect {
                systemUiController.setStatusBarColor(
                    color = Color.Transparent,
                    darkIcons = true
                )
            }

            DAM_frontTheme {
                val tokenManager = remember { TokenManager(this) }
                val userRoleState = tokenManager.getUserRole().collectAsState(initial = null)
                val userIdState = tokenManager.getUserId().collectAsState(initial = null)
                val userRole = userRoleState.value
                val userId = userIdState.value

                var showSplash by remember { mutableStateOf(true) }
                
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(2000)
                    showSplash = false
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    when {
                        showSplash -> SplashScreen(onSplashFinished = { showSplash = false })
                        userId == null -> AuthNavigation()
                        else -> {
                            val isCoach = userRole?.lowercase()?.contains("coach") == true || 
                                          userRole?.lowercase()?.contains("admin") == true ||
                                          userRole?.lowercase()?.contains("acad") == true ||
                                          userRole?.lowercase()?.contains("entrain") == true
                            
                            Log.d("MainActivity", "Navigation pour role: $userRole (isCoach: $isCoach)")
                            
                            if (isCoach) {
                            CoachNavigation(onLogout = { /* Splash seulement au lancement */ })
                        } else {
                            MainScreen(onLogout = { /* Splash seulement au lancement */ })
                        }
                        }
                    }
                }
            }
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
                    Log.d("MainActivity", "Demande de permission de notification...")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            initializeFirebase()
        }
    }
    
    override fun onResume() {
        super.onResume()
        initializeFirebase()
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
                
                if (userId != null) {
                    if (userRole == null || userRole == "null" || userRole.isEmpty()) {
                        Log.d("MainActivity", "⏳ Role en attente pour userId=$userId...")
                        return@launch
                    }
                    
                    Log.d("MainActivity", "🔔 Initialisation Firebase pour role=$userRole")
                    
                    val fcmToken = try {
                        firebaseTokenManager.getFCMToken()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ Erreur token FCM", e)
                        null
                    }

                    if (fcmToken != null) {
                        Log.d("MainActivity", "✅ Token FCM prêt")
                        userRepository.updateFcmToken(userId, fcmToken)
                        
                        val roleLower = userRole.lowercase()
                        if (roleLower.contains("coach") || 
                            roleLower.contains("acadé") || 
                            roleLower.contains("entrain") || 
                            roleLower.contains("admin")) {
                            Log.d("MainActivity", "👨‍🏫 Staff - Inscription aux topics notification")
                            firebaseTokenManager.subscribeToTopic("coaches")
                            firebaseTokenManager.subscribeToTopic("coach")
                        } else {
                            Log.d("MainActivity", "👨‍👩‍👧‍👦 Parent - Nettoyage topics staff")
                            firebaseTokenManager.unsubscribeFromTopic("coaches")
                            firebaseTokenManager.unsubscribeFromTopic("coach")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "💥 Crash lors de l'initialisation Firebase", e)
            }
        }
    }
}
