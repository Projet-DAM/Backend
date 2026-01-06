package com.example.dam_front.ui.screens

import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.dam_front.config.ApiConfig
import com.example.dam_front.models.User
import com.example.dam_front.utils.ImageUtils
import com.example.dam_front.repository.AuthRepository
import com.example.dam_front.ui.theme.*
import com.example.dam_front.viewmodels.ConversationViewModel
import com.example.dam_front.viewmodels.ConversationViewModelFactory
import kotlin.collections.filter
import kotlin.let
import kotlin.text.contains
import kotlin.text.ifBlank
import kotlin.text.isBlank
import kotlin.text.trim
import kotlin.text.uppercase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactListScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val application = context.applicationContext as Application
    
    val vm: ConversationViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = ConversationViewModelFactory(application)
    )
    
    val uiState by vm.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val authRepo = AuthRepository(context)
            userRole = authRepo.getUserRole() ?: ""
            Log.d("ContactListScreen", "User role retrieved: '$userRole'")
            vm.loadAvailableContacts()
        } catch (e: Exception) {
            Log.e("ContactListScreen", "Error loading contacts", e)
        }
    }
    
    val filteredContacts = remember(uiState.availableContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.availableContacts
        } else {
            uiState.availableContacts.filter { user ->
                val fullName = "${user.prenom} ${user.nom}".trim()
                fullName.contains(searchQuery, ignoreCase = true) ||
                user.email?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parents", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HeaderBlue
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {

        // Search Bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Rechercher un contact", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        // Contacts List / States
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentOrange)
            }
        } else if (uiState.error != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Erreur de connexion",
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            uiState.error ?: "Erreur inconnue",
                            color = Color(0xFFD32F2F),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Rôle utilisateur: $userRole",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { vm.loadAvailableContacts() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
                ) {
                    Text("Réessayer")
                }
            }
        } else if (filteredContacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val emptyText = if (userRole?.uppercase() == "COACH") 
                    "Aucun parent disponible" 
                else 
                    "Aucun coach disponible"
                Text(emptyText, color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts) { user ->
                    ContactItem(
                        user = user,
                        onClick = {
                            navController.navigate("chat/${user.id}")
                        }
                    )
                }
            }
        }
        } // Close Column
    } // Close Scaffold
}

@Composable
fun ContactItem(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, AccentOrange.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            val photoUrl = ImageUtils.getPhotoUrl(user.photoProfil) ?: "https://via.placeholder.com/64"

            AsyncImage(
                model = photoUrl,
                contentDescription = "Photo de profil",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(NavGrayBg),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                val fullName = "${user.prenom} ${user.nom}".trim().ifBlank { "Utilisateur" }
                Text(
                    text = fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AccentOrange
                )
                user.email?.let { email ->
                    Text(
                        text = email,
                        color = IconInactiveGray,
                        fontSize = 14.sp
                    )
                }
            }

            // Arrow Icon
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ouvrir conversation",
                tint = AccentOrange
            )
        }
    }
}