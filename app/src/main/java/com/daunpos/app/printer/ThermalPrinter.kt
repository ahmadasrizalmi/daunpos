package com.daunpos.app.printer

import android.graphics.Bitmap
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.ReceiptSettings
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import com.daunpos.app.data.qris.QrisConverter
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generator perintah ESC/POS untuk printer thermal 58mm / 80mm.
 * Menggunakan charset US-ASCII agar aman untuk mayoritas printer SPP.
 */
object ThermalPrinter {

    const val CHARS_58 = 32
    const val CHARS_80 = 48

    fun init() = byteArrayOf(0x1B, 0x40)
    fun alignLeft() = byteArrayOf(0x1B, 0x61, 0x00)
    fun alignCenter() = byteArrayOf(0x1B, 0x61, 0x01)
    fun bold(on: Boolean) = byteArrayOf(0x1B, 0x45, (if (on) 1 else 0).toByte())
    fun doubleHeightWidth(on: Boolean) = byteArrayOf(0x1D, 0x21, (if (on) 0x11 else 0x00).toByte())
    fun feed(lines: Int) = ByteArray(lines) { 0x0A }
    fun cut() = byteArrayOf(0x1D, 0x56, 0x42, 0x00)
    // Standar ESC/POS cash drawer kick pulse: ESC p m t1 t2 (pin 2 & pin 5)
    fun openCashDrawer() = byteArrayOf(0x1B, 0x70, 0x00, 0x19, 0xFA.toByte())

