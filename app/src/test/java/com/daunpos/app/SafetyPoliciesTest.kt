package com.daunpos.app

import com.daunpos.app.data.FinancialPolicy
import com.daunpos.app.data.PaymentCompletionPolicy
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.PinPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyPoliciesTest {
    @Test fun cashRequiresSufficientReceivedAmount() {
        assertTrue(PaymentCompletionPolicy.canComplete(PaymentMethod.CASH, 25000, 30000, false))
        assertFalse(PaymentCompletionPolicy.canComplete(PaymentMethod.CASH, 25000, 20000, false))
        assertFalse(PaymentCompletionPolicy.canComplete(PaymentMethod.CASH, 0, 0, false))
        assertFalse(PaymentCompletionPolicy.canComplete(PaymentMethod.CASH, 25000, null, false))
    }

    @Test fun nonCashRequiresExplicitVerification() {
        assertFalse(PaymentCompletionPolicy.canComplete(PaymentMethod.QRIS, 25000, null, false))
        assertFalse(PaymentCompletionPolicy.canComplete(PaymentMethod.TRANSFER, 25000, null, false))
        assertTrue(PaymentCompletionPolicy.canComplete(PaymentMethod.QRIS, 25000, null, true))
        assertTrue(PaymentCompletionPolicy.canComplete(PaymentMethod.TRANSFER, 25000, null, true))
    }

    @Test fun unverifiedPaymentNeverGetsSuccessStatus() {
        assertEquals("MENUNGGU_PEMBAYARAN", PaymentCompletionPolicy.status(PaymentMethod.QRIS, 25000, null, false))
        assertEquals("SUKSES", PaymentCompletionPolicy.status(PaymentMethod.CASH, 25000, 25000, false))
    }

    @Test fun financialPolicyClampsDiscountAndCalculatesTaxOnNetSubtotal() {
        assertEquals(10000, FinancialPolicy.normalizeDiscount(12000, 10000))
        assertEquals(900, FinancialPolicy.calculateTax(10000, 1000, 10))
        assertEquals(0, FinancialPolicy.calculateTax(10000, 1000, 101))
    }

    @Test fun defaultPinsRequireRotationAndNewPinsMustBeFourToSixDigits() {
        assertTrue(PinPolicy.requiresRotation("owner", "0000"))
        assertTrue(PinPolicy.requiresRotation("kasir", "1234"))
        assertFalse(PinPolicy.isAcceptableNewPin("0000"))
        assertFalse(PinPolicy.isAcceptableNewPin("1234"))
        assertFalse(PinPolicy.isAcceptableNewPin("12"))
        assertFalse(PinPolicy.isAcceptableNewPin("1234567"))
        assertFalse(PinPolicy.isAcceptableNewPin("12a4"))
        assertTrue(PinPolicy.isAcceptableNewPin("8462"))
        assertTrue(PinPolicy.isAcceptableNewPin("846291"))
    }
}
