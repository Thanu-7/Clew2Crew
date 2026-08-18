package com.clue2crew.app.presentation.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.clue2crew.app.presentation.navigation.Screen

sealed class BottomNavItem(val screen: Screen, val icon: ImageVector, val label: String) {
    object Home : BottomNavItem(Screen.Home, Icons.Default.Home, "Home")
    object Family : BottomNavItem(Screen.FamilyDashboard, Icons.Default.FamilyRestroom, "Family")
    object Find : BottomNavItem(Screen.FindFamily, Icons.Default.Search, "Find")
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Family,
        BottomNavItem.Find
    )
    NavigationBar(
        containerColor = Color(0xFF1B263B),
        contentColor = Color.White
    ) {
        val navBackStackEntry = navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry.value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.screen.route || (item.screen is Screen.FindFamily && currentRoute?.startsWith("find_family") == true),
                onClick = {
                    navController.navigate(if (item.screen is Screen.FindFamily) Screen.FindFamily.createRoute("1") else item.screen.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF415A77)
                )
            )
        }
    }
}
