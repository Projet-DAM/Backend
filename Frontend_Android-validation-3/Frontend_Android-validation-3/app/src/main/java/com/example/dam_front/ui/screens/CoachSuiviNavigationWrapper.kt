package com.example.dam_front.ui.screens

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dam_front.viewmodels.CoachHomeViewModel
import com.example.dam_front.viewmodels.CoachHomeViewModelFactory
import com.example.dam_front.viewmodels.SuiviSharedViewModel
import com.example.dam_front.viewmodels.SuiviSharedViewModelFactory

/**
 * Wrapper de navigation pour la section "Mon Enfant" du coach
 * Gère la navigation interne sans toucher à la bottom nav de CoachMainScreen
 */
@Composable
fun CoachSuiviNavigationWrapper(
    onChatClick: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    
    // ViewModels
    val coachViewModel: CoachHomeViewModel = viewModel(
        factory = CoachHomeViewModelFactory(application)
    )
    
    val activity = LocalContext.current as androidx.activity.ComponentActivity
    val sharedVm: SuiviSharedViewModel = viewModel(
        viewModelStoreOwner = activity,
        factory = SuiviSharedViewModelFactory(application)
    )
    
    // Internal navigation controller for this section
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "suivi_list"
    ) {
        // List screen
        composable("suivi_list") {
            CoachSuiviListScreen(
                coachViewModel = coachViewModel,
                sharedViewModel = sharedVm,
                onCreateSuivi = {
                    navController.navigate("suivi_create/new_suivi")
                },
                onChildDetail = { childId ->
                    navController.navigate("suivi_detail/$childId")
                },
                onChatClick = onChatClick,
                onAiSummaryClick = { childId ->
                    navController.navigate("ai_summary/$childId")
                }
            )
        }

        // AI Summary Screen
        composable("ai_summary/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            SuiviAiSummaryScreen(
                navController = navController,
                childId = childId
            )
        }
        
        // Create screen
        composable("suivi_create/{enfantId}") { backStackEntry ->
            val idArg = backStackEntry.arguments?.getString("enfantId")
            val enfantId = if (idArg == "new_suivi") "" else idArg ?: ""
            CoachCreateSuiviScreen(
                navController = navController,
                enfantId = enfantId
            )
        }
        
        // Detail screen
        composable("suivi_detail/{childId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            CoachChildDetailScreen(
                navController = navController,
                childId = childId
            )
        }

        // Edit screen
        composable("suivi_edit/{childId}/{suiviId}") { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val suiviId = backStackEntry.arguments?.getString("suiviId")
            CoachCreateSuiviScreen(
                navController = navController,
                enfantId = childId,
                suiviId = suiviId
            )
        }
    }
}
