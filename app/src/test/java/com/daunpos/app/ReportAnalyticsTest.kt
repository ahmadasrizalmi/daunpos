package com.daunpos.app

import com.daunpos.app.data.OrderItem
import com.daunpos.app.data.PaymentMethod
import com.daunpos.app.data.ReportAnalytics
import com.daunpos.app.data.Transaction
import com.daunpos.app.data.formatRupiah
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportAnalyticsTest {
    private val utc = TimeZone.getTimeZone("UTC")

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(utc).apply {
            set(year, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun transaction(
        id: String,
        dateTime: Long,
        total: Int,
        method: PaymentMethod = PaymentMethod.CASH,
        status: String = "SUKSES",
        quantity: Int = 1,
        costPrice: Int? = null
    ) = Transaction(
        id = id,
        dateTime = dateTime,
        items = listOf(
            OrderItem("P001", "Produk", 1000, quantity, "📦", costPrice = costPrice)
        ),
        subtotal = total,
        total = total,
        paymentMethod = method,
        status = status
    )

    @Test
    fun summaryExcludesCanceledAndPendingTransactionsFromRevenueAndUnits() {
        val summary = ReportAnalytics.summarize(
            listOf(
                transaction("A", at(2026, 11, 20, 9), 20000, quantity = 2),
                transaction("B", at(2026, 11, 20, 10), 5000, quantity = 1),
                transaction("C", at(2026, 11, 20, 11), 100000, PaymentMethod.QRIS, "BATAL", 4),
                transaction("D", at(2026, 11, 20, 12), 80000, PaymentMethod.TRANSFER, "MENUNGGU_PEMBAYARAN", 3)
            )
        )

        assertEquals(2, summary.successfulTransactions)
        assertEquals(1, summary.cancelledTransactions)
        assertEquals(1, summary.pendingTransactions)
        assertEquals(3L, summary.itemsSold)
        assertEquals(25000L, summary.revenue)
        assertEquals(null, summary.grossProfit)
        assertEquals(2, summary.transactionsWithoutCost)
        assertEquals(2, summary.paymentTotals.getValue(PaymentMethod.CASH).count)
        assertEquals(25000L, summary.paymentTotals.getValue(PaymentMethod.CASH).amount)
        assertEquals(0L, summary.paymentTotals.getValue(PaymentMethod.QRIS).amount)
    }

    @Test
    fun grossProfitUsesCheckoutCostSnapshotsAndExcludesIncompleteHistory() {
        val complete = ReportAnalytics.summarize(
            listOf(
                transaction("A", at(2026, 11, 20), 20000, quantity = 2, costPrice = 500),
                transaction("B", at(2026, 11, 20), 5000, quantity = 1, costPrice = 1000)
            )
        )
        assertEquals(23000L, complete.grossProfit)
        assertEquals(0, complete.transactionsWithoutCost)

        val partial = ReportAnalytics.summarize(
            listOf(
                transaction("A", at(2026, 11, 20), 20000, quantity = 2, costPrice = 500),
                transaction("OLD", at(2026, 11, 20), 5000)
            )
        )
        assertEquals(null, partial.grossProfit)
        assertEquals(1, partial.transactionsWithoutCost)
    }

    @Test
    fun rupiahFormattingDoesNotClampLargeReportTotalsToIntRange() {
        assertEquals("Rp 3.000.000.000", formatRupiah(3_000_000_000L))
    }

    @Test
    fun dailySalesUsesFullCalendarDateAndCombinesTransactionsWithinOneDay() {
        val buckets = ReportAnalytics.dailySales(
            listOf(
                transaction("A", at(2026, 11, 20, 9), 20000),
                transaction("B", at(2026, 11, 20, 17), 5000),
                transaction("C", at(2026, 11, 21, 9), 30000),
                transaction("D", at(2026, 12, 20, 9), 40000),
                transaction("E", at(2026, 11, 20, 11), 90000, status = "BATAL")
            ),
            utc
        )

        assertEquals(3, buckets.size)
        assertEquals(at(2026, 11, 20, 0), buckets[0].dayStartMillis)
        assertEquals(25000L, buckets[0].amount)
        assertEquals(2, buckets[0].transactionCount)
        assertEquals(at(2026, 11, 21, 0), buckets[1].dayStartMillis)
        assertEquals(at(2026, 12, 20, 0), buckets[2].dayStartMillis)
    }
}
