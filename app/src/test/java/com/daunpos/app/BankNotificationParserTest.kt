package com.daunpos.app

import com.daunpos.app.notification.BankNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BankNotificationParserTest {

    @Test
    fun testBcaMobileTransferMasuk() {
        val event = BankNotificationParser.parseNotification(
            packageName = "com.bca",
            title = "Transfer Masuk",
            text = "Anda menerima transfer Rp 50.000 dari SITI AMINAH"
        )
        assertNotNull(event)
        assertEquals(50000, event?.amount)
        assertEquals("BCA Mobile", event?.bankName)
        assertEquals("SITI AMINAH", event?.senderName)
    }

    @Test
    fun testMandiriLivinDanaMasukWithCents() {
        val event = BankNotificationParser.parseNotification(
            packageName = "id.bmri.livin",
            title = "Dana Masuk",
            text = "Dana masuk sebesar Rp 125.000,00 dari BUDI SANTOSO ke tabungan Anda"
        )
        assertNotNull(event)
        assertEquals(125000, event?.amount)
        assertEquals("Mandiri Livin'", event?.bankName)
        assertEquals("BUDI SANTOSO", event?.senderName)
    }

    @Test
    fun testBriBrimoTransferMasuk() {
        val event = BankNotificationParser.parseNotification(
            packageName = "id.co.bri.brimo",
            title = "Transaksi Sukses",
            text = "Transfer masuk sebesar Rp 35.000 dari AHMAD RIZAL"
        )
        assertNotNull(event)
        assertEquals(35000, event?.amount)
        assertEquals("BRImo (Bank BRI)", event?.bankName)
        assertEquals("AHMAD RIZAL", event?.senderName)
    }

    @Test
    fun testBniMobileTransferMasuk() {
        val event = BankNotificationParser.parseNotification(
            packageName = "id.co.bni.newmobile",
            title = "BNI Notifikasi",
            text = "Uang masuk Rp 80.000 dari DONI PRATAMA"
        )
        assertNotNull(event)
        assertEquals(80000, event?.amount)
        assertEquals("BNI Mobile", event?.bankName)
        assertEquals("DONI PRATAMA", event?.senderName)
    }

    @Test
    fun testGoBizQrisPayment() {
        val event = BankNotificationParser.parseNotification(
            packageName = "com.gojek.gobiz",
            title = "Pembayaran QRIS Berhasil",
            text = "Menerima pembayaran QRIS Rp 45.000 di outlet DaunPOS"
        )
        assertNotNull(event)
        assertEquals(45000, event?.amount)
        assertEquals("GoBiz Merchant", event?.bankName)
    }

    @Test
    fun testDanaBisnisIncome() {
        val event = BankNotificationParser.parseNotification(
            packageName = "id.dana",
            title = "DANA Bisnis",
            text = "Uang Masuk Rp 60.000 dari Pelanggan DANA"
        )
        assertNotNull(event)
        assertEquals(60000, event?.amount)
        assertEquals("DANA Bisnis", event?.bankName)
    }

    @Test
    fun testOvoMerchantQris() {
        val event = BankNotificationParser.parseNotification(
            packageName = "ovo.id",
            title = "OVO Merchant",
            text = "Pembayaran QRIS sebesar Rp 22.500 berhasil diterima"
        )
        assertNotNull(event)
        assertEquals(22500, event?.amount)
        assertEquals("OVO Merchant", event?.bankName)
    }

    @Test
    fun testSeaBankTransferMasuk() {
        val event = BankNotificationParser.parseNotification(
            packageName = "id.co.seabank.android",
            title = "Transfer Masuk",
            text = "Kamu menerima transfer Rp 150.000 dari RIAN"
        )
        assertNotNull(event)
        assertEquals(150000, event?.amount)
        assertEquals("SeaBank", event?.bankName)
        assertEquals("RIAN", event?.senderName)
    }

    @Test
    fun testRejectOutgoingTransactions() {
        val outgoingBca = BankNotificationParser.parseNotification(
            packageName = "com.bca",
            title = "Transfer Berhasil",
            text = "Kamu telah mentransfer Rp 50.000 ke REKENING 12345678"
        )
        assertNull("Notifikasi transfer keluar harus ditolak", outgoingBca)

        val debitMandiri = BankNotificationParser.parseNotification(
            packageName = "id.bmri.livin",
            title = "Transaksi Debit",
            text = "Transaksi debit kartu sebesar Rp 100.000 berhasil"
        )
        assertNull("Notifikasi debit harus ditolak", debitMandiri)
    }

    @Test
    fun testRejectOtpAndGeneralChatNotifications() {
        val otpNotif = BankNotificationParser.parseNotification(
            packageName = "com.bca",
            title = "Kode OTP Anda",
            text = "Jangan berikan kode OTP 592812 kepada siapapun termasuk petugas bank"
        )
        assertNull("Notifikasi OTP harus ditolak", otpNotif)

        val chatNotif = BankNotificationParser.parseNotification(
            packageName = "com.whatsapp",
            title = "Andi",
            text = "Halo, apakah pesanan saya sudah selesai?"
        )
        assertNull("Notifikasi non-bank non-pembayaran harus ditolak", chatNotif)
    }

    @Test
    fun testExtractAmountFormats() {
        assertEquals(1500000, BankNotificationParser.extractAmount("Transfer masuk Rp 1.500.000 dari PT ABC"))
        assertEquals(50000, BankNotificationParser.extractAmount("Rp50.000,00 diterima"))
        assertEquals(75000, BankNotificationParser.extractAmount("IDR 75000 berhasil"))
        assertEquals(20000, BankNotificationParser.extractAmount("sebesar Rp 20.000.00 ke dompet"))
    }
}
