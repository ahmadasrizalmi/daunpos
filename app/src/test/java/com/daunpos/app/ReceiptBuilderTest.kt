package com.daunpos.app

import com.daunpos.app.data.OrderItem
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.ReceiptSettings
import com.daunpos.app.data.Transaction
import com.daunpos.app.ui.receipt.ReceiptBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptBuilderTest {

    private fun sampleTransaction() = Transaction(
        id = "#INV-0001",
        dateTime = 1_700_000_000_000L,
        items = listOf(
            OrderItem(
                productId = "P001",
                name = "Kopi Susu Gula Aren Ukuran Besar Extra Shot",
                price = 25000,
                quantity = 2,
                emoji = "☕"
            ),
            OrderItem(
                productId = "P002",
                name = "Roti Bakar",
                price = 15000,
                quantity = 1,
                emoji = "🍞"
            )
        ),
        subtotal = 65000,
        discount = 5000,
        tax = 6000,
        total = 66000,
        paymentMethod = PaymentMethod.CASH,
        cashReceived = 70000,
        change = 4000,
        status = "SUKSES",
        tableNumber = "Take Away"
    )

    @Test
    fun receiptLinesStayWithin58MillimeterThermalWidth() {
        val text = ReceiptBuilder.buildText(
            sampleTransaction(),
            ReceiptSettings(paperWidth = 58, headerLines = listOf("Nama Toko"))
        )
        assertTrue(text.lines().filter { it.isNotEmpty() }.all { it.length <= 32 })
        assertTrue(text.contains("TOTAL"))
        assertTrue(text.contains("Kembalian"))
    }

    @Test
    fun receiptLinesStayWithin80MillimeterThermalWidth() {
        val text = ReceiptBuilder.buildText(
            sampleTransaction(),
            ReceiptSettings(paperWidth = 80, headerLines = listOf("Nama Toko"))
        )
        assertTrue(text.lines().filter { it.isNotEmpty() }.all { it.length <= 48 })
    }

    @Test
    fun receiptNeverPrintsAnUnimplementedDigitalQrPlaceholder() {
        val text = ReceiptBuilder.buildText(
            sampleTransaction(),
            ReceiptSettings(paperWidth = 58, showQR = true, headerLines = listOf("Nama Toko"))
        )
        assertFalse(text.contains("QR DIGITAL"))
        assertFalse(text.contains("████"))
    }
}
