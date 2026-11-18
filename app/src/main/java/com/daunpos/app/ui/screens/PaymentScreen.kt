package com.daunpos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint

@Composable
fun PaymentScreen(
    vm: DaunPOSViewModel,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val cart by vm.cart.collectAsState()
    val total = vm.cartTotal
    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashInput by remember { mutableStateOf("") }
    val cashReceived = cashInput.toIntOrNull() ?: 0
    val change = cashReceived - total

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
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
            }
            Text("Pembayaran", style = MaterialTheme.typography.titleLarge, color = Ink)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Total bill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(GreenDeep)
                    .padding(20.dp)
            ) {
                Column {
                    Text("Total Tagihan", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFB7DFC8))
                    Text(
                        formatRupiah(total),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Order summary (compact)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Rincian Pesanan", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(10.dp))
                    cart.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${item.quantity}x ${item.product.name}", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                            Text(formatRupiah(item.product.price * item.quantity), style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Payment methods
            Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium, color = Ink)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentMethod.values().forEach { m ->
                    val active = method == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) GreenSoft else Color.White)
                            .border(1.5.dp, if (active) Green else Line, RoundedCornerShape(16.dp))
                            .clickable { method = m }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                when (m) {
                                    PaymentMethod.CASH -> Icons.Filled.Payments
                                    PaymentMethod.QRIS -> Icons.Filled.QrCode
                                    PaymentMethod.TRANSFER -> Icons.Filled.AccountBalance
                                },
                                contentDescription = null,
                                tint = if (active) GreenDeep else InkSoft,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(m.label, style = MaterialTheme.typography.labelMedium, color = if (active) GreenDeep else Ink)
                        }
                    }
                }
            }

            when (method) {
                PaymentMethod.CASH -> {
                    // Cash input
                    OutlinedTextField(
                        value = cashInput,
                        onValueChange = { v -> cashInput = v.filter { it.isDigit() }.take(8) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Uang diterima (Rp)") },
                        prefix = { Text("Rp ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Line,
                            focusedLabelColor = GreenDeep,
                            cursorColor = GreenDeep
                        )
                    )

                    // Quick amounts
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "Uang Pas" to total,
                            "Rp20.000" to 20000,
                            "Rp50.000" to 50000,
                            "Rp100.000" to 100000
                        ).forEach { (label, amount) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                                    .clickable { cashInput = amount.toString() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Change info
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (change >= 0) GreenSoft else Color(0xFFFBEAE7))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Kembalian", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                                Text(
                                    if (change >= 0) formatRupiah(change) else "Uang kurang " + formatRupiah(-change),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = if (change >= 0) GreenDeep else Color(0xFFC9483C),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                PaymentMethod.QRIS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, Line, RoundedCornerShape(18.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Scan QRIS untuk membayar", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Spacer(Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GreenSoft)
                                    .border(2.dp, Green, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("QR", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = GreenDeep)
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Tampilkan kode ke pelanggan", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                        }
                    }
                }
                PaymentMethod.TRANSFER -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(1.dp, Line, RoundedCornerShape(18.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Text("Transfer ke rekening berikut:", style = MaterialTheme.typography.titleMedium, color = Ink)
                            Spacer(Modifier.height(10.dp))
                            listOf("BCA 1234567890", "Mandiri 0987654321", "a.n. DaunPOS Indonesia").forEach {
                                Text("• $it", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            Button(
                onClick = {
                    val cash = if (method == PaymentMethod.CASH) cashReceived else null
                    val id = vm.checkout(method, cash)
                    onSuccess(id)
                },
                enabled = method != PaymentMethod.CASH || change >= 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = Color.White,
                    disabledContainerColor = GreenSoft,
                    disabledContentColor = GreenDeep
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Selesaikan Transaksi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
