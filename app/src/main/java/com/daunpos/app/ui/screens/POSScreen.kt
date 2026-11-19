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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.data.CartItem
import com.daunpos.app.data.Product
import com.daunpos.app.data.StoreStaff
import com.daunpos.app.data.formatRupiah
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

@Composable
fun POSScreen(
    vm: DaunPOSViewModel,
    onCheckout: () -> Unit,
    onPrinterSettings: () -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.isTablet

    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val cashier by vm.cashier.collectAsState()
    val bluetooth by vm.bluetoothState.collectAsState()
    val selectedTable by vm.selectedTable.collectAsState()
    val discount by vm.discount.collectAsState()
    val storeProfile by vm.storeProfile.collectAsState()
    val staffList by vm.staffList.collectAsState()

    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var tableMenuOpen by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }
    var activeNoteItem by remember { mutableStateOf<CartItem?>(null) }
    var barcodeFeedback by remember { mutableStateOf<String?>(null) }

    val category = storeProfile.businessCategory
    val isRetail = category == BusinessCategory.RETAIL
    val isService = category == BusinessCategory.SERVICE

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
                        Text(if (isRetail) "Cari nama barang atau ketik SKU… (⌘K)" else "Cari menu atau layanan… (⌘K)")
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
                            ProductMenuCard(product = product, onClick = { vm.addToCart(product) })
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

                        // Adaptive Destination Selector (Meja vs Pelanggan vs Staf)
                        AdaptiveDestinationBadge(
                            category = category,
                            selectedTable = selectedTable,
                            onOpenFnbDropdown = { tableMenuOpen = true },
                            onOpenStaffDialog = { showStaffDialog = true }
                        )

                        // Dropdown for F&B or Retail
                        if (!isService) {
                            DropdownMenu(expanded = tableMenuOpen, onDismissRequest = { tableMenuOpen = false }) {
                                val options = if (isRetail) retailCustomerOptions else fnbTableOptions
                                options.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            vm.setTable(opt)
                                            tableMenuOpen = false
                                        }
                                    )
                                }
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
                                "Estimasi Pengerjaan: $totalDurationMinutes Menit",
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
                                    isServiceMode = isService,
                                    onIncrement = { vm.increment(item.product.id) },
                                    onDecrement = { vm.decrement(item.product.id) },
                                    onRemove = { vm.removeFromCart(item.product.id) },
                                    onEditNote = { activeNoteItem = item }
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

                        if (discount > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = Danger)
                                Text("-" + formatRupiah(discount), style = MaterialTheme.typography.bodyMedium, color = Danger)
                            }
                        }

                        if (vm.cartTax > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pajak (${vm.receiptSettings.value.taxPercentage}%)", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍃", fontSize = 20.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("DaunPOS", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                    Text(cashier, style = MaterialTheme.typography.labelSmall, color = InkSoft)
                }

                // Adaptive Destination Selector
                AdaptiveDestinationBadge(
                    category = category,
                    selectedTable = selectedTable,
                    onOpenFnbDropdown = { tableMenuOpen = true },
                    onOpenStaffDialog = { showStaffDialog = true }
                )

                if (!isService) {
                    DropdownMenu(expanded = tableMenuOpen, onDismissRequest = { tableMenuOpen = false }) {
                        val options = if (isRetail) retailCustomerOptions else fnbTableOptions
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    vm.setTable(opt)
                                    tableMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                IconButton(onClick = onPrinterSettings) {
                    Icon(
                        Icons.Filled.Print,
                        contentDescription = "Printer",
                        tint = if (bluetooth.connected) Green else Danger,
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
                            ProductMenuCard(product = product, onClick = { vm.addToCart(product) })
                        }
                    }
                }
            }

            // Cart Bottom Panel
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                if (cart.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🛒", fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Keranjang kosong", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {},
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(disabledContainerColor = GreenSoft),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Bayar " + formatRupiah(0), color = GreenDeep, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Pesanan (${cart.sumOf { it.quantity }} item) · $selectedTable",
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink,
                                fontWeight = FontWeight.Bold
                            )
                            if (isService && totalDurationMinutes > 0) {
                                Text("⏱ $totalDurationMinutes mnt", style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            cart.forEach { item ->
                                CartItemRow(
                                    item = item,
                                    isServiceMode = isService,
                                    onIncrement = { vm.increment(item.product.id) },
                                    onDecrement = { vm.decrement(item.product.id) },
                                    onRemove = { vm.removeFromCart(item.product.id) },
                                    onEditNote = { activeNoteItem = item }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Total", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                                Text(formatRupiah(vm.cartTotal), style = MaterialTheme.typography.headlineSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = onCheckout,
                                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Bayar " + formatRupiah(vm.cartTotal), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Staff Selection (Service Mode)
    if (showStaffDialog) {
        StaffSelectionDialog(
            currentStaff = selectedTable,
            staffList = staffList,
            onDismiss = { showStaffDialog = false },
            onSelectStaff = { staff ->
                vm.setTable(staff)
                showStaffDialog = false
            },
            onAddNewStaff = { newName ->
                vm.addStaff(newName)
            }
        )
    }

    // Dialog: Cart Item Note & Custom Staff
    activeNoteItem?.let { item ->
        ItemNoteDialog(
            item = item,
            staffList = staffList,
            onDismiss = { activeNoteItem = null },
            onSave = { note, staff ->
                vm.updateItemNote(item.product.id, note)
                vm.updateItemStaff(item.product.id, staff)
            }
        )
    }
}

@Composable
private fun AdaptiveDestinationBadge(
    category: BusinessCategory,
    selectedTable: String,
    onOpenFnbDropdown: () -> Unit,
    onOpenStaffDialog: () -> Unit
) {
    val isService = category == BusinessCategory.SERVICE
    val isRetail = category == BusinessCategory.RETAIL

    val (icon, label) = when {
        isService -> Pair(
            Icons.Filled.Badge,
            if (selectedTable.startsWith("Staf:")) selectedTable else "Staf: $selectedTable"
        )
        isRetail -> Pair(Icons.Filled.Storefront, selectedTable)
        else -> Pair(Icons.Filled.TableRestaurant, selectedTable)
    }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(GreenSoft)
                .clickable {
                    if (isService) onOpenStaffDialog() else onOpenFnbDropdown()
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
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
    isServiceMode: Boolean = false,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
    onEditNote: () -> Unit
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
                    if (item.product.durationMinutes > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text("⏱ ${item.product.durationMinutes} mnt", style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.SemiBold)
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
                    item.quantity.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink
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
            IconButton(onClick = onEditNote, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Filled.EditNote,
                    contentDescription = "Catatan",
                    tint = if (item.note.isNotBlank() || item.staffName.isNotBlank()) GreenDeep else InkSoft,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Hapus", tint = InkSoft, modifier = Modifier.size(16.dp))
            }
        }

        if (item.note.isNotBlank() || item.staffName.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.staffName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(GreenSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("👤 ${item.staffName}", fontSize = 10.sp, color = GreenDeep, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (item.note.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF1F3F5))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("📝 ${item.note}", fontSize = 10.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun StaffSelectionDialog(
    currentStaff: String,
    staffList: List<StoreStaff>,
    onDismiss: () -> Unit,
    onSelectStaff: (String) -> Unit,
    onAddNewStaff: (String) -> Unit
) {
    var newStaffInput by remember { mutableStateOf("") }
    val cleanedCurrent = if (currentStaff.startsWith("Staf:")) currentStaff.substringAfter(":").trim() else currentStaff

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pilih Staf / Pelaksana", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Pilih staf yang bertugas untuk pesanan ini (misal: Fotografer, Stylist, Teknisi, dll.)",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )

                // Option: Umum
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectStaff("Staf: Umum") },
                    color = if (cleanedCurrent.equals("Umum", ignoreCase = true)) GreenSoft else Color(0xFFF8F9FA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (cleanedCurrent.equals("Umum", ignoreCase = true)) Green else Line)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = GreenDeep)
                        Spacer(Modifier.width(10.dp))
                        Text("Staf Umum / Bersama", fontWeight = FontWeight.SemiBold, color = Ink)
                    }
                }

                // Existing staff
                staffList.forEach { staff ->
                    val selected = cleanedCurrent.equals(staff.name, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectStaff("Staf: ${staff.name}") },
                        color = if (selected) GreenSoft else Color(0xFFF8F9FA),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Green else Line)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = GreenDeep)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(staff.name, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text("Peran: ${staff.role}", fontSize = 11.sp, color = InkSoft)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                Text("Tambah Staf Baru", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Ink)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newStaffInput,
                        onValueChange = { newStaffInput = it },
                        placeholder = { Text("Nama staf / petugas...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newStaffInput.isNotBlank()) {
                                onAddNewStaff(newStaffInput.trim())
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDeep),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun ItemNoteDialog(
    item: CartItem,
    staffList: List<StoreStaff>,
    onDismiss: () -> Unit,
    onSave: (note: String, staff: String) -> Unit
) {
    var note by remember { mutableStateOf(item.note) }
    var selectedStaff by remember { mutableStateOf(item.staffName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catatan Layanan: ${item.product.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Keterangan / Request Khusus") },
                    placeholder = { Text("Contoh: Background Navy, 5 Orang, Ganti LCD...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (staffList.isNotEmpty()) {
                    Text("Pilih Staf Khusus untuk Item Ini (Opsional):", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isNone = selectedStaff.isBlank()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isNone) GreenDeep else Color.White)
                                .border(1.dp, if (isNone) GreenDeep else Line, RoundedCornerShape(50))
                                .clickable { selectedStaff = "" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Sesuai Pesanan", color = if (isNone) Color.White else Ink, fontSize = 11.sp)
                        }

                        staffList.forEach { s ->
                            val isMatch = selectedStaff == s.name
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isMatch) GreenDeep else Color.White)
                                    .border(1.dp, if (isMatch) GreenDeep else Line, RoundedCornerShape(50))
                                    .clickable { selectedStaff = s.name }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(s.name, color = if (isMatch) Color.White else Ink, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(note.trim(), selectedStaff.trim())
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