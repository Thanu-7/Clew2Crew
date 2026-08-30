package com.clue2crew.app.presentation.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel

@Composable
fun Clue2CrewScaffold(
    navController: NavController,
    viewModel: FamilyViewModel,
    showBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController, viewModel = viewModel)
            }
        },
        containerColor = Color(0xFF0D1B2A)
    ) { innerPadding ->
        content(innerPadding)
    }
}
