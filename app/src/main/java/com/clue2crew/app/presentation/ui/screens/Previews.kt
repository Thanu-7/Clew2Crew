package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.clue2crew.app.ui.theme.Clue2CrewTheme
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.navigation.compose.rememberNavController

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    Clue2CrewTheme {
        SplashScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
fun DashboardCardPreview() {
    Clue2CrewTheme {
        DashboardCard(
            title = "The Smith Family",
            subtitle = "4 Members • 3 Connected",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1B2A)
@Composable
fun StatusCardPreview() {
    Clue2CrewTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusCard(
                title = "Offline Status",
                icon = androidx.compose.material.icons.Icons.Default.Info,
                color = androidx.compose.ui.graphics.Color(0xFF415A77),
                onClick = {}
            )
            StatusCard(
                title = "Emergency Mode",
                icon = androidx.compose.material.icons.Icons.Default.Warning,
                color = androidx.compose.ui.graphics.Color(0xFF780000),
                onClick = {}
            )
        }
    }
}
