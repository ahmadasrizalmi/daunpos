package com.daunpos.app.data.qris

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
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

object QrisConverter {

    /**
     * QRIS Contoh / Demo (Sample DANA & National Interoperability).
     * HANYA digunakan untuk mode demo eksplisit atau pengujian unit test.
     */
    const val DEMO_SAMPLE_STATIC_QRIS =
        "00020101021126570011ID.DANA.WWW011893600915302195026202090219502620303UMI51440014ID.CO.QRIS.WWW0215ID10200219502620303UMI5204581253033605802ID5918DAUNPOS CAFE RESTO6007BANDUNG61054011562070703A016304F0D1"

    const val DEFAULT_SAMPLE_STATIC_QRIS = DEMO_SAMPLE_STATIC_QRIS

    /**
     * Menghitung Checksum CRC16-CCITT standar Bank Indonesia (Polinom 0x1021, Init 0xFFFF).
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
     * Mengonversi string QRIS Statis menjadi QRIS Dinamis dengan nominal belanja terkunci.
     *
     * Langkah:
     * 1. Ubah Tag 01 (Point of Initiation Method) dari '11' (statis) menjadi '12' (dinamis).
     * 2. Sisipkan/perbarui Tag 54 (Transaction Amount) dengan nominal Rupiah.
     * 3. Pastikan Tag 53 ('360' IDR) dan Tag 58 ('ID') tersedia.
     * 4. Hitung ulang Checksum CRC16-CCITT (Tag 63).
     */
    fun convertStaticToDynamic(staticQris: String, amount: Int): String {
        val clean = staticQris.trim().replace("[\r\n\t]+".toRegex(), "")
        if (clean.isBlank() || clean.length < 10) return clean

        val tlvs = parseTlvs(clean).toMutableList()
        if (tlvs.isEmpty()) return clean

        // 1. Tag 01: Point of Initiation Method
        // Menjaga Tag 01 tetap '11' (atau nilai bawaan QRIS statis merchant).
        // Sesuai standar ASPI/EMVCo, Tag 01=11 yang disertai Tag 54 (Transaction Amount)
        // didukung luas dan aman diproses oleh seluruh bank & e-wallet tanpa memerlukan Tag 28 URL.
        val tag01Index = tlvs.indexOfFirst { it.tag == "01" }
        if (tag01Index == -1) {
            tlvs.add(0, QrisTlv("01", 2, "11"))
        }

        // 2. Tag 53: Currency Code (360 = IDR)
        if (tlvs.none { it.tag == "53" }) {
            val insertPos = tlvs.indexOfFirst { it.tag > "53" }.let { if (it == -1) tlvs.size else it }
            tlvs.add(insertPos, QrisTlv("53", 3, "360"))
        }

        // 3. Tag 54: Transaction Amount
        val amountStr = amount.coerceAtLeast(1).toString()
        val tag54 = QrisTlv("54", amountStr.length, amountStr)
        val tag54Index = tlvs.indexOfFirst { it.tag == "54" }
        if (tag54Index != -1) {
            tlvs[tag54Index] = tag54
        } else {
            val pos58 = tlvs.indexOfFirst { it.tag >= "58" }
            if (pos58 != -1) {
                tlvs.add(pos58, tag54)
            } else {
                tlvs.add(tag54)
            }
        }

        // 4. Tag 58: Country Code (ID)
        if (tlvs.none { it.tag == "58" }) {
            tlvs.add(QrisTlv("58", 2, "ID"))
        }

        // 5. Rekonstruksi string tanpa Tag 63
        val sb = StringBuilder()
        for (tlv in tlvs) {
            if (tlv.tag != "63") {
                sb.append(tlv.tag)
                sb.append(tlv.length.toString().padStart(2, '0'))
                sb.append(tlv.value)
            }
        }

        // 6. Tambahkan prefix Tag 63 panjang 04
        sb.append("6304")

        // 7. Hitung CRC16 baru
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
     */
    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
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
        val colorDark = 0xFF14251E.toInt() // Ink / Hijau Tua DaunPOS
        val colorLight = 0xFFFFFFFF.toInt() // Putih Bersih

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
}
