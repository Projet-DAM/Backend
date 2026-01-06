package com.example.dam_front.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import com.example.dam_front.ui.screens.CoachCreateSuiviScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dam_front.ui.screens.COACH_CHILD_DETAIL_ROUTE
import com.example.dam_front.ui.screens.COACH_HOME_ROUTE
import com.example.dam_front.ui.screens.CoachChildDetailScreen
import com.example.dam_front.ui.screens.CoachMainScreen
import com.example.dam_front.ui.screens.ContactListScreen
import com.example.dam_front.ui.screens.ChatScreen

@Composable
fun CoachNavigation(
    navController: NavHostController = rememberNavController(),
    onLogout: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = "coach_main"
    ) {
        composable("coach_main") {
            CoachMainScreen(
                navController = navController,
                onLogout = onLogout
            )
        }
        // allow opening create screen without an enfantId (coach selects from dropdown)
        composable(route = "coach_create_suivi") {
            CoachCreateSuiviScreen(navController = navController, enfantId = "")
        }
        composable(route = "coach_create_suivi/{enfantId}") { backStackEntry ->
            val enfantId = backStackEntry.arguments?.getString("enfantId") ?: ""
            CoachCreateSuiviScreen(navController = navController, enfantId = enfantId)
        }
        composable(route = "coach_edit_suivi/{enfantId}/{suiviId}") { backStackEntry ->
            val enfantId = backStackEntry.arguments?.getString("enfantId") ?: ""
            val suiviId = backStackEntry.arguments?.getString("suiviId") ?: ""
            CoachCreateSuiviScreen(navController = navController, enfantId = enfantId, suiviId = suiviId)
        }
        composable(
            route = "$COACH_CHILD_DETAIL_ROUTE/{childId}",
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
            CoachChildDetailScreen(navController = navController, childId = childId)
        }
        composable("chat_list") {
            ContactListScreen(navController = navController)
        }
        composable(
            route = "chat/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            if (userId != null) {
                ChatScreen(userId = userId, navController = navController)
            }
        }
    }
}
