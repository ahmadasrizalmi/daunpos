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
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import com.daunpos.app.ui.components.DaunPOSButton as Button
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.Product
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.formatDurationMinutes
import com.daunpos.app.ui.components.ProductThumbnail
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Danger
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MenuManagementScreen(vm: DaunPOSViewModel) {
    val products by vm.products.collectAsState()
    val storeProfile by vm.storeProfile.collectAsState()
    val itemLabel = storeProfile.businessCategory.itemLabel
    val trackStock = storeProfile.businessCategory != com.daunpos.app.data.BusinessCategory.SERVICE
    val defaultCategories = storeProfile.businessCategory.defaultProductCategories
    val categoryOptions = remember(products, defaultCategories) {
        listOf("Semua") + (defaultCategories + products.map { it.category }).distinct()
    }
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
                    Text("Manajemen $itemLabel", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
                    Text(
                        if (trackStock) "${products.size} $itemLabel · ${products.count { it.available && it.stock > 0 }} tersedia"
                        else "${products.size} $itemLabel · ${products.count { it.available }} aktif",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                placeholder = { Text("Cari nama $itemLabel...") },
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
                categoryOptions.forEach { cat ->
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
                        trackStock = trackStock,
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
            containerColor = Color.Transparent,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF3E963C), GreenDeep)),
                    RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah $itemLabel", modifier = Modifier.size(28.dp))
        }
    }

    if (showDialog) {
        ProductDialog(
            initial = editing,
            nextId = vm.nextProductId(),
            existingCategories = products.map { it.category }.distinct().filter { it.isNotBlank() },
            defaultCategories = defaultCategories,
            itemLabel = itemLabel,
            trackStock = trackStock,
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
            title = { Text("Hapus $itemLabel (Otorisasi Pemilik)") },
            text = {
                Column {
                    Text("Menghapus $itemLabel memerlukan otorisasi PIN pemilik.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ownerPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) ownerPin = it },
                        label = { Text("PIN Pemilik (4–6 digit)") },
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
    trackStock: Boolean,
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
                buildString {
                    append(product.category)
                    if (!trackStock) {
                        append(" · ")
                        append(product.unitLabel)
                    }
                    if (product.durationMinutes > 0) {
                        append(" · ")
                        append(formatDurationMinutes(product.durationMinutes))
                    }
                    if (trackStock) append(" · Stok: ${product.stock} ${product.unitLabel}")
                    else append(" · Stok tidak dilacak")
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (trackStock && product.stock <= 5) Danger else InkSoft
            )
            Spacer(Modifier.height(2.dp))
            Text("${formatRupiah(product.price)} / ${product.unitLabel}", style = MaterialTheme.typography.bodyMedium, color = GreenDeep, fontWeight = FontWeight.SemiBold)
            if (product.description.isNotBlank()) {
                Text(product.description, style = MaterialTheme.typography.labelSmall, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = product.available && (!trackStock || product.stock > 0),
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
    defaultCategories: List<String>,
    itemLabel: String,
    trackStock: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val isEdit = initial != null
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var stockText by remember { mutableStateOf(initial?.stock?.toString() ?: "0") }
    var costPriceText by remember { mutableStateOf(initial?.costPrice?.toString() ?: "0") }
    var formError by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf(initial?.category ?: defaultCategories.firstOrNull() ?: "Umum") }
    var barcodeSku by remember { mutableStateOf(initial?.barcodeSku ?: "") }
    val initialDurationMinutes = initial?.durationMinutes ?: 0
    var durationUnit by remember {
        mutableStateOf(
            when {
                initialDurationMinutes > 0 && initialDurationMinutes % 1440 == 0 -> "Hari"
                initialDurationMinutes > 0 && initialDurationMinutes % 60 == 0 -> "Jam"
                else -> "Menit"
            }
        )
    }
    var durationMinutesText by remember {
        mutableStateOf(
            when (durationUnit) {
                "Hari" -> if (initialDurationMinutes > 0) (initialDurationMinutes / 1440).toString() else ""
                "Jam" -> if (initialDurationMinutes > 0) (initialDurationMinutes / 60).toString() else ""
                else -> initialDurationMinutes.takeIf { it > 0 }?.toString() ?: ""
            }
        )
    }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var unitLabel by remember {
        mutableStateOf(
            initial?.unitLabel?.takeIf { it != "unit" } ?: ""
        )
    }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var showUrlInput by remember { mutableStateOf(initial?.imageUrl?.startsWith("http", true) == true) }
    var imageError by remember { mutableStateOf<String?>(null) }
    var catExpanded by remember { mutableStateOf(false) }
    var durationUnitExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val imageScope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            imageScope.launch {
                val copied = runCatching {
                    withContext(Dispatchers.IO) {
                        val mime = context.contentResolver.getType(uri).orEmpty()
                        val extension = when (mime.lowercase()) {
                            "image/png" -> "png"
                            "image/webp" -> "webp"
                            "image/gif" -> "gif"
                            else -> "jpg"
                        }
                        val directory = File(context.filesDir, "product-images").apply { mkdirs() }
                        val destination = File(directory, nextId + "_" + System.currentTimeMillis() + "." + extension)
                        val input = context.contentResolver.openInputStream(uri)
                            ?: error("Foto tidak dapat dibaca.")
                        input.use { source ->
                            destination.outputStream().use { output -> source.copyTo(output) }
                        }
                        Uri.fromFile(destination).toString()
                    }
                }
                copied.onSuccess {
                    imageUrl = it
                    imageError = null
                    showUrlInput = false
                }.onFailure {
                    imageError = "Foto gagal disimpan. Coba pilih gambar lain."
                }
            }
        }
    }

    val durationMultiplier = when (durationUnit) {
        "Jam" -> 60
        "Hari" -> 1440
        else -> 1
    }

    val categories = remember(existingCategories, defaultCategories) {
        (defaultCategories + existingCategories + "Umum").distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Ubah $itemLabel" else "Tambah $itemLabel Baru", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                formError?.let {
                    Text(
                        it,
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // Image selection is deliberately placed at the top of the form so offline users can find it.
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FAF8),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Line)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto $itemLabel",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(76.dp).clip(RoundedCornerShape(12.dp)).background(GreenSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = GreenDeep, modifier = Modifier.size(30.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Button(
                                onClick = { imagePicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenDeep)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (imageUrl.isBlank()) "Ambil dari galeri" else "Ganti foto")
                            }
                            Text(
                                "Foto disimpan di perangkat. Tidak perlu URL atau internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSoft
                            )
                            if (imageUrl.isNotBlank()) {
                                TextButton(onClick = { imageUrl = "" }) { Text("Hapus foto") }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { showUrlInput = !showUrlInput },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(if (showUrlInput) "Sembunyikan URL gambar" else "Opsi lanjutan: URL gambar")
                }
                if (showUrlInput) {
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("URL gambar (opsional)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                imageError?.let { Text(it, color = Danger, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama $itemLabel") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GreenDeep,
                        unfocusedBorderColor = Line,
                        focusedLabelColor = GreenDeep,
                        unfocusedLabelColor = InkSoft,
                        cursorColor = GreenDeep
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                Box(Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori") },
                        trailingIcon = {
                            IconButton(onClick = { catExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih kategori")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { v -> priceText = v.filter { it.isDigit() } },
                    label = { Text("Harga Jual (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GreenDeep,
                        unfocusedBorderColor = Line,
                        focusedLabelColor = GreenDeep,
                        unfocusedLabelColor = InkSoft,
                        cursorColor = GreenDeep
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = costPriceText,
                    onValueChange = { v -> costPriceText = v.filter { it.isDigit() } },
                    label = { Text("Harga Modal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GreenDeep,
                        unfocusedBorderColor = Line,
                        focusedLabelColor = GreenDeep,
                        unfocusedLabelColor = InkSoft,
                        cursorColor = GreenDeep
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Harga modal per unit dipakai untuk menghitung profit. Isi 0 jika tidak ada biaya modal langsung.",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft
                )

                if (trackStock) {
                    OutlinedTextField(
                        value = stockText,
                        onValueChange = { v -> stockText = v.filter { it.isDigit() } },
                        label = { Text("Jumlah stok") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = GreenDeep,
                            unfocusedBorderColor = Line,
                            focusedLabelColor = GreenDeep,
                            unfocusedLabelColor = InkSoft,
                            cursorColor = GreenDeep
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (itemLabel == "Barang") {
                    OutlinedTextField(
                        value = barcodeSku,
                        onValueChange = { barcodeSku = it },
                        label = { Text("Barcode / SKU (opsional)") },
                        placeholder = { Text("Contoh: 899123456001") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = GreenDeep,
                            unfocusedBorderColor = Line,
                            focusedLabelColor = GreenDeep,
                            unfocusedLabelColor = InkSoft,
                            cursorColor = GreenDeep
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = unitLabel,
                    onValueChange = { unitLabel = it.take(24) },
                    label = { Text(if (itemLabel == "Layanan") "Satuan layanan" else "Satuan jual (per item)") },
                    placeholder = {
                        Text(if (itemLabel == "Layanan") "Contoh: sesi, paket, orang, kunjungan" else "Contoh: pcs, kg, botol, box")
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GreenDeep,
                        unfocusedBorderColor = Line,
                        focusedLabelColor = GreenDeep,
                        unfocusedLabelColor = InkSoft,
                        cursorColor = GreenDeep
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (itemLabel == "Layanan") {
                    Text(
                        "Jumlah di kasir dihitung berdasarkan satuan layanan di atas (misalnya sesi atau paket), bukan berdasarkan menit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = durationMinutesText,
                            onValueChange = { v -> durationMinutesText = v.filter { it.isDigit() } },
                            label = { Text("Estimasi durasi per layanan") },
                            placeholder = { Text("Contoh: 30") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = GreenDeep,
                                unfocusedBorderColor = Line
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { durationUnitExpanded = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(durationUnit)
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih satuan durasi")
                            }
                            DropdownMenu(
                                expanded = durationUnitExpanded,
                                onDismissRequest = { durationUnitExpanded = false }
                            ) {
                                listOf("Menit", "Jam", "Hari").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            val oldMultiplier = when (durationUnit) {
                                                "Jam" -> 60
                                                "Hari" -> 1440
                                                else -> 1
                                            }
                                            val newMultiplier = when (option) {
                                                "Jam" -> 60
                                                "Hari" -> 1440
                                                else -> 1
                                            }
                                            val currentMinutes = (durationMinutesText.toIntOrNull() ?: 0) * oldMultiplier
                                            if (currentMinutes == 0 || currentMinutes % newMultiplier == 0) {
                                                durationUnit = option
                                                durationMinutesText = if (currentMinutes == 0) "" else (currentMinutes / newMultiplier).toString()
                                            } else {
                                                // Keep the exact duration instead of silently changing (e.g. 90 minutes to 90 hours).
                                                durationUnit = "Menit"
                                                durationMinutesText = currentMinutes.toString()
                                            }
                                            durationUnitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        "Durasi ini disimpan per layanan dan dihitung otomatis di keranjang.",
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Detail & catatan bawaan layanan (opsional)") },
                        placeholder = { Text("Contoh: termasuk cuci, potong, styling, bahan yang digunakan, syarat, atau informasi untuk pelanggan...") },
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = GreenDeep,
                            unfocusedBorderColor = Line
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Deskripsi / detail produk (opsional)") },
                        placeholder = { Text("Informasi yang ingin ditampilkan pada katalog") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }


            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toIntOrNull()
                    val stock = if (trackStock) stockText.toIntOrNull() else 0
                    val costPrice = costPriceText.toIntOrNull()
                    formError = when {
                        name.isBlank() -> "Nama wajib diisi."
                        category.isBlank() -> "Kategori wajib diisi."
                        price == null || price <= 0 -> "Harga jual harus lebih besar dari nol."
                        trackStock && (stock == null || stock < 0) -> "Stok harus berupa angka nol atau lebih."
                        costPrice == null || costPrice < 0 -> "Isi harga modal; masukkan 0 jika tidak ada biaya modal langsung."
                        else -> null
                    }
                    if (formError == null) {
                        onSave(
                            Product(
                                id = initial?.id ?: nextId,
                                name = name.trim(),
                                category = category.trim(),
                                price = price!!,
                                costPrice = costPrice!!,
                                stock = stock ?: 0,
                                available = if (trackStock) (stock ?: 0) > 0 && (initial?.available ?: true) else (initial?.available ?: true),
                                barcodeSku = barcodeSku.trim(),
                                durationMinutes = (durationMinutesText.toIntOrNull() ?: 0) * durationMultiplier,
                                emoji = initial?.emoji ?: "📦",
                                colorHex = initial?.colorHex ?: 0xFFE7F4EA,
                                imageUrl = imageUrl.trim().ifBlank { null },
                                description = description.trim(),
                                unitLabel = unitLabel.trim().ifBlank {
                                    if (itemLabel == "Layanan") "sesi" else if (itemLabel == "Barang") "pcs" else "porsi"
                                }
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenDeep)
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
