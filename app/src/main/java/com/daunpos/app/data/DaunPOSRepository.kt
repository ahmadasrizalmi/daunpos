package com.daunpos.app.data

import androidx.room.withTransaction
import com.daunpos.app.data.local.DaunPOSDatabase
import com.daunpos.app.data.local.StoreStaffEntity
import com.daunpos.app.data.local.UserEntity
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

    val staffList: Flow<List<StoreStaff>> =
        db.storeStaffDao().observeAll().map { list -> list.map { it.toDomain() } }

    val receiptSettings: StateFlow<ReceiptSettings> = settingsStore.receipt
    val printerSettings: StateFlow<PrinterSettings> = settingsStore.printer
    val staticQris: StateFlow<String> = settingsStore.staticQris
    val bankAccountSettings: StateFlow<BankAccountSettings> = settingsStore.bankAccount
    val isOnboardingCompleted: StateFlow<Boolean> = settingsStore.isOnboardingCompleted
    val storeProfile: StateFlow<StoreProfile> = settingsStore.storeProfile

    fun updateStaticQris(raw: String) = settingsStore.updateStaticQris(raw)

    fun updateBankAccountSettings(settings: BankAccountSettings) =
        settingsStore.updateBankAccount(settings)

    suspend fun saveStoreProfile(profile: StoreProfile) {
        db.storeProfileDao().upsert(profile.toEntity())
        settingsStore.completeOnboarding(
            name = profile.storeName,
            category = profile.businessCategory,
            address = profile.address,
            phone = profile.phoneNumber,
            qris = profile.staticQris,
            isMultiUser = profile.isMultiUserEnabled
        )
    }

    suspend fun completeOnboarding(
        name: String,
        category: BusinessCategory,
        address: String,
        phone: String,
        staticQris: String,
        isMultiUser: Boolean,
        ownerPin: String,
        cashierPin: String? = null
    ) {
        require(name.isNotBlank()) { "Nama toko wajib diisi." }
        require(PinPolicy.isAcceptableNewPin(ownerPin)) { "PIN pemilik harus 4–6 digit dan bukan PIN bawaan." }
        if (isMultiUser) {
            require(cashierPin != null && PinPolicy.isAcceptableNewPin(cashierPin)) {
                "PIN kasir harus 4–6 digit dan bukan PIN bawaan."
            }
        }

        val profile = StoreProfile(
            storeName = name.trim(),
            businessCategory = category,
            address = address.trim(),
            phoneNumber = phone.trim(),
            taxPercentage = 0,
            staticQris = staticQris.trim(),
            isMultiUserEnabled = isMultiUser,
            isOnboardingCompleted = true
        )

        val ownerSalt = PinHasher.generateSalt()
        val ownerUser = UserEntity(
            username = "owner",
            pinHash = PinHasher.hash(ownerPin, ownerSalt),
            pinSalt = ownerSalt,
            role = UserRole.OWNER.name
        )

        val cashierUser = if (isMultiUser) {
            val cashierSalt = PinHasher.generateSalt()
            UserEntity(
                username = "kasir",
                pinHash = PinHasher.hash(requireNotNull(cashierPin), cashierSalt),
                pinSalt = cashierSalt,
                role = UserRole.KASIR.name
            )
        } else null

        db.withTransaction {
            db.storeProfileDao().upsert(profile.toEntity())
            db.userDao().upsert(ownerUser)
            if (cashierUser != null) {
                db.userDao().upsert(cashierUser)
            } else {
                db.userDao().deleteByUsername("kasir")
            }
        }

        settingsStore.completeOnboarding(
            name = profile.storeName,
            category = category,
            address = profile.address,
            phone = profile.phoneNumber,
            qris = profile.staticQris,
            isMultiUser = isMultiUser
        )
    }

    suspend fun upsertProduct(product: Product) {
        require(product.id.isNotBlank()) { "ID produk wajib diisi." }
        require(product.name.isNotBlank()) { "Nama produk wajib diisi." }
        require(product.category.isNotBlank()) { "Kategori produk wajib diisi." }
        require(product.price > 0) { "Harga jual harus lebih besar dari nol." }
        require(product.costPrice >= 0) { "Harga modal tidak boleh negatif." }
        require(product.stock >= 0) { "Stok tidak boleh negatif." }
        require(product.durationMinutes >= 0) { "Durasi tidak boleh negatif." }
        db.productDao().upsert(product.toEntity())
    }

    suspend fun deleteProduct(id: String) = db.productDao().deleteById(id)

    suspend fun toggleProductAvailable(id: String, trackStock: Boolean = true) {
        val target = db.productDao().getById(id) ?: return
        db.productDao().upsert(
            target.copy(available = !target.available && (!trackStock || target.stock > 0))
        )
    }

    suspend fun nextProductId(): String {
        val all = db.productDao().getAll()
        val max = all.mapNotNull { it.id.removePrefix("P").toIntOrNull() }.maxOrNull() ?: 0
        return "P" + (max + 1).toString().padStart(3, '0')
    }

    suspend fun findProductByBarcodeOrId(code: String): Product? =
        db.productDao().getByBarcodeOrId(code.trim())?.toDomain()

    suspend fun addStaff(name: String, role: String = "STAFF"): StoreStaff {
        require(name.isNotBlank()) { "Nama staf wajib diisi." }
        val id = "STF-${System.currentTimeMillis()}"
        val entity = StoreStaffEntity(
            id = id,
            name = name.trim(),
            role = role,
            pinSalt = "-",
            pinHash = "NO_LOGIN",
            isActive = true
        )
        db.storeStaffDao().upsert(entity)
        return entity.toDomain()
    }

    suspend fun deleteStaff(id: String) {
        db.storeStaffDao().deleteById(id)
    }

    suspend fun insertTransaction(tx: Transaction, trackStock: Boolean = true) {
        require(tx.items.isNotEmpty()) { "Keranjang transaksi kosong." }
        require(tx.total > 0) { "Total transaksi harus lebih besar dari nol." }
        require(tx.items.all { it.productId.isNotBlank() && it.quantity > 0 && it.price > 0 }) {
            "Rincian item transaksi tidak valid."
        }

        val calculatedSubtotal = tx.items.sumOf { it.price.toLong() * it.quantity.toLong() }
        require(calculatedSubtotal <= Int.MAX_VALUE && calculatedSubtotal.toInt() == tx.subtotal) {
            "Subtotal berubah. Periksa kembali keranjang."
        }
        require(tx.discount == FinancialPolicy.normalizeDiscount(tx.discount, tx.subtotal)) {
            "Diskon berada di luar rentang subtotal."
        }
        require(tx.total.toLong() == tx.subtotal.toLong() - tx.discount.toLong() + tx.tax.toLong()) {
            "Total transaksi tidak konsisten."
        }

        db.withTransaction {
            check(db.transactionDao().getById(tx.id) == null) { "Nomor transaksi sudah digunakan." }

            val quantities = tx.items.groupBy { it.productId }
                .mapValues { (_, items) -> items.sumOf { it.quantity } }

            quantities.forEach { (productId, quantity) ->
                val product = db.productDao().getById(productId)
                    ?: error("Produk ${productId} tidak ditemukan. Muat ulang keranjang.")
                check(product.available) {
                    "${product.name} sedang dinonaktifkan."
                }
                if (trackStock) {
                    check(product.stock >= quantity) {
                        "Stok ${product.name} tidak mencukupi. Stok tersedia: ${product.stock}."
                    }
                }
                val transactionPrice = tx.items.first { it.productId == productId }.price
                check(product.price == transactionPrice) {
                    "Harga ${product.name} berubah. Muat ulang keranjang sebelum membayar."
                }
            }

            db.transactionDao().insert(tx.toEntity())

            if (trackStock) {
                quantities.forEach { (productId, quantity) ->
                    check(db.productDao().deductStock(productId, quantity) == 1) {
                        "Stok berubah saat checkout. Transaksi dibatalkan."
                    }
                    val updated = db.productDao().getById(productId)
                    if (updated != null && updated.stock == 0) {
                        db.productDao().setAvailable(productId, false)
                    }
                }
            }
        }
    }

    suspend fun getTransaction(id: String): Transaction? =
        db.transactionDao().getById(id)?.toDomain()

    suspend fun cancelTransaction(id: String, restoreStock: Boolean = true) {
        db.withTransaction {
            val entity = db.transactionDao().getById(id) ?: return@withTransaction
            if (entity.status != "SUKSES") return@withTransaction

            val transaction = entity.toDomain()
            db.transactionDao().updateStatus(id, "BATAL")
            if (restoreStock) {
                transaction.items.groupBy { it.productId }
                    .forEach { (productId, items) ->
                        db.productDao().restoreStock(productId, items.sumOf { it.quantity })
                    }
            }
        }
    }

    suspend fun authenticate(username: String, pin: String): User? {
        val entity = db.userDao().getByUsername(username) ?: return null
        if (!PinHasher.verify(pin, entity.pinSalt, entity.pinHash)) return null
        return User(
            username = entity.username,
            role = UserRole.valueOf(entity.role),
            requiresPinChange = PinPolicy.requiresRotation(entity.username, pin)
        )
    }

    suspend fun changePin(username: String, newPin: String) {
        require(PinPolicy.isAcceptableNewPin(newPin)) { "PIN baru harus 4–6 digit dan tidak boleh memakai PIN bawaan." }
        val current = db.userDao().getByUsername(username) ?: error("Pengguna tidak ditemukan.")
        val salt = PinHasher.generateSalt()
        db.userDao().upsert(
            current.copy(
                pinSalt = salt,
                pinHash = PinHasher.hash(newPin, salt)
            )
        )
    }

    suspend fun listTransactions(): List<Transaction> =
        db.transactionDao().getAll().map { it.toDomain() }

    fun nextInvoiceNumber(): String = settingsStore.nextInvoiceNumber()

    fun updateReceiptSettings(settings: ReceiptSettings) = settingsStore.updateReceipt(settings)

    fun updatePrinterSettings(settings: PrinterSettings) = settingsStore.updatePrinter(settings)

    suspend fun getActiveShift(): ShiftRecord? = db.shiftDao().getActiveShift()?.toDomain()

    suspend fun openShift(cashierName: String, startCash: Int): ShiftRecord {
        require(startCash >= 0) { "Saldo awal kas tidak boleh negatif." }
        val active = db.shiftDao().getActiveShift()
        check(active == null) { "Masih ada shift yang belum ditutup." }
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
        require(endCash >= 0) { "Saldo akhir kas tidak boleh negatif." }
        val current = db.shiftDao().getActiveShift()?.toDomain() ?: return null
        if (current.id != shiftId) return null
        val closedAt = System.currentTimeMillis()
        val shiftTransactions = transactions.filter {
            it.dateTime in current.startTime..closedAt && it.status == "SUKSES"
        }
        val cashSales = shiftTransactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.total }
        val qrisSales = shiftTransactions.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.total }
        val transferSales = shiftTransactions.filter { it.paymentMethod == PaymentMethod.TRANSFER }.sumOf { it.total }

        val closed = current.copy(
            endTime = closedAt,
            endCash = endCash,
            totalCashSales = cashSales,
            totalQrisSales = qrisSales,
            totalTransferSales = transferSales,
            totalTransactions = shiftTransactions.size,
            status = "CLOSED"
        )
        db.shiftDao().upsert(closed.toEntity())
        return closed
    }
}
