package com.example.dam_front.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dam_front.ui.screens.SignInScreen
import com.example.dam_front.ui.screens.SignUpScreen

sealed class AuthScreen(val route: String) {
    object SignIn : AuthScreen("sign_in")
    object SignUp : AuthScreen("sign_up")
}

@Composable
fun AuthNavigation(
    navController: NavHostController = rememberNavController(),
    onNavigateToMain: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = AuthScreen.SignIn.route
    ) {
        composable(AuthScreen.SignIn.route) {
            SignInScreen(
                onSignInClick = {
                    // TODO: Implement sign in logic
                    // For now, navigate to main screen
                    onNavigateToMain()
                },
                onSignUpClick = {
                    navController.navigate(AuthScreen.SignUp.route)
                },
                onForgotPasswordClick = {
                    // TODO: Implement forgot password logic
                }
            )
        }
        
        composable(AuthScreen.SignUp.route) {
            SignUpScreen(
                onSignUpClick = { role ->
                    // Le rôle est déjà sauvegardé dans TokenManager par AuthRepository
                    // On navigue vers l'écran principal qui déterminera l'interface selon le rôle
                    onNavigateToMain()
                },
                onSignInClick = {
                    navController.navigate(AuthScreen.SignIn.route) {
                        popUpTo(AuthScreen.SignIn.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}



