package com.clue2crew.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel
import com.clue2crew.app.presentation.ui.screens.*

@Composable
fun AppNavigation(navController: NavHostController) {
    val familyViewModel: FamilyViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Onboarding.route) { OnboardingScreen(navController) }
        composable(Screen.LocalAccess.route) { LocalAccessScreen(navController) }
        composable(Screen.Home.route) { HomeScreen(navController, familyViewModel) }
        composable(Screen.FamilyDashboard.route) { FamilyDashboardScreen(navController, familyViewModel) }
        composable(Screen.CreateFamily.route) { CreateFamilyScreen(navController, familyViewModel) }
        composable(Screen.JoinFamily.route) { JoinFamilyScreen(navController, familyViewModel) }
        composable(Screen.FindFamily.route) { backStackEntry ->
            val memberId = backStackEntry.arguments?.getString("memberId")
            FindFamilyScreen(navController, memberId, familyViewModel)
        }
        composable(Screen.Emergency.route) { EmergencyScreen(navController, familyViewModel) }
        composable(Screen.OfflineTransfer.route) { OfflineTransferScreen(navController, familyViewModel) }
    }
}
