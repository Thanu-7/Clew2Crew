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
fun CreateFamilyScreen(navController: NavController, viewModel: FamilyViewModel) {
    var familyName by remember { mutableStateOf("") }
    val family by viewModel.family.collectAsState()
    val error by viewModel.error.collectAsState()

    // Navigate to dashboard once family is created
    LaunchedEffect(family) {
        if (family != null) {
            navController.navigate(Screen.FamilyDashboard.route) {
                popUpTo(Screen.CreateFamily.route) { inclusive = true }
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
                text = "Create Family Group",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = familyName,
                onValueChange = { familyName = it },
                label = { Text("Family Group Name", color = Color.Gray) },
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

            Button(
                onClick = { 
                    if (familyName.isNotEmpty()) {
                        viewModel.createFamily(familyName)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A77)),
                shape = RoundedCornerShape(12.dp),
                enabled = familyName.isNotEmpty()
            ) {
                Text("Create Family", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(text = "Sharing Options (Post-Creation)", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { /* Future Implementation */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                enabled = false
            ) {
                Text("Show QR Code (Upcoming)")
            }
        }
    }
}
