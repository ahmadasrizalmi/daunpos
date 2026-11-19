package com.daunpos.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    fun exportCsv(context: Context, transactions: List<Transaction>): File {
        val sb = StringBuilder()
        sb.append("ID,Waktu,Meja,Metode,Subtotal,Diskon,Pajak,Total,Status,Item\n")
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID"))
        transactions.forEach { tx ->
            val itemDesc = tx.items.joinToString(" | ") { "${it.quantity}x ${it.name}" }
                .replace(",", ";")
                .replace("\"", "'")
            sb.append("${tx.id},${df.format(Date(tx.dateTime))},${tx.tableNumber},${tx.paymentMethod.label},${tx.subtotal},${tx.discount},${tx.tax},${tx.total},${tx.status},\"$itemDesc\"\n")
        }
        val file = File(context.cacheDir, "DaunPOS-${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun exportPdf(context: Context, transactions: List<Transaction>): File {
        val file = File(context.cacheDir, "DaunPOS-${System.currentTimeMillis()}.pdf")
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40
        val document = PdfDocument()

        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
        var canvas = page.canvas
        var y = margin

        val titlePaint = Paint().apply {
            color = Color.rgb(31, 92, 67)
            textSize = 20f
            isFakeBoldText = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(20, 37, 30)
            textSize = 12f
            isFakeBoldText = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(20, 37, 30)
            textSize = 10f
        }
        val softPaint = Paint().apply {
            color = Color.rgb(110, 128, 120)
            textSize = 9f
        }
        val linePaint = Paint().apply {
            color = Color.rgb(230, 235, 227)
            strokeWidth = 1f
        }

        fun newPage() {
            document.finishPage(page)
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            canvas = page.canvas
            y = margin
        }

        fun drawRow(label: String, value: String, valuePaint: Paint = bodyPaint) {
            if (y > pageHeight - margin) newPage()
            canvas.drawText(label, margin.toFloat(), y.toFloat(), bodyPaint)
            canvas.drawText(value, (pageWidth - margin - 180).toFloat(), y.toFloat(), valuePaint)
            y += 18
        }

        canvas.drawText("Laporan Penjualan DaunPOS", margin.toFloat(), y.toFloat(), titlePaint)
        y += 26
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("id", "ID"))
        canvas.drawText("Dibuat: ${df.format(Date())}", margin.toFloat(), y.toFloat(), softPaint)
        y += 24

        transactions.forEach { tx ->
            if (y > pageHeight - margin - 80) newPage()
            canvas.drawRect(margin.toFloat(), y - 12f, (pageWidth - margin).toFloat(), y + 60f, linePaint)
            canvas.drawText(tx.id, margin.toFloat(), y + 4f, headerPaint)
            canvas.drawText(tx.status, (pageWidth - margin - 60).toFloat(), y + 4f, headerPaint)
            y += 18
            canvas.drawText("${df.format(Date(tx.dateTime))}  •  ${tx.paymentMethod.label}  •  ${tx.tableNumber}", margin.toFloat(), y + 4f, softPaint)
            y += 16
            tx.items.forEach { item ->
                canvas.drawText("  ${item.quantity}x ${item.name}", margin.toFloat(), y + 4f, bodyPaint)
                canvas.drawText(formatRupiah(item.total), (pageWidth - margin - 120).toFloat(), y + 4f, bodyPaint)
                y += 14
            }
            canvas.drawText("Total", margin.toFloat(), y + 6f, headerPaint)
            canvas.drawText(formatRupiah(tx.total), (pageWidth - margin - 120).toFloat(), y + 6f, headerPaint)
            y += 26
        }

        document.finishPage(page)
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
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
        context.startActivity(Intent.createChooser(send, "Bagikan laporan"))
    }
}
