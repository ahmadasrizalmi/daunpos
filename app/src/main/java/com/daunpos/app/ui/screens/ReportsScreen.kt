package com.daunpos.app.ui.screens

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
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.DaunPOSViewModel
import com.daunpos.app.data.Exporter
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.ui.components.StatusBadge
import com.daunpos.app.ui.theme.Bg
import com.daunpos.app.ui.theme.Green
import com.daunpos.app.ui.theme.GreenDeep
import com.daunpos.app.ui.theme.GreenSoft
import com.daunpos.app.ui.theme.Ink
import com.daunpos.app.ui.theme.InkSoft
import com.daunpos.app.ui.theme.Line
import com.daunpos.app.ui.theme.Mint
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
    val transactions by vm.transactions.collectAsState()
    var filter by remember { mutableStateOf("Hari Ini") }
    var exportMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val now = Calendar.getInstance()
    val filtered = transactions.filter { tx ->
        val c = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
        when (filter) {
            "Hari Ini" -> c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            "Minggu Ini" -> now.timeInMillis - tx.dateTime <= 7L * 24 * 60 * 60 * 1000
            else -> now.timeInMillis - tx.dateTime <= 30L * 24 * 60 * 60 * 1000
        }
    }

    val totalRevenue = filtered.filter { it.status == "SUKSES" }.sumOf { it.total }
    val txCount = filtered.size
    val itemsSold = filtered.flatMap { it.items }.sumOf { it.quantity }
    val bestSeller = filtered.flatMap { it.items }
        .groupBy { it.name }
        .maxByOrNull { it.value.sumOf { i -> i.quantity } }
        ?.key ?: "-"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Laporan Penjualan", style = MaterialTheme.typography.headlineSmall, color = Ink)
                    Text("Pantau performa kafe kamu", style = MaterialTheme.typography.bodySmall, color = InkSoft)
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
                            text = { Text("Unduh PDF") },
                            onClick = {
                                exportMenu = false
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) { Exporter.exportPdf(context, filtered) }
                                    Exporter.shareFile(context, file, "application/pdf")
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Unduh CSV (Excel)") },
                            onClick = {
                                exportMenu = false
                                scope.launch {
                                    val file = withContext(Dispatchers.IO) { Exporter.exportCsv(context, filtered) }
                                    Exporter.shareFile(context, file, "text/csv")
                                }
                            }
                        )
                    }
                }
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
                        Text(f, color = if (active) Color.White else Ink, style = MaterialTheme.typography.labelLarge, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }

        // Summary cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Total Pendapatan", formatRupiah(totalRevenue), "💰", Modifier.weight(1f))
                MetricCard("Transaksi", txCount.toString(), "🧾", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Item Terjual", itemsSold.toString(), "📦", Modifier.weight(1f))
                MetricCard("Menu Terlaris", bestSeller, "⭐", Modifier.weight(1f))
            }
        }

        // Chart
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Tren Penjualan", style = MaterialTheme.typography.titleMedium, color = Ink)
                    Spacer(Modifier.height(14.dp))
                    SalesBarChart(filtered)
                }
            }
        }

        // Transaction list
        item {
            Text("Riwayat Transaksi", style = MaterialTheme.typography.titleMedium, color = Ink)
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
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SalesBarChart(transactions: List<Transaction>) {
    val grouped = transactions
        .filter { it.status == "SUKSES" }
        .groupBy { tx ->
            val c = Calendar.getInstance().apply { timeInMillis = tx.dateTime }
            c.get(Calendar.DAY_OF_MONTH)
        }
        .mapValues { it.value.sumOf { t -> t.total } }
        .toSortedMap()

    val maxValue = (grouped.values.maxOrNull() ?: 0).coerceAtLeast(1)

    if (grouped.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Belum ada data grafik", style = MaterialTheme.typography.bodySmall, color = InkSoft)
        }
        return
    }

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val gap = 14.dp.toPx()
        val barWidth = (size.width - gap * (grouped.size - 1)) / grouped.size
        grouped.entries.forEachIndexed { index, entry ->
            val barHeight = (entry.value.toFloat() / maxValue) * (size.height - 40.dp.toPx())
            val x = index * (barWidth + gap)
            val y = size.height - barHeight
            drawRoundRect(
                color = if (index % 2 == 0) Green else Mint,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val time = SimpleDateFormat("dd MMM, HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))
    val success = tx.status == "SUKSES"

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
                .background(if (success) GreenSoft else Color(0xFFFBEAE7)),
            contentAlignment = Alignment.Center
        ) {
            Text(if (success) "✅" else "❌", fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.id, style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("$time • ${tx.paymentMethod.label}", style = MaterialTheme.typography.labelSmall, color = InkSoft)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRupiah(tx.total), style = MaterialTheme.typography.titleSmall, color = Ink, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            StatusBadge(if (success) "Sukses" else "Batal", positive = success)
        }
    }
}
