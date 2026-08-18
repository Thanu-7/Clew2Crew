package com.clue2crew.app.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.common.models.FamilyMember
import com.clue2crew.app.common.utils.MockData
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold

@Composable
fun FamilyDashboardScreen(navController: NavController) {
    val members = MockData.familyGroup.members

    Clue2CrewScaffold(navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Family",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { navController.navigate(Screen.CreateFamily.route) },
                    modifier = Modifier.background(Color(0xFF415A77), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(members) { member ->
                    FamilyMemberCard(member = member) {
                        navController.navigate(Screen.FindFamily.createRoute(member.id))
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.JoinFamily.route) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B263B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join Existing Family")
                    }
                }
            }
        }
    }
}

@Composable
fun FamilyMemberCard(member: FamilyMember, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = member.name,
                    color = Color(0xFF0D1B2A),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                ConnectionBadge(isConnected = member.isConnected)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    LocationStatus(isLive = member.isLive)
                    Text(text = "Distance: ${member.distance}", color = Color.DarkGray, fontSize = 14.sp)
                }
                Text(text = "Updated: ${member.lastUpdated}", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ConnectionBadge(isConnected: Boolean) {
    Surface(
        color = if (isConnected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isConnected) "Connected" else "Unreachable",
            color = if (isConnected) Color(0xFF2E7D32) else Color(0xFFC62828),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LocationStatus(isLive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isLive) Color.Green else Color.Gray)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = if (isLive) "LIVE" else "LAST KNOWN LOCATION",
            color = if (isLive) Color(0xFF2E7D32) else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
