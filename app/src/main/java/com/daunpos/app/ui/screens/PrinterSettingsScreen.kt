package com.daunpos.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.ui.components.StatusBadge
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.DangerSoft
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line

@SuppressLint("MissingPermission")
@Composable
fun PrinterSettingsScreen(
    vm: DaunPOSViewModel,
    onOpenReceiptCustomization: () -> Unit
) {
    val context = LocalContext.current
    val bluetooth by vm.bluetoothState.collectAsState()
    val receipt by vm.receiptSettings.collectAsState()

    var pendingScan by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted && pendingScan) {
            vm.startBluetoothDiscovery()
        }
        pendingScan = false
    }

    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    fun onScanClick() {
        if (!vm.printerManager.hasBluetoothPermission()) {
            pendingScan = true
            requestBluetoothPermissions()
        } else {
            vm.startBluetoothDiscovery()
        }
    }

    LaunchedEffect(Unit) {
        vm.printerManager.refreshBonded()
    }

    val devices: List<BluetoothDevice> = (bluetooth.bonded + bluetooth.discovered)
        .distinctBy { it.address }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Pengaturan Printer & Struk", style = MaterialTheme.typography.headlineSmall, color = Ink)
        }

        // Connection status
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (bluetooth.connected) GreenSoft else DangerSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Print,
                                contentDescription = null,
                                tint = if (bluetooth.connected) GreenDeep else Danger,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (bluetooth.connected) bluetooth.deviceName else "Belum terhubung",
                                style = MaterialTheme.typography.titleSmall,
                                color = Ink,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            StatusBadge(if (bluetooth.connected) "Terhubung" else "Terputus", positive = bluetooth.connected)
                        }
                        if (bluetooth.connected) {
                            OutlinedButton(
                                onClick = { vm.disconnectPrinter() },
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                            ) {
                                Text("Putuskan", color = InkSoft, fontSize = 12.sp)
                            }
                        }
                    }
                    bluetooth.error?.let { err ->
                        Spacer(Modifier.height(10.dp))
                        Text(err, style = MaterialTheme.typography.labelSmall, color = Danger)
                    }
                }
            }
        }

        // Device scanner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Pemindai Perangkat Bluetooth", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { onScanClick() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (bluetooth.discovering) {
                            val transition = rememberInfiniteTransition(label = "scan")
                            val angle by transition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
                                label = "radar"
                            )
                            Icon(
                                Icons.Filled.Radar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp).rotate(angle),
                                tint = Color.White
                            )
                        } else {
                            Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (bluetooth.discovering) "Memindai..." else "Cari Perangkat", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(12.dp))

                    if (devices.isEmpty() && !bluetooth.discovering) {
                        Text(
                            "Belum ada perangkat. Pastikan Bluetooth aktif lalu tekan Cari Perangkat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft
                        )
                    }

                    devices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (bluetooth.connected && device.name == bluetooth.deviceName) Green else GreenSoft)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(device.name ?: "Perangkat", style = MaterialTheme.typography.bodyMedium, color = Ink)
                                Text(device.address, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                            }
                            if (bluetooth.connected && device.name == bluetooth.deviceName) {
                                Text("Terhubung", style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                            } else {
                                OutlinedButton(
                                    onClick = { vm.connectPrinter(device.address) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Green),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Hubungkan", fontSize = 12.sp, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    if (!bluetooth.enabled) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Buka Pengaturan Bluetooth", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Paper & print settings
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Ukuran Kertas & Pencetakan", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(12.dp))

                    Text("Ukuran kertas thermal", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(58, 80).forEach { w ->
                            val active = receipt.paperWidth == w
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) GreenSoft else Bg)
                                    .border(1.5.dp, if (active) Green else Line, RoundedCornerShape(12.dp))
                                    .clickable { vm.updateReceiptSettings(receipt.copy(paperWidth = w)) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${w}mm", style = MaterialTheme.typography.titleSmall, color = if (active) GreenDeep else Ink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    ToggleRow(
                        title = "Cetak Struk Otomatis",
                        subtitle = "Cetak setelah pembayaran berhasil",
                        checked = receipt.autoPrint,
                        onChecked = { vm.updateReceiptSettings(receipt.copy(autoPrint = it)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    ToggleRow(
                        title = "Buka Laci Kasir",
                        subtitle = "Buka cash drawer saat cetak tunai",
                        checked = receipt.cashDrawer,
                        onChecked = { vm.updateReceiptSettings(receipt.copy(cashDrawer = it)) }
                    )
                }
            }
        }

        // Customization button
        item {
            OutlinedButton(
                onClick = onOpenReceiptCustomization,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Green)
            ) {
                Text("Kustomisasi Tampilan Struk", color = GreenDeep, fontWeight = FontWeight.SemiBold)
            }
        }

        // Test print
        item {
            OutlinedButton(
                onClick = { vm.testPrint() },
                enabled = bluetooth.connected,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (bluetooth.connected) Green else Line)
            ) {
                Icon(Icons.Filled.Print, contentDescription = null, tint = if (bluetooth.connected) GreenDeep else InkSoft, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (bluetooth.printing) "Mencetak..." else "Tes Cetak Struk (Test Print)",
                    color = if (bluetooth.connected) GreenDeep else InkSoft,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Line
            )
        )
    }
}
