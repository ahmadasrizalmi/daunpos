package com.daunpos.app.data

object PaymentCompletionPolicy {
    fun canComplete(method: PaymentMethod, total: Int, cashReceived: Int?, verifiedPaymentEvent: Boolean): Boolean {
        if (total <= 0) return false
        return when (method) {
            PaymentMethod.CASH -> cashReceived != null && cashReceived >= total
            PaymentMethod.QRIS, PaymentMethod.TRANSFER -> verifiedPaymentEvent
        }
    }

    fun status(method: PaymentMethod, total: Int, cashReceived: Int?, verifiedPaymentEvent: Boolean): String =
        if (canComplete(method, total, cashReceived, verifiedPaymentEvent)) "SUKSES" else "MENUNGGU_PEMBAYARAN"
}

object FinancialPolicy {
    fun normalizeDiscount(discount: Int, subtotal: Int): Int =
        discount.coerceIn(0, subtotal.coerceAtLeast(0))

    fun calculateTax(subtotal: Int, discount: Int, taxPercentage: Int): Int {
        if (taxPercentage !in 0..100) return 0
        val taxable = (subtotal - normalizeDiscount(discount, subtotal)).coerceAtLeast(0)
        return ((taxable.toLong() * taxPercentage) / 100L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }
}

object PinPolicy {
    fun requiresRotation(username: String, pin: String): Boolean =
        (username.equals("owner", ignoreCase = true) && pin == "0000") ||
            (username.equals("kasir", ignoreCase = true) && pin == "1234")

    fun isAcceptableNewPin(pin: String): Boolean =
        pin.length in 4..6 && pin.all(Char::isDigit) && pin != "0000" && pin != "1234"
}
