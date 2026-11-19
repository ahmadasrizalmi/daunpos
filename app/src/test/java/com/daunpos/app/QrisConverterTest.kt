package com.daunpos.app

import com.daunpos.app.data.qris.QrisConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrisConverterTest {

    @Test
    fun testDefaultSampleStaticQrisIsValid() {
        val sample = QrisConverter.DEFAULT_SAMPLE_STATIC_QRIS
        val info = QrisConverter.parseMerchantInfo(sample)

        assertEquals("DAUNPOS CAFE RESTO", info.merchantName)
        assertEquals("BANDUNG", info.city)
        assertFalse("Sample awal harus bertipe statis (Tag 01 = 11)", info.isDynamic)
        assertTrue("Checksum CRC16 harus valid", info.isValidCrc)
        assertEquals("ID1020021950262", info.nmid)

        val validation = QrisConverter.validateQris(sample)
        assertTrue("Validasi struktur QRIS harus valid", validation.isValid)
        assertTrue("Tidak boleh ada error validasi", validation.errors.isEmpty())
    }

    @Test
    fun testStaticToDynamicConversion() {
        val sample = QrisConverter.DEFAULT_SAMPLE_STATIC_QRIS
        val amount = 75000
        val dynamicPayload = QrisConverter.convertStaticToDynamic(sample, amount)

        val info = QrisConverter.parseMerchantInfo(dynamicPayload)
        assertTrue("Setelah konversi, harus bertipe dinamis / memiliki nominal", info.isDynamic)
        assertEquals(75000, info.amount)
        assertEquals("DAUNPOS CAFE RESTO", info.merchantName)
        assertEquals("BANDUNG", info.city)
        assertTrue("Checksum CRC16 dari payload dinamis yang baru harus valid", info.isValidCrc)

        // Cek secara spesifik bahwa Tag 01 berubah menjadi 12 (Dinamis) dan Tag 54 ada
        val tlvs = QrisConverter.parseTlvs(dynamicPayload)
        val tag01 = tlvs.find { it.tag == "01" }
        assertEquals("12", tag01?.value)

        val tag54 = tlvs.find { it.tag == "54" }
        assertNotNull(tag54)
        assertEquals("75000", tag54?.value)
        assertEquals(5, tag54?.length)
    }

    @Test
    fun testBitsConverterSampleCompatibility() {
        // Sample static QRIS resmi dari repositori BITS-QRIS-Converter
        val sampleBits = "00020101021126560014ID.CO.QRIS.WWW0115ID10231625260990215ID10231625260995204581253033605802ID5914TOKO BITS JAYA6007JAKARTA6105123456304DA44"
        val dynamicPayload = QrisConverter.convertStaticToDynamic(sampleBits, 50000)

        // Verifikasi hasil konversi sama persis dengan BITS: Tag 54 di depan 58, Tag 01 = 12, CRC = 5C74
        val expectedBitsOutput = "00020101021226560014ID.CO.QRIS.WWW0115ID10231625260990215ID10231625260995204581253033605405500005802ID5914TOKO BITS JAYA6007JAKARTA61051234563045C74"
        assertEquals(expectedBitsOutput, dynamicPayload)
    }

    @Test
    fun testManagedTagsStrippedOnDynamicConversion() {
        // QRIS statis yang memiliki Tag 55 (Tip Indicator) dan dummy Tag 54
        val staticWithTip = "00020101021126560014ID.CO.QRIS.WWW0115ID10231625260990215ID10231625260995204581253033605502015802ID5914TOKO BITS JAYA6007JAKARTA6105123456304"
        val full = staticWithTip + QrisConverter.calculateCrc16(staticWithTip)

        val dynamicPayload = QrisConverter.convertStaticToDynamic(full, 25000)
        val tlvs = QrisConverter.parseTlvs(dynamicPayload)

        // Tag 55 harus dibersihkan agar tidak bentrok dengan pembayaran dinamis
        assertNull("Tag 55 (Tip indicator) harus dihapus saat konversi dinamis", tlvs.find { it.tag == "55" })
        assertEquals("25000", tlvs.find { it.tag == "54" }?.value)
        assertEquals("12", tlvs.find { it.tag == "01" }?.value)
    }

    @Test
    fun testMissingTag60AutoRepaired() {
        // Payload tanpa Tag 60 (Merchant City)
        val staticMissing60 = "00020101021126610014COM.GO-JEK.WWW01189360091438110491800210G8110491800303UMI51440014ID.CO.QRIS.WWW0215ID10254436457060303UMI5204733353033605802ID5909DNAPHOTO663046DD0"
        val dynamicPayload = QrisConverter.convertStaticToDynamic(staticMissing60, 46200, "BANDUNG")

        val tlvs = QrisConverter.parseTlvs(dynamicPayload)
        val tag60 = tlvs.find { it.tag == "60" }
        assertNotNull("Tag 60 harus otomatis disisipkan jika tidak ada pada payload asal", tag60)
        assertEquals("BANDUNG", tag60?.value)

        val validation = QrisConverter.validateQris(dynamicPayload)
        assertTrue("Payload dinamis hasil perbaikan harus valid", validation.isValid)
    }

    @Test
    fun testCrcCalculationConsistency() {
        val prefix = "00020101021153033605802ID5908TESTKAFE6007BANDUNG6304"
        val crc = QrisConverter.calculateCrc16(prefix)
        assertEquals(4, crc.length)
        val full = prefix + crc
        val info = QrisConverter.parseMerchantInfo(full)
        assertTrue(info.isValidCrc)
    }
}