    fun buildReceiptBytes(tx: Transaction, settings: ReceiptSettings): ByteArray {
        val chars = if (settings.paperWidth == 80) CHARS_80 else CHARS_58
        val out = ByteArrayOutputStream()

        fun w(bytes: ByteArray) = out.write(bytes)
        fun text(s: String) = w(ascii(s).toByteArray(Charsets.US_ASCII))
        fun line(s: String = "") { text(s); w(byteArrayOf(0x0A)) }
        fun center(s: String) = line(s.padStart((chars + s.length) / 2).padEnd(chars))
        fun right(s: String) = line(s.padStart(chars))

        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))

        // Trigger laci kasir jika diaktifkan dan metode pembayaran Tunai
        if (settings.cashDrawer && tx.paymentMethod == PaymentMethod.CASH) {
            w(openCashDrawer())
        }

        w(init())
        w(alignCenter())
        w(doubleHeightWidth(true))
        w(bold(true))
        center("DAUNPOS")
        w(doubleHeightWidth(false))
        w(bold(false))
        settings.headerLines.forEach { center(it) }
        center("-".repeat(chars))
        w(alignLeft())
        line("No    : ${tx.id}")
        line("Meja  : ${tx.tableNumber}")
        line("Waktu : $date")
        center("-".repeat(chars))
        tx.items.forEach { item ->
            line(ascii(item.name).take(chars - 6) + " x" + item.quantity)
            right(formatRupiah(item.total))
        }
        center("-".repeat(chars))
        line("Subtotal".padEnd(chars - 14) + formatRupiah(tx.subtotal).padStart(14))
        if (tx.discount > 0) {
            line("Diskon".padEnd(chars - 14) + ("-" + formatRupiah(tx.discount)).padStart(14))
        }
        if (tx.tax > 0) {
            line("Pajak".padEnd(chars - 14) + formatRupiah(tx.tax).padStart(14))
        }
        w(bold(true))
        line("TOTAL".padEnd(chars - 14) + formatRupiah(tx.total).padStart(14))
        w(bold(false))
        center("=".repeat(chars))
        line("Bayar  : ${tx.paymentMethod.label}")
        tx.cashReceived?.let { line("Tunai  : ${formatRupiah(it)}") }
        tx.change?.let { line("Kembali: ${formatRupiah(it)}") }
        center("-".repeat(chars))
        settings.footerText.split("\n").forEach { center(it) }
        w(feed(4))
        w(cut())
        return out.toByteArray()
    }

    fun buildQrisBillBytes(
        invoiceId: String,
        tableNumber: String,
        amount: Int,
        dynamicQrisPayload: String,
        settings: ReceiptSettings
    ): ByteArray {
        val chars = if (settings.paperWidth == 80) CHARS_80 else CHARS_58
        val out = ByteArrayOutputStream()
        fun w(bytes: ByteArray) = out.write(bytes)
        fun text(s: String) = w(ascii(s).toByteArray(Charsets.US_ASCII))
        fun line(s: String = "") { text(s); w(byteArrayOf(0x0A)) }
        fun center(s: String) = line(s.padStart((chars + s.length) / 2).padEnd(chars))

        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())

        w(init())
        w(alignCenter())
        w(bold(true))
        center("TAGIHAN PEMBAYARAN")
        w(doubleHeightWidth(true))
        center("QRIS DINAMIS")
        w(doubleHeightWidth(false))
        w(bold(false))
        center("-".repeat(chars))
        w(alignLeft())
        line("No. Inv: $invoiceId")
        line("Meja   : $tableNumber")
        line("Waktu  : $date")
        center("-".repeat(chars))
        w(alignCenter())
        w(bold(true))
        line("TOTAL BAYAR:")
        w(doubleHeightWidth(true))
        line(formatRupiah(amount))
        w(doubleHeightWidth(false))
        w(bold(false))
        center("-".repeat(chars))

        // Cetak QR Bitmap (ukuran 240px lebar untuk 58mm, 320px untuk 80mm)
        runCatching {
            val qrSize = if (settings.paperWidth == 80) 320 else 240
            val bitmap = QrisConverter.generateQrBitmap(dynamicQrisPayload, qrSize)
            w(alignCenter())
            w(rasterBitImage(bitmap))
            w(feed(1))
        }

        center("Scan via BCA, Mandiri, GoPay,")
        center("OVO, DANA, ShopeePay, dsb.")
        center("Nominal otomatis terkunci.")
        center("-".repeat(chars))
        w(feed(4))
        w(cut())
        return out.toByteArray()
    }

    /**
     * Mengonversi Android Bitmap menjadi perintah ESC/POS Raster Bit Image (GS v 0).
     * Didukung oleh 100% printer thermal generic (RPP02N, ZJ-58, Panda, dll).
     */
    fun rasterBitImage(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val widthBytes = (width + 7) / 8

        val out = ByteArrayOutputStream()
        // Header ESC/POS GS v 0 m xL xH yL yH
        out.write(byteArrayOf(
            0x1D, 0x76, 0x30, 0x00,
            (widthBytes % 256).toByte(),
            (widthBytes / 256).toByte(),
            (height % 256).toByte(),
            (height / 256).toByte()
        ))

        for (y in 0 until height) {
            for (x in 0 until widthBytes) {
                var byteVal = 0
                for (bit in 0 until 8) {
                    val px = x * 8 + bit
                    if (px < width) {
                        val pixel = bitmap.getPixel(px, y)
                        // Ambil luminance: jika gelap, set bit = 1 (hitam pada thermal)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        val luminance = (r * 299 + g * 587 + b * 114) / 1000
                        if (luminance < 160) {
                            byteVal = byteVal or (1 shl (7 - bit))
                        }
                    }
                }
                out.write(byteVal)
            }
        }
        return out.toByteArray()
    }

    fun buildTestPrintBytes(settings: ReceiptSettings): ByteArray {
        val chars = if (settings.paperWidth == 80) CHARS_80 else CHARS_58
        val out = ByteArrayOutputStream()
        fun w(bytes: ByteArray) = out.write(bytes)
        fun text(s: String) = w(ascii(s).toByteArray(Charsets.US_ASCII))
        fun line(s: String = "") { text(s); w(byteArrayOf(0x0A)) }
        fun center(s: String) = line(s.padStart((chars + s.length) / 2).padEnd(chars))

        w(init())
        w(alignCenter())
        w(bold(true))
        center("DAUNPOS")
        w(bold(false))
        center("TEST PRINT")
        center("-".repeat(chars))
        w(alignLeft())
        line("Koneksi printer berhasil.")
        line("Ukuran kertas: ${settings.paperWidth}mm")
        line("Laci kasir: ${if (settings.cashDrawer) "Aktif" else "Nonaktif"}")
        line("")
        line("Jika struk ini tercetak, berarti")
        line("printer siap digunakan di POS.")
        w(feed(4))
        w(cut())
        return out.toByteArray()
    }

    private fun ascii(s: String): String =
        s.map { ch -> if (ch.code in 32..126) ch else '?' }.joinToString("")
}
