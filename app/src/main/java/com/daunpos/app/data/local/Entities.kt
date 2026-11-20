package com.daunpos.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val costPrice: Int = 0,
    val available: Boolean,
    val stock: Int = 0,
    val barcodeSku: String = "",
    val durationMinutes: Int = 0,
    val emoji: String,
    val colorHex: Long,
    val imageUrl: String? = null,
    val description: String = "",
    val unitLabel: String = "unit"
)

@Entity(tableName = "store_profile")
data class StoreProfileEntity(
    @PrimaryKey val id: Int = 1,
    val storeName: String,
    val businessType: String, // "FNB", "RETAIL", "SERVICE"
    val address: String,
    val phoneNumber: String,
    val logoUri: String? = null,
    val taxPercentage: Int = 0,
    val staticQrisRaw: String = "",
    val merchantNmid: String = "",
    val isMultiUserEnabled: Boolean = false,
    val isAutoVerificationEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)

@Entity(tableName = "store_staff")
data class StoreStaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    val role: String, // "OWNER", "CASHIER", "STAFF"
    val pinSalt: String,
    val pinHash: String,
    val isActive: Boolean = true
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val dateTime: Long,
    val itemsJson: String,
    val subtotal: Int,
    val discount: Int = 0,
    val tax: Int,
    val total: Int,
    val paymentMethod: String,
    val cashReceived: Int?,
    val change: Int?,
    val status: String,
    val note: String,
    val tableNumber: String = "Take Away"
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val pinHash: String,
    val pinSalt: String,
    val role: String
)

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey val id: String,
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
