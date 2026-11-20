package com.daunpos.app

import com.daunpos.app.data.BankAccountSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankAccountSettingsTest {

    @Test
    fun completeAccountIsConfigured() {
        val account = BankAccountSettings(
            bankName = "BCA",
            accountNumber = "1234567890",
            accountHolder = "Dhea Store"
        )

        assertTrue(account.isConfigured)
        assertEquals(null, account.validationError())
    }

    @Test
    fun missingBankNameIsRejected() {
        val account = BankAccountSettings(
            bankName = " ",
            accountNumber = "1234567890",
            accountHolder = "Dhea Store"
        )

        assertFalse(account.isConfigured)
        assertEquals("Nama bank wajib diisi.", account.validationError())
    }

    @Test
    fun accountNumberMustContainFiveToThirtyDigits() {
        val shortNumber = BankAccountSettings("BCA", "1234", "Dhea Store")
        val nonNumeric = BankAccountSettings("BCA", "12345A", "Dhea Store")
        val validNumber = BankAccountSettings("BCA", "12345", "Dhea Store")

        assertFalse(shortNumber.isConfigured)
        assertFalse(nonNumeric.isConfigured)
        assertTrue(validNumber.isConfigured)
    }

    @Test
    fun accountHolderIsRequired() {
        val account = BankAccountSettings(
            bankName = "BCA",
            accountNumber = "1234567890",
            accountHolder = ""
        )

        assertFalse(account.isConfigured)
        assertEquals("Nama pemilik rekening wajib diisi.", account.validationError())
    }
}
