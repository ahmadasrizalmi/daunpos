package com.daunpos.app.data.qris

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

data class QrisTlv(
    val tag: String,
    val length: Int,
    val value: String
)

data class QrisMerchantInfo(
    val merchantName: String = "Merchant QRIS",
    val city: String = "Indonesia",
    val postalCode: String = "",
    val nmid: String = "",
    val isDynamic: Boolean = false,
    val amount: Int? = null,
    val isValidCrc: Boolean = false,
    val rawPayload: String = ""
)

data class QrisValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val merchantName: String = "",
    val city: String = "",
    val nmid: String = ""
)

object QrisConverter {

    /**
     * QRIS Contoh / Demo (Sample DANA & National Interoperability).
     * HANYA digunakan untuk mode demo eksplisit atau pengujian unit test.
     */
    const val DEMO_SAMPLE_STATIC_QRIS =
        "00020101021126570011ID.DANA.WWW011893600915302195026202090219502620303UMI51440014ID.CO.QRIS.WWW0215ID10200219502620303UMI5204581253033605802ID5918DAUNPOS CAFE RESTO6007BANDUNG61054011562070703A016304F0D1"

    const val DEFAULT_SAMPLE_STATIC_QRIS = DEMO_SAMPLE_STATIC_QRIS

    /**
     * Tag EMVCo yang dikelola/dibersihkan secara otomatis saat konversi ke QRIS Dinamis.
     * Mengikuti spesifikasi BITS-QRIS-Converter & EMVCo:
     * - Tag 54: Transaction Amount
     * - Tag 55: Tip / Convenience Indicator (wajib dihapus jika dinamis, agar tidak bentrok dengan tip prompt)
     * - Tag 56: Convenience Fee Fixed
     * - Tag 57: Convenience Fee Percentage
     * - Tag 63: Checksum CRC16
     */
    private val MANAGED_TAGS = setOf("54", "55", "56", "57", "63")

    /**
     * Menghitung Checksum CRC16-CCITT standar Bank Indonesia & EMVCo (Polinom 0x1021, Init 0xFFFF).
     */
    fun calculateCrc16(data: String): String {
        var crc = 0xFFFF
        val polynomial = 0x1021
        val bytes = data.toByteArray(Charsets.ISO_8859_1)
        for (b in bytes) {
            for (i in 0 until 8) {
                val bit = (b.toInt() shr (7 - i) and 1) == 1
                val c15 = (crc shr 15 and 1) == 1
                crc = crc shl 1
                if (c15 xor bit) {
                    crc = crc xor polynomial
                }
            }
        }
        crc = crc and 0xFFFF
        return crc.toString(16).uppercase(Locale.US).padStart(4, '0')
    }

    /**
     * Mem-parsing string QRIS menjadi daftar komponen TLV (Tag-Length-Value).
     */
    fun parseTlvs(raw: String): List<QrisTlv> {
        val tlvs = mutableListOf<QrisTlv>()
        var index = 0
        while (index + 4 <= raw.length) {
            val tag = raw.substring(index, index + 2)
            val lengthStr = raw.substring(index + 2, index + 4)
            val length = lengthStr.toIntOrNull() ?: break
            if (index + 4 + length > raw.length) break
            val value = raw.substring(index + 4, index + 4 + length)
            tlvs.add(QrisTlv(tag, length, value))
            index += 4 + length
        }
        return tlvs
    }

