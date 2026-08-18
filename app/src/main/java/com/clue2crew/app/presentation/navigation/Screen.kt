package com.clue2crew.app.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object LocalAccess : Screen("local_access")
    object Home : Screen("home")
    object FamilyDashboard : Screen("family_dashboard")
    object CreateFamily : Screen("create_family")
    object JoinFamily : Screen("join_family")
    object FindFamily : Screen("find_family/{memberId}") {
        fun createRoute(memberId: String) = "find_family/$memberId"
    }
    object Emergency : Screen("emergency")
    object OfflineTransfer : Screen("offline_transfer")
}
