package com.daunpos.app.notification

object BankNotificationParser {

    private val KNOWN_BANK_PACKAGES = mapOf(
        "com.bca" to "BCA Mobile",
        "com.bca.mybca" to "myBCA",
        "com.bca.klikbca" to "KlikBCA",
        "id.bmri.livin" to "Mandiri Livin'",
        "com.bankmandiri.mandirionline" to "Mandiri Online",
        "id.co.bri.brimo" to "BRImo (Bank BRI)",
        "com.bri" to "Bank BRI",
        "id.co.bni.newmobile" to "BNI Mobile",
        "src.com.bni" to "Bank BNI",
        "com.gojek.gobiz" to "GoBiz Merchant",
        "com.gojek.app" to "GoPay",
        "id.dana" to "DANA Bisnis",
        "ovo.id" to "OVO Merchant",
        "com.ovo.merchant" to "OVO Merchant",
        "id.co.seabank.android" to "SeaBank",
        "com.shopee.id" to "ShopeePay"
    )

    private val INCOMING_KEYWORDS = listOf(
        "transfer masuk",
        "dana masuk",
        "uang masuk",
        "pembayaran qris",
        "qris berhasil",
        "transaksi qris",
        "menerima pembayaran",
        "menerima transfer",
        "berhasil diterima",
        "masuk sebesar",
        "kredit",
        "transaksi sukses: masuk",
        "setoran masuk",
        "telah menerima",
        "menerima dana",
        "pembayaran diterima",
        "saldo masuk",
        "top up masuk"
    )

    private val OUTGOING_KEYWORDS = listOf(
        "transfer keluar",
        "uang keluar",
        "mentransfer",
        "telah mentransfer",
        "debit",
        "kamu telah membayar",
        "pembayaran berhasil ke",
        "pembayaran ke",
        "transfer ke",
        "pulsa berhasil",
        "pembelian",
        "tagihan anda",
        "pembayaran merchant",
        "pembayaran di",
        "kode otp",
        "jangan berikan kode"
    )

    /**
     * Memeriksa apakah package aplikasi terdaftar sebagai aplikasi perbankan / e-wallet resmi.
     * Digunakan sebagai whitelist privasi sebelum membaca konten notifikasi di Service.
     */
    fun isKnownBankPackage(packageName: String): Boolean {
        return KNOWN_BANK_PACKAGES.containsKey(packageName)
    }

    /**
     * Mem-parsing notifikasi status bar Android.
     * Mengembalikan BankNotificationEvent jika notifikasi adalah uang/pembayaran masuk yang valid,
     * atau null jika bukan notifikasi uang masuk.
     */
    fun parseNotification(
        packageName: String,
        title: String?,
        text: String?
    ): BankNotificationEvent? {
        val safeTitle = title?.trim() ?: ""
        val safeText = text?.trim() ?: ""
        val combined = "$safeTitle $safeText".trim()

        if (combined.isBlank()) return null

        val lowerCombined = combined.lowercase()

        // 1. Filter out outgoing transactions or OTP spam
        val hasOutgoing = OUTGOING_KEYWORDS.any { lowerCombined.contains(it) }
        val hasIncoming = INCOMING_KEYWORDS.any { lowerCombined.contains(it) }

        if (hasOutgoing) {
            return null
        }

        // Wajibkan sinyal masuk eksplisit (tidak boleh fallback package semata)
        if (!hasIncoming) {
            return null
        }

        // 2. Extract nominal amount in Rupiah
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0) return null

        // 3. Determine bank / source name
        val bankName = KNOWN_BANK_PACKAGES[packageName]
            ?: detectBankFromText(combined)
            ?: "Bank / E-Wallet"

        // 4. Extract sender name if available
        val sender = extractSender(combined)

        return BankNotificationEvent(
            sourcePackage = packageName,
            bankName = bankName,
            amount = amount,
            senderName = sender,
            rawTitle = safeTitle,
            rawText = safeText
        )
    }

    /**
     * Mengekstrak angka nominal Rupiah dari teks notifikasi.
     * Mendukung format seperti:
     * - Rp 50.000
     * - Rp45.000,00
     * - Masuk Rp 75.000
     * - sebesar Rp 100.000
     * - IDR 25000
     */
    fun extractAmount(text: String): Int? {
        // Regex menangkap awalan Rp / IDR / sebesar diikuti digit dan pemisah titik/koma
        val regex = """(?:Rp\.?|IDR|sebesar)\s*([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{2})?|[0-9]+(?:[.,][0-9]{2})?)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return null
        var rawDigits = match.groupValues[1].trim()

        // Hapus sen format Indonesia di ujung (,00 atau .00)
        if (rawDigits.endsWith(",00") || rawDigits.endsWith(".00")) {
            rawDigits = rawDigits.substring(0, rawDigits.length - 3)
        }

        // Hapus pemisah ribuan titik atau koma
        val cleanDigits = rawDigits.replace("[.,]".toRegex(), "")
        return cleanDigits.toIntOrNull()
    }

    /**
     * Mengekstrak nama pengirim jika terdeteksi pola "dari <NAMA>".
     */
    fun extractSender(text: String): String {
        val regex = """(?:dari|from)\s+([A-Za-z0-9\s]{2,30}?)(?:\s+(?:ke|pada|sebesar|di|melalui|untuk|dengan|telah|berhasil)|[.,;]|$)""".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(text) ?: return ""
        val candidate = match.groupValues[1].trim()
        // Bersihkan jika menangkap kata umum seperti "QRIS" atau "Rekening"
        if (candidate.equals("QRIS", ignoreCase = true) || candidate.startsWith("rekening", ignoreCase = true)) {
            return ""
        }
        return candidate
    }

    private fun detectBankFromText(text: String): String? {
        val lower = text.lowercase()
        return when {
            lower.contains("bca") -> "Bank BCA"
            lower.contains("mandiri") || lower.contains("livin") -> "Bank Mandiri"
            lower.contains("brimo") || lower.contains("bri") -> "Bank BRI"
            lower.contains("bni") -> "Bank BNI"
            lower.contains("dana") -> "DANA"
            lower.contains("gopay") || lower.contains("gobiz") -> "GoPay"
            lower.contains("ovo") -> "OVO"
            lower.contains("seabank") -> "SeaBank"
            lower.contains("shopeepay") -> "ShopeePay"
            else -> null
        }
    }
}
