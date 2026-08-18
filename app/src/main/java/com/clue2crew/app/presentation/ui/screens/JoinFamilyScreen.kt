package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold

@Composable
fun JoinFamilyScreen(navController: NavController) {
    var pairingCode by remember { mutableStateOf("") }

    Clue2CrewScaffold(navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Text(
                text = "Join Family",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Mock QR Scan */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B263B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scan Family QR", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "OR", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = pairingCode,
                onValueChange = { pairingCode = it },
                label = { Text("Enter Pairing Code", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF415A77),
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (pairingCode.isNotEmpty()) {
                        navController.navigate(Screen.FamilyDashboard.route) 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A77)),
                shape = RoundedCornerShape(12.dp),
                enabled = pairingCode.isNotEmpty()
            ) {
                Text("Join Family", fontSize = 18.sp)
            }
        }
    }
}
