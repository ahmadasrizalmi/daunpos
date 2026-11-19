package com.daunpos.app.data

import com.daunpos.app.data.local.DaunPOSDatabase
import com.daunpos.app.data.local.ShiftEntity
import com.daunpos.app.data.local.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class DaunPOSRepository(
    private val db: DaunPOSDatabase,
    private val settingsStore: SettingsStore
) {
    val products: Flow<List<Product>> =
        db.productDao().observeAll().map { list -> list.map { it.toDomain() } }

    val transactions: Flow<List<Transaction>> =
        db.transactionDao().observeAll().map { list -> list.map { it.toDomain() } }

    val shifts: Flow<List<ShiftRecord>> =
        db.shiftDao().observeAll().map { list -> list.map { it.toDomain() } }

    val receiptSettings: StateFlow<ReceiptSettings> = settingsStore.receipt
    val printerSettings: StateFlow<PrinterSettings> = settingsStore.printer
    val staticQris: StateFlow<String> = settingsStore.staticQris

    fun updateStaticQris(raw: String) = settingsStore.updateStaticQris(raw)

    suspend fun upsertProduct(product: Product) = db.productDao().upsert(product.toEntity())

    suspend fun deleteProduct(id: String) = db.productDao().deleteById(id)

    suspend fun toggleProductAvailable(id: String) {
        val all = db.productDao().getAll()
        val target = all.find { it.id == id } ?: return
        db.productDao().upsert(target.copy(available = !target.available))
    }

    suspend fun nextProductId(): String {
        val all = db.productDao().getAll()
        val max = all.mapNotNull { it.id.removePrefix("P").toIntOrNull() }.maxOrNull() ?: 0
        return "P" + (max + 1).toString().padStart(3, '0')
    }

    suspend fun insertTransaction(tx: Transaction) {
        // 1. Simpan transaksi
        db.transactionDao().insert(tx.toEntity())

        // 2. Potong stok otomatis untuk setiap item
        tx.items.forEach { item ->
            db.productDao().deductStock(item.productId, item.quantity)
            // Jika stok habis, nonaktifkan otomatis
            val updated = db.productDao().getAll().find { it.id == item.productId }
            if (updated != null && updated.stock <= 0) {
                db.productDao().setAvailable(item.productId, false)
            }
        }
    }

    suspend fun cancelTransaction(id: String) = db.transactionDao().updateStatus(id, "BATAL")

    suspend fun authenticate(username: String, pin: String): User? {
        val entity = db.userDao().getByUsername(username) ?: return null
        return if (PinHasher.verify(pin, entity.pinSalt, entity.pinHash)) {
            User(username = entity.username, role = UserRole.valueOf(entity.role))
        } else {
            null
        }
    }

    suspend fun listTransactions(): List<Transaction> =
        db.transactionDao().getAll().map { it.toDomain() }

    fun nextInvoiceNumber(): String = settingsStore.nextInvoiceNumber()

    fun updateReceiptSettings(settings: ReceiptSettings) = settingsStore.updateReceipt(settings)

    fun updatePrinterSettings(settings: PrinterSettings) = settingsStore.updatePrinter(settings)

    // Shift management
    suspend fun getActiveShift(): ShiftRecord? = db.shiftDao().getActiveShift()?.toDomain()

    suspend fun openShift(cashierName: String, startCash: Int): ShiftRecord {
        val shift = ShiftRecord(
            id = "SH-${System.currentTimeMillis()}",
            cashierName = cashierName,
            startTime = System.currentTimeMillis(),
            startCash = startCash,
            status = "OPEN"
        )
        db.shiftDao().upsert(shift.toEntity())
        return shift
    }

    suspend fun closeShift(shiftId: String, endCash: Int, transactions: List<Transaction>): ShiftRecord? {
        val current = db.shiftDao().getActiveShift() ?: return null
        val cashSales = transactions.filter { it.paymentMethod == PaymentMethod.CASH && it.status == "SUKSES" }.sumOf { it.total }
        val qrisSales = transactions.filter { it.paymentMethod == PaymentMethod.QRIS && it.status == "SUKSES" }.sumOf { it.total }
        val transferSales = transactions.filter { it.paymentMethod == PaymentMethod.TRANSFER && it.status == "SUKSES" }.sumOf { it.total }

        val closed = current.copy(
            endTime = System.currentTimeMillis(),
            endCash = endCash,
            totalCashSales = cashSales,
            totalQrisSales = qrisSales,
            totalTransferSales = transferSales,
            totalTransactions = transactions.size,
            status = "CLOSED"
        )
        db.shiftDao().upsert(closed)
        return closed.toDomain()
    }
}
