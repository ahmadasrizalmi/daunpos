package com.daunpos.app.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

data class BluetoothPrinterState(
    val enabled: Boolean = false,
    val connected: Boolean = false,
    val deviceName: String = "",
    val discovering: Boolean = false,
    val discovered: List<BluetoothDevice> = emptyList(),
    val bonded: List<BluetoothDevice> = emptyList(),
    val error: String? = null,
    val printing: Boolean = false
)

class BluetoothPrinterManager(private val context: Context) {

    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = manager?.adapter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(BluetoothPrinterState(enabled = adapter?.isEnabled == true))
    val state: StateFlow<BluetoothPrinterState> = _state.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null && !device.name.isNullOrBlank()) {
                        _state.update { s ->
                            if (s.discovered.any { it.address == device.address }) s
                            else s.copy(discovered = s.discovered + device)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _state.update { it.copy(discovering = false) }
                }
            }
        }
    }

    fun hasBluetoothPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }

    fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

    @SuppressLint("MissingPermission")
    fun refreshBonded() {
        try {
            val bonded = adapter?.bondedDevices?.filter { !it.name.isNullOrBlank() } ?: emptyList()
            _state.update { it.copy(enabled = adapter?.isEnabled == true, bonded = bonded) }
        } catch (_: SecurityException) {
            _state.update { it.copy(enabled = adapter?.isEnabled == true) }
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (adapter == null || !adapter.isEnabled) {
            _state.update { it.copy(error = "Bluetooth belum aktif. Aktifkan Bluetooth terlebih dahulu.") }
            return
        }
        refreshBonded()
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            _state.update { it.copy(discovered = emptyList(), discovering = true, error = null) }
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            _state.update { it.copy(error = "Izin Bluetooth belum diberikan.") }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        try {
            if (adapter?.isDiscovering == true) adapter?.cancelDiscovery()
        } catch (_: SecurityException) {
        }
        _state.update { it.copy(discovering = false) }
    }

    fun connect(address: String) {
        scope.launch {
            _state.update { it.copy(error = null, connected = false) }
            try {
                val device = adapter?.getRemoteDevice(address) ?: error("Perangkat tidak ditemukan")
                adapter?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                outputStream = socket?.outputStream
                _state.update {
                    it.copy(
                        connected = true,
                        deviceName = device.name ?: address,
                        discovering = false
                    )
                }
            } catch (e: IOException) {
                _state.update { it.copy(error = "Gagal terhubung: ${e.message ?: "perangkat tidak merespon"}") }
            } catch (e: SecurityException) {
                _state.update { it.copy(error = "Izin Bluetooth belum diberikan.") }
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                outputStream?.close()
                socket?.close()
            } catch (_: Exception) {
            }
            outputStream = null
            socket = null
            _state.update { it.copy(connected = false, deviceName = "") }
        }
    }

    fun print(bytes: ByteArray, onDone: (Boolean) -> Unit = {}) {
        scope.launch {
            _state.update { it.copy(printing = true, error = null) }
            try {
                val os = outputStream ?: error("Printer tidak terhubung")
                os.write(bytes)
                os.flush()
                _state.update { it.copy(printing = false) }
                onDone(true)
            } catch (e: Exception) {
                _state.update { it.copy(printing = false, error = "Gagal mencetak: ${e.message}") }
                onDone(false)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun close() {
        stopDiscovery()
        scope.cancel()
    }

    companion object {
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}
