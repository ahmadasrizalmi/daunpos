package com.daunpos.app.data

import com.daunpos.app.data.local.ProductEntity
import com.daunpos.app.data.local.ShiftEntity
import com.daunpos.app.data.local.StoreProfileEntity
import com.daunpos.app.data.local.StoreStaffEntity
import com.daunpos.app.data.local.TransactionEntity
import org.json.JSONArray

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    category = category,
    price = price,
    costPrice = costPrice,
    available = available,
    stock = stock,
    barcodeSku = barcodeSku,
    durationMinutes = durationMinutes,
    emoji = emoji,
    colorHex = colorHex,
    imageUrl = imageUrl
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name,
    category = category,
    price = price,
    costPrice = costPrice,
    available = available,
    stock = stock,
    barcodeSku = barcodeSku,
    durationMinutes = durationMinutes,
    emoji = emoji,
    colorHex = colorHex,
    imageUrl = imageUrl
)

fun StoreProfileEntity.toDomain(): StoreProfile = StoreProfile(
    storeName = storeName,
    businessCategory = BusinessCategory.fromId(businessType),
    address = address,
    phoneNumber = phoneNumber,
    taxPercentage = taxPercentage,
    staticQris = staticQrisRaw,
    isMultiUserEnabled = isMultiUserEnabled,
    isOnboardingCompleted = isOnboardingCompleted
)

fun StoreProfile.toEntity(): StoreProfileEntity = StoreProfileEntity(
    id = 1,
    storeName = storeName,
    businessType = businessCategory.id,
    address = address,
    phoneNumber = phoneNumber,
    logoUri = null,
    taxPercentage = taxPercentage,
    staticQrisRaw = staticQris,
    merchantNmid = "",
    isMultiUserEnabled = isMultiUserEnabled,
    isAutoVerificationEnabled = true,
    isOnboardingCompleted = isOnboardingCompleted
)

fun ShiftEntity.toDomain(): ShiftRecord = ShiftRecord(
    id = id,
    cashierName = cashierName,
    startTime = startTime,
    endTime = endTime,
    startCash = startCash,
    endCash = endCash,
    totalCashSales = totalCashSales,
    totalQrisSales = totalQrisSales,
    totalTransferSales = totalTransferSales,
    totalTransactions = totalTransactions,
    status = status
)

fun ShiftRecord.toEntity(): ShiftEntity = ShiftEntity(
    id = id,
    cashierName = cashierName,
    startTime = startTime,
    endTime = endTime,
    startCash = startCash,
    endCash = endCash,
    totalCashSales = totalCashSales,
    totalQrisSales = totalQrisSales,
    totalTransferSales = totalTransferSales,
    totalTransactions = totalTransactions,
    status = status
)

fun StoreStaffEntity.toDomain(): StoreStaff = StoreStaff(
    id = id,
    name = name,
    role = role,
    isActive = isActive
)

fun StoreStaff.toEntity(pinSalt: String = "", pinHash: String = ""): StoreStaffEntity = StoreStaffEntity(
    id = id,
    name = name,
    role = role,
    pinSalt = pinSalt,
    pinHash = pinHash,
    isActive = isActive
)

fun TransactionEntity.toDomain(): Transaction {
    val items = runCatching {
        val arr = JSONArray(itemsJson)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            OrderItem(
                productId = obj.getString("productId"),
                name = obj.getString("name"),
                price = obj.getInt("price"),
                quantity = obj.getInt("quantity"),
                emoji = obj.optString("emoji", "🍽️"),
                note = obj.optString("note", ""),
                staffName = obj.optString("staffName", "")
            )
        }
    }.getOrDefault(emptyList())

    return Transaction(
        id = id,
        dateTime = dateTime,
        items = items,
        subtotal = subtotal,
        discount = discount,
        tax = tax,
        total = total,
        paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH),
        cashReceived = cashReceived,
        change = change,
        status = status,
        note = note,
        tableNumber = tableNumber
    )
}

fun Transaction.toEntity(): TransactionEntity {
    val arr = JSONArray()
    items.forEach { item ->
        arr.put(
            org.json.JSONObject().apply {
                put("productId", item.productId)
                put("name", item.name)
                put("price", item.price)
                put("quantity", item.quantity)
                put("emoji", item.emoji)
                put("note", item.note)
                put("staffName", item.staffName)
            }
        )
    }
    return TransactionEntity(
        id = id,
        dateTime = dateTime,
        itemsJson = arr.toString(),
        subtotal = subtotal,
        discount = discount,
        tax = tax,
        total = total,
        paymentMethod = paymentMethod.name,
        cashReceived = cashReceived,
        change = change,
        status = status,
        note = note,
        tableNumber = tableNumber
    )
}
