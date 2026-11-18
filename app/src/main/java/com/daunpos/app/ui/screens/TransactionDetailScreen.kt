package com.daunpos.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.components.StatusBadge
import com.daunpos.app.ui.receipt.ReceiptPreview
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(
    vm: DaunPOSViewModel,
    transactionId: String,
    onBack: () -> Unit
) {
    val transactions by vm.transactions.collectAsState()
    val settings by vm.receiptSettings.collectAsState()
    val tx = transactions.find { it.id == transactionId } ?: return
    val success = tx.status == "SUKSES"
    val time = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
            }
            Text("Detail Transaksi", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.weight(1f))
            StatusBadge(if (success) "Sukses" else "Batal", positive = success)
            Spacer(Modifier.width(12.dp))
        }

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
                    Text(tx.id, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
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
                    Text("Rincian Item", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    tx.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
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
                    if (tx.tax > 0) DetailRow("Pajak", formatRupiah(tx.tax))
                    DetailRow("Total", formatRupiah(tx.total), bold = true)
                }
            }

            // Payment info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Info Pembayaran", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    DetailRow("Metode", tx.paymentMethod.label)
                    tx.cashReceived?.let { DetailRow("Uang Diterima", formatRupiah(it)) }
                    tx.change?.let { DetailRow("Kembalian", formatRupiah(it)) }
                    if (tx.note.isNotEmpty()) DetailRow("Catatan", tx.note)
                }
            }

            // Receipt preview
            ReceiptPreview(tx = tx, settings = settings)

            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.printReceipt(tx) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cetak Ulang", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { /* whatsapp mock */ },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Green)
                ) {
                    Text("WhatsApp", color = GreenDeep, fontWeight = FontWeight.SemiBold)
                }
            }
            Button(
                onClick = { /* pdf mock */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Unduh PDF", fontWeight = FontWeight.Bold)
            }

            if (!success) {
                Button(
                    onClick = { vm.cancelTransaction(tx.id) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBEAE7), contentColor = Color(0xFFC9483C)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Batalkan Transaksi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = if (bold) GreenDeep else Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
