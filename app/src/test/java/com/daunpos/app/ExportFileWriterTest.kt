package com.daunpos.app

import com.daunpos.app.data.ExportFileWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class ExportFileWriterTest {
    @Test
    fun copiesPdfBytesExactlyToSelectedDocumentOutputStream() {
        val pdfBytes = "%PDF-1.4\nDaunPOS report\n%%EOF".toByteArray()
        val destination = ByteArrayOutputStream()

        ExportFileWriter.copy(ByteArrayInputStream(pdfBytes), destination)

        assertArrayEquals(pdfBytes, destination.toByteArray())
    }

    @Test
    fun copiesCsvWithUtf8BomAndIndonesianCharactersWithoutCorruption() {
        val csvBytes = "\uFEFFTanggal,Produk\n20/11/2026,Kopi Susu Gula Aren".toByteArray(Charsets.UTF_8)
        val destination = ByteArrayOutputStream()

        ExportFileWriter.copy(ByteArrayInputStream(csvBytes), destination)

        assertArrayEquals(csvBytes, destination.toByteArray())
    }
}
