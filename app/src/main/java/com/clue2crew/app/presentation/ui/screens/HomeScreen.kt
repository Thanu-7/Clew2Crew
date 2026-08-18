package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold

@Composable
fun HomeScreen(navController: NavController) {
    Clue2CrewScaffold(navController = navController) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Good Morning, User",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                DashboardCard(
                    title = "My Family Group",
                    subtitle = "3 Members • 2 Connected",
                    onClick = { navController.navigate(Screen.FamilyDashboard.route) }
                )
            }

            item {
                Button(
                    onClick = { navController.navigate(Screen.FindFamily.createRoute("1")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B263B)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("FIND FAMILY", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatusCard(
                        modifier = Modifier.weight(1f),
                        title = "Offline Status",
                        icon = Icons.Default.Info,
                        color = Color(0xFF415A77),
                        onClick = { navController.navigate(Screen.OfflineTransfer.route) }
                    )
                    StatusCard(
                        modifier = Modifier.weight(1f),
                        title = "Emergency Mode",
                        icon = Icons.Default.Warning,
                        color = Color(0xFF780000),
                        onClick = { navController.navigate(Screen.Emergency.route) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(text = title, color = Color(0xFF0D1B2A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(text = subtitle, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun StatusCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color.White)
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
