package com.daunpos.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.Exporter
import com.daunpos.app.data.ReportAnalytics
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.isTablet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val filters = listOf("Hari Ini", "Minggu Ini", "Bulan Ini")

@Composable
fun ReportsScreen(
    vm: DaunPOSViewModel,
    onOpenDetail: (String) -> Unit
) {
    val config = LocalConfiguration.current
    val isTablet = config.isTablet

    val transactions by vm.transactions.collectAsState()
    val storeProfile by vm.storeProfile.collectAsState()
    var filter by remember { mutableStateOf("Hari Ini") }
    var exportMenu by remember { mutableStateOf(false) }
    var exportFeedback by remember { mutableStateOf<String?>(null) }
    var exportFeedbackIsError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val nowMillis = System.currentTimeMillis()
    val periodStart = Calendar.getInstance().apply {
        when (filter) {
            "Hari Ini" -> Unit
            "Minggu Ini" -> {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            else -> set(Calendar.DAY_OF_MONTH, 1)
        }
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val filtered = transactions.filter { it.dateTime in periodStart..nowMillis }
    val summary = remember(filtered) { ReportAnalytics.summarize(filtered) }
    val totalRevenue = summary.revenue
    val grossProfitLabel = summary.grossProfit?.let { formatRupiah(it) } ?: "Belum lengkap"
    val txCount = summary.successfulTransactions
    val itemsSold = summary.itemsSold.toString()
    val itemLabel = storeProfile.businessCategory.itemLabel

    val pdfSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destination ->
        if (destination != null) {
            scope.launch {
                runCatching {
                    val file = withContext(Dispatchers.IO) {
                        Exporter.exportPdf(context, filtered, storeProfile.storeName, filter)
                    }
                    try {
                        withContext(Dispatchers.IO) {
                            Exporter.saveToUri(context, file, destination)
                        }
                    } finally {
                        withContext(Dispatchers.IO) { file.delete() }
                    }
                }.onSuccess {
                    exportFeedback = "PDF berhasil disimpan ke lokasi yang dipilih."
                    exportFeedbackIsError = false
                }.onFailure { error ->
                    exportFeedback = error.message ?: "PDF gagal disimpan."
                    exportFeedbackIsError = true
                }
            }
        }
    }

    val csvSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { destination ->
        if (destination != null) {
            scope.launch {
                runCatching {
                    val file = withContext(Dispatchers.IO) {
                        Exporter.exportCsv(context, filtered)
                    }
                    try {
                        withContext(Dispatchers.IO) {
                            Exporter.saveToUri(context, file, destination)
                        }
                    } finally {
                        withContext(Dispatchers.IO) { file.delete() }
                    }
                }.onSuccess {
                    exportFeedback = "CSV berhasil disimpan dan dapat dibuka dengan Excel."
                    exportFeedbackIsError = false
                }.onFailure { error ->
                    exportFeedback = error.message ?: "CSV gagal disimpan."
                    exportFeedbackIsError = true
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Export
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Laporan Penjualan", style = MaterialTheme.typography.headlineSmall, color = Ink, fontWeight = FontWeight.Bold)
                    Text("Ringkasan omzet dan profit usaha", style = MaterialTheme.typography.bodySmall, color = InkSoft)
                }
                Box {
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Filled.Download, contentDescription = "Ekspor", tint = GreenDeep)
                    }
                    DropdownMenu(
                        expanded = exportMenu,
                        onDismissRequest = { exportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Simpan PDF ke perangkat") },
                            onClick = {
                                exportMenu = false
                                exportFeedback = null
                                val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                pdfSaveLauncher.launch("DaunPOS-Laporan-$timestamp.pdf")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bagikan PDF") },
                            onClick = {
                                exportMenu = false
                                scope.launch {
                                    runCatching {
                                        val file = withContext(Dispatchers.IO) {
                                            Exporter.exportPdf(context, filtered, storeProfile.storeName, filter)
                                        }
                                        Exporter.shareFile(context, file, "application/pdf")
                                    }.onFailure { error ->
                                        exportFeedback = error.message ?: "PDF gagal dibuat."
                                        exportFeedbackIsError = true
                                    }
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Simpan CSV ke perangkat") },
                            onClick = {
                                exportMenu = false
                                exportFeedback = null
                                val timestamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
                                csvSaveLauncher.launch("DaunPOS-Laporan-$timestamp.csv")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bagikan CSV") },
                            onClick = {
                                exportMenu = false
                                scope.launch {
                                    runCatching {
                                        val file = withContext(Dispatchers.IO) {
                                            Exporter.exportCsv(context, filtered)
                                        }
                                        Exporter.shareFile(context, file, "text/csv")
                                    }.onFailure { error ->
                                        exportFeedback = error.message ?: "CSV gagal dibuat."
                                        exportFeedbackIsError = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        exportFeedback?.let { message ->
            item {
                Text(
                    message,
                    color = if (exportFeedbackIsError) com.daunpos.app.ui.theme.Danger else GreenDeep,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { f ->
                    val active = f == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) GreenDeep else Color.White)
                            .border(1.dp, if (active) GreenDeep else Line, RoundedCornerShape(50))
                            .clickable { filter = f }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            f,
                            color = if (active) Color.White else Ink,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Core business KPIs: omzet, gross profit, transactions, and units sold.
        item {
            if (isTablet) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Omzet", formatRupiah(totalRevenue), "Rp", Modifier.weight(1f))
                    MetricCard("Profit Kotor", grossProfitLabel, "↗", Modifier.weight(1f))
                    MetricCard("Transaksi", txCount.toString(), "#", Modifier.weight(1f))
                    MetricCard("$itemLabel Terjual", itemsSold, "▦", Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("Omzet", formatRupiah(totalRevenue), "Rp", Modifier.weight(1f))
                        MetricCard("Profit Kotor", grossProfitLabel, "↗", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard("Transaksi", txCount.toString(), "#", Modifier.weight(1f))
                        MetricCard("$itemLabel Terjual", itemsSold, "▦", Modifier.weight(1f))
                    }
                }
            }
        }

        if (summary.transactionsWithoutCost > 0) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFFFF6E7))
                        .border(1.dp, Color(0xFFE9D5A8), RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Profit belum dapat dihitung penuh", style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold)
                        Text(
                            "${summary.transactionsWithoutCost} transaksi lama belum memiliki snapshot harga modal (HPP). Transaksi baru akan menyimpan HPP saat checkout.",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSoft
                        )
                    }
                }
            }
        }

        // Chart & Trend
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text("Tren Penjualan", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    SalesBarChart(filtered)
                }
            }
        }

        // Transaction list / Table
        item {
            Text("Riwayat Transaksi", style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold)
        }

        items(filtered, key = { it.id }) { tx ->
            TransactionRow(tx = tx, onClick = { onOpenDetail(tx.id) })
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada transaksi untuk periode ini.", style = MaterialTheme.typography.bodyMedium, color = InkSoft)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SalesBarChart(transactions: List<Transaction>) {
    val buckets = remember(transactions) { ReportAnalytics.dailySales(transactions) }

    if (buckets.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Belum ada data grafik untuk periode ini.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft
            )
        }
        return
    }

    val maxValue = buckets.maxOf { it.amount }.coerceAtLeast(1L)
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("id", "ID")) }
    val dateLabels = listOfNotNull(
        buckets.firstOrNull(),
        buckets.getOrNull(buckets.size / 2).takeIf { buckets.size > 2 },
        buckets.lastOrNull().takeIf { buckets.size > 1 }
    ).distinctBy { it.dayStartMillis }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(142.dp)) {
            val top = 8.dp.toPx()
            val bottom = size.height - 6.dp.toPx()
            val plotHeight = (bottom - top).coerceAtLeast(1f)

            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                val y = bottom - plotHeight * fraction
                drawLine(
                    color = Line,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val cellWidth = size.width / buckets.size
            val barWidth = minOf(32.dp.toPx(), cellWidth * 0.56f).coerceAtLeast(5.dp.toPx())
            buckets.forEachIndexed { index, bucket ->
                val ratio = (bucket.amount.toDouble() / maxValue.toDouble()).toFloat()
                val barHeight = (plotHeight * ratio).coerceAtLeast(4.dp.toPx())
                val x = index * cellWidth + (cellWidth - barWidth) / 2f
                drawRoundRect(
                    color = GreenDeep,
                    topLeft = Offset(x, bottom - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dateLabels.forEach { bucket ->
                Text(
                    dateFormat.format(Date(bucket.dayStartMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSoft
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val time = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))
    val isSuccess = tx.status == "SUKSES"
    val isCancelled = tx.status == "BATAL"
    val statusLabel = when {
        isSuccess -> "Sukses"
        isCancelled -> "Batal"
        else -> "Menunggu"
    }
    val statusBackground = when {
        isSuccess -> GreenSoft
        isCancelled -> Color(0xFFFBEAE7)
        else -> Color(0xFFFFF4E5)
    }
    val statusForeground = when {
        isSuccess -> GreenDeep
        isCancelled -> com.daunpos.app.ui.theme.Danger
        else -> Color(0xFF9A5B00)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(statusBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                when {
                    isSuccess -> "✓"
                    isCancelled -> "×"
                    else -> "…"
                },
                color = statusForeground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tx.id, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Bg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(tx.tableNumber, fontSize = 10.sp, color = InkSoft, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("$time • ${tx.paymentMethod.label}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRupiah(tx.total), style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(statusBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    statusLabel,
                    color = statusForeground,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
