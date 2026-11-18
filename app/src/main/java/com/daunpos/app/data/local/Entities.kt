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
    val emoji: String,
    val colorHex: Long
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val dateTime: Long,
    val itemsJson: String,
    val subtotal: Int,
    val tax: Int,
    val total: Int,
    val paymentMethod: String,
    val cashReceived: Int?,
    val change: Int?,
    val status: String,
    val note: String
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val pinHash: String,
    val pinSalt: String,
    val role: String
)
