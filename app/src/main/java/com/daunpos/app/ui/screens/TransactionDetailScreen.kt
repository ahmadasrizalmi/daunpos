package com.daunpos.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.components.StatusBadge
import com.daunpos.app.ui.receipt.ReceiptPreview
import com.daunpos.app.ui.receipt.buildReceiptText
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.isTablet
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    vm: DaunPOSViewModel,
    transactionId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTablet = config.isTablet
    val scope = rememberCoroutineScope()

    val transactions by vm.transactions.collectAsState()
    val lastTx by vm.lastTransaction.collectAsState()
    val settings by vm.receiptSettings.collectAsState()
    val tx = (if (lastTx?.id == transactionId) lastTx else null)
        ?: transactions.find { it.id == transactionId }

    if (tx == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Transaksi tidak ditemukan", style = MaterialTheme.typography.titleMedium, color = Ink)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                    Text("Kembali", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }
    val success = tx.status == "SUKSES"
    val time = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))

    var showCancelDialog by remember { mutableStateOf(false) }
    var ownerPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    fun shareWhatsApp() {
        val text = buildReceiptText(tx, settings)
        val sendIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(text))
        }
        runCatching { context.startActivity(sendIntent) }
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
            Text("Detail Transaksi", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            StatusBadge(if (success) "Sukses" else "Batal", positive = success)
            Spacer(Modifier.width(12.dp))
        }

        if (isTablet) {
            // Tablet Dual-Pane
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Column: Details & Items
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(GreenDeep)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tx.id, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x33FFFFFF))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(tx.tableNumber, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(time, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB7DFC8))
                            Spacer(Modifier.height(10.dp))
                            Text(formatRupiah(tx.total), style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Items card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, Line, RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    ) {
                        Column {
                            Text("Rincian Item", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            tx.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.emoji, fontSize = 18.sp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Ink)
                                        Text("${item.quantity} x ${formatRupiah(item.price)}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                                    }
                                    Text(formatRupiah(item.total), style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                            Spacer(Modifier.height(10.dp))
                            DetailRow("Subtotal", formatRupiah(tx.subtotal))
                            if (tx.discount > 0) DetailRow("Diskon", "-" + formatRupiah(tx.discount))
                            if (tx.tax > 0) DetailRow("Pajak", formatRupiah(tx.tax))
                            DetailRow("Total Akhir", formatRupiah(tx.total), bold = true)
                        }
                    }

                    // Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { vm.printReceipt(tx) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cetak Ulang Struk", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { shareWhatsApp() },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Kirim WhatsApp", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (success) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
                        ) {
                            Text("Batalkan Transaksi (Otorisasi Pemilik)", color = Danger, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Right Column: Thermal receipt preview
                Box(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ReceiptPreview(tx = tx, settings = settings)
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
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenDeep)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tx.id, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x33FFFFFF))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(tx.tableNumber, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(time, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB7DFC8))
                        Spacer(Modifier.height(10.dp))
                        Text(formatRupiah(tx.total), style = MaterialTheme.typography.displayMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                // Items
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Rincian Item", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        tx.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium, color = Ink)
                                    Text("${item.quantity} x ${formatRupiah(item.price)}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                                }
                                Text(formatRupiah(item.total), style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        Spacer(Modifier.height(10.dp))
                        DetailRow("Subtotal", formatRupiah(tx.subtotal))
                        if (tx.discount > 0) DetailRow("Diskon", "-" + formatRupiah(tx.discount))
                        if (tx.tax > 0) DetailRow("Pajak", formatRupiah(tx.tax))
                        DetailRow("Total Akhir", formatRupiah(tx.total), bold = true)
                    }
                }

                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { vm.printReceipt(tx) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cetak Ulang", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { shareWhatsApp() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("WhatsApp", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (success) {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Danger)
                    ) {
                        Text("Batalkan Transaksi", color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false; ownerPin = ""; pinError = null },
            title = { Text("Otorisasi Pemilik (Owner PIN)") },
            text = {
                Column {
                    Text("Pembatalan transaksi memerlukan verifikasi PIN pemilik (default: 0000).", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) ownerPin = it },
                        label = { Text("PIN Pemilik 4-Digit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null
                    )
                    if (pinError != null) {
                        Text(pinError ?: "", color = Danger, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val valid = vm.verifyOwnerPin(ownerPin)
                            if (valid) {
                                vm.cancelTransaction(tx.id)
                                showCancelDialog = false
                                ownerPin = ""
                                pinError = null
                            } else {
                                pinError = "PIN pemilik salah."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Batalkan Transaksi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false; ownerPin = ""; pinError = null }) {
                    Text("Kembali")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (bold) Ink else InkSoft, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = if (bold) GreenDeep else Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}
