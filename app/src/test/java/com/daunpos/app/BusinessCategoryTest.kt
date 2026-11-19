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
        assertEquals(10, fnb.defaultTax)
        assertTrue(fnb.features.contains("Manajemen Meja"))
        assertTrue(fnb.features.contains("Dine-In / Take Away"))

        val retail = BusinessCategory.RETAIL
        assertEquals(0, retail.defaultTax)
        assertTrue(retail.features.contains("Barcode Scanner Cepat"))
        assertTrue(retail.features.contains("Stok & SKU Realtime"))

        val service = BusinessCategory.SERVICE
        assertEquals(0, service.defaultTax)
        assertTrue(service.features.contains("Pilihan Staf / Kapster"))
        assertTrue(service.features.contains("Durasi Pengerjaan"))
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
