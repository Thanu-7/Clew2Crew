package com.clue2crew.app.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.clue2crew.app.common.utils.BleUtils
import com.clue2crew.app.data.security.AuthProtocol
import com.clue2crew.app.data.security.AuthProtocol.hexToByteArray
import com.clue2crew.app.data.security.AuthProtocol.toHex
import com.clue2crew.app.data.security.CryptoManager
import com.clue2crew.app.data.security.KeyStoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.crypto.SecretKey

class BleManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    private var activeGatt: BluetoothGatt? = null

    private val handler = Handler(Looper.getMainLooper())

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState: StateFlow<String> = _connectionState.asStateFlow()

    private val _lastReceivedMessage = MutableStateFlow<String?>(null)
    val lastReceivedMessage: StateFlow<String?> = _lastReceivedMessage.asStateFlow()

    private val _statusMessage = MutableStateFlow("Idle")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authenticatedMemberId = MutableStateFlow<String?>(null)
    val authenticatedMemberId: StateFlow<String?> = _authenticatedMemberId.asStateFlow()

    private var currentPairingCode: String = ""
    private var currentMemberId: String = ""

    // Ephemeral Session Cryptographic State
    private var serverCClient: ByteArray? = null
    private var serverCServer: ByteArray? = null
    private var serverSessionKey: SecretKey? = null

    private var clientCClient: ByteArray? = null
    private var clientCServer: ByteArray? = null
    private var clientSessionKey: SecretKey? = null

    private val stopScanRunnable = Runnable { stopScan() }

    fun setCredentials(pairingCode: String, memberId: String) {
        this.currentPairingCode = pairingCode
        this.currentMemberId = memberId
    }

    // ------------------------------------------------------------------------
    // BLE ADVERTISING
    // ------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun startAdvertising(pairingCode: String = currentPairingCode, memberId: String = currentMemberId) {
        setCredentials(pairingCode, memberId)

        if (!BleUtils.hasBluetoothPermissions(context)) {
            _statusMessage.value = "Missing Bluetooth Permissions"
            Log.e(BleUtils.LOG_TAG, "Cannot start advertising: Missing Bluetooth Permissions")
            return
        }
        if (!BleUtils.isBluetoothEnabled(context)) {
            _statusMessage.value = "Bluetooth Disabled"
            Log.e(BleUtils.LOG_TAG, "Cannot start advertising: Bluetooth Disabled")
            return
        }

        val adapter = bluetoothManager?.adapter
        advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            _statusMessage.value = "BLE Advertising Not Supported"
            Log.e(BleUtils.LOG_TAG, "BluetoothLeAdvertiser is null")
            return
        }

        openGattServer()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleUtils.SERVICE_UUID))
            .build()

        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            _isAdvertising.value = true
            _statusMessage.value = "Advertising Clew2Crew Service"
            Log.d(BleUtils.LOG_TAG, "Started BLE Advertising for service ${BleUtils.SERVICE_UUID}")
        } catch (e: SecurityException) {
            _statusMessage.value = "Security Exception on Advertise"
            Log.e(BleUtils.LOG_TAG, "SecurityException during startAdvertising", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        if (_isAdvertising.value) {
            try {
                advertiser?.stopAdvertising(advertiseCallback)
                _isAdvertising.value = false
                _statusMessage.value = "Advertising Stopped"
                Log.d(BleUtils.LOG_TAG, "Stopped BLE Advertising")
            } catch (e: Exception) {
                Log.e(BleUtils.LOG_TAG, "Error stopping advertising", e)
            }
        }
        closeGattServer()
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            _isAdvertising.value = true
            _statusMessage.value = "Advertising Active"
            Log.d(BleUtils.LOG_TAG, "AdvertiseCallback: onStartSuccess")
        }

        override fun onStartFailure(errorCode: Int) {
            _isAdvertising.value = false
            _statusMessage.value = "Advertise Failed ($errorCode)"
            Log.e(BleUtils.LOG_TAG, "AdvertiseCallback: onStartFailure errorCode=$errorCode")
        }
    }

    // ------------------------------------------------------------------------
    // GATT SERVER
    // ------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    private fun openGattServer() {
        if (gattServer != null) return
        try {
            val service = BluetoothGattService(
                BleUtils.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val characteristic = BluetoothGattCharacteristic(
                BleUtils.CHARACTERISTIC_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            service.addCharacteristic(characteristic)

            gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
            gattServer?.addService(service)
            Log.d(BleUtils.LOG_TAG, "Opened GATT Server and added Clew2Crew service")
        } catch (e: SecurityException) {
            Log.e(BleUtils.LOG_TAG, "SecurityException opening GATT Server", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGattServer() {
        try {
            gattServer?.close()
            gattServer = null
            serverSessionKey = null
            Log.d(BleUtils.LOG_TAG, "Closed GATT Server")
        } catch (e: Exception) {
            Log.e(BleUtils.LOG_TAG, "Error closing GATT server", e)
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            Log.d(BleUtils.LOG_TAG, "GATT Server ConnectionStateChange: device=${device?.address}, newState=$newState")
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _connectionState.value = "Server: Device Connected (${device?.address})"
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _connectionState.value = "Server: Device Disconnected"
                _isAuthenticated.value = false
                _authenticatedMemberId.value = null
                serverSessionKey = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            val receivedMsg = value?.let { String(it, Charsets.UTF_8) } ?: ""
            Log.d(BleUtils.LOG_TAG, "GATT Server received write from ${device?.address}: $receivedMsg")

            if (receivedMsg.startsWith("AUTH_INIT:")) {
                try {
                    val (cClient, clientMemberId) = AuthProtocol.parseAuthInit(receivedMsg)
                    serverCClient = cClient
                    serverCServer = AuthProtocol.generateNonce()

                    val familySharedKey = KeyStoreManager.deriveFamilySharedKey(currentPairingCode)
                    serverSessionKey = KeyStoreManager.deriveSessionKey(familySharedKey, cClient, serverCServer!!)

                    val proofServer = AuthProtocol.createServerProof(
                        serverSessionKey!!,
                        currentMemberId,
                        cClient.toHex()
                    )

                    val challengeResponse = "AUTH_CHALLENGE:${serverCServer!!.toHex()}:${proofServer.toHex()}"
                    _lastReceivedMessage.value = "AUTH_INIT from $clientMemberId"
                    _connectionState.value = "Server: Sent AUTH_CHALLENGE"

                    if (responseNeeded && device != null) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            challengeResponse.toByteArray(Charsets.UTF_8)
                        )
                    }
                    Log.d(BleUtils.LOG_TAG, "GATT Server sent AUTH_CHALLENGE to ${device?.address}")
                } catch (e: Exception) {
                    Log.e(BleUtils.LOG_TAG, "GATT Server: Failed AUTH_INIT processing", e)
                    _statusMessage.value = "Server Auth Failed: ${e.message}"
                    if (responseNeeded && device != null) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                }
            } else if (receivedMsg.startsWith("AUTH_CONFIRM:")) {
                try {
                    val clientProofHex = receivedMsg.substringAfter("AUTH_CONFIRM:")
                    val sKey = serverSessionKey ?: throw IllegalArgumentException("No server session key")
                    val verifiedClientId = AuthProtocol.parseAndVerifyClientProof(
                        clientProofHex.hexToByteArray(),
                        sKey,
                        serverCServer!!.toHex()
                    )

                    _isAuthenticated.value = true
                    _authenticatedMemberId.value = verifiedClientId
                    _lastReceivedMessage.value = "AUTH_SUCCESS from $verifiedClientId"
                    _connectionState.value = "Server: Authenticated with $verifiedClientId"

                    if (responseNeeded && device != null) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            offset,
                            "AUTH_SUCCESS".toByteArray(Charsets.UTF_8)
                        )
                    }
                    Log.d(BleUtils.LOG_TAG, "GATT Server: Successfully Authenticated Client ($verifiedClientId)")
                } catch (e: Exception) {
                    Log.e(BleUtils.LOG_TAG, "GATT Server: Failed AUTH_CONFIRM verification", e)
                    _statusMessage.value = "Server Auth Failed: ${e.message}"
                    if (responseNeeded && device != null) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    // BLE SCANNING
    // ------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000L) {
        if (!BleUtils.hasBluetoothPermissions(context)) {
            _statusMessage.value = "Missing Bluetooth Permissions"
            Log.e(BleUtils.LOG_TAG, "Cannot start scan: Missing Bluetooth Permissions")
            return
        }
        if (!BleUtils.isBluetoothEnabled(context)) {
            _statusMessage.value = "Bluetooth Disabled"
            Log.e(BleUtils.LOG_TAG, "Cannot start scan: Bluetooth Disabled")
            return
        }

        val adapter = bluetoothManager?.adapter
        scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            _statusMessage.value = "BLE Scanner Not Available"
            Log.e(BleUtils.LOG_TAG, "BluetoothLeScanner is null")
            return
        }

        _discoveredDevices.value = emptyList()
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleUtils.SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            _isScanning.value = true
            _statusMessage.value = "Scanning for Clew2Crew devices..."
            Log.d(BleUtils.LOG_TAG, "Started BLE Scan for service ${BleUtils.SERVICE_UUID}")

            handler.removeCallbacks(stopScanRunnable)
            handler.postDelayed(stopScanRunnable, durationMs)
        } catch (e: SecurityException) {
            _statusMessage.value = "Security Exception on Scan"
            Log.e(BleUtils.LOG_TAG, "SecurityException during startScan", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        handler.removeCallbacks(stopScanRunnable)
        if (_isScanning.value) {
            try {
                scanner?.stopScan(scanCallback)
                _isScanning.value = false
                _statusMessage.value = "Scan Stopped"
                Log.d(BleUtils.LOG_TAG, "Stopped BLE Scan")
            } catch (e: SecurityException) {
                Log.e(BleUtils.LOG_TAG, "SecurityException during stopScan", e)
            }
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { res ->
                val device = res.device
                val address = device.address ?: "Unknown"
                val name = try {
                    device.name ?: res.scanRecord?.deviceName ?: "Clew2Crew Device"
                } catch (e: SecurityException) {
                    "Clew2Crew Device"
                }
                val rssi = res.rssi

                Log.d(BleUtils.LOG_TAG, "Discovered device: $name ($address), RSSI: $rssi")

                val currentList = _discoveredDevices.value.toMutableList()
                val existingIndex = currentList.indexOfFirst { it.address == address }
                if (existingIndex >= 0) {
                    currentList[existingIndex] = currentList[existingIndex].copy(
                        name = name,
                        rssi = rssi
                    )
                } else {
                    currentList.add(DiscoveredDevice(name = name, address = address, rssi = rssi))
                }
                _discoveredDevices.value = currentList
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _statusMessage.value = "Scan Failed ($errorCode)"
            Log.e(BleUtils.LOG_TAG, "ScanCallback: onScanFailed errorCode=$errorCode")
        }
    }

    // ------------------------------------------------------------------------
    // GATT CLIENT CONNECTION
    // ------------------------------------------------------------------------

    @SuppressLint("MissingPermission")
    fun connectToDevice(
        address: String,
        pairingCode: String = currentPairingCode,
        memberId: String = currentMemberId
    ) {
        setCredentials(pairingCode, memberId)

        if (!BleUtils.hasBluetoothPermissions(context)) {
            _statusMessage.value = "Missing Permissions"
            return
        }
        val adapter = bluetoothManager?.adapter ?: return
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            Log.e(BleUtils.LOG_TAG, "Invalid remote address: $address", e)
            return
        }

        disconnectGatt()

        _connectionState.value = "Connecting to $address..."
        Log.d(BleUtils.LOG_TAG, "Connecting GATT client to $address")

        try {
            @Suppress("DEPRECATION")
            activeGatt = device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
            updateDeviceConnectionState(address, "Connecting")
        } catch (e: SecurityException) {
            Log.e(BleUtils.LOG_TAG, "SecurityException in connectGatt", e)
            _connectionState.value = "Connection Failed (Permission)"
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
            activeGatt = null
            _connectionState.value = "Disconnected"
            _isAuthenticated.value = false
            _authenticatedMemberId.value = null
            clientSessionKey = null
            Log.d(BleUtils.LOG_TAG, "Disconnected and closed GATT client")
        } catch (e: Exception) {
            Log.e(BleUtils.LOG_TAG, "Error disconnecting GATT client", e)
        }
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            val address = gatt?.device?.address ?: ""
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(BleUtils.LOG_TAG, "GattClient: Connected to $address. Discovering services...")
                _connectionState.value = "Connected to $address"
                updateDeviceConnectionState(address, "Connected")
                try {
                    gatt?.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(BleUtils.LOG_TAG, "SecurityException discovering services", e)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(BleUtils.LOG_TAG, "GattClient: Disconnected from $address")
                _connectionState.value = "Disconnected"
                _isAuthenticated.value = false
                _authenticatedMemberId.value = null
                clientSessionKey = null
                updateDeviceConnectionState(address, "Disconnected")
                try {
                    gatt?.close()
                } catch (e: Exception) {
                    Log.e(BleUtils.LOG_TAG, "Error closing GATT", e)
                }
                if (activeGatt == gatt) activeGatt = null
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                Log.d(BleUtils.LOG_TAG, "GattClient: Services discovered. Starting Auth Handshake...")
                val service = gatt.getService(BleUtils.SERVICE_UUID)
                if (service != null) {
                    val char = service.getCharacteristic(BleUtils.CHARACTERISTIC_UUID)
                    if (char != null) {
                        startAuthenticationHandshake(gatt, char)
                    } else {
                        Log.e(BleUtils.LOG_TAG, "GattClient: Characteristic not found")
                    }
                } else {
                    Log.e(BleUtils.LOG_TAG, "GattClient: Clew2Crew Service not found")
                }
            } else {
                Log.e(BleUtils.LOG_TAG, "GattClient: Service discovery failed status=$status")
            }
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(BleUtils.LOG_TAG, "GattClient: Successfully wrote AUTH packet")
            } else {
                Log.e(BleUtils.LOG_TAG, "GattClient: Characteristic write failed status=$status")
                _statusMessage.value = "Client Write Failed"
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic != null) {
                val valueStr = String(characteristic.value ?: byteArrayOf(), Charsets.UTF_8)
                handleServerResponse(gatt, valueStr)
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val valueStr = String(value, Charsets.UTF_8)
                handleServerResponse(gatt, valueStr)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAuthenticationHandshake(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        clientCClient = AuthProtocol.generateNonce()
        val authInitMsg = AuthProtocol.createAuthInit(clientCClient!!, currentMemberId)
        val bytes = authInitMsg.toByteArray(Charsets.UTF_8)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = bytes
                @Suppress("DEPRECATION")
                characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
            Log.d(BleUtils.LOG_TAG, "GattClient: Triggered AUTH_INIT write")
            _connectionState.value = "Handshake: Sent AUTH_INIT"
        } catch (e: SecurityException) {
            Log.e(BleUtils.LOG_TAG, "SecurityException sending AUTH_INIT", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleServerResponse(gatt: BluetoothGatt?, responseStr: String) {
        if (responseStr.startsWith("AUTH_CHALLENGE:")) {
            try {
                val parts = responseStr.split(":")
                require(parts.size == 3) { "Invalid AUTH_CHALLENGE format" }

                val cServerHex = parts[1]
                val proofServerHex = parts[2]
                val cServerBytes = cServerHex.hexToByteArray()
                clientCServer = cServerBytes

                val familySharedKey = KeyStoreManager.deriveFamilySharedKey(currentPairingCode)
                clientSessionKey = KeyStoreManager.deriveSessionKey(familySharedKey, clientCClient!!, cServerBytes)

                val verifiedServerMemberId = AuthProtocol.parseAndVerifyServerProof(
                    proofServerHex.hexToByteArray(),
                    clientSessionKey!!,
                    clientCClient!!.toHex()
                )

                _lastReceivedMessage.value = "Verified Server: $verifiedServerMemberId"
                Log.d(BleUtils.LOG_TAG, "GattClient: Server verified ($verifiedServerMemberId). Sending AUTH_CONFIRM...")

                // Send Client Proof
                val clientProof = AuthProtocol.createClientProof(
                    clientSessionKey!!,
                    currentMemberId,
                    cServerHex
                )

                val confirmMsg = "AUTH_CONFIRM:${clientProof.toHex()}"
                val char = gatt?.getService(BleUtils.SERVICE_UUID)?.getCharacteristic(BleUtils.CHARACTERISTIC_UUID)
                if (char != null) {
                    val bytes = confirmMsg.toByteArray(Charsets.UTF_8)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        @Suppress("DEPRECATION")
                        char.value = bytes
                        @Suppress("DEPRECATION")
                        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(char)
                    }
                    _isAuthenticated.value = true
                    _authenticatedMemberId.value = verifiedServerMemberId
                    _connectionState.value = "Authenticated with $verifiedServerMemberId"
                    updateDeviceConnectionState(gatt.device?.address ?: "", "Authenticated")
                    updateDeviceLastMessage(gatt.device?.address ?: "", "AUTH_SUCCESS")
                }
            } catch (e: Exception) {
                Log.e(BleUtils.LOG_TAG, "GattClient: Failed server challenge verification", e)
                _statusMessage.value = "Client Auth Failed: ${e.message}"
                _connectionState.value = "Auth Failed"
                disconnectGatt()
            }
        }
    }

    private fun updateDeviceConnectionState(address: String, state: String) {
        val currentList = _discoveredDevices.value.map {
            if (it.address == address) it.copy(connectionState = state) else it
        }
        _discoveredDevices.value = currentList
    }

    private fun updateDeviceLastMessage(address: String, message: String) {
        val currentList = _discoveredDevices.value.map {
            if (it.address == address) it.copy(lastMessage = message) else it
        }
        _discoveredDevices.value = currentList
    }

    fun cleanup() {
        stopAdvertising()
        stopScan()
        disconnectGatt()
    }
}
