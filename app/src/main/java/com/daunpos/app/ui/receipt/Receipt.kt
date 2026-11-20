package com.daunpos.app.ui.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.data.ReceiptSettings
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.formatDurationMinutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val ReceiptFontFamily = FontFamily.Monospace

object ReceiptBuilder {
    fun buildText(tx: Transaction, settings: ReceiptSettings): String {
        val columns = if (settings.paperWidth == 80) 48 else 32
        val output = StringBuilder()
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))

        fun line(value: String = "") {
            output.append(value).append('\n')
        }

        fun wrap(value: String, maxWidth: Int = columns): List<String> {
            val text = value.trim()
            if (text.isEmpty()) return listOf("")
            val result = mutableListOf<String>()
            var remaining = text
            while (remaining.length > maxWidth) {
                var split = remaining.lastIndexOf(' ', maxWidth)
                if (split <= 0) split = maxWidth
                result += remaining.substring(0, split).trimEnd()
                remaining = remaining.substring(split).trimStart()
            }
            if (remaining.isNotEmpty()) result += remaining
            return result
        }

        fun wrappedLine(value: String) {
            wrap(value).forEach(::line)
        }

        fun center(value: String) {
            wrap(value).forEach { part ->
                val left = ((columns - part.length) / 2).coerceAtLeast(0)
                line(" ".repeat(left) + part)
            }
        }

        fun divider(character: Char = '-') {
            line(character.toString().repeat(columns))
        }

        fun row(label: String, value: String) {
            val right = value.trim()
            val leftWidth = (columns - right.length - 1).coerceAtLeast(8)
            val leftLines = wrap(label, leftWidth)
            if (right.length >= columns - 5) {
                leftLines.forEach(::line)
                line(right)
            } else {
                leftLines.dropLast(1).forEach(::line)
                val last = leftLines.lastOrNull().orEmpty()
                val gap = (columns - last.length - right.length).coerceAtLeast(1)
                line(last + " ".repeat(gap) + right)
            }
        }

        if (settings.showLogo) center("DAUNPOS")
        settings.headerLines.filter { it.isNotBlank() }.forEach(::center)
        divider('=')
        row("No. Transaksi", tx.id)
        row("Tanggal", date)
        wrappedLine("Tipe  : ${tx.tableNumber.ifBlank { "Take Away" }}")
        row("Metode", tx.paymentMethod.label)
        if (tx.status != "SUKSES") row("Status", tx.status)
        divider()
        center("RINCIAN PESANAN")
        divider()

        tx.items.forEach { item ->
            wrappedLine(item.name)
            row("${item.quantity} ${item.unitLabel} x ${formatRupiah(item.price)}", formatRupiah(item.total))
            if (item.description.isNotBlank()) wrappedLine("Detail: ${item.description}")
            if (item.durationMinutes > 0) wrappedLine("Durasi: ${formatDurationMinutes(item.durationMinutes)} per item")
            if (item.note.isNotBlank()) wrappedLine("Catatan: ${item.note}")
            // Preserve historical staff attribution on old receipts; staff assignment is no longer offered in the UI.
            if (item.staffName.isNotBlank()) wrappedLine("Staf  : ${item.staffName}")
        }

        divider()
        row("Subtotal", formatRupiah(tx.subtotal))
        if (tx.discount > 0) row("Diskon", "-${formatRupiah(tx.discount)}")
        if (tx.tax > 0) row("Pajak", formatRupiah(tx.tax))
        divider('=')
        row("TOTAL", formatRupiah(tx.total))
        divider('=')
        tx.cashReceived?.let { row("Tunai", formatRupiah(it)) }
        tx.change?.let { row("Kembalian", formatRupiah(it)) }
        divider('-')
        settings.footerText.split('\n').filter { it.isNotBlank() }.forEach(::center)
        line()
        return output.toString()
    }
}

fun buildReceiptText(tx: Transaction, settings: ReceiptSettings): String =
    ReceiptBuilder.buildText(tx, settings)

@Composable
fun ReceiptPreview(
    tx: Transaction,
    settings: ReceiptSettings,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 560.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        val targetWidth = if (settings.paperWidth == 58) 220.dp else 300.dp
        val availableWidth = (maxWidth - 8.dp).coerceAtLeast(1.dp)
        val previewWidth = minOf(targetWidth, availableWidth)
        Box(
            modifier = Modifier
                .width(previewWidth)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE1E7E1), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            Text(
                text = buildReceiptText(tx, settings),
                fontFamily = ReceiptFontFamily,
                fontSize = if (settings.paperWidth == 58) 8.5.sp else 9.sp,
                lineHeight = if (settings.paperWidth == 58) 10.5.sp else 11.sp,
                color = Color(0xFF14251E),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
