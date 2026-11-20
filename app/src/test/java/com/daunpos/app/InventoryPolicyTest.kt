package com.daunpos.app

import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.data.InventoryPolicy
import com.daunpos.app.data.Product
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryPolicyTest {
    private fun product(
        stock: Int,
        available: Boolean = true
    ) = Product(
        id = "P001",
        name = "Layanan",
        category = "Umum",
        price = 10000,
        stock = stock,
        available = available
    )

    @Test
    fun serviceItemsRemainSellableWithoutInventoryQuantity() {
        assertTrue(InventoryPolicy.canSell(product(stock = 0), BusinessCategory.SERVICE))
        assertTrue(InventoryPolicy.canSell(product(stock = 0), BusinessCategory.SERVICE, quantityInCart = 12))
    }

    @Test
    fun retailAndFoodItemsStillRequireAvailableStock() {
        assertFalse(InventoryPolicy.canSell(product(stock = 0), BusinessCategory.RETAIL))
        assertFalse(InventoryPolicy.canSell(product(stock = 0), BusinessCategory.FNB))
        assertTrue(InventoryPolicy.canSell(product(stock = 3), BusinessCategory.RETAIL, quantityInCart = 2))
        assertFalse(InventoryPolicy.canSell(product(stock = 3), BusinessCategory.RETAIL, quantityInCart = 3))
    }

    @Test
    fun unavailableItemsCannotBeSoldInAnyBusinessCategory() {
        BusinessCategory.entries.forEach { category ->
            assertFalse(InventoryPolicy.canSell(product(stock = 100, available = false), category))
        }
    }
}
