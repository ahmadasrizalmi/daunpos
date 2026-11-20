package com.daunpos.app.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    private fun csv(value: String): String =
        "\"" + value.replace("\"", "\"\"").replace("\r", " ").replace("\n", " ") + "\""

    fun exportCsv(context: Context, transactions: List<Transaction>): File {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID"))
        val rows = StringBuilder("\uFEFF")
        rows.append(
            listOf("ID", "Waktu", "Tujuan", "Metode", "Subtotal", "Diskon", "Pajak", "Total", "HPP", "Profit Kotor", "Status", "Item")
                .joinToString(",") { csv(it) }
        ).append('\n')

        transactions.sortedBy { it.dateTime }.forEach { tx ->
            val itemDescription = tx.items.joinToString(" | ") {
                "${it.quantity}x ${it.name} @ ${it.price}"
            }
            val costOfGoods = tx.items.takeIf { items -> items.isNotEmpty() && items.all { it.costPrice != null } }
                ?.sumOf { it.costPrice!!.toLong() * it.quantity.toLong() }
            val grossProfit = costOfGoods?.let { (tx.subtotal - tx.discount).toLong() - it }
            val fields = listOf(
                tx.id,
                df.format(Date(tx.dateTime)),
                tx.tableNumber,
                tx.paymentMethod.label,
                tx.subtotal.toString(),
                tx.discount.toString(),
                tx.tax.toString(),
                tx.total.toString(),
                costOfGoods?.toString() ?: "",
                grossProfit?.toString() ?: "",
                tx.status,
                itemDescription
            )
            rows.append(fields.joinToString(",") { csv(it) }).append('\n')
        }

        val file = File(context.cacheDir, "DaunPOS-${System.currentTimeMillis()}.csv")
        file.writeText(rows.toString(), Charsets.UTF_8)
        return file
    }

    fun exportPdf(
        context: Context,
        transactions: List<Transaction>,
        storeName: String = "DaunPOS",
        periodLabel: String = "Semua Periode"
    ): File {
        val file = File(context.cacheDir, "DaunPOS-${System.currentTimeMillis()}.pdf")
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36
        val contentRight = pageWidth - margin
        val contentWidth = contentRight - margin
        val footerY = pageHeight - 30
        val document = PdfDocument()
        val locale = Locale("id", "ID")
        val dateFormat = SimpleDateFormat("dd MMM yyyy", locale)
        val dateTimeFormat = SimpleDateFormat("dd/MM/yy HH:mm", locale)
        val generatedAt = SimpleDateFormat("dd MMM yyyy HH:mm", locale).format(Date())
        val rangeStart = transactions.minOfOrNull { it.dateTime }?.let { dateFormat.format(Date(it)) } ?: "-"
        val rangeEnd = transactions.maxOfOrNull { it.dateTime }?.let { dateFormat.format(Date(it)) } ?: "-"
        val summary = ReportAnalytics.summarize(transactions)
        val moneyFormat = NumberFormat.getIntegerInstance(locale)

        fun rupiah(value: Long): String = "Rp ${moneyFormat.format(value)}"

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 92, 67)
            textSize = 18f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 37, 30)
            textSize = 14f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 37, 30)
            textSize = 10f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 48, 42)
            textSize = 8.5f
        }
        val smallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(104, 119, 111)
            textSize = 8f
        }
        val boldPaint = Paint(bodyPaint).apply {
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val totalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 92, 67)
            textSize = 11f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val whiteBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 228, 221)
            strokeWidth = 1f
        }
        val accentFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(31, 92, 67)
            style = Paint.Style.FILL
        }
        val paleFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(240, 247, 242)
            style = Paint.Style.FILL
        }
        val whiteFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val altFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(248, 250, 248)
            style = Paint.Style.FILL
        }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas: Canvas = page.canvas
        var y = 0

        fun drawRight(text: String, right: Int, baseline: Int, paint: Paint) {
            canvas.drawText(text, right - paint.measureText(text), baseline.toFloat(), paint)
        }

        fun drawFit(text: String, x: Int, baseline: Int, maxWidth: Int, paint: Paint) {
            var output = text
            while (output.length > 2 && paint.measureText(output) > maxWidth) {
                output = output.dropLast(2) + "…"
            }
            canvas.drawText(output, x.toFloat(), baseline.toFloat(), paint)
        }

        fun wrapLines(text: String, paint: Paint, maxWidth: Float): List<String> {
            var remaining = text.trim()
            if (remaining.isEmpty()) return listOf("-")
            val lines = mutableListOf<String>()
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                val split = if (count < remaining.length) {
                    remaining.lastIndexOf(' ', count).takeIf { it > 0 } ?: count
                } else {
                    count
                }
                lines += remaining.substring(0, split).trimEnd()
                remaining = remaining.substring(split).trimStart()
            }
            return lines
        }

        fun drawPageHeader() {
            canvas.drawColor(Color.WHITE)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 7f, accentFill)
            drawFit(storeName.ifBlank { "DaunPOS" }, margin, 42, 330, brandPaint)
            canvas.drawText("LAPORAN PENJUALAN", margin.toFloat(), 65f, titlePaint)
            canvas.drawText(
                "Periode: $periodLabel  ·  $rangeStart - $rangeEnd",
                margin.toFloat(),
                84f,
                smallPaint
            )
            drawRight("Dibuat $generatedAt", contentRight, 84, smallPaint)
            canvas.drawLine(margin.toFloat(), 99f, contentRight.toFloat(), 99f, rulePaint)
            y = 119
        }

        fun finishCurrentPage() {
            canvas.drawLine(
                margin.toFloat(),
                (footerY - 14).toFloat(),
                contentRight.toFloat(),
                (footerY - 14).toFloat(),
                rulePaint
            )
            val footerPaint = Paint(smallPaint).apply { textAlign = Paint.Align.RIGHT }
            canvas.drawText("DaunPOS  ·  Halaman $pageNumber", contentRight.toFloat(), footerY.toFloat(), footerPaint)
            document.finishPage(page)
        }

        fun startNextPage() {
            finishCurrentPage()
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            drawPageHeader()
        }

        fun drawSectionTitle(text: String) {
            canvas.drawText(text, margin.toFloat(), y.toFloat(), sectionPaint)
            y += 18
        }

        fun drawKpiCard(x: Int, width: Int, label: String, value: String) {
            canvas.drawRoundRect(
                x.toFloat(),
                y.toFloat(),
                (x + width).toFloat(),
                (y + 58).toFloat(),
                7f,
                7f,
                paleFill
            )
            drawFit(label, x + 10, y + 20, width - 20, smallPaint)
            drawFit(value, x + 10, y + 43, width - 20, totalPaint)
        }

        fun drawPaymentRow(label: String, count: Int, amount: Long) {
            canvas.drawText(label, margin.toFloat(), y.toFloat(), bodyPaint)
            canvas.drawText("$count transaksi", (margin + 210).toFloat(), y.toFloat(), smallPaint)
            drawRight(rupiah(amount), contentRight, y, boldPaint)
            y += 17
        }

        fun drawTransactionTableHeader() {
            canvas.drawRoundRect(
                margin.toFloat(),
                y.toFloat(),
                contentRight.toFloat(),
                (y + 24).toFloat(),
                4f,
                4f,
                accentFill
            )
            val x = intArrayOf(margin + 8, margin + 86, margin + 164, margin + 230, margin + 400, margin + 478)
            val labels = listOf("Invoice", "Waktu", "Bayar", "Rincian item", "Total", "Status")
            labels.forEachIndexed { index, label ->
                drawFit(label, x[index], y + 16, if (index == 5) 43 else 72, whiteBoldPaint)
            }
            y += 30
        }

        drawPageHeader()

        val cardGap = 7
        val cardWidth = (contentWidth - cardGap * 3) / 4
        drawKpiCard(margin, cardWidth, "OMZET", rupiah(summary.revenue))
        drawKpiCard(margin + cardWidth + cardGap, cardWidth, "PROFIT KOTOR", summary.grossProfit?.let { rupiah(it) } ?: "Belum lengkap")
        drawKpiCard(margin + (cardWidth + cardGap) * 2, cardWidth, "TRANSAKSI", summary.successfulTransactions.toString())
        drawKpiCard(margin + (cardWidth + cardGap) * 3, cardWidth, "ITEM TERJUAL", summary.itemsSold.toString())
        y += 74
        if (summary.transactionsWithoutCost > 0) {
            drawFit("Profit belum lengkap: ${summary.transactionsWithoutCost} transaksi tidak memiliki data HPP.", margin, y, contentWidth, smallPaint)
            y += 16
        }

        drawSectionTitle("RINGKASAN PEMBAYARAN")
        PaymentMethod.entries.forEach { method ->
            val totals = summary.paymentTotals.getValue(method)
            drawPaymentRow(method.label, totals.count, totals.amount)
        }
        drawPaymentRow("Transaksi dibatalkan", summary.cancelledTransactions, 0L)
        drawPaymentRow("Menunggu verifikasi", summary.pendingTransactions, 0L)
        y += 8
        canvas.drawLine(margin.toFloat(), y.toFloat(), contentRight.toFloat(), y.toFloat(), rulePaint)
        y += 22

        drawSectionTitle("DETAIL TRANSAKSI")
        if (transactions.isEmpty()) {
            canvas.drawRoundRect(
                margin.toFloat(),
                y.toFloat(),
                contentRight.toFloat(),
                (y + 48).toFloat(),
                6f,
                6f,
                paleFill
            )
            canvas.drawText("Tidak ada transaksi pada periode laporan.", (margin + 12).toFloat(), (y + 29).toFloat(), bodyPaint)
            y += 58
        } else {
            drawTransactionTableHeader()
            val ordered = transactions.sortedBy { it.dateTime }
            ordered.forEachIndexed { index, tx ->
                val itemText = tx.items.joinToString(", ") { "${it.quantity}x ${it.name}" }
                val itemLines = wrapLines(itemText, bodyPaint, 158f)
                val rowHeight = maxOf(30, itemLines.size * 11 + 12)

                if (y + rowHeight > footerY - 20) {
                    startNextPage()
                    drawSectionTitle("DETAIL TRANSAKSI  ·  LANJUTAN")
                    drawTransactionTableHeader()
                }

                if (index % 2 == 0) {
                    canvas.drawRect(
                        margin.toFloat(),
                        y.toFloat(),
                        contentRight.toFloat(),
                        (y + rowHeight).toFloat(),
                        altFill
                    )
                }
                val x = intArrayOf(margin + 8, margin + 86, margin + 164, margin + 230, margin + 400, margin + 478)
                val baseline = y + 17
                drawFit(tx.id, x[0], baseline, 72, boldPaint)
                drawFit(dateTimeFormat.format(Date(tx.dateTime)), x[1], baseline, 72, smallPaint)
                drawFit(tx.paymentMethod.label, x[2], baseline, 60, smallPaint)
                itemLines.forEachIndexed { lineIndex, line ->
                    canvas.drawText(line, x[3].toFloat(), (baseline + lineIndex * 11).toFloat(), bodyPaint)
                }
                drawRight(formatRupiah(tx.total), x[5] - 7, baseline, boldPaint)
                val statusLabel = when (tx.status) {
                    "SUKSES" -> "SUKSES"
                    "BATAL" -> "BATAL"
                    else -> "PENDING"
                }
                drawFit(statusLabel, x[5], baseline, 43, smallPaint)
                y += rowHeight
                canvas.drawLine(margin.toFloat(), y.toFloat(), contentRight.toFloat(), y.toFloat(), rulePaint)
            }
        }

        finishCurrentPage()
        try {
            FileOutputStream(file).use { output -> document.writeTo(output) }
        } finally {
            document.close()
        }
        return file
    }

    fun saveToUri(context: Context, file: File, destination: Uri) {
        val output = context.contentResolver.openOutputStream(destination, "wt")
            ?: error("Lokasi penyimpanan tidak dapat dibuka.")
        output.use { destinationStream ->
            file.inputStream().use { sourceStream ->
                ExportFileWriter.copy(sourceStream, destinationStream)
            }
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Bagikan laporan")
        if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
