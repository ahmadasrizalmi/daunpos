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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint
import kotlinx.coroutines.delay

@Composable
fun PaymentScreen(
    vm: DaunPOSViewModel,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.smallestScreenWidthDp >= 600

    val cart by vm.cart.collectAsState()
    val tableNumber by vm.selectedTable.collectAsState()
    val total = vm.cartTotal
    val subtotal = vm.cartSubtotal
    val tax = vm.cartTax
    val discount by vm.discount.collectAsState()

    var method by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashInput by remember { mutableStateOf("") }
    val cashReceived = cashInput.toIntOrNull() ?: 0
    val change = cashReceived - total

    // Prevent double submission
    var isSubmitting by remember { mutableStateOf(false) }

    // QRIS state
    var qrisChecking by remember { mutableStateOf(false) }
    var qrisPaid by remember { mutableStateOf(false) }
    var qrisCountdown by remember { mutableStateOf(300) }

    LaunchedEffect(method) {
        if (method == PaymentMethod.QRIS) {
            while (qrisCountdown > 0 && !qrisPaid) {
                delay(1000)
                qrisCountdown--
            }
        }
    }

    fun submitCheckout() {
        if (isSubmitting) return
        isSubmitting = true
        val cash = if (method == PaymentMethod.CASH) cashReceived else null
        val id = vm.checkout(method, cash, tableNumber)
        onSuccess(id)
    }

    // Guard: Jika keranjang kosong
    if (cart.isEmpty()) {
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
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Ink)
                }
                Text("Pembayaran", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🛒", fontSize = 54.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("Keranjang Masih Kosong", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Silakan pilih menu pesanan terlebih dahulu sebelum masuk ke proses bayar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Kembali ke Kasir", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
    ) {
        // Top Bar
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
            Text("Pembayaran", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(GreenSoft)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.TableRestaurant, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(tableNumber, style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
        }

        if (isTablet) {
            // Tablet Dual-Pane
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left: Order Summary & Bill
                Box(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Total Tagihan Header
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(GreenDeep)
                                    .padding(18.dp)
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

                            Spacer(Modifier.height(18.dp))

                            Text("Rincian Pesanan", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                cart.forEach { item ->
                                    Row(Modifier.fillMaxWidth()) {
                                        Text("${item.quantity}x ${item.product.name}", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                                        Text(formatRupiah(item.product.price * item.quantity), style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Breakdown footer
                        Column(Modifier.padding(top = 16.dp)) {
                            Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                                Text(formatRupiah(subtotal), style = MaterialTheme.typography.bodyMedium, color = Ink)
                            }
                            if (discount > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = Danger)
                                    Text("-" + formatRupiah(discount), style = MaterialTheme.typography.bodyMedium, color = Danger)
                                }
                            }
                            if (tax > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Pajak (${vm.receiptSettings.value.taxPercentage}%)", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                                    Text(formatRupiah(tax), style = MaterialTheme.typography.bodyMedium, color = Ink)
                                }
                            }
                        }
                    }
                }

                // Right: Payment Methods & Actions
                Box(
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(20.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        PaymentOptionsContent(
                            method = method,
                            onSelectMethod = { method = it },
                            total = total,
                            cashInput = cashInput,
                            onChangeCashInput = { cashInput = it },
                            change = change,
                            qrisCountdown = qrisCountdown,
                            qrisChecking = qrisChecking,
                            qrisPaid = qrisPaid,
                            onSimulateQrisPaid = {
                                qrisChecking = true
                                qrisPaid = true
                                qrisChecking = false
                                submitCheckout()
                            },
                            onSubmit = { submitCheckout() }
                        )
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

                // Compact order summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Rincian Pesanan · $tableNumber", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        cart.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Text("${item.quantity}x ${item.product.name}", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                                Text(formatRupiah(item.product.price * item.quantity), style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                PaymentOptionsContent(
                    method = method,
                    onSelectMethod = { method = it },
                    total = total,
                    cashInput = cashInput,
                    onChangeCashInput = { cashInput = it },
                    change = change,
                    qrisCountdown = qrisCountdown,
                    qrisChecking = qrisChecking,
                    qrisPaid = qrisPaid,
                    onSimulateQrisPaid = {
                        qrisChecking = true
                        qrisPaid = true
                        qrisChecking = false
                        submitCheckout()
                    },
                    onSubmit = { submitCheckout() }
                )
            }
        }
    }
}

