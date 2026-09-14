package com.clue2crew.app.common.utils

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.util.UUID

object BleUtils {
    const val LOG_TAG = "Clew2CrewBLE"

    // Fixed stable UUIDs for Clew2Crew BLE Service and Characteristic
    val SERVICE_UUID: UUID = UUID.fromString("0000C2C0-0000-1000-8000-00805F9B34FB")
    val CHARACTERISTIC_UUID: UUID = UUID.fromString("0000C2C1-0000-1000-8000-00805F9B34FB")

    // Protocol Messages
    const val MSG_HELLO = "CLEW2CREW_HELLO"
    const val MSG_ACK = "CLEW2CREW_ACK"
    const val MSG_EMERGENCY = "CLEW2CREW_EMERGENCY"

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasBluetoothPermissions(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isBluetoothSupported(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
                bluetoothManager?.adapter != null
    }

    fun isBluetoothEnabled(context: Context): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return bluetoothManager?.adapter?.isEnabled == true
    }
}
