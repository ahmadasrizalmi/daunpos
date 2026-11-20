package com.daunpos.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppRoutesTest {
    @Test
    fun successRouteEncodesInvoiceNumberAsSinglePathSegment() {
        assertEquals("success/%23INV-0001", AppRoutes.success("#INV-0001"))
    }

    @Test
    fun successRouteEncodesReservedCharactersWithoutChangingInvoiceIdentity() {
        assertEquals("success/A%2FB%23C%3F%26D", AppRoutes.success("A/B#C?&D"))
    }

    @Test
    fun successRouteDoesNotTreatPlusAsSpace() {
        assertEquals("success/INV%2B0001", AppRoutes.success("INV+0001"))
    }

    @Test
    fun detailRouteAlsoEncodesInvoiceNumberAsSinglePathSegment() {
        assertEquals("detail/%23INV-0001", AppRoutes.detail("#INV-0001"))
    }
}
