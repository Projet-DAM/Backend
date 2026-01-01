package com.example.dam_front.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dam_front.R
import com.example.dam_front.ui.theme.CardWhite
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Configurer la status bar pour qu'elle soit blanche
    val systemUiController = rememberSystemUiController()
    
    LaunchedEffect(systemUiController) {
        systemUiController.setStatusBarColor(
            color = CardWhite,
            darkIcons = true // Icônes sombres sur fond blanc
        )
    }
    
    // Afficher la splash screen pendant 2 secondes
    LaunchedEffect(Unit) {
        delay(2000) // 2 secondes
        onSplashFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo Sporty Kids
            Image(
                painter = painterResource(id = R.drawable.logo2),
                contentDescription = "Sporty Kids Logo",
                modifier = Modifier.size(280.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = com.example.dam_front.ui.theme.SportyKidsOrange
            )
        }
    }
}

