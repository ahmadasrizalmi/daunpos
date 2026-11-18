package com.daunpos.app.data

import java.text.NumberFormat
import java.util.Locale

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val available: Boolean = true,
    val stock: Int = 50,
    val emoji: String = "🍽️",
    val colorHex: Long = 0xFFE7F4EA,
    val imageUrl: String? = null
)

data class CartItem(
    val product: Product,
    val quantity: Int,
    val note: String = ""
)

data class OrderItem(
    val productId: String,
    val name: String,
    val price: Int,
    val quantity: Int,
    val emoji: String
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
)

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
