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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.platform.LocalConfiguration
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

private val connectionTypes = listOf("Bluetooth", "USB", "LAN / Wi-Fi")

@SuppressLint("MissingPermission")
@Composable
fun PrinterSettingsScreen(
    vm: DaunPOSViewModel,
    onOpenReceiptCustomization: () -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTablet = config.smallestScreenWidthDp >= 600

    val bluetooth by vm.bluetoothState.collectAsState()
    val receipt by vm.receiptSettings.collectAsState()

    var selectedType by remember { mutableStateOf("Bluetooth") }
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

    if (isTablet) {
        // Tablet Dual-Pane Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: Scanner & Devices
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Printer & Struk", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)

                // Status card
                PrinterStatusCard(bluetooth = bluetooth, onDisconnect = { vm.disconnectPrinter() })

                // Connection tabs (Bluetooth, USB, LAN)
                ConnectionTypeTabs(selectedType = selectedType, onSelect = { selectedType = it })

                // Scanner section
                ScannerCard(
                    bluetooth = bluetooth,
                    devices = devices,
                    selectedType = selectedType,
                    onScanClick = { onScanClick() },
                    onConnect = { vm.connectPrinter(it) },
                    onOpenSettings = { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
                )
            }

            // Right Column: Preferences & Customization
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PaperAndPreferencesCard(
                    receipt = receipt,
                    onUpdateReceipt = { vm.updateReceiptSettings(it) }
                )

                OutlinedButton(
                    onClick = onOpenReceiptCustomization,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                ) {
                    Text("Kustomisasi Tampilan Struk", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                }

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
    } else {
        // Phone Layout
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Printer & Struk", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
            }

            item {
                PrinterStatusCard(bluetooth = bluetooth, onDisconnect = { vm.disconnectPrinter() })
            }

            item {
                ConnectionTypeTabs(selectedType = selectedType, onSelect = { selectedType = it })
            }

            item {
                ScannerCard(
                    bluetooth = bluetooth,
                    devices = devices,
                    selectedType = selectedType,
                    onScanClick = { onScanClick() },
                    onConnect = { vm.connectPrinter(it) },
                    onOpenSettings = { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
                )
            }

            item {
                PaperAndPreferencesCard(
                    receipt = receipt,
                    onUpdateReceipt = { vm.updateReceiptSettings(it) }
                )
            }

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
}

@Composable
private fun ConnectionTypeTabs(selectedType: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        connectionTypes.forEach { t ->
            val active = t == selectedType
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) GreenDeep else Color.White)
                    .border(1.dp, if (active) GreenDeep else Line, RoundedCornerShape(50))
                    .clickable { onSelect(t) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    t,
                    color = if (active) Color.White else Ink,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PrinterStatusCard(
    bluetooth: com.daunpos.app.printer.BluetoothPrinterState,
    onDisconnect: () -> Unit
) {
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
                        if (bluetooth.connected) (bluetooth.deviceName.ifBlank { "RPP02N Bluetooth Printer" }) else "Belum terhubung",
                        style = MaterialTheme.typography.titleSmall,
                        color = Ink,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    StatusBadge(if (bluetooth.connected) "Terhubung" else "Terputus", positive = bluetooth.connected)
                }
                if (bluetooth.connected) {
                    OutlinedButton(
                        onClick = onDisconnect,
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

@SuppressLint("MissingPermission")
@Composable
private fun ScannerCard(
    bluetooth: com.daunpos.app.printer.BluetoothPrinterState,
    devices: List<BluetoothDevice>,
    selectedType: String,
    onScanClick: () -> Unit,
    onConnect: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text("Pemindai Perangkat $selectedType", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            if (selectedType == "Bluetooth") {
                Button(
                    onClick = onScanClick,
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
                    Text(if (bluetooth.discovering) "Memindai perangkat sekitar..." else "Cari Perangkat", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(12.dp))

                if (devices.isEmpty() && !bluetooth.discovering) {
                    Text(
                        "Belum ada perangkat terdeteksi. Pastikan Bluetooth aktif dan printer dalam mode pairing.",
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
                            Text(device.name ?: "Printer Bluetooth", style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(device.address, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                        if (bluetooth.connected && device.name == bluetooth.deviceName) {
                            Text("Terhubung", style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                        } else {
                            OutlinedButton(
                                onClick = { onConnect(device.address) },
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
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Buka Pengaturan Bluetooth HP/Tablet", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else if (selectedType == "USB") {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Usb, contentDescription = null, tint = InkSoft, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Hubungkan kabel USB OTG printer thermal ke port USB tablet/smartphone.", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            } else {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Wifi, contentDescription = null, tint = InkSoft, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Masukkan alamat IP printer thermal dapur (LAN / Wi-Fi POS): e.g. 192.168.1.200:9100", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }
        }
    }
}

@Composable
private fun PaperAndPreferencesCard(
    receipt: com.daunpos.app.data.ReceiptSettings,
    onUpdateReceipt: (com.daunpos.app.data.ReceiptSettings) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column {
            Text("Ukuran Kertas & Pencetakan", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
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
                            .clickable { onUpdateReceipt(receipt.copy(paperWidth = w)) }
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
                subtitle = "Cetak otomatis setelah pembayaran berhasil",
                checked = receipt.autoPrint,
                onChecked = { onUpdateReceipt(receipt.copy(autoPrint = it)) }
            )
            Spacer(Modifier.height(10.dp))
            ToggleRow(
                title = "Buka Laci Kasir (Cash Drawer)",
                subtitle = "Kirim pulsa ESC/POS laci kasir saat bayar tunai",
                checked = receipt.cashDrawer,
                onChecked = { onUpdateReceipt(receipt.copy(cashDrawer = it)) }
            )
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
