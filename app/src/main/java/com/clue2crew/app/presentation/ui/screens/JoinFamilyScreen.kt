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
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold

@Composable
fun JoinFamilyScreen(navController: NavController, viewModel: FamilyViewModel) {
    val savedUserName by viewModel.userName.collectAsState()
    var userName by remember { mutableStateOf(if (savedUserName == "Me") "" else savedUserName) }
    var pairingCode by remember { mutableStateOf("") }
    val family by viewModel.family.collectAsState()
    val error by viewModel.error.collectAsState()
    val isJoining by viewModel.isJoiningInProgress.collectAsState()
    val statusMessage by viewModel.bleStatusMessage.collectAsState()

    // Navigate to dashboard if family is successfully joined
    LaunchedEffect(family) {
        if (family != null) {
            navController.navigate(Screen.FamilyDashboard.route) {
                popUpTo(Screen.JoinFamily.route) { inclusive = true }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearError()
        }
    }

    Clue2CrewScaffold(navController = navController, viewModel = viewModel) { innerPadding ->
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
            
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Your Name", color = Color.Gray) },
                placeholder = { Text("e.g. Bob", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF415A77),
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pairingCode,
                onValueChange = { pairingCode = it.uppercase() },
                label = { Text("Family ID or Pairing Code", color = Color.Gray) },
                placeholder = { Text("e.g. AB3DX9", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF415A77),
                    unfocusedBorderColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                isError = error != null
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isJoining) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFF415A77))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = statusMessage, color = Color.LightGray, fontSize = 14.sp)
                }
            } else {
                Button(
                    onClick = { 
                        if (pairingCode.isNotBlank() && userName.isNotBlank()) {
                            viewModel.initiateJoinProcess(pairingCode, userName)
                            navController.navigate(Screen.OfflineTransfer.route)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A77)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = pairingCode.isNotBlank() && userName.isNotBlank()
                ) {
                    Text("SEND", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
