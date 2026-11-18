package com.daunpos.app.ui.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.ReceiptSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun buildReceiptText(tx: Transaction, settings: ReceiptSettings): String {
    val width = settings.paperWidth
    val sb = StringBuilder()
    val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))
    fun line(s: String = "") {
        sb.append(s).append("\n")
    }
    fun center(s: String) {
        val pad = ((width / 2 - s.length / 2)).coerceAtLeast(0)
        line(" ".repeat(pad) + s)
    }
    fun divider(ch: Char = '-') {
        line(ch.toString().repeat(width / 2))
    }

    if (settings.showLogo) {
        center("[ DAUNPOS ]")
        center("Coffee & Eatery")
    } else {
        center("DAUNPOS")
    }
    settings.headerLines.forEach { center(it) }
    divider('=')
    line("No  : ${tx.id}")
    line("Kasir: DaunPOS")
    line("Waktu: $date")
    divider('-')
    line("ITEM".padEnd(width - 24) + "QTY" + "  " + "HARGA")
    divider('-')
    tx.items.forEach { item ->
        val name = item.name.take(22)
        val qtyStr = "x${item.quantity}"
        line(name)
        line(
            " ".repeat(2) +
                formatRupiah(item.price).padEnd(width - 22 - qtyStr.length) +
                qtyStr
        )
    }
    divider('-')
    line("Subtotal".padEnd(width - 22) + formatRupiah(tx.subtotal))
    if (tx.tax > 0) {
        line("Pajak   ".padEnd(width - 22) + formatRupiah(tx.tax))
    }
    divider('=')
    line("TOTAL".padEnd(width - 22) + formatRupiah(tx.total))
    divider('=')
    line("Bayar  : ${tx.paymentMethod.label}")
    tx.cashReceived?.let { line("Tunai  : ${formatRupiah(it)}") }
    tx.change?.let { line("Kembali: ${formatRupiah(it)}") }
    divider('-')
    settings.footerText.split("\n").forEach { center(it) }
    if (settings.showQR) {
        line("")
        center("[ QR DIGITAL STRUK ]")
        center("  █▀▀▀▀▀▀█ ▀█▀ █▀▀▀▀▀▀█  ")
        center("  █ ███ █  █  █ ███ █  ")
        center("  █ ▀▀▀ █  █  █ ▀▀▀ █  ")
        center("  ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀  ")
    }
    line("")
    return sb.toString()
}

@Composable
fun ReceiptPreview(
    tx: Transaction,
    settings: ReceiptSettings,
    modifier: Modifier = Modifier
) {
    val widthDp = if (settings.paperWidth == 58) 300.dp else 380.dp
    Box(
        modifier = modifier
            .width(widthDp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFE6EBE3), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = buildReceiptText(tx, settings),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            color = Color(0xFF14251E),
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
