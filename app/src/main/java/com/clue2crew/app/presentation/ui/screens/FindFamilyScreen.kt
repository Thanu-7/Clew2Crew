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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold

import com.clue2crew.app.common.utils.LocationUtils

@Composable
fun FindFamilyScreen(navController: NavController, memberId: String?, viewModel: FamilyViewModel) {
    val members by viewModel.members.collectAsState()
    val myLocation by viewModel.currentDeviceLocation.collectAsState()
    val member = members.find { it.memberId == memberId }

    val hasData = myLocation != null && member?.latitude != null && member.longitude != null
    
    val distance = if (hasData) {
        LocationUtils.calculateDistance(myLocation!!.latitude, myLocation!!.longitude, member!!.latitude!!, member.longitude!!)
    } else {
        "---"
    }

    val bearing = if (hasData) {
        LocationUtils.calculateBearing(myLocation!!.latitude, myLocation!!.longitude, member!!.latitude!!, member.longitude!!)
    } else {
        0f
    }

    val direction = if (hasData) {
        LocationUtils.getCardinalDirection(bearing)
    } else {
        "SEARCHING..."
    }

    Clue2CrewScaffold(navController = navController, viewModel = viewModel) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (member != null) "Finding ${member.name}" else "Member not found",
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
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "Direction",
                    tint = Color.White,
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(bearing)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = distance,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = direction,
                color = Color(0xFF415A77),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (hasData) "Walk in this direction to find your family member." else "Waiting for GPS fix...",
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                ConnectionBadge(isConnected = member?.status == "Connected")
                Spacer(modifier = Modifier.width(16.dp))
                LocationStatus(isLive = true)
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
