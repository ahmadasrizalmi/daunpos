package com.daunpos.app.printer

import com.daunpos.app.data.ReceiptSettings
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
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

    fun buildReceiptBytes(tx: Transaction, settings: ReceiptSettings): ByteArray {
        val chars = if (settings.paperWidth == 80) CHARS_80 else CHARS_58
        val out = ByteArrayOutputStream()

        fun w(bytes: ByteArray) = out.write(bytes)
        fun text(s: String) = w(ascii(s).toByteArray(Charsets.US_ASCII))
        fun line(s: String = "") { text(s); w(byteArrayOf(0x0A)) }
        fun center(s: String) = line(s.padStart((chars + s.length) / 2).padEnd(chars))
        fun right(s: String) = line(s.padStart(chars))

        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(tx.dateTime))

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
        line("No  : ${tx.id}")
        line("Waktu: $date")
        center("-".repeat(chars))
        tx.items.forEach { item ->
            line(ascii(item.name).take(chars - 6) + " x" + item.quantity)
            right(formatRupiah(item.total))
        }
        center("-".repeat(chars))
        line("Subtotal".padEnd(chars - 14) + formatRupiah(tx.subtotal).padStart(14))
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
        line("")
        line("Jika struk ini tercetak, berarti")
        line("printer sudah siap digunakan.")
        w(feed(4))
        w(cut())
        return out.toByteArray()
    }

    private fun ascii(s: String): String =
        s.map { if (it.code < 128) it else '?' }.joinToString("")
}
