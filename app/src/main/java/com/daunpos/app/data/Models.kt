package com.daunpos.app.data

import java.text.NumberFormat
import java.util.Locale

enum class BusinessCategory(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val features: List<String>,
    val defaultTax: Int
) {
    FNB(
        id = "FNB",
        title = "Kuliner & F&B",
        subtitle = "Resto, Kafe, Angkringan, Bakery",
        description = "Pengaturan nomor meja, pesanan dine-in & take away, cetak struk dapur, dan split bill.",
        features = listOf("Manajemen Meja", "Dine-In / Take Away", "Struk Dapur", "Split Bill"),
        defaultTax = 10
    ),
    RETAIL(
        id = "RETAIL",
        title = "Retail & Toko",
        subtitle = "Kelontong, Butik, Minimarket, Petshop",
        description = "Barcode scanner cepat, manajemen inventaris stok & SKU realtime, varian harga, dan cetak label rak.",
        features = listOf("Barcode Scanner Cepat", "Stok & SKU Realtime", "Grosir / Varian", "Cetak Label Rak"),
        defaultTax = 0
    ),
    SERVICE(
        id = "SERVICE",
        title = "Jasa & Layanan",
        subtitle = "Studio Foto, Barbershop, Salon, Servis, Laundry",
        description = "Pilihan staf pelaksana (fotografer/teknisi/stylist), estimasi durasi, catatan khusus order, dan struk fleksibel.",
        features = listOf("Pilihan Staf Pelaksana", "Durasi Pengerjaan", "Pencatatan Komisi", "Catatan Layanan"),
        defaultTax = 0
    );

    companion object {
        fun fromId(id: String): BusinessCategory =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: FNB
    }
}

data class StoreProfile(
    val storeName: String = "DaunPOS",
    val businessCategory: BusinessCategory = BusinessCategory.FNB,
    val address: String = "Jl. Daun Hijau No. 8",
    val phoneNumber: String = "0812-3456-7890",
    val taxPercentage: Int = 10,
    val staticQris: String = "",
    val isMultiUserEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val costPrice: Int = 0,
    val available: Boolean = true,
    val stock: Int = 50,
    val barcodeSku: String = "",
    val durationMinutes: Int = 0,
    val emoji: String = "🍽️",
    val colorHex: Long = 0xFFE7F4EA,
    val imageUrl: String? = null
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
    val staffName: String = ""
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
    val autoPrint: Boolean = true,
    val cashDrawer: Boolean = false,
    val showLogo: Boolean = true,
    val showQR: Boolean = true,
    val taxPercentage: Int = 0,
    val headerLines: List<String> = listOf(
        "DaunPOS Coffee & Eatery",
        "Jl. Daun Hijau No. 8, Bandung",
        "0812-3456-7890"
    ),
    val footerText: String = "Terima kasih sudah berkunjung!\nPassword Wi-Fi: kopi123"
)

data class PrinterSettings(
    val connected: Boolean = false,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val scanType: String = "Bluetooth",
    val foundDevices: List<String> = listOf(
        "RPP02N Bluetooth Printer",
        "DAUNPOS-KITCHEN",
        "58BT-Thermal-01"
    )
)

val CATEGORIES = listOf("Semua", "Kopi", "Non-Kopi", "Makanan Berat", "Snack", "Dessert")

enum class UserRole { KASIR, OWNER }

data class User(
    val username: String,
    val role: UserRole
) {
    val displayName: String
        get() = if (role == UserRole.OWNER) "Pemilik" else "Kasir"
}

object CurrencyUtils {
    fun formatRupiah(amount: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return "Rp " + nf.format(amount.toLong())
    }

    fun formatNumber(amount: Int): String {
        val nf = NumberFormat.getNumberInstance(Locale("id", "ID"))
        return nf.format(amount.toLong())
    }
}

fun formatRupiah(amount: Int): String = CurrencyUtils.formatRupiah(amount)
fun formatNumber(amount: Int): String = CurrencyUtils.formatNumber(amount)
