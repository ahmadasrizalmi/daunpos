package com.daunpos.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import com.daunpos.app.ui.components.DaunPOSButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.receipt.ReceiptPreview
import com.daunpos.app.ui.receipt.ReceiptSharing
import com.daunpos.app.ui.receipt.buildReceiptText
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.isTablet

@Composable
fun SuccessScreen(
    vm: DaunPOSViewModel,
    transactionId: String,
    onNewTransaction: () -> Unit
) {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val isTablet = config.isTablet

    val transactions by vm.transactions.collectAsState()
    val lastTx by vm.lastTransaction.collectAsState()
    val settings by vm.receiptSettings.collectAsState()
    val whatsappAvailable = remember(context) { ReceiptSharing.isWhatsAppAvailable(context) }
    var databaseTx by remember(transactionId) { mutableStateOf<Transaction?>(null) }
    var loadingReceipt by remember(transactionId) { mutableStateOf(true) }

    LaunchedEffect(transactionId) {
        databaseTx = runCatching { vm.getTransactionFromDatabase(transactionId) }.getOrNull()
        loadingReceipt = false
    }

    val tx = (if (lastTx?.id == transactionId) lastTx else null)
        ?: transactions.find { it.id == transactionId }
        ?: databaseTx

    if (tx == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (loadingReceipt) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(16.dp))
                    Text("Memuat struk dari database…", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                } else {
                    Text("Struk belum tersedia", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Transaksi $transactionId tidak ditemukan di penyimpanan lokal. Jangan membuat transaksi pengganti sebelum memeriksa riwayat.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onNewTransaction,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDeep, contentColor = Color.White)
                    ) {
                        Text("Kembali ke Kasir")
                    }
                }
            }
        }
        return
    }

    var scale by remember { mutableStateOf(0.4f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(500),
        label = "success"
    )
    LaunchedEffect(Unit) { scale = 1f }

    fun shareWhatsApp() {
        runCatching { ReceiptSharing.share(context, buildReceiptText(tx, settings)) }
    }

    if (isTablet) {
        // Tablet Wide Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Left Column: Status & Actions
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(52.dp))
                }

                Spacer(Modifier.height(18.dp))
                Text("Pembayaran Berhasil!", style = MaterialTheme.typography.headlineMedium, color = Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("${tx.id} · ${tx.tableNumber}", style = MaterialTheme.typography.bodyLarge, color = InkSoft)

                Spacer(Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        SummaryRow("Total", formatRupiah(tx.total), bold = true)
                        SummaryRow("Metode", tx.paymentMethod.label)
                        tx.cashReceived?.let { SummaryRow("Tunai", formatRupiah(it)) }
                        tx.change?.let { SummaryRow("Kembalian", formatRupiah(it)) }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { vm.printReceipt(tx) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Cetak Struk", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { shareWhatsApp() },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (whatsappAvailable) "WhatsApp" else "Bagikan", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onNewTransaction,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDeep, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Transaksi Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            // Right Column: Thermal receipt preview
            Box(
                modifier = Modifier
                    .weight(0.5f)
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
                .background(Bg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(Green),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("Transaksi Berhasil!", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("${tx.id} · ${tx.tableNumber}", style = MaterialTheme.typography.bodyMedium, color = InkSoft)

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    SummaryRow("Total", formatRupiah(tx.total), bold = true)
                    SummaryRow("Metode", tx.paymentMethod.label)
                    tx.cashReceived?.let { SummaryRow("Tunai", formatRupiah(it)) }
                    tx.change?.let { SummaryRow("Kembalian", formatRupiah(it)) }
                }
            }

            Spacer(Modifier.height(18.dp))

            ReceiptPreview(tx = tx, settings = settings)

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { vm.printReceipt(tx) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak Struk", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { shareWhatsApp() },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                ) {
                    Text(if (whatsappAvailable) "WhatsApp" else "Bagikan", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = onNewTransaction,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDeep, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Transaksi Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}
