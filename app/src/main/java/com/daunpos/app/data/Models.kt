package com.daunpos.app.data

import java.text.NumberFormat
import java.util.Locale

enum class BusinessCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val features: List<String>,
    val defaultTax: Int,
    val itemLabel: String,
    val defaultProductCategories: List<String>
) {
    FNB(
        id = "FNB",
        title = "Kuliner & F&B",
        subtitle = "Restoran, kafe, bakery, warung, angkringan",
        description = "Kelola menu dan kategori, catat pesanan per meja atau takeaway, atur stok, pembayaran, dan cetak struk.",
        features = listOf("Meja & Takeaway", "Stok Menu", "Pembayaran & Struk"),
        defaultTax = 0,
        itemLabel = "Menu",
        defaultProductCategories = listOf("Kopi", "Non-Kopi", "Makanan Berat", "Snack", "Dessert", "Lainnya")
    ),
    RETAIL(
        id = "RETAIL",
        title = "Retail & Toko",
        subtitle = "Kelontong, butik, minimarket, petshop, elektronik",
        description = "Kelola barang, SKU/barcode untuk input cepat, stok, harga, transaksi, dan laporan penjualan.",
        features = listOf("SKU/Barcode", "Stok Barang", "Laporan Penjualan"),
        defaultTax = 0,
        itemLabel = "Barang",
        defaultProductCategories = listOf(
            "Sembako", "Makanan", "Minuman", "Kebutuhan Harian", "Rumah Tangga",
            "Fashion", "Kecantikan", "Petshop", "Elektronik", "Perlengkapan Bayi",
            "Alat Tulis", "Otomotif", "Aksesori", "Lainnya"
        )
    ),
    SERVICE(
        id = "SERVICE",
        title = "Jasa & Layanan",
        subtitle = "Salon, barbershop, laundry, servis, studio foto",
        description = "Kelola daftar layanan, harga, estimasi durasi, catatan permintaan pelanggan, pembayaran, dan laporan.",
        features = listOf("Daftar Layanan", "Estimasi Durasi", "Catatan Pesanan"),
        defaultTax = 0,
        itemLabel = "Layanan",
        defaultProductCategories = listOf(
            "Salon & Grooming", "Laundry", "Perbaikan", "Fotografi",
            "Konsultasi", "Kursus", "Transportasi", "Perawatan", "Lainnya"
        )
    );

    companion object {
        fun fromId(id: String): BusinessCategory =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: FNB
    }
}

data class StoreProfile(
    val storeName: String = "Nama Toko",
    val businessCategory: BusinessCategory = BusinessCategory.FNB,
    val address: String = "",
    val phoneNumber: String = "",
    val taxPercentage: Int = 0,
    val staticQris: String = "",
    val isMultiUserEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)

data class BankAccountSettings(
    val bankName: String = "",
    val accountNumber: String = "",
    val accountHolder: String = ""
) {
    val isConfigured: Boolean
        get() = validationError() == null

    fun validationError(): String? = when {
        bankName.isBlank() -> "Nama bank wajib diisi."
        accountNumber.length !in 5..30 || accountNumber.any { !it.isDigit() } ->
            "Nomor rekening harus terdiri dari 5–30 angka."
        accountHolder.isBlank() -> "Nama pemilik rekening wajib diisi."
        else -> null
    }
}

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val costPrice: Int = 0,
    val available: Boolean = false,
    val stock: Int = 0,
    val barcodeSku: String = "",
    val durationMinutes: Int = 0,
    val emoji: String = "🍽️",
    val colorHex: Long = 0xFFE7F4EA,
    val imageUrl: String? = null,
    val description: String = "",
    val unitLabel: String = "unit"
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val note: String = "",
    val staffName: String = ""
)

data class StoreStaff(
    val id: String,
    val name: String,
    val role: String = "STAFF",
    val isActive: Boolean = true
)

