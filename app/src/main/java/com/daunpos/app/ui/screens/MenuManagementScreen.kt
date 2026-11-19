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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.CATEGORIES
import com.daunpos.app.data.Product
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.components.ProductThumbnail
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import kotlinx.coroutines.launch

@Composable
fun MenuManagementScreen(vm: DaunPOSViewModel) {
    val products by vm.products.collectAsState()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Semua") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Owner protection for delete
    var deleteCandidate by remember { mutableStateOf<String?>(null) }
    var ownerPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val filtered = products.filter {
        (category == "Semua" || it.category == category) &&
            it.name.contains(query, ignoreCase = true)
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Manajemen Menu", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
                    Text("${products.size} menu · ${products.count { it.available && it.stock > 0 }} tersedia", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
            }

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Cari nama menu...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = InkSoft) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Green,
                    unfocusedBorderColor = Line,
                    cursorColor = GreenDeep
                )
            )

            // Category filter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATEGORIES.forEach { cat ->
                    val active = cat == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) GreenDeep else Color.White)
                            .border(1.dp, if (active) GreenDeep else Line, RoundedCornerShape(50))
                            .clickable { category = cat }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(cat, color = if (active) Color.White else Ink, style = MaterialTheme.typography.labelMedium, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { product ->
                    ProductRow(
                        product = product,
                        onToggle = { vm.toggleProductAvailable(product.id) },
                        onEdit = {
                            editing = product
                            showDialog = true
                        },
                        onDelete = {
                            deleteCandidate = product.id
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editing = null
                showDialog = true
            },
            containerColor = Green,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah Menu", modifier = Modifier.size(28.dp))
        }
    }

    if (showDialog) {
        ProductDialog(
            initial = editing,
            nextId = vm.nextProductId(),
            existingCategories = products.map { it.category }.distinct().filter { it.isNotBlank() },
            onDismiss = { showDialog = false },
            onSave = { product ->
                vm.upsertProduct(product)
                showDialog = false
            }
        )
    }

    // Owner PIN confirmation for delete
    deleteCandidate?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null; ownerPin = ""; pinError = null },
            title = { Text("Hapus Menu (Otorisasi Pemilik)") },
            text = {
                Column {
                    Text("Menghapus menu memerlukan konfirmasi PIN pemilik kafe (default: 0000).", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) ownerPin = it },
                        label = { Text("PIN Pemilik 4-Digit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = pinError != null
                    )
                    if (pinError != null) {
                        Text(pinError ?: "", color = Danger, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val valid = vm.verifyOwnerPin(ownerPin)
                            if (valid) {
                                vm.deleteProduct(id)
                                deleteCandidate = null
                                ownerPin = ""
                                pinError = null
                            } else {
                                pinError = "PIN pemilik salah."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null; ownerPin = ""; pinError = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ProductRow(
    product: Product,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            ProductThumbnail(product = product, height = 52)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(product.name, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${product.category} · Stok: ${product.stock} porsi",
                style = MaterialTheme.typography.labelSmall,
                color = if (product.stock <= 5) Danger else InkSoft
            )
            Spacer(Modifier.height(2.dp))
            Text(formatRupiah(product.price), style = MaterialTheme.typography.bodyMedium, color = GreenDeep, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = product.available && product.stock > 0,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Green,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Line
                )
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = InkSoft, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = Danger, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ProductDialog(
    initial: Product?,
    nextId: String,
    existingCategories: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isEdit = initial != null
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stockText by remember { mutableStateOf(initial?.stock?.toString() ?: "50") }
    var category by remember { mutableStateOf(initial?.category ?: existingCategories.firstOrNull() ?: "Umum") }
    var barcodeSku by remember { mutableStateOf(initial?.barcodeSku ?: "") }
    var durationMinutesText by remember { mutableStateOf(if ((initial?.durationMinutes ?: 0) > 0) initial!!.durationMinutes.toString() else "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var catExpanded by remember { mutableStateOf(false) }

    val categories = remember(existingCategories) {
        (existingCategories + listOf("Makanan", "Minuman", "Pakaian", "Aksesoris", "Studio Foto", "Haircut", "Treatment", "Servis", "Umum")).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Ubah Menu / Layanan" else "Tambah Menu / Layanan Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Menu / Layanan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category dropdown
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori (Ketik atau Pilih)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { it.isDigit() } },
                    label = { Text("Harga Jual (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stockText,
                    onValueChange = { v -> stockText = v.filter { it.isDigit() } },
                    label = { Text("Kuantitas Stok / Kuota") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = barcodeSku,
                    onValueChange = { barcodeSku = it },
                    label = { Text("Barcode / SKU (Opsional, Retail)") },
                    placeholder = { Text("Contoh: 899123456001") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationMinutesText,
                    onValueChange = { v -> durationMinutesText = v.filter { it.isDigit() } },
                    label = { Text("Estimasi Durasi (Menit, Opsional Jasa)") },
                    placeholder = { Text("Contoh: 30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL Foto Produk (Opsional)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toIntOrNull() ?: 0
                    val stock = stockText.toIntOrNull() ?: 50
                    if (name.isNotBlank() && price > 0) {
                        onSave(
                            Product(
                                id = initial?.id ?: nextId,
                                name = name.trim(),
                                category = category.trim().ifBlank { "Umum" },
                                price = price,
                                costPrice = initial?.costPrice ?: 0,
                                stock = stock,
                                available = stock > 0,
                                barcodeSku = barcodeSku.trim(),
                                durationMinutes = durationMinutesText.toIntOrNull() ?: 0,
                                emoji = initial?.emoji ?: "🍽️",
                                colorHex = initial?.colorHex ?: 0xFFE7F4EA,
                                imageUrl = imageUrl.ifBlank { null }
                            )
                        )
                    }
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