@Composable
private fun PaymentOptionsContent(
    method: PaymentMethod,
    onSelectMethod: (PaymentMethod) -> Unit,
    total: Int,
    cashInput: String,
    onChangeCashInput: (String) -> Unit,
    change: Int,
    qrisCountdown: Int,
    qrisChecking: Boolean,
    qrisPaid: Boolean,
    onSimulateQrisPaid: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Metode Pembayaran", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)

        // Tabs
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaymentMethod.values().forEach { m ->
                val active = method == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) GreenSoft else Color.White)
                        .border(1.5.dp, if (active) Green else Line, RoundedCornerShape(16.dp))
                        .clickable { onSelectMethod(m) }
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
                        Text(
                            m.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) GreenDeep else Ink,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        when (method) {
            PaymentMethod.CASH -> {
                // Info Laci Kasir Offline
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenSoft)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PointOfSale, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Kasir Langsung (Offline) · Laci kasir akan terbuka otomatis saat selesai",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenDeep,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { v -> onChangeCashInput(v.filter { it.isDigit() }.take(8)) },
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

                // Quick amount buttons
                val quickAmounts = remember(total) {
                    val list = mutableListOf<Pair<String, Int>>()
                    list.add("Uang Pas" to total)
                    val next50k = (((total + 49999) / 50000) * 50000).coerceAtLeast(50000)
                    if (next50k != total && next50k > total) {
                        list.add(formatRupiah(next50k) to next50k)
                    }
                    if (total < 100000) {
                        list.add("Rp 100.000" to 100000)
                    }
                    if (total < 200000) {
                        list.add("Rp 200.000" to 200000)
                    }
                    list.take(4)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickAmounts.forEach { (label, amount) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, Line, RoundedCornerShape(12.dp))
                                .clickable { onChangeCashInput(amount.toString()) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenDeep,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
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
                    Column {
                        Text("Kembalian", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (change >= 0) formatRupiah(change) else "Uang kurang " + formatRupiah(-change),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (change >= 0) GreenDeep else Danger,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            PaymentMethod.QRIS -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFFBFDFB))
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("QRIS Dinamis — DaunPOS", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Batas waktu: ${qrisCountdown / 60}:${(qrisCountdown % 60).toString().padStart(2, '0')}", style = MaterialTheme.typography.labelSmall, color = InkSoft)

                        Spacer(Modifier.height(14.dp))

                        // Realistic Monospace QR Code Box
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White)
                                .border(2.dp, GreenDeep, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "█▀▀▀▀▀█ ▀█ █▀▀▀▀▀█\n" +
                                    "█ ███ █ █▀ █ ███ █\n" +
                                    "█ ▀▀▀ █ ▄▀ █ ▀▀▀ █\n" +
                                    "▀▀▀▀▀▀▀ █ ▀▀▀▀▀▀▀\n" +
                                    "▀▄▀▄▀▄▀▀▄█▄▀▄▀▄▀▄\n" +
                                    "█▀▀▀▀▀█ ▄▄ █ █ ▀▄\n" +
                                    "█ ███ █ █▀▀██ █▀█\n" +
                                    "█ ▀▀▀ █ █ ▄▄█ ▀ █\n" +
                                    "▀▀▀▀▀▀▀ ▀ ▀▀▀ ▀▀▀",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 12.sp,
                                    color = Ink,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(formatRupiah(total), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenDeep)
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onSimulateQrisPaid,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (qrisChecking) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Simulasikan Pembayaran Sukses (Webhook)", fontSize = 13.sp)
                        }
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Transfer ke rekening kafe:", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Text("• BCA: 8830-192-881 (a.n. DaunPOS Cafe)", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        Text("• Mandiri: 131-00-9821-332 (a.n. DaunPOS)", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        Text("• Pastikan nominal transfer sesuai: " + formatRupiah(total), style = MaterialTheme.typography.bodySmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onSubmit,
            enabled = (method != PaymentMethod.CASH || change >= 0),
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
            Text(
                when (method) {
                    PaymentMethod.CASH -> "Selesaikan Pembayaran Tunai"
                    PaymentMethod.QRIS -> "Konfirmasi Transaksi QRIS"
                    PaymentMethod.TRANSFER -> "Konfirmasi Transfer Bank"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
