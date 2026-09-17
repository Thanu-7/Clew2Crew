package com.clue2crew.app.presentation.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.clue2crew.app.common.utils.BleUtils
import com.clue2crew.app.common.utils.LocationUtils
import com.clue2crew.app.data.bluetooth.DiscoveredDevice
import com.clue2crew.app.presentation.ui.components.Clue2CrewScaffold
import com.clue2crew.app.presentation.viewmodel.FamilyViewModel

@Composable
fun OfflineTransferScreen(navController: NavController, viewModel: FamilyViewModel) {
    val context = LocalContext.current

    val isAdvertising by viewModel.isBleAdvertising.collectAsState()
    val isScanning by viewModel.isBleScanning.collectAsState()
    val discoveredDevices by viewModel.discoveredBleDevices.collectAsState()
    val connectionState by viewModel.bleConnectionState.collectAsState()
    val lastMessage by viewModel.lastBleMessage.collectAsState()
    val statusMessage by viewModel.bleStatusMessage.collectAsState()
    val isAuthenticated by viewModel.isBleAuthenticated.collectAsState()
    val authenticatedMemberId by viewModel.authenticatedMemberId.collectAsState()

    val myLocation by viewModel.currentDeviceLocation.collectAsState()
    val incomingLocation by viewModel.incomingBleLocation.collectAsState()
    val members by viewModel.members.collectAsState()

    var hasPermissions by remember { mutableStateOf(BleUtils.hasBluetoothPermissions(context)) }
    var isBleEnabled by remember { mutableStateOf(BleUtils.isBluetoothEnabled(context)) }
    var isGpsEnabled by remember { mutableStateOf(BleUtils.isLocationEnabled(context)) }

    LaunchedEffect(Unit) {
        while(true) {
            hasPermissions = BleUtils.hasBluetoothPermissions(context)
            isBleEnabled = BleUtils.isBluetoothEnabled(context)
            isGpsEnabled = BleUtils.isLocationEnabled(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasPermissions = BleUtils.hasBluetoothPermissions(context)
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
                    text = "Offline Reunification",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Encrypted BLE Location Exchange",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            // Warnings section
            if (!hasPermissions || !isBleEnabled || !isGpsEnabled) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF780000)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Action Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (!hasPermissions) {
                                Text("• Permissions missing (Bluetooth & Location)", color = Color.LightGray, fontSize = 12.sp)
                                Button(
                                    onClick = { permissionLauncher.launch(BleUtils.getRequiredPermissions()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Grant Permissions", color = Color(0xFF780000))
                                }
                            }
                            
                            if (!isBleEnabled) {
                                Text("• Bluetooth is turned OFF", color = Color.LightGray, fontSize = 12.sp)
                            }
                            
                            if (!isGpsEnabled) {
                                Text("• Location Services (GPS) are turned OFF", color = Color.LightGray, fontSize = 12.sp)
                            }

                            if (!isBleEnabled || !isGpsEnabled) {
                                Text(
                                    text = "Please turn them on in your phone settings.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Reunification Live Banner (if locations exist)
            item {
                val locRemote = incomingLocation
                val locMy = myLocation

                if (locMy != null && locRemote != null) {
                    val distMeters = LocationUtils.calculateDistanceMeters(
                        locMy.latitude, locMy.longitude,
                        locRemote.latitude, locRemote.longitude
                    )
                    val distFormatted = LocationUtils.calculateDistance(
                        locMy.latitude, locMy.longitude,
                        locRemote.latitude, locRemote.longitude
                    )
                    val bearing = LocationUtils.calculateBearing(
                        locMy.latitude, locMy.longitude,
                        locRemote.latitude, locRemote.longitude
                    )
                    val cardinal = LocationUtils.getCardinalDirection(bearing)
                    val fullCardinal = LocationUtils.getFullCardinalDirection(bearing)
                    val isReunited = LocationUtils.isReunited(distMeters)

                    val remoteMember = members.find { it.memberId == locRemote.memberId }
                    val remoteName = remoteMember?.name ?: "Family Member"

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isReunited) Color(0xFF1B5E20) else Color(0xFF1B263B)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isReunited) {
                                Text(
                                    text = "REUNITED",
                                    color = Color.Green,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Family member is nearby!",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text(
                                text = "Family Member: $remoteName",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Distance", color = Color.LightGray, fontSize = 12.sp)
                                    Text(text = distFormatted, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Direction", color = Color.LightGray, fontSize = 12.sp)
                                    Text(text = "$cardinal ($fullCardinal)", color = Color(0xFF415A77), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "GPS Accuracy: ±${locMy.accuracy.toInt()}m (My) • ±${locRemote.accuracy.toInt()}m (Remote)",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (isAuthenticated) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (locMy == null) "Waiting for local GPS fix..." else "Waiting for remote location packet over BLE...",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Status Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Status: $statusMessage", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Connection: $connectionState", color = Color(0xFF415A77), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        
                        if (isAuthenticated) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF1B5E20),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AUTHENTICATED & ENCRYPTED (AES-GCM)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Member ID: ${authenticatedMemberId ?: "Verified"}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        if (lastMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = Color(0xFF0D1B2A),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Last Received: $lastMessage",
                                    color = Color.Green,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Controls Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isAdvertising) viewModel.stopBleAdvertising() else viewModel.startBleAdvertising()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdvertising) Color(0xFF780000) else Color(0xFF415A77)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = hasPermissions
                    ) {
                        Icon(imageVector = Icons.Default.CellTower, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAdvertising) "Stop Adv" else "Start Adv", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (isScanning) viewModel.stopBleScan() else viewModel.startBleScan(10000L)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) Color(0xFF780000) else Color(0xFF415A77)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = hasPermissions
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isScanning) "Stop Scan" else "Scan (10s)", fontSize = 14.sp)
                    }
                }
            }

            // Discovered Devices Header
            item {
                Text(
                    text = "Discovered Clew2Crew Devices (${discoveredDevices.size})",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (discoveredDevices.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B263B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isScanning) "Searching for nearby Clew2Crew devices..." else "No devices discovered. Tap 'Scan (10s)' to search.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(discoveredDevices) { device ->
                    DiscoveredDeviceCard(device = device) {
                        viewModel.connectBleDevice(device.address)
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredDeviceCard(device: DiscoveredDevice, onConnect: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = device.name,
                        color = Color(0xFF0D1B2A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Address: ${device.address} (Metadata)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "RSSI: ${device.rssi} dBm",
                        color = Color(0xFF415A77),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B263B)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (device.connectionState == "Connected") "Connected" else "Connect & HELLO",
                        fontSize = 12.sp
                    )
                }
            }

            if (device.lastMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Message: ${device.lastMessage}",
                    color = Color(0xFF2E7D32),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
