package com.daunpos.app

import com.daunpos.app.data.BusinessCategory
import com.daunpos.app.data.StoreProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BusinessCategoryTest {

    @Test
    fun testBusinessCategoryFromId() {
        assertEquals(BusinessCategory.FNB, BusinessCategory.fromId("FNB"))
        assertEquals(BusinessCategory.FNB, BusinessCategory.fromId("fnb"))
        assertEquals(BusinessCategory.RETAIL, BusinessCategory.fromId("RETAIL"))
        assertEquals(BusinessCategory.RETAIL, BusinessCategory.fromId("retail"))
        assertEquals(BusinessCategory.SERVICE, BusinessCategory.fromId("SERVICE"))
        assertEquals(BusinessCategory.SERVICE, BusinessCategory.fromId("service"))
        // Unknown defaults to FNB
        assertEquals(BusinessCategory.FNB, BusinessCategory.fromId("UNKNOWN"))
    }

    @Test
    fun testCategoryProperties() {
        val fnb = BusinessCategory.FNB
        assertEquals(0, fnb.defaultTax)
        assertEquals("Menu", fnb.itemLabel)
        assertTrue(fnb.defaultProductCategories.contains("Kopi"))
        assertTrue(fnb.features.contains("Meja & Takeaway"))
        assertFalse(fnb.features.any { it.contains("Split Bill", ignoreCase = true) })

        val retail = BusinessCategory.RETAIL
        assertEquals(0, retail.defaultTax)
        assertEquals("Barang", retail.itemLabel)
        assertTrue(retail.defaultProductCategories.contains("Fashion"))
        assertTrue(retail.features.contains("SKU/Barcode"))
        assertTrue(retail.features.contains("Stok Barang"))
        assertFalse(retail.features.any { it.contains("Cetak Label", ignoreCase = true) })

        val service = BusinessCategory.SERVICE
        assertEquals(0, service.defaultTax)
        assertEquals("Layanan", service.itemLabel)
        assertTrue(service.defaultProductCategories.contains("Perawatan"))
        assertTrue(service.features.contains("Daftar Layanan"))
        assertTrue(service.features.contains("Estimasi Durasi"))
        assertTrue(service.features.contains("Catatan Pesanan"))
        assertFalse(service.features.any { it.contains("Staf", ignoreCase = true) })
    }

    @Test
    fun testStoreProfileInitialization() {
        val profile = StoreProfile(
            storeName = "Barbershop Bro",
            businessCategory = BusinessCategory.SERVICE,
            address = "Jl. Merdeka No. 1",
            phoneNumber = "081299998888",
            taxPercentage = 0,
            staticQris = "000201...",
            isMultiUserEnabled = true,
            isOnboardingCompleted = true
        )

        assertEquals("Barbershop Bro", profile.storeName)
        assertEquals(BusinessCategory.SERVICE, profile.businessCategory)
        assertTrue(profile.isMultiUserEnabled)
        assertTrue(profile.isOnboardingCompleted)
    }
}
