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

        // Security guards for uninitialized or placeholder staff
        assertFalse(PinHasher.verify("1234", "", ""))
        assertFalse(PinHasher.verify("1234", "-", "NO_LOGIN"))
        assertFalse(PinHasher.verify("", "-", "NO_LOGIN"))
    }

    @Test
    fun testTransactionDestinationProperties() {
        val txFnb = Transaction(
            id = "TX-01",
            dateTime = System.currentTimeMillis(),
            items = emptyList(),
            subtotal = 10000,
            total = 10000,
            paymentMethod = PaymentMethod.CASH,
            tableNumber = "Meja 05"
        )
        org.junit.Assert.assertEquals("Meja", txFnb.destinationLabel)
        org.junit.Assert.assertNull(txFnb.assignedStaff)
        org.junit.Assert.assertNull(txFnb.customerType)

        val txService = txFnb.copy(tableNumber = "Staf: Dimas")
        org.junit.Assert.assertEquals("Staf", txService.destinationLabel)
        org.junit.Assert.assertEquals("Dimas", txService.assignedStaff)
        org.junit.Assert.assertNull(txService.customerType)

        val txRetail = txFnb.copy(tableNumber = "Pelanggan Umum")
        org.junit.Assert.assertEquals("Pelanggan", txRetail.destinationLabel)
        org.junit.Assert.assertEquals("Pelanggan Umum", txRetail.customerType)
        org.junit.Assert.assertNull(txRetail.assignedStaff)
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

    @Test
    fun testServiceReceiptWithStaffAndNotes() {
        val tx = Transaction(
            id = "#INV-FOTO-01",
            dateTime = System.currentTimeMillis(),
            items = listOf(
                OrderItem(
                    productId = "P001",
                    name = "Sesi Foto Wisuda",
                    price = 250000,
                    quantity = 1,
                    emoji = "🎓",
                    note = "Background Navy, 4 Orang",
                    staffName = "Dimas (Fotografer)"
                )
            ),
            subtotal = 250000,
            discount = 0,
            tax = 0,
            total = 250000,
            paymentMethod = PaymentMethod.TRANSFER,
            tableNumber = "Staf: Dimas"
        )

        val receiptText = ReceiptBuilder.buildText(tx, ReceiptSettings())
        assertTrue(receiptText.contains("Staf  : Dimas"))
        assertTrue(receiptText.contains("Sesi Foto Wisuda"))
        assertTrue(receiptText.contains("Background Navy"))

        val printerBytes = ThermalPrinter.buildReceiptBytes(tx, ReceiptSettings())
        val printerString = String(printerBytes, Charsets.US_ASCII)
        assertTrue(printerString.contains("Staf  : Dimas"))
        assertTrue(printerString.contains("Sesi Foto Wisuda"))
        assertTrue(printerString.contains("Background Navy"))
    }

    @Test
    fun testRetailReceiptWithCustomerType() {
        val tx = Transaction(
            id = "#INV-RET-01",
            dateTime = System.currentTimeMillis(),
            items = listOf(
                OrderItem(
                    productId = "P002",
                    name = "Kaos Polos Combed",
                    price = 65000,
                    quantity = 2,
                    emoji = "👕"
                )
            ),
            subtotal = 130000,
            discount = 10000,
            tax = 0,
            total = 120000,
            paymentMethod = PaymentMethod.CASH,
            tableNumber = "Pelanggan Umum"
        )

        val receiptText = ReceiptBuilder.buildText(tx, ReceiptSettings())
        assertTrue(receiptText.contains("Tipe  : Pelanggan Umum"))
        assertTrue(receiptText.contains("Kaos Polos Combed"))
    }
}
