package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.common.utils.MockData
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold

@Composable
fun FindFamilyScreen(navController: NavController, memberId: String?) {
    val member = MockData.familyGroup.members.find { it.id == memberId } ?: MockData.mom

    Clue2CrewScaffold(navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Finding ${member.name}",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Compass/Directional UI
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color(0xFF1B263B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Mock rotation based on direction
                val rotation = when(member.direction) {
                    "NORTH" -> 0f
                    "NORTH-EAST" -> 45f
                    "EAST" -> 90f
                    "SOUTH-EAST" -> 135f
                    "SOUTH" -> 180f
                    "SOUTH-WEST" -> 225f
                    "WEST" -> 270f
                    "NORTH-WEST" -> 315f
                    else -> 0f
                }
                
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Direction",
                    tint = Color.White,
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(rotation)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = member.distance,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = member.direction,
                color = Color(0xFF415A77),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Walk in this direction to find your family member.",
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionBadge(isConnected = member.isConnected)
                Spacer(modifier = Modifier.width(16.dp))
                LocationStatus(isLive = member.isLive)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(Screen.FamilyDashboard.route) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF415A77)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Select Family Member")
            }
        }
    }
}
