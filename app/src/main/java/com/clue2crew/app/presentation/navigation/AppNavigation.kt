package com.clue2crew.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clue2crew.app.presentation.ui.screens.*

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.LocalAccess.route) { LocalAccessScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController) }
        composable(Screen.FamilyDashboard.route) { FamilyDashboardScreen(navController) }
        composable(Screen.CreateFamily.route) { CreateFamilyScreen(navController) }
        composable(Screen.JoinFamily.route) { JoinFamilyScreen(navController) }
        composable(Screen.FindFamily.route) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId")
            FindFamilyScreen(navController, memberId)
        }
        composable(Screen.Emergency.route) { EmergencyScreen(navController) }
        composable(Screen.OfflineTransfer.route) { OfflineTransferScreen(navController) }
    }
}
