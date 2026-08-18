package com.clue2crew.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.clue2crew.app.presentation.navigation.AppNavigation
import com.clue2crew.app.ui.theme.Clue2CrewTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Clue2CrewTheme {
                val navController = rememberNavController()
                AppNavigation(navController = navController)
            }
        }
    }
}
