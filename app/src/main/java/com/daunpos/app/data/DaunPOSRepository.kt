package com.daunpos.app.data

import com.daunpos.app.data.local.DaunPOSDatabase
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

    val receiptSettings: StateFlow<ReceiptSettings> = settingsStore.receipt
    val printerSettings: StateFlow<PrinterSettings> = settingsStore.printer

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

    suspend fun insertTransaction(tx: Transaction) = db.transactionDao().insert(tx.toEntity())

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
}
