package com.daunpos.app

import com.daunpos.app.data.CurrencyUtils
import com.daunpos.app.data.OrderItem
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.PinHasher
import com.daunpos.app.data.ReceiptSettings
import com.daunpos.app.data.Transaction
import com.daunpos.app.printer.ThermalPrinter
import com.daunpos.app.ui.receipt.ReceiptBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class POSLogicTest {

    @Test
    fun testPinHasherVerification() {
        val pin = "1234"
        val salt = PinHasher.generateSalt()
        val hash = PinHasher.hash(pin, salt)

        assertTrue(PinHasher.verify("1234", salt, hash))
        assertFalse(PinHasher.verify("0000", salt, hash))
        assertFalse(PinHasher.verify("1235", salt, hash))
    }

    @Test
    fun testRupiahFormatting() {
        val formatted = CurrencyUtils.formatRupiah(25000)
        assertTrue(formatted.contains("25.000"))
        val formattedLarge = CurrencyUtils.formatRupiah(100000)
        assertTrue(formattedLarge.contains("100.000"))
    }

    @Test
    fun testReceiptTextContainsTableAndItems() {
        val tx = Transaction(
            id = "#INV-9999",
            dateTime = System.currentTimeMillis(),
            items = listOf(
                OrderItem("P001", "Es Kopi Daun", 22000, 2, "☕")
            ),
            subtotal = 44000,
            discount = 4000,
            tax = 4000,
            total = 44000,
            paymentMethod = PaymentMethod.CASH,
            cashReceived = 50000,
            change = 6000,
            tableNumber = "Meja 04"
        )
        val text = ReceiptBuilder.buildText(tx, ReceiptSettings())

        assertTrue(text.contains("#INV-9999"))
        assertTrue(text.contains("Meja 04"))
        assertTrue(text.contains("Es Kopi Daun"))
        assertTrue(text.contains("Diskon"))
    }

    @Test
    fun testThermalPrinterCashDrawerPulseOnCash() {
        val txCash = Transaction(
            id = "#INV-0001",
            dateTime = System.currentTimeMillis(),
            items = listOf(OrderItem("P001", "Es Kopi Daun", 22000, 1, "☕")),
            subtotal = 22000,
            total = 22000,
            paymentMethod = PaymentMethod.CASH,
            tableNumber = "Meja 01"
        )

        val settingsWithDrawer = ReceiptSettings(cashDrawer = true)
        val bytesCash = ThermalPrinter.buildReceiptBytes(txCash, settingsWithDrawer)

        // Verifikasi ada command ESC p (0x1B, 0x70)
        var hasDrawerCommand = false
        for (i in 0 until bytesCash.size - 1) {
            if (bytesCash[i] == 0x1B.toByte() && bytesCash[i + 1] == 0x70.toByte()) {
                hasDrawerCommand = true
                break
            }
        }
        assertTrue(hasDrawerCommand)

        // Jika QRIS, laci kasir tidak dibuka
        val txQris = txCash.copy(paymentMethod = PaymentMethod.QRIS)
        val bytesQris = ThermalPrinter.buildReceiptBytes(txQris, settingsWithDrawer)
        var hasDrawerCommandQris = false
        for (i in 0 until bytesQris.size - 1) {
            if (bytesQris[i] == 0x1B.toByte() && bytesQris[i + 1] == 0x70.toByte()) {
                hasDrawerCommandQris = true
                break
            }
        }
        assertFalse(hasDrawerCommandQris)
    }
}
