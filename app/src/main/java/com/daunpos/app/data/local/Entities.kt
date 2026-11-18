package com.daunpos.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val price: Int,
    val available: Boolean,
    val stock: Int = 50,
    val emoji: String,
    val colorHex: Long,
    val imageUrl: String? = null
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
