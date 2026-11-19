package com.daunpos.app

import com.daunpos.app.data.qris.QrisConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

        // Cek secara spesifik bahwa Tag 01 tetap 11 dan Tag 54 ada
        val tlvs = QrisConverter.parseTlvs(dynamicPayload)
        val tag01 = tlvs.find { it.tag == "01" }
        assertEquals("11", tag01?.value)

        val tag54 = tlvs.find { it.tag == "54" }
        assertNotNull(tag54)
        assertEquals("75000", tag54?.value)
        assertEquals(5, tag54?.length)
    }

    @Test
    fun testCrcCalculationConsistency() {
        val prefix = "00020101021153033605802ID5908TESTKAFE6007BANDUNG6304"
        val crc = QrisConverter.calculateCrc16(prefix)
        assertEquals(4, crc.length)
        // Verifikasi bahwa jika ditempelkan, info.isValidCrc menghasilkan true
        val full = prefix + crc
        val info = QrisConverter.parseMerchantInfo(full)
        assertTrue(info.isValidCrc)
    }
}
