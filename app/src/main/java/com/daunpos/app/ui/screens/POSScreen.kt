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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableRestaurant
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.CATEGORIES
import com.daunpos.app.data.CartItem
import com.daunpos.app.data.Product
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
import com.daunpos.app.ui.theme.Mint

private val tableOptions = listOf("Take Away", "Meja 01", "Meja 02", "Meja 03", "Meja 04", "Meja 05", "Meja 06")

@Composable
fun POSScreen(
    vm: DaunPOSViewModel,
    onCheckout: () -> Unit,
    onPrinterSettings: () -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.screenWidthDp >= 700

    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val cashier by vm.cashier.collectAsState()
    val bluetooth by vm.bluetoothState.collectAsState()
    val selectedTable by vm.selectedTable.collectAsState()
    val discount by vm.discount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var tableMenuOpen by remember { mutableStateOf(false) }

    val filtered = products.filter {
        (selectedCategory == "Semua" || it.category == selectedCategory) &&
            it.name.contains(searchQuery, ignoreCase = true)
    }

    if (isTablet) {
        // Tablet Dual-Pane Split Layout (1024px design)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Bg)
        ) {
            // Left Pane: Search + Categories + 4-Column Grid Menu
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
                    placeholder = { Text("Cari menu… (⌘K)") },
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

                Spacer(Modifier.height(12.dp))

                // Categories
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIES.forEach { cat ->
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
                    EmptyState("🔍", "Menu kosong", "Tidak ada menu yang sesuai kriteria.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
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

            // Right Pane: Sticky Order Summary / Cart Panel (380dp)
            Box(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight()
                    .background(Color.White)
                    .border(1.dp, Line)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pesanan Pelanggan", style = MaterialTheme.typography.titleLarge, color = Ink)
                            // Table selector
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(GreenSoft)
                                        .clickable { tableMenuOpen = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.TableRestaurant, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(selectedTable, style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(expanded = tableMenuOpen, onDismissRequest = { tableMenuOpen = false }) {
                                    tableOptions.forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text(t) },
                                            onClick = {
                                                vm.setTable(t)
                                                tableMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada item di keranjang", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 380.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(cart, key = { it.product.id }) { item ->
                                    CartItemRow(
                                        item = item,
                                        onIncrement = { vm.increment(item.product.id) },
                                        onDecrement = { vm.decrement(item.product.id) },
                                        onRemove = { vm.removeFromCart(item.product.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Bill breakdown & Checkout button
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                        Spacer(Modifier.height(12.dp))

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

                        Spacer(Modifier.height(6.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Total", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                            Text(formatRupiah(vm.cartTotal), style = MaterialTheme.typography.headlineSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = onCheckout,
                            enabled = cart.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Text("Bayar " + formatRupiah(vm.cartTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

                // Table selector button
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(GreenSoft)
                            .clickable { tableMenuOpen = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedTable, style = MaterialTheme.typography.labelSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = tableMenuOpen, onDismissRequest = { tableMenuOpen = false }) {
                        tableOptions.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t) },
                                onClick = {
                                    vm.setTable(t)
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

            // Category tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { cat ->
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
                    EmptyState("🔍", "Menu kosong", "Belum ada produk di kategori ini.")
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Pesanan (${cart.sumOf { it.quantity }} item) · $selectedTable",
                                style = MaterialTheme.typography.titleMedium,
                                color = Ink
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            cart.forEach { item ->
                                CartItemRow(
                                    item = item,
                                    onIncrement = { vm.increment(item.product.id) },
                                    onDecrement = { vm.decrement(item.product.id) },
                                    onRemove = { vm.removeFromCart(item.product.id) }
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
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.product.emoji, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(item.product.name, style = MaterialTheme.typography.bodyMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatRupiah(item.product.price), style = MaterialTheme.typography.labelSmall, color = InkSoft)
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
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Hapus", tint = InkSoft, modifier = Modifier.size(16.dp))
        }
    }
}
