package com.daunpos.app.data

import java.util.Calendar
import java.util.TimeZone

data class PaymentTotals(
    val count: Int,
    val amount: Long
)

data class ReportSummary(
    val successfulTransactions: Int,
    val cancelledTransactions: Int,
    val pendingTransactions: Int,
    val itemsSold: Long,
    val revenue: Long,
    val grossProfit: Long?,
    val transactionsWithoutCost: Int,
    val paymentTotals: Map<PaymentMethod, PaymentTotals>
)

data class DailySalesBucket(
    val dayStartMillis: Long,
    val amount: Long,
    val transactionCount: Int
)

object ReportAnalytics {
    fun summarize(transactions: List<Transaction>): ReportSummary {
        val successful = transactions.filter { it.status == "SUKSES" }
        val payments = PaymentMethod.entries.associateWith { method ->
            val matching = successful.filter { it.paymentMethod == method }
            PaymentTotals(
                count = matching.size,
                amount = matching.sumOf { it.total.toLong() }
            )
        }

        // Never infer historical HPP from today's product master: cost basis is snapshotted at checkout.
        val completeCostTransactions = successful.filter { tx ->
            tx.items.isNotEmpty() && tx.items.all { it.costPrice != null && it.costPrice >= 0 }
        }
        val transactionsWithoutCost = successful.size - completeCostTransactions.size
        val grossProfit = if (transactionsWithoutCost == 0) {
            completeCostTransactions.sumOf { tx ->
                val costOfGoods = tx.items.sumOf { item ->
                    item.costPrice!!.toLong() * item.quantity.toLong()
                }
                (tx.subtotal.toLong() - tx.discount.toLong()) - costOfGoods
            }
        } else {
            null
        }

        return ReportSummary(
            successfulTransactions = successful.size,
            cancelledTransactions = transactions.count { it.status == "BATAL" },
            pendingTransactions = transactions.count { it.status != "SUKSES" && it.status != "BATAL" },
            itemsSold = successful.sumOf { tx -> tx.items.sumOf { it.quantity.toLong() } },
            revenue = successful.sumOf { (it.subtotal - it.discount).toLong() },
            grossProfit = grossProfit,
            transactionsWithoutCost = transactionsWithoutCost,
            paymentTotals = payments
        )
    }

    fun dailySales(
        transactions: List<Transaction>,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<DailySalesBucket> {
        return transactions
            .asSequence()
            .filter { it.status == "SUKSES" }
            .groupBy { transaction ->
                Calendar.getInstance(timeZone).apply {
                    timeInMillis = transaction.dateTime
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            .map { (dayStart, dayTransactions) ->
                DailySalesBucket(
                    dayStartMillis = dayStart,
                    amount = dayTransactions.sumOf { (it.subtotal - it.discount).toLong() },
                    transactionCount = dayTransactions.size
                )
            }
            .sortedBy { it.dayStartMillis }
    }
}