    /**
     * Memvalidasi struktur QRIS sesuai standar EMVCo & ASPI:
     * - Awalan 000201
     * - Checksum CRC16 (Tag 63)
     * - 8 Tag wajib: 00, 01, 52, 53, 58, 59, 60, 63
     * - Informasi akun merchant (Tag 26..51)
     */
    fun validateQris(raw: String): QrisValidationResult {
        val clean = raw.trim().replace("[\r\n\t]+".toRegex(), "")
        if (clean.isBlank()) {
            return QrisValidationResult(false, listOf("String QRIS kosong"))
        }
        if (!clean.startsWith("000201")) {
            return QrisValidationResult(false, listOf("QRIS harus diawali dengan '000201'"))
        }
        if (clean.length < 20) {
            return QrisValidationResult(false, listOf("String QRIS terlalu pendek"))
        }

        val errors = mutableListOf<String>()
        val tlvs = parseTlvs(clean)
        if (tlvs.isEmpty()) {
            return QrisValidationResult(false, listOf("Gagal mengurai struktur TLV QRIS"))
        }

        // Validasi CRC16
        val crcTlv = tlvs.find { it.tag == "63" }
        if (crcTlv != null && clean.contains("6304")) {
            val withoutCrc = clean.substring(0, clean.lastIndexOf("6304") + 4)
            val expectedCrc = calculateCrc16(withoutCrc)
            if (!expectedCrc.equals(crcTlv.value, ignoreCase = true)) {
                errors.add("CRC tidak cocok: dihitung $expectedCrc, tertera ${crcTlv.value}")
            }
        } else {
            errors.add("Tag 63 (CRC) tidak ditemukan")
        }

        val presentTags = tlvs.map { it.tag }.toSet()
        val requiredTags = listOf(
            "00" to "Payload Format Indicator",
            "01" to "Point of Initiation Method",
            "52" to "Merchant Category Code",
            "53" to "Transaction Currency",
            "58" to "Country Code",
            "59" to "Merchant Name",
            "60" to "Merchant City",
            "63" to "CRC"
        )
        for ((tag, name) in requiredTags) {
            if (!presentTags.contains(tag)) {
                errors.add("Tag wajib $tag ($name) tidak ditemukan")
            }
        }

        // Cek info merchant (Tag 26..51)
        val hasMerchantInfo = tlvs.any {
            val num = it.tag.toIntOrNull() ?: 0
            num in 26..51
        }
        if (!hasMerchantInfo) {
            errors.add("Informasi akun merchant (Tag 26..51) tidak ditemukan")
        }

        val merchantInfo = parseMerchantInfo(clean)
        return QrisValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            merchantName = merchantInfo.merchantName,
            city = merchantInfo.city,
            nmid = merchantInfo.nmid
        )
    }

    /**
     * Mengonversi string QRIS Statis menjadi QRIS Dinamis dengan nominal belanja terkunci.
     * Mengikuti arsitektur TLV presisi BITS-QRIS-Converter:
     * 1. Bersihkan karakter non-ASCII dan whitespace.
     * 2. Bersihkan tag kelolaan: Tag 54, Tag 55, Tag 56, Tag 57, Tag 63.
     * 3. Ubah Tag 01 dari '11' (statis) menjadi '12' (dinamis).
     * 4. Pastikan Tag 53 ('360' IDR) tersedia sebelum Tag 58.
     * 5. Sisipkan Tag 54 (Transaction Amount) tepat sebelum Tag 58 (Country Code).
     * 6. Pastikan Tag 58 ('ID') dan Tag 60 (Merchant City) terjamin ada.
     * 7. Hitung ulang Checksum CRC16-CCITT (Tag 63).
     */
    fun convertStaticToDynamic(staticQris: String, amount: Int, fallbackCity: String = "INDONESIA"): String {
        val clean = staticQris.trim().replace("[\r\n\t]+".toRegex(), "")
        if (clean.isBlank() || clean.length < 10) return clean

        val tlvs = parseTlvs(clean)
        if (tlvs.isEmpty()) return clean

        val result = mutableListOf<QrisTlv>()
        var amountInserted = false
        var hasTag53 = tlvs.any { it.tag == "53" }
        var hasTag58 = tlvs.any { it.tag == "58" }
        var hasTag60 = tlvs.any { it.tag == "60" }

        val amountStr = amount.coerceAtLeast(1).toString()

        for (el in tlvs) {
            // Hapus tag kelolaan lama (54, 55, 56, 57, 63)
            if (MANAGED_TAGS.contains(el.tag)) continue

            // 1. Tag 01: Set ke '12' (Dynamic QRIS)
            if (el.tag == "01") {
                result.add(QrisTlv("01", 2, "12"))
                continue
            }

            // 2. Tag 53: Pastikan ada sebelum Tag 58
            if (el.tag == "58" && !hasTag53) {
                result.add(QrisTlv("53", 3, "360"))
                hasTag53 = true
            }

            // 3. Tag 54: Sisipkan tepat sebelum Tag 58 (Country Code)
            if (el.tag == "58" && !amountInserted) {
                result.add(QrisTlv("54", amountStr.length, amountStr))
                amountInserted = true
            }

            result.add(el)

            // 4. Tag 60: Jika Tag 60 hilang di payload asli, sisipkan tepat setelah Tag 59
            if (el.tag == "59" && !hasTag60) {
                val city = fallbackCity.trim().ifBlank { "INDONESIA" }.uppercase(Locale.US)
                result.add(QrisTlv("60", city.length, city))
                hasTag60 = true
            }
        }

        // Fallback jika urutan tag non-standar
        if (!amountInserted) {
            result.add(QrisTlv("54", amountStr.length, amountStr))
        }
        if (!hasTag58) {
            result.add(QrisTlv("58", 2, "ID"))
        }
        if (!hasTag60) {
            val city = fallbackCity.trim().ifBlank { "INDONESIA" }.uppercase(Locale.US)
            result.add(QrisTlv("60", city.length, city))
        }

        // Rekonstruksi string TLV tanpa Tag 63
        val sb = StringBuilder()
        for (tlv in result) {
            sb.append(tlv.tag)
            sb.append(tlv.length.toString().padStart(2, '0'))
            sb.append(tlv.value)
        }

        // Tambahkan placeholder Tag 63 panjang 04
        sb.append("6304")

        // Hitung CRC16-CCITT baru
        val newCrc = calculateCrc16(sb.toString())
        sb.append(newCrc)

        return sb.toString()
    }

    /**
     * Mem-parsing informasi merchant yang terkandung dalam QRIS (Nama, Kota, NMID, Nominal, Status CRC).
     */
    fun parseMerchantInfo(qris: String): QrisMerchantInfo {
        val clean = qris.trim().replace("[\r\n\t]+".toRegex(), "")
        if (clean.isBlank()) return QrisMerchantInfo()

        val tlvs = parseTlvs(clean)
        val merchantName = tlvs.find { it.tag == "59" }?.value ?: "Merchant DaunPOS"
        val city = tlvs.find { it.tag == "60" }?.value ?: "Indonesia"
        val postalCode = tlvs.find { it.tag == "61" }?.value ?: ""
        val isDynamic = tlvs.find { it.tag == "01" }?.value == "12" || tlvs.any { it.tag == "54" }
        val amount = tlvs.find { it.tag == "54" }?.value?.toIntOrNull()

        // Cari NMID: utamakan Tag 51 (Standar NMID Nasional Bank Indonesia), lalu tag 26..50
        var nmid = ""
        tlvs.find { it.tag == "51" }?.let { tlv51 ->
            val subTlvs = parseTlvs(tlv51.value)
            val idVal = subTlvs.find { (it.tag == "02" || it.tag == "01") && !it.value.contains(".") }?.value
            if (!idVal.isNullOrBlank()) {
                nmid = idVal
            }
        }

        if (nmid.isBlank()) {
            for (tagId in 26..50) {
                val tagStr = tagId.toString().padStart(2, '0')
                val accountTlv = tlvs.find { it.tag == tagStr } ?: continue
                val subTlvs = parseTlvs(accountTlv.value)
                val idVal = subTlvs.find { (it.tag == "02" || it.tag == "01") && !it.value.contains(".") && it.value.length >= 8 }?.value
                if (!idVal.isNullOrBlank()) {
                    nmid = idVal
                    break
                }
            }
        }

        // Validasi integritas CRC16
        val crcTlv = tlvs.find { it.tag == "63" }
        val isValidCrc = if (crcTlv != null && clean.contains("6304")) {
            val withoutCrc = clean.substring(0, clean.lastIndexOf("6304") + 4)
            calculateCrc16(withoutCrc).equals(crcTlv.value, ignoreCase = true)
        } else false

        return QrisMerchantInfo(
            merchantName = merchantName,
            city = city,
            postalCode = postalCode,
            nmid = nmid,
            isDynamic = isDynamic,
            amount = amount,
            isValidCrc = isValidCrc,
            rawPayload = clean
        )
    }

    /**
     * Merender payload QRIS menjadi Android Bitmap resolusi tajam via ZXing.
     * Menggunakan setting standar industri (BITS-QRIS-Converter):
     * - Modul Hitam Murni (#000000) & Putih Murni (#FFFFFF) untuk rasio kontras maksimal kamera bank.
     * - Margin 2 modul solid (Quiet Zone) sesuai standar ISO/IEC 18004.
     * - Error Correction Level M (15% pemulihan data).
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val bitMatrix: BitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        val colorDark = 0xFF000000.toInt() // Hitam Murni (#000000)
        val colorLight = 0xFFFFFFFF.toInt() // Putih Murni (#FFFFFF)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) colorDark else colorLight
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Membaca dan mendekode QR code dari Android Bitmap secara 100% offline & lokal via ZXing.
     */
    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)
        )
        // Percobaan 1: HybridBinarizer (optimal untuk kontras tinggi/normal)
        try {
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap, hints)
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {}

        // Percobaan 2: GlobalHistogramBinarizer (fallback untuk foto/kertas dengan bayangan/cahaya redup)
        try {
            val binaryBitmap = BinaryBitmap(GlobalHistogramBinarizer(source))
            val result = MultiFormatReader().decode(binaryBitmap, hints)
            if (!result.text.isNullOrBlank()) return result.text
        } catch (_: Exception) {}

        return null
    }

    /**
     * Membaca dan mendekode QR code dari Uri gambar (Galeri / Foto) dengan downsampling aman memori.
     */
    fun decodeQrFromUri(context: Context, uri: Uri): String? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            val maxDim = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 1200) {
                sampleSize *= 2
            }
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null
            decodeQrFromBitmap(bitmap)
        } catch (e: Exception) {
            null
        }
    }
}
