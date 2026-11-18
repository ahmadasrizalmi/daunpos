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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.CATEGORIES
import com.daunpos.app.data.Product
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.components.EmptyState
import com.daunpos.app.ui.components.ProductEmoji
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint

@Composable
fun POSScreen(
    vm: DaunPOSViewModel,
    onCheckout: () -> Unit,
    onPrinterSettings: () -> Unit
) {
    val products by vm.products.collectAsState()
    val cart by vm.cart.collectAsState()
    val cashier by vm.cashier.collectAsState()
    val bluetooth by vm.bluetoothState.collectAsState()
    val total = vm.cartTotal

    var selectedCategory by remember { mutableStateOf("Semua") }

    val filtered = if (selectedCategory == "Semua") products
    else products.filter { it.category == selectedCategory }

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
                    .background(Green),
                contentAlignment = Alignment.Center
            ) {
                Text("🍃", fontSize = 20.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("DaunPOS", style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(cashier, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
            IconButton(onClick = onPrinterSettings) {
                Icon(
                    Icons.Filled.Print,
                    contentDescription = "Printer",
                    tint = if (bluetooth.connected) Green else Danger,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Mint),
                contentAlignment = Alignment.Center
            ) {
                Text("DP", style = MaterialTheme.typography.labelMedium, color = GreenDeep, fontWeight = FontWeight.Bold)
            }
        }

        // Category tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
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

        // Product grid
        Box(Modifier.weight(1f)) {
            if (filtered.isEmpty()) {
                EmptyState("🔍", "Menu kosong", "Belum ada produk di kategori ini.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 118.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { product ->
                        ProductCard(product = product, onClick = { vm.addToCart(product) })
                    }
                }
            }
        }

        // Cart panel
        CartPanel(
            cart = cart,
            total = total,
            onIncrement = { vm.increment(it) },
            onDecrement = { vm.decrement(it) },
            onRemove = { vm.removeFromCart(it) },
            onCheckout = onCheckout
        )
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val outOfStock = !product.available
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(enabled = !outOfStock, onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Box {
                ProductEmoji(
                    emoji = product.emoji,
                    colorHex = product.colorHex,
                    size = 64,
                    modifier = Modifier.fillMaxWidth()
                )
                if (outOfStock) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x88FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Stok Habis",
                            color = Ink,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Text(
                formatRupiah(product.price),
                style = MaterialTheme.typography.bodyMedium,
                color = GreenDeep,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CartPanel(
    cart: List<com.daunpos.app.data.CartItem>,
    total: Int,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
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
                Text(
                    "Pesanan (${cart.sumOf { it.quantity }} item)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Ink
                )
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    cart.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.product.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.product.name, style = MaterialTheme.typography.bodyMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    formatRupiah(item.product.price),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = InkSoft
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(GreenSoft)
                                        .clickable { onDecrement(item.product.id) },
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
                                        .clickable { onIncrement(item.product.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = { onRemove(item.product.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Hapus", tint = InkSoft, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.labelMedium, color = InkSoft)
                        Text(formatRupiah(total), style = MaterialTheme.typography.headlineSmall, color = GreenDeep, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = onCheckout,
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Bayar " + formatRupiah(total), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
