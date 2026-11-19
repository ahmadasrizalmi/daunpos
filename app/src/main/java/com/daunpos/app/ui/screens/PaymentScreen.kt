package com.daunpos.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.qris.QrisConverter
import com.daunpos.app.notification.BankNotificationEvent
import com.daunpos.app.notification.PaymentNotificationBridge
import com.daunpos.app.ui.components.QrisTutorialDialog
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint
import com.daunpos.app.ui.theme.isTablet
import kotlinx.coroutines.delay

@Composable
fun PaymentScreen(
    vm: DaunPOSViewModel,
    onBack: () -> Unit,
    onSuccess: (String) -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.isTablet

    val cart by vm.cart.collectAsState()
    val tableNumber by vm.selectedTable.collectAsState()
    val total = vm.cartTotal
    val subtotal = vm.cartSubtotal
    val tax = vm.cartTax
    val discount by vm.discount.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // Notification listener status & verified feedback
    val invoiceOpenedTimestamp = remember { System.currentTimeMillis() }
    var pendingVerifiedEvent by remember { mutableStateOf<BankNotificationEvent?>(null) }
    var isNotificationAccessGranted by remember {
        mutableStateOf(PaymentNotificationBridge.isNotificationAccessGranted(context))
    }
    var autoVerifiedMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = PaymentNotificationBridge.isNotificationAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    fun confirmVerifiedPayment() {
        val event = pendingVerifiedEvent ?: return
        PaymentNotificationBridge.markConsumed(event.id)
        qrisChecking = false
        qrisPaid = true
        submitCheckout()
    }

    fun dismissVerifiedPayment() {
        pendingVerifiedEvent = null
        autoVerifiedMessage = null
    }

    // Deteksi pembayaran via Android Notification Listener Service
    // Dilindungi: (a) jendela waktu aktif, (b) penandaan konsumsi unik notifikasi, dan (c) konfirmasi kasir
    LaunchedEffect(method, total) {
        if (method == PaymentMethod.QRIS || method == PaymentMethod.TRANSFER) {
            PaymentNotificationBridge.incomingPayments.collect { event ->
                val isRecent = event.timestamp >= (invoiceOpenedTimestamp - 10000L)
                val isNotConsumed = !PaymentNotificationBridge.isConsumed(event.id)
                if (event.amount == total && isRecent && isNotConsumed && !qrisPaid && !isSubmitting) {
                    pendingVerifiedEvent = event
                    autoVerifiedMessage = "✓ Notifikasi ${event.bankName}: Rp ${formatRupiah(event.amount)} terdeteksi!"
                }
            }
        }
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
                            vm = vm,
                            method = method,
                            onSelectMethod = { method = it },
                            total = total,
                            cashInput = cashInput,
                            onChangeCashInput = { cashInput = it },
                            change = change,
                            qrisCountdown = qrisCountdown,
                            qrisChecking = qrisChecking,
                            qrisPaid = qrisPaid,
                            isNotificationAccessGranted = isNotificationAccessGranted,
                            onOpenNotificationSettings = {
                                PaymentNotificationBridge.openNotificationSettings(context)
                            },
                            pendingVerifiedEvent = pendingVerifiedEvent,
                            onConfirmVerifiedPayment = { confirmVerifiedPayment() },
                            onDismissVerifiedPayment = { dismissVerifiedPayment() },
                            autoVerifiedMessage = autoVerifiedMessage,
                            onSimulateBankNotification = {
                                PaymentNotificationBridge.postPayment(
                                    BankNotificationEvent(
                                        sourcePackage = "id.bmri.livin",
                                        bankName = "Mandiri Livin'",
                                        amount = total,
                                        senderName = "Pelanggan Demo",
                                        rawTitle = "Transfer Masuk",
                                        rawText = "Dana masuk sebesar Rp ${formatRupiah(total)} dari Pelanggan Demo"
                                    )
                                )
                            },
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
                    vm = vm,
                    method = method,
                    onSelectMethod = { method = it },
                    total = total,
                    cashInput = cashInput,
                    onChangeCashInput = { cashInput = it },
                    change = change,
                    qrisCountdown = qrisCountdown,
                    qrisChecking = qrisChecking,
                    qrisPaid = qrisPaid,
                    isNotificationAccessGranted = isNotificationAccessGranted,
                    onOpenNotificationSettings = {
                        PaymentNotificationBridge.openNotificationSettings(context)
                    },
                    pendingVerifiedEvent = pendingVerifiedEvent,
                    onConfirmVerifiedPayment = { confirmVerifiedPayment() },
                    onDismissVerifiedPayment = { dismissVerifiedPayment() },
                    autoVerifiedMessage = autoVerifiedMessage,
                    onSimulateBankNotification = {
                        PaymentNotificationBridge.postPayment(
                            BankNotificationEvent(
                                sourcePackage = "id.bmri.livin",
                                bankName = "Mandiri Livin'",
                                amount = total,
                                senderName = "Pelanggan Demo",
                                rawTitle = "Transfer Masuk",
                                rawText = "Dana masuk sebesar Rp ${formatRupiah(total)} dari Pelanggan Demo"
                            )
                        )
                    },
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
    vm: DaunPOSViewModel,
    method: PaymentMethod,
    onSelectMethod: (PaymentMethod) -> Unit,
    total: Int,
    cashInput: String,
    onChangeCashInput: (String) -> Unit,
    change: Int,
    qrisCountdown: Int,
    qrisChecking: Boolean,
    qrisPaid: Boolean,
    isNotificationAccessGranted: Boolean,
    onOpenNotificationSettings: () -> Unit,
    pendingVerifiedEvent: BankNotificationEvent?,
    onConfirmVerifiedPayment: () -> Unit,
    onDismissVerifiedPayment: () -> Unit,
    autoVerifiedMessage: String?,
    onSimulateBankNotification: () -> Unit,
    onSimulateQrisPaid: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    val bluetooth by vm.bluetoothState.collectAsState()
    val staticQris by vm.staticQris.collectAsState()
    var showTutorialDialog by remember { mutableStateOf(false) }
    var qrisScanFeedback by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = QrisConverter.decodeQrFromUri(context, uri)
            if (!decoded.isNullOrBlank()) {
                vm.updateStaticQris(decoded.trim())
                qrisScanFeedback = "✅ QRIS Berhasil Terbaca & Tersimpan!"
            } else {
                qrisScanFeedback = "⚠️ QR Code tidak terdeteksi pada gambar."
            }
        }
    }

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
                if (staticQris.isBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFBFDFB))
                            .border(1.5.dp, Line, RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFBEAE7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Danger,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = "QRIS Toko Belum Dikonfigurasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                            Text(
                                text = "Silakan scan foto stiker QRIS merchant Anda atau gunakan QRIS contoh untuk mode demo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { photoPickerLauncher.launch("image/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenSoft, contentColor = GreenDeep),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Scan Foto QRIS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { showTutorialDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = InkSoft, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Panduan", color = InkSoft, fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = { vm.updateStaticQris(QrisConverter.DEMO_SAMPLE_STATIC_QRIS) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Gunakan QRIS Contoh (Mode Demo)", fontSize = 12.sp)
                            }

                            qrisScanFeedback?.let { msg ->
                                Text(msg, fontSize = 11.sp, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (showTutorialDialog) {
                        QrisTutorialDialog(onDismiss = { showTutorialDialog = false })
                    }
                } else {
                    // Generate Real Dynamic QRIS Payload & Real Bitmap
                    val dynamicPayload = remember(staticQris, total) {
                        vm.generateDynamicQris(total)
                    }
                    val merchantInfo = remember(dynamicPayload) {
                        QrisConverter.parseMerchantInfo(dynamicPayload)
                    }
                    val qrBitmap = remember(dynamicPayload) {
                        runCatching { QrisConverter.generateQrBitmap(dynamicPayload, 512) }.getOrNull()
                    }

                    var qrisPrintedMessage by remember { mutableStateOf<String?>(null) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFFBFDFB))
                            .border(1.5.dp, GreenSoft, RoundedCornerShape(20.dp))
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(merchantInfo.merchantName, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${merchantInfo.city} · NMID: ${merchantInfo.nmid.ifBlank { "ID1020021950262" }}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                            }

                            // Real Scannable High-Res QR Code Image
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, GreenDeep, RoundedCornerShape(16.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (qrBitmap != null) {
                                    Image(
                                        bitmap = qrBitmap.asImageBitmap(),
                                        contentDescription = "QRIS Dinamis Asli",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    CircularProgressIndicator(color = Green, strokeWidth = 3.dp)
                                }
                            }

                            // Locked Amount Banner
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(GreenSoft)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Nominal Terkunci: ${formatRupiah(total)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GreenDeep,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Auto-Verifikasi Bank & E-Wallet Status Banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isNotificationAccessGranted) GreenSoft else Color(0xFFFFF4E5))
                                    .border(1.dp, if (isNotificationAccessGranted) Green else Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isNotificationAccessGranted) Icons.Filled.NotificationsActive else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (isNotificationAccessGranted) GreenDeep else Color(0xFFE65100),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        if (isNotificationAccessGranted) "Auto-Verifikasi Bank & QRIS Aktif" else "Izin Notifikasi Belum Aktif",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNotificationAccessGranted) GreenDeep else Color(0xFFE65100)
                                    )
                                    Text(
                                        if (isNotificationAccessGranted) "POS mendeteksi otomatis saat ada notifikasi masuk Rp ${formatRupiah(total)}"
                                        else "Aktifkan akses notifikasi agar POS membaca mutasi masuk secara offline",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = InkSoft
                                    )
                                }
                                if (!isNotificationAccessGranted) {
                                    Spacer(Modifier.width(6.dp))
                                    Button(
                                        onClick = onOpenNotificationSettings,
                                        colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Aktifkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Feedback Banner / Konfirmasi Pembayaran Terdeteksi via Notifikasi
                            if (pendingVerifiedEvent != null) {
                                PendingNotificationConfirmationCard(
                                    event = pendingVerifiedEvent,
                                    onConfirm = onConfirmVerifiedPayment,
                                    onDismiss = onDismissVerifiedPayment
                                )
                            } else if (autoVerifiedMessage != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GreenSoft)
                                        .border(1.5.dp, Green, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        autoVerifiedMessage,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenDeep
                                    )
                                }
                            }

                            Text(
                                "Mendukung push notifikasi: BCA, Mandiri, BRI, BNI, GoBiz, OVO, Dana, ShopeePay",
                                style = MaterialTheme.typography.labelSmall,
                                color = InkSoft,
                                textAlign = TextAlign.Center
                            )

                            // Tombol Cetak Struk QRIS Meja
                            OutlinedButton(
                                onClick = {
                                    if (bluetooth.connected) {
                                        vm.printQrisBill(total)
                                        qrisPrintedMessage = "Struk QRIS berhasil dikirim ke printer!"
                                    } else {
                                        qrisPrintedMessage = "Printer thermal belum terhubung! Silakan hubungkan di menu Printer."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (bluetooth.connected) Green else Line)
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, tint = if (bluetooth.connected) GreenDeep else InkSoft, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (bluetooth.connected) "Cetak QR Tagihan ke Struk" else "Printer Belum Terhubung",
                                    color = if (bluetooth.connected) GreenDeep else InkSoft,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }

                            if (qrisPrintedMessage != null) {
                                Text(
                                    qrisPrintedMessage!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (bluetooth.connected) GreenDeep else Danger,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Tombol Simulasi Pembayaran / Notifikasi Masuk
                            Button(
                                onClick = onSimulateBankNotification,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (qrisChecking) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Simulasikan Notifikasi Bank Masuk (${formatRupiah(total)})", fontSize = 13.sp)
                            }
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Transfer ke rekening kafe:", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                        Text("• BCA: 8830-192-881 (a.n. DaunPOS Cafe)", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        Text("• Mandiri: 131-00-9821-332 (a.n. DaunPOS)", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        Text("• Pastikan nominal transfer sesuai: " + formatRupiah(total), style = MaterialTheme.typography.bodySmall, color = GreenDeep, fontWeight = FontWeight.Bold)

                        // Auto-verifikasi transfer status
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNotificationAccessGranted) GreenSoft else Color(0xFFFFF4E5))
                                .border(1.dp, if (isNotificationAccessGranted) Green else Color(0xFFFFB74D), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isNotificationAccessGranted) Icons.Filled.NotificationsActive else Icons.Filled.Warning,
                                contentDescription = null,
                                tint = if (isNotificationAccessGranted) GreenDeep else Color(0xFFE65100),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isNotificationAccessGranted) "Auto-Verifikasi Transfer Aktif" else "Izin Notifikasi Belum Aktif",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNotificationAccessGranted) GreenDeep else Color(0xFFE65100)
                                )
                                Text(
                                    if (isNotificationAccessGranted) "POS otomatis mendeteksi mutasi transfer masuk Rp ${formatRupiah(total)}"
                                    else "Aktifkan agar POS mendeteksi notifikasi m-banking secara offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = InkSoft
                                )
                            }
                        }

                        // Feedback Banner / Konfirmasi Pembayaran Terdeteksi via Notifikasi
                        if (pendingVerifiedEvent != null) {
                            PendingNotificationConfirmationCard(
                                event = pendingVerifiedEvent,
                                onConfirm = onConfirmVerifiedPayment,
                                onDismiss = onDismissVerifiedPayment
                            )
                        } else if (autoVerifiedMessage != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GreenSoft)
                                    .border(1.5.dp, Green, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    autoVerifiedMessage,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDeep
                                )
                            }
                        }

                        Button(
                            onClick = onSimulateBankNotification,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Simulasikan Notifikasi Transfer Masuk", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val isCheckoutEnabled = when (method) {
            PaymentMethod.CASH -> change >= 0
            PaymentMethod.QRIS -> staticQris.isNotBlank()
            PaymentMethod.TRANSFER -> true
        }

        Button(
            onClick = onSubmit,
            enabled = isCheckoutEnabled,
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

@Composable
private fun PendingNotificationConfirmationCard(
    event: BankNotificationEvent,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F5E9))
            .border(2.dp, Green, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Green),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Notifikasi Pembayaran Cocok!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = GreenDeep
                    )
                    Text(
                        "Uang masuk terdeteksi via status bar. Konfirmasi kasir untuk menyelesaikan:",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = InkSoft
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bank / E-Wallet:", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        Text(event.bankName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nominal Diterima:", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                        Text(formatRupiah(event.amount), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreenDeep)
                    }
                    if (event.senderName.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Nama Pengirim:", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                            Text(event.senderName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Ink)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.38f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Text("Abaikan", color = InkSoft, fontSize = 12.sp)
                }

                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(0.62f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Konfirmasi Bayar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
