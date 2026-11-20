package com.daunpos.app.data

object InventoryPolicy {
    fun tracksStock(category: BusinessCategory): Boolean =
        category != BusinessCategory.SERVICE

    fun canSell(
        product: Product,
        category: BusinessCategory,
        quantityInCart: Int = 0
    ): Boolean {
        if (!product.available || quantityInCart < 0) return false
        if (!tracksStock(category)) return true
        return product.stock > quantityInCart
    }
}
