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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.receipt.ReceiptPreview
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line

@Composable
fun SuccessScreen(
    vm: DaunPOSViewModel,
    transactionId: String,
    onNewTransaction: () -> Unit
) {
    val transactions by vm.transactions.collectAsState()
    val settings by vm.receiptSettings.collectAsState()
    val tx = transactions.find { it.id == transactionId } ?: return

    var scale by remember { mutableStateOf(0.4f) }
    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = tween(500),
        label = "success"
    )
    LaunchedEffect(Unit) { scale = 1f }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(animatedScale)
                .clip(CircleShape)
                .background(Green),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
        }

        Spacer(Modifier.height(18.dp))
        Text("Transaksi Berhasil!", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(tx.id, style = MaterialTheme.typography.bodyMedium, color = InkSoft)

        Spacer(Modifier.height(22.dp))

        // Summary
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

        // Receipt preview
        ReceiptPreview(tx = tx, settings = settings)

        Spacer(Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                onClick = { /* mock whatsapp */ },
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Green)
            ) {
                Text("Kirim WhatsApp", color = GreenDeep, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onNewTransaction,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Transaksi Baru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = InkSoft)
        Spacer(Modifier.weight(1f))
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, color = if (bold) GreenDeep else Ink, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}