data class OrderItem(
    val productId: String,
    val name: String,
    val price: Int,
    val quantity: Int,
    val emoji: String,
    val note: String = "",
    val staffName: String = "",
    // Snapshot product details at checkout so later catalog edits do not rewrite old receipts.
    val costPrice: Int? = null,
    val unitLabel: String = "unit",
    val description: String = "",
    val durationMinutes: Int = 0
) {
    val total: Int get() = price * quantity
}

enum class PaymentMethod(val label: String) {
    CASH("Tunai"),
    QRIS("QRIS"),
    TRANSFER("Transfer Bank")
}

data class Transaction(
    val id: String,
    val dateTime: Long,
    val items: List<OrderItem>,
    val subtotal: Int,
    val discount: Int = 0,
    val tax: Int = 0,
    val total: Int,
    val paymentMethod: PaymentMethod,
    val cashReceived: Int? = null,
    val change: Int? = null,
    val status: String = "SUKSES",
    val note: String = "",
    val tableNumber: String = "Take Away"
) {
    val destinationLabel: String
        get() = when {
            tableNumber.startsWith("Staf:") || tableNumber.startsWith("Staff:") || tableNumber.startsWith("Petugas:") -> "Staf"
            tableNumber == "Layanan" -> "Layanan"
            tableNumber == "Take Away" -> "Pesanan"
            tableNumber.startsWith("Pelanggan") || tableNumber in listOf("Member", "Grosir", "Reseller", "Online / Kurir") -> "Pelanggan"
            else -> "Meja"
        }

    val assignedStaff: String?
        get() = if (destinationLabel == "Staf") tableNumber.substringAfter(":").trim() else null

    val customerType: String?
        get() = if (destinationLabel == "Pelanggan") tableNumber else null
}

data class ShiftRecord(
    val id: String,
    val cashierName: String,
    val startTime: Long,
    val endTime: Long? = null,
    val startCash: Int,
    val endCash: Int? = null,
    val totalCashSales: Int = 0,
    val totalQrisSales: Int = 0,
    val totalTransferSales: Int = 0,
    val totalTransactions: Int = 0,
    val status: String = "OPEN"
)

data class ReceiptSettings(
    val paperWidth: Int = 58,
    val autoPrint: Boolean = false,
    val showLogo: Boolean = false,
    val showQR: Boolean = false,
    val taxPercentage: Int = 0,
    val headerLines: List<String> = listOf("Nama Toko"),
    val footerText: String = "Terima kasih telah berbelanja."
)

data class PrinterSettings(
    val connected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val scanType: String = "Bluetooth",
    val foundDevices: List<String> = emptyList()
)

val CATEGORIES = listOf("Semua", "Kopi", "Non-Kopi", "Makanan Berat", "Snack", "Dessert")

enum class UserRole { KASIR, OWNER }

data class User(
    val username: String,
    val role: UserRole,
    val requiresPinChange: Boolean = false
) {
    val displayName: String
        get() = if (role == UserRole.OWNER) "Pemilik" else "Kasir"
}

object CurrencyUtils {
    fun formatRupiah(amount: Int): String = formatRupiah(amount.toLong())

    fun formatRupiah(amount: Long): String {
        val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp " + nf.format(amount)
    }

    fun formatNumber(amount: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return nf.format(amount.toLong())
    }
}

fun formatRupiah(amount: Int): String = CurrencyUtils.formatRupiah(amount)
fun formatRupiah(amount: Long): String = CurrencyUtils.formatRupiah(amount)
fun formatNumber(amount: Int): String = CurrencyUtils.formatNumber(amount)

fun formatDurationMinutes(totalMinutes: Int): String {
    if (totalMinutes <= 0) return ""
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val minutes = totalMinutes % 60
    return buildList {
        if (days > 0) add("$days hari")
        if (hours > 0) add("$hours jam")
        if (minutes > 0) add("$minutes menit")
    }.joinToString(" ")
}
