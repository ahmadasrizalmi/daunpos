package com.daunpos.app.ui.screens

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.AlertDialog
import com.daunpos.app.ui.components.DaunPOSButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.data.CartItem
import com.daunpos.app.data.Product
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.formatDurationMinutes
import com.daunpos.app.ui.components.DaunPOSLogoMark
import com.daunpos.app.ui.components.EmptyState
import com.daunpos.app.ui.components.ProductMenuCard
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.isTablet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val fnbTableOptions = listOf("Take Away", "Meja 01", "Meja 02", "Meja 03", "Meja 04", "Meja 05", "Meja 06")
private val retailCustomerOptions = listOf("Pelanggan Umum", "Member", "Grosir", "Reseller", "Online / Kurir")
private val serviceDestinationOptions = listOf("Layanan", "Pelanggan", "Online / Kurir")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSScreen(
    vm: DaunPOSViewModel,
    onCheckout: () -> Unit,
    onPrinterSettings: () -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.isTablet
    val compactPhone = config.screenWidthDp <= 360

    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val cashier by vm.cashier.collectAsState()
    val bluetooth by vm.bluetoothState.collectAsState()
    val selectedTable by vm.selectedTable.collectAsState()
    val storeProfile by vm.storeProfile.collectAsState()
    val receiptSettings by vm.receiptSettings.collectAsState()

    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var tableMenuOpen by remember { mutableStateOf(false) }
    var activeNoteItem by remember { mutableStateOf<CartItem?>(null) }
    var barcodeFeedback by remember { mutableStateOf<String?>(null) }
    var showCartSheet by remember { mutableStateOf(false) }
    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val category = storeProfile.businessCategory
    val isRetail = category == BusinessCategory.RETAIL
    val isService = category == BusinessCategory.SERVICE

    LaunchedEffect(category, selectedTable) {
        if (isService && selectedTable == "Take Away") {
            vm.setTable("Layanan")
        }
    }

    // Dynamic category chips extracted from available products
    val dynamicCategories = remember(products) {
        listOf("Semua") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filtered = products.filter {
        (selectedCategory == "Semua" || it.category == selectedCategory) &&
            (it.name.contains(searchQuery, ignoreCase = true) || it.barcodeSku.contains(searchQuery, ignoreCase = true))
    }

    val totalDurationMinutes = remember(cart) {
        cart.sumOf { (it.product.durationMinutes) * it.quantity }
    }

    // Function to handle barcode scan result
    fun handleBarcodeScan(code: String) {
        vm.scanBarcode(code) { found ->
            if (found != null) {
                barcodeFeedback = "✓ ${found.name} ditambahkan"
            } else {
                barcodeFeedback = "SKU/Barcode '$code' tidak ditemukan"
            }
            scope.launch {
                delay(3000)
                barcodeFeedback = null
            }
        }
    }

    if (isTablet) {
        // Tablet Dual-Pane Split Layout (1024px design)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
        ) {
            // Left Pane: Search + Barcode Scanner (if Retail) + Categories + 4-Column Grid Menu
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(if (isRetail) "Cari nama barang atau ketik SKU…" else "Cari ${category.itemLabel.lowercase()}…")
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = InkSoft) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Line
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Retail Mode: Barcode Quick Input Bar
                if (isRetail) {
                    Spacer(Modifier.height(10.dp))
                    BarcodeInputBar(
                        onScan = { handleBarcodeScan(it) },
                        feedback = barcodeFeedback
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dynamicCategories.forEach { cat ->
                        val active = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (active) GreenDeep else Color.White)
                                .border(1.dp, if (active) GreenDeep else Line, RoundedCornerShape(50))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Text(
                                cat,
                                color = if (active) Color.White else Ink,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Product grid 4 columns
                if (filtered.isEmpty()) {
                    EmptyState("🔍", "Katalog kosong", "Tidak ada item yang sesuai kriteria pencarian.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            ProductMenuCard(
                                product = product,
                                onClick = { vm.addToCart(product) },
                                trackStock = !isService
                            )
                        }
                    }
                }
            }

            // Right Pane: Sticky Order Summary / Cart Panel (350dp)
            Box(
                modifier = Modifier
                    .width(350.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .border(1.dp, Line)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pesanan", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)

                        AdaptiveDestinationBadge(
                            category = category,
                            selectedTable = selectedTable,
                            onOpenDropdown = { tableMenuOpen = true }
                        )

                        DropdownMenu(expanded = tableMenuOpen, onDismissRequest = { tableMenuOpen = false }) {
                            val options = when {
                                isRetail -> retailCustomerOptions
                                isService -> serviceDestinationOptions
                                else -> fnbTableOptions
                            }
                            options.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        vm.setTable(option)
                                        tableMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Service Duration Summary Header
                    if (isService && totalDurationMinutes > 0) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GreenSoft)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Estimasi Pengerjaan: ${formatDurationMinutes(totalDurationMinutes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GreenDeep,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    if (cart.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada item di keranjang", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart, key = { it.product.id }) { item ->
                                CartItemRow(
                                    item = item,
                                    onIncrement = { vm.increment(item.product.id) },
                                    onDecrement = { vm.decrement(item.product.id) },
                                    onRemove = { vm.removeFromCart(item.product.id) },
                                    onEditNote = { activeNoteItem = item },
                                    isService = isService
                                )
                            }
                        }
                    }

                    // Bill breakdown & Checkout button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        Spacer(Modifier.height(8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                            Text(formatRupiah(vm.cartSubtotal), style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }

                        if (vm.cartDiscount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = Danger)
                                Text("-" + formatRupiah(vm.cartDiscount), style = MaterialTheme.typography.bodyMedium, color = Danger)
                            }
                        }

                        if (vm.cartTax > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pajak (${receiptSettings.taxPercentage}%)", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                                Text(formatRupiah(vm.cartTax), style = MaterialTheme.typography.bodyMedium, color = Ink)
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(vm.cartTotal), style = MaterialTheme.typography.headlineSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = onCheckout,
                            enabled = cart.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Bayar " + formatRupiah(vm.cartTotal), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    } else {
        // Phone Layout (390px design)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DaunPOSLogoMark(size = if (compactPhone) 36.dp else 42.dp)
                Spacer(Modifier.width(if (compactPhone) 6.dp else 10.dp))
                Column(Modifier.weight(1f)) {
                    Text("DaunPOS", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                    Text(cashier, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }

                AdaptiveDestinationBadge(
                    category = category,
                    selectedTable = selectedTable,
                    onOpenDropdown = { tableMenuOpen = true },
                    compact = compactPhone
                )

                DropdownMenu(expanded = tableMenuOpen, onDismissRequest = { tableMenuOpen = false }) {
                    val options = when {
                        isRetail -> retailCustomerOptions
                        isService -> serviceDestinationOptions
                        else -> fnbTableOptions
                    }
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                vm.setTable(option)
                                tableMenuOpen = false
                            }
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(onClick = onPrinterSettings, modifier = Modifier.size(if (compactPhone) 40.dp else 48.dp)) {
                    Icon(
                        Icons.Filled.Print,
                        contentDescription = "Printer",
                        tint = if (bluetooth.connected) GreenDeep else InkSoft,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Retail Barcode Input on Phone
            if (isRetail) {
                BarcodeInputBar(
                    onScan = { handleBarcodeScan(it) },
                    feedback = barcodeFeedback,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Category tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dynamicCategories.forEach { cat ->
                    val active = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) GreenDeep else Color.White)
                            .border(1.dp, if (active) GreenDeep else Line, RoundedCornerShape(50))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            cat,
                            color = if (active) Color.White else Ink,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Product grid
            Box(Modifier.weight(1f)) {
                if (filtered.isEmpty()) {
                    EmptyState("🔍", "Katalog kosong", "Belum ada item di kategori ini.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { product ->
                            ProductMenuCard(
                                product = product,
                                onClick = { vm.addToCart(product) },
                                trackStock = !isService
                            )
                        }
                    }
                }
            }

            // Compact cart launcher stays clear of the bottom navigation.
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Line)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (cart.isEmpty()) "Keranjang kosong" else "${cart.sumOf { it.quantity }} item · $selectedTable",
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSoft
                        )
                        Text(
                            formatRupiah(vm.cartTotal),
                            style = MaterialTheme.typography.titleMedium,
                            color = Ink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { showCartSheet = true },
                        enabled = cart.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenDeep,
                            contentColor = Color.White,
                            disabledContainerColor = GreenSoft,
                            disabledContentColor = InkSoft
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(if (cart.isEmpty()) "Kosong" else "Lihat Keranjang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = cartSheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tinjau Pesanan", style = MaterialTheme.typography.titleLarge, color = Ink, fontWeight = FontWeight.Bold)
                        Text("${cart.sumOf { it.quantity }} item · $selectedTable", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                    }
                    if (isService && totalDurationMinutes > 0) {
                        Text(formatDurationMinutes(totalDurationMinutes), style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
                    }
                }

                cart.forEach { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { vm.increment(item.product.id) },
                        onDecrement = { vm.decrement(item.product.id) },
                        onRemove = { vm.removeFromCart(item.product.id) },
                        onEditNote = { activeNoteItem = item },
                        isService = isService
                    )
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", color = InkSoft, style = MaterialTheme.typography.bodyMedium)
                    Text(formatRupiah(vm.cartSubtotal), color = Ink, style = MaterialTheme.typography.bodyMedium)
                }
                if (vm.cartDiscount > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Diskon", color = Danger, style = MaterialTheme.typography.bodyMedium)
                        Text("-${formatRupiah(vm.cartDiscount)}", color = Danger, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (vm.cartTax > 0) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pajak (${receiptSettings.taxPercentage}%)", color = InkSoft, style = MaterialTheme.typography.bodyMedium)
                        Text(formatRupiah(vm.cartTax), color = Ink, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(formatRupiah(vm.cartTotal), color = GreenDeep, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        showCartSheet = false
                        onCheckout()
                    },
                    enabled = cart.isNotEmpty() && vm.cartTotal > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenDeep,
                        contentColor = Color.White,
                        disabledContainerColor = GreenSoft,
                        disabledContentColor = InkSoft
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Lanjut ke Pembayaran", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // Personal-use checkout keeps order notes without employee assignment.
    activeNoteItem?.let { item ->
        ItemNoteDialog(
            item = item,
            isService = isService,
            onDismiss = { activeNoteItem = null },
            onSave = { note -> vm.updateItemNote(item.product.id, note) }
        )
    }
}

@Composable
private fun AdaptiveDestinationBadge(
    category: BusinessCategory,
    selectedTable: String,
    onOpenDropdown: () -> Unit,
    compact: Boolean = false
) {
    val icon = when (category) {
        BusinessCategory.SERVICE -> Icons.Filled.Schedule
        BusinessCategory.RETAIL -> Icons.Filled.Storefront
        BusinessCategory.FNB -> Icons.Filled.TableRestaurant
    }
    val label = if (category == BusinessCategory.SERVICE && selectedTable == "Take Away") {
        "Layanan"
    } else {
        selectedTable
    }
    val displayLabel = if (compact) {
        when (label) {
            "Pelanggan Umum" -> "Umum"
            "Online / Kurir" -> "Online"
            else -> label
        }
    } else label

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(GreenSoft)
                .clickable(onClick = onOpenDropdown)
                .padding(horizontal = if (compact) 8.dp else 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(if (compact) 14.dp else 16.dp))
            Spacer(Modifier.width(if (compact) 4.dp else 6.dp))
            Text(displayLabel, style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BarcodeInputBar(
    onScan: (String) -> Unit,
    feedback: String?,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                placeholder = { Text("Scan Barcode atau ketik SKU (Enter)...") },
                leadingIcon = {
                    Icon(Icons.Filled.QrCodeScanner, contentDescription = "Barcode", tint = GreenDeep)
                },
                trailingIcon = {
                    if (code.isNotBlank()) {
                        IconButton(onClick = {
                            onScan(code)
                            code = ""
                        }) {
                            Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = GreenDeep)
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (code.isNotBlank()) {
                        onScan(code)
                        code = ""
                    }
                }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Green,
                    unfocusedBorderColor = Line
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (!feedback.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                feedback,
                style = MaterialTheme.typography.labelSmall,
                color = if (feedback.startsWith("✓")) GreenDeep else Danger,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEditNote: () -> Unit,
    isService: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(item.product.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.product.name, style = MaterialTheme.typography.bodyMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatRupiah(item.product.price), style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    Text("/ ${item.product.unitLabel}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
                    if (item.product.durationMinutes > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("· ${formatDurationMinutes(item.product.durationMinutes)}", style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GreenSoft)
                        .clickable { onDecrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                }
                Text(
                    "${item.quantity} ${item.product.unitLabel}",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GreenDeep)
                        .clickable { onIncrement() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Hapus", tint = InkSoft, modifier = Modifier.size(16.dp))
            }
        }

        TextButton(
            onClick = onEditNote,
            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)
        ) {
            Icon(
                Icons.Filled.EditNote,
                contentDescription = null,
                tint = if (item.note.isNotBlank()) GreenDeep else InkSoft,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                when {
                    item.note.isNotBlank() -> "Ubah catatan"
                    isService -> "Tambah catatan / permintaan pelanggan"
                    else -> "Tambah catatan"
                },
                color = if (item.note.isNotBlank()) GreenDeep else InkSoft,
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (item.note.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, top = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF1F3F5))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    "Catatan: ${item.note}",
                    fontSize = 10.sp,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ItemNoteDialog(
    item: CartItem,
    isService: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (note: String) -> Unit
) {
    var note by remember(item.product.id) { mutableStateOf(item.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                (if (isService) "Catatan Layanan: " else "Catatan Item: ") + item.product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (isService) "Permintaan pelanggan (opsional)" else "Catatan pesanan (opsional)") },
                placeholder = {
                    Text(
                        if (isService) "Contoh: model potongan, warna, ukuran, kondisi barang, atau instruksi khusus..."
                        else "Contoh: tanpa gula, warna hitam, layanan ekspres..."
                    )
                },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(note.trim())
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}