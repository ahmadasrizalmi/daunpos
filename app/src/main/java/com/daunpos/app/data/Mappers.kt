package com.daunpos.app.data

import com.daunpos.app.data.local.ProductEntity
import com.daunpos.app.data.local.TransactionEntity
import org.json.JSONArray

fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    category = category,
    price = price,
    available = available,
    emoji = emoji,
    colorHex = colorHex
)

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    name = name,
    category = category,
    price = price,
    available = available,
    emoji = emoji,
    colorHex = colorHex
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
                emoji = obj.optString("emoji", "🍽️")
            )
        }
    }.getOrDefault(emptyList())

    return Transaction(
        id = id,
        dateTime = dateTime,
        items = items,
        subtotal = subtotal,
        tax = tax,
        total = total,
        paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.CASH),
        cashReceived = cashReceived,
        change = change,
        status = status,
        note = note
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
            }
        )
    }
    return TransactionEntity(
        id = id,
        dateTime = dateTime,
        itemsJson = arr.toString(),
        subtotal = subtotal,
        tax = tax,
        total = total,
        paymentMethod = paymentMethod.name,
        cashReceived = cashReceived,
        change = change,
        status = status,
        note = note
    )
}
