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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.qris.QrisConverter
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
    onOpenReceiptCustomization: () -> Unit,
    onEditStoreProfile: () -> Unit = {}
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
                StoreProfileSummaryCard(vm = vm, onEditProfile = onEditStoreProfile)

                QrisSettingsCard(vm = vm)

                BankNotificationListenerCard(context = context)

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
                Text("Printer & Pengaturan", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
            }

            item {
                StoreProfileSummaryCard(vm = vm, onEditProfile = onEditStoreProfile)
            }

            item {
                QrisSettingsCard(vm = vm)
            }

            item {
                BankNotificationListenerCard(context = context)
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

@Composable
private fun QrisSettingsCard(
    vm: DaunPOSViewModel
) {
    val staticQris by vm.staticQris.collectAsState()
    val bluetooth by vm.bluetoothState.collectAsState()
    var input by remember(staticQris) { mutableStateOf(staticQris) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    val merchantInfo = remember(input) {
        if (input.isNotBlank()) QrisConverter.parseMerchantInfo(input.trim()) else null
    }

    val dynamicPreview = remember(input, merchantInfo) {
        if (merchantInfo != null && merchantInfo.isValidCrc) {
            try {
                val dyn = QrisConverter.convertStaticToDynamic(input.trim(), 10000)
                QrisConverter.generateQrBitmap(dyn, 320)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.QrCode,
                        contentDescription = null,
                        tint = GreenDeep,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "QRIS Dinamis Otomatis",
                        style = MaterialTheme.typography.titleMedium,
                        color = Ink,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Konversi QRIS Statis UMKM ke Dinamis",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSoft
                    )
                }
            }

            Text(
                "Masukkan string QRIS Statis toko Anda (BCA, GoPay, ShopeePay, Mandiri, OVO, dll). Sistem akan otomatis menambahkan nominal transaksi belanja pelanggan sehingga pembayaran terkunci akurat.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                lineHeight = 18.sp
            )

            // Merchant status badge
            if (merchantInfo != null && merchantInfo.isValidCrc) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenSoft)
                        .border(1.dp, Green.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("EMVCo QRIS Valid & Siap Digunakan", style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
                        }
                        if (merchantInfo.merchantName.isNotBlank()) {
                            Text("Nama Merchant: ${merchantInfo.merchantName}", style = MaterialTheme.typography.bodySmall, color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                        if (merchantInfo.city.isNotBlank()) {
                            Text("Kota: ${merchantInfo.city}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                        if (merchantInfo.nmid.isNotBlank()) {
                            Text("NMID: ${merchantInfo.nmid}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        }
                    }
                }
            } else if (input.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DangerSoft)
                        .border(1.dp, Danger.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Format QRIS atau Checksum CRC16 tidak valid. Pastikan string EMVCo lengkap.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Danger,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = {
                    input = it
                    saveMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("String EMVCo QRIS Statis") },
                placeholder = { Text("00020101021126...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            saveMessage?.let { msg ->
                Text(msg, style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        vm.updateStaticQris(input.trim())
                        saveMessage = "Pengaturan QRIS Toko Berhasil Disimpan!"
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan QRIS", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        input = QrisConverter.DEFAULT_SAMPLE_STATIC_QRIS
                        vm.updateStaticQris(input)
                        saveMessage = "Contoh QRIS standar diterapkan!"
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Text("Pakai Demo", color = InkSoft, fontSize = 12.sp)
                }
            }

            // Preview QR & Test Print
            dynamicPreview?.let { bmp ->
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Bg)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Simulasi QRIS Dinamis (Rp 10.000)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Ink,
                            fontWeight = FontWeight.Bold
                        )
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Simulasi QRIS",
                            modifier = Modifier
                                .size(140.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(6.dp)
                        )
                        if (bluetooth.connected) {
                            OutlinedButton(
                                onClick = { vm.printQrisBill(10000) },
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Green),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Tes Cetak Slip QRIS Rp 10.000", fontSize = 12.sp, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreProfileSummaryCard(
    vm: DaunPOSViewModel,
    onEditProfile: () -> Unit
) {
    val profile by vm.storeProfile.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Storefront,
                            contentDescription = null,
                            tint = GreenDeep,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = profile.storeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "${profile.address} · ${profile.phoneNumber}",
                            fontSize = 11.sp,
                            color = InkSoft,
                            maxLines = 1
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(GreenDeep)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = profile.businessCategory.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = onEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Green)
            ) {
                Text(
                    text = "Ubah Profil Toko & Kategori Usaha",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenDeep
                )
            }
        }
    }
}

@Composable
private fun BankNotificationListenerCard(
    context: android.content.Context
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isGranted by remember {
        mutableStateOf(com.daunpos.app.notification.PaymentNotificationBridge.isNotificationAccessGranted(context))
    }
    val isConnected by com.daunpos.app.notification.PaymentNotificationBridge.isServiceConnected.collectAsState()
    val recentList by com.daunpos.app.notification.PaymentNotificationBridge.recentNotifications.collectAsState()

    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isGranted = com.daunpos.app.notification.PaymentNotificationBridge.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isGranted) GreenSoft else DangerSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = if (isGranted) GreenDeep else Danger,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Verifikasi Bank & QRIS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Deteksi mutasi BCA, Mandiri, BRI, BNI, GoBiz, DANA, OVO, ShopeePay",
                        fontSize = 11.sp,
                        color = InkSoft
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Status chip & actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGranted) GreenSoft else Color(0xFFFBEAE7))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (isGranted) GreenDeep else Danger,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isGranted) {
                            if (isConnected) "Layanan Aktif & Terhubung di Latar Belakang" else "Izin Aktif (Menunggu Notifikasi)"
                        } else {
                            "Izin Akses Notifikasi Belum Diaktifkan"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isGranted) GreenDeep else Danger
                    )
                }

                if (!isGranted) {
                    Button(
                        onClick = { com.daunpos.app.notification.PaymentNotificationBridge.openNotificationSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Aktifkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Recent detected notifications
            Text(
                text = "Riwayat Notifikasi Terbaru (Live Listener):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
            Spacer(Modifier.height(6.dp))

            if (recentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF7F9F7))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada notifikasi pembayaran yang tertangkap.",
                        fontSize = 11.sp,
                        color = InkSoft
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentList.take(3).forEach { notif ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(GreenSoft)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${notif.bankName} ${if (notif.senderName.isNotBlank()) "· " + notif.senderName else ""}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDeep
                                )
                                Text(
                                    notif.rawTitle.ifBlank { notif.rawText },
                                    fontSize = 10.sp,
                                    color = InkSoft,
                                    maxLines = 1
                                )
                            }
                            Text(
                                com.daunpos.app.data.formatRupiah(notif.amount),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenDeep
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Test simulation button
            OutlinedButton(
                onClick = {
                    com.daunpos.app.notification.PaymentNotificationBridge.postPayment(
                        com.daunpos.app.notification.BankNotificationEvent(
                            sourcePackage = "id.co.bri.brimo",
                            bankName = "BRImo (Bank BRI)",
                            amount = 25000,
                            senderName = "Budi Santoso",
                            rawTitle = "Transfer Masuk Berhasil",
                            rawText = "Dana masuk sebesar Rp 25.000 dari Budi Santoso"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Text("Kirim Notifikasi Uji Coba (Simulasi BRImo Rp 25.000)", fontSize = 11.sp, color = InkSoft)
            }
        }
    }
}
