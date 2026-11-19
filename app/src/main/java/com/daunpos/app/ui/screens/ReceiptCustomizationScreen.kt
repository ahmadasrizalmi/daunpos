package com.daunpos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.OrderItem
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.Transaction
import com.daunpos.app.ui.receipt.ReceiptPreview
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.isTablet

@Composable
fun ReceiptCustomizationScreen(
    vm: DaunPOSViewModel,
    onBack: () -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.isTablet

    val settings by vm.receiptSettings.collectAsState()

    var line1 by remember { mutableStateOf(settings.headerLines.getOrElse(0) { "DaunPOS Coffee & Eatery" }) }
    var line2 by remember { mutableStateOf(settings.headerLines.getOrElse(1) { "Jl. Daun Hijau No. 8, Bandung" }) }
    var line3 by remember { mutableStateOf(settings.headerLines.getOrElse(2) { "0812-3456-7890" }) }
    var footer by remember { mutableStateOf(settings.footerText) }
    var showLogo by remember { mutableStateOf(settings.showLogo) }
    var showQR by remember { mutableStateOf(settings.showQR) }
    var taxRate by remember { mutableStateOf(settings.taxPercentage) }

    val currentSettings = settings.copy(
        headerLines = listOf(line1, line2, line3).filter { it.isNotBlank() },
        footerText = footer,
        showLogo = showLogo,
        showQR = showQR,
        taxPercentage = taxRate
    )

    val sampleTx = remember {
        Transaction(
            id = "#INV-0183",
            dateTime = System.currentTimeMillis(),
            items = listOf(
                OrderItem("P001", "Es Kopi Daun", 22000, 1, "☕"),
                OrderItem("P003", "Cappuccino", 28000, 1, "🥛"),
                OrderItem("P010", "Butter Croissant", 22000, 1, "🥐"),
                OrderItem("P006", "Es Teh Manis", 12000, 1, "🧋")
            ),
            subtotal = 84000,
            discount = 0,
            tax = 0,
            total = 84000,
            paymentMethod = PaymentMethod.CASH,
            cashReceived = 100000,
            change = 16000,
            tableNumber = "Meja 04"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
            }
            Text("Kustomisasi Struk", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
        }

        if (isTablet) {
            // Tablet Dual-Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Form Column
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ReceiptFormContent(
                        line1 = line1, onLine1 = { line1 = it },
                        line2 = line2, onLine2 = { line2 = it },
                        line3 = line3, onLine3 = { line3 = it },
                        footer = footer, onFooter = { footer = it },
                        showLogo = showLogo, onShowLogo = { showLogo = it },
                        showQR = showQR, onShowQR = { showQR = it },
                        taxRate = taxRate, onTaxRate = { taxRate = it }
                    )

                    Button(
                        onClick = {
                            vm.updateReceiptSettings(currentSettings)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text("Simpan Pengaturan Struk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Live Preview Column
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE9EDE6))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Live Preview Struk Thermal", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        ReceiptPreview(tx = sampleTx, settings = currentSettings)
                    }
                }
            }
        } else {
            // Phone Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Live Preview Struk", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE9EDE6))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ReceiptPreview(tx = sampleTx, settings = currentSettings)
                }

                ReceiptFormContent(
                    line1 = line1, onLine1 = { line1 = it },
                    line2 = line2, onLine2 = { line2 = it },
                    line3 = line3, onLine3 = { line3 = it },
                    footer = footer, onFooter = { footer = it },
                    showLogo = showLogo, onShowLogo = { showLogo = it },
                    showQR = showQR, onShowQR = { showQR = it },
                    taxRate = taxRate, onTaxRate = { taxRate = it }
                )

                Button(
                    onClick = {
                        vm.updateReceiptSettings(currentSettings)
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Green),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Simpan Perubahan Struk", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun ReceiptFormContent(
    line1: String, onLine1: (String) -> Unit,
    line2: String, onLine2: (String) -> Unit,
    line3: String, onLine3: (String) -> Unit,
    footer: String, onFooter: (String) -> Unit,
    showLogo: Boolean, onShowLogo: (Boolean) -> Unit,
    showQR: Boolean, onShowQR: (Boolean) -> Unit,
    taxRate: Int, onTaxRate: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Logo Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, tint = GreenDeep)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Logo Kafe di Struk", style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("Tampilkan header logo di bagian atas", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }
                Switch(
                    checked = showLogo,
                    onCheckedChange = onShowLogo,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Green, uncheckedThumbColor = Color.White, uncheckedTrackColor = Line)
                )
            }
        }

        // Header Text Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Header Struk", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = line1,
                    onValueChange = onLine1,
                    label = { Text("Nama Kafe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line2,
                    onValueChange = onLine2,
                    label = { Text("Alamat") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = line3,
                    onValueChange = onLine3,
                    label = { Text("No. Telepon") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Footer Text Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Footer Struk", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = footer,
                    onValueChange = onFooter,
                    label = { Text("Pesan Penutup Struk") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Taxes & QR Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pajak & Fitur Tambahan", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)

                Text("Tarif Pajak (PPN)", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Tanpa PPN (0%)", 10 to "10%", 11 to "11%").forEach { (rate, label) ->
                        val active = taxRate == rate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) GreenSoft else Bg)
                                .border(1.5.dp, if (active) Green else Line, RoundedCornerShape(10.dp))
                                .clickable { onTaxRate(rate) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) GreenDeep else Ink, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("QR Struk Digital", style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                        Text("Cetak kode QR struk di bawah struk", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    }
                    Switch(
                        checked = showQR,
                        onCheckedChange = onShowQR,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Green, uncheckedThumbColor = Color.White, uncheckedTrackColor = Line)
                    )
                }
            }
        }
    }
}
