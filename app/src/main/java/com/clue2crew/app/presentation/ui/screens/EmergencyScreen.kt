package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel

@Composable
fun EmergencyScreen(navController: NavController, viewModel: FamilyViewModel) {
    Clue2CrewScaffold(navController = navController, viewModel = viewModel) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Emergency Mode",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Family Reunification Actions",
                color = Color.LightGray,
                fontSize = 16.sp
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            EmergencyButton(
                text = "ALERT FAMILY",
                description = "Notify all paired devices that you need assistance.",
                color = Color(0xFF780000),
                onClick = { viewModel.sendEmergencyAlert() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            EmergencyButton(
                text = "SHARE LAST KNOWN LOCATION",
                description = "Broadcast your last GPS coordinates to nearby family devices.",
                color = Color(0xFF1B263B),
                onClick = { /* Mock */ }
            )

            Spacer(modifier = Modifier.height(24.dp))

            EmergencyButton(
                text = "MARK SAFE",
                description = "Inform your family that you are safe and no longer need assistance.",
                color = Color(0xFF2E7D32),
                onClick = { /* Mock */ }
            )
        }
    }
}

@Composable
fun EmergencyButton(text: String, description: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text(text = description, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = Color.White.copy(alpha = 0.8f))
        }
    }
}
