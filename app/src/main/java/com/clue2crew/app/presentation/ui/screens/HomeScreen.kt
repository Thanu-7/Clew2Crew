package com.clue2crew.app.presentation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel
import com.clue2crew.app.presentation.navigation.Screen
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold
import com.clue2crew.app.common.utils.LocationHelper
import com.clue2crew.app.common.utils.BleUtils
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build

@Composable
fun HomeScreen(navController: NavController, viewModel: FamilyViewModel) {
    val family by viewModel.family.collectAsState()
    val members by viewModel.members.collectAsState()
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                             permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        
        if (locationGranted) {
            locationHelper.getCurrentLocation { location ->
                location?.let { 
                    viewModel.updateCurrentDeviceLocation(it)
                    viewModel.updateFirstMemberLocation(it.latitude, it.longitude) 
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }

        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        if (!allGranted) {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        } else {
            locationHelper.getCurrentLocation { location ->
                location?.let { 
                    viewModel.updateCurrentDeviceLocation(it)
                    viewModel.updateFirstMemberLocation(it.latitude, it.longitude) 
                }
            }
        }
    }

    Clue2CrewScaffold(navController = navController, viewModel = viewModel) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (family != null) "Hello, Family" else "Welcome to Clue2Crew",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                if (family != null) {
                    DashboardCard(
                        title = family?.familyName ?: "My Family Group",
                        subtitle = "${members.size} Members • ${members.count { it.status == "Connected" }} Connected",
                        onClick = { navController.navigate(Screen.FamilyDashboard.route) }
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.CreateFamily.route) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text(text = "No Family Group", color = Color(0xFF0D1B2A), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Tap to create one", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { 
                        val target = members.firstOrNull { !it.isMe }
                        if (target != null) {
                            navController.navigate(Screen.FindFamily.createRoute(target.memberId)) 
                        } else {
                            navController.navigate(Screen.FamilyDashboard.route)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B263B)),
                    shape = RoundedCornerShape(16.dp),
                    enabled = family != null
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
