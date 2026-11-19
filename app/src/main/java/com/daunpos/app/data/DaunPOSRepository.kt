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

    val staffList: Flow<List<StoreStaff>> =
        db.storeStaffDao().observeAll().map { list -> list.map { it.toDomain() } }

    val receiptSettings: StateFlow<ReceiptSettings> = settingsStore.receipt
    val printerSettings: StateFlow<PrinterSettings> = settingsStore.printer
    val staticQris: StateFlow<String> = settingsStore.staticQris
    val isOnboardingCompleted: StateFlow<Boolean> = settingsStore.isOnboardingCompleted
    val storeProfile: StateFlow<StoreProfile> = settingsStore.storeProfile

    fun updateStaticQris(raw: String) = settingsStore.updateStaticQris(raw)

    suspend fun saveStoreProfile(profile: StoreProfile) {
        settingsStore.completeOnboarding(
            name = profile.storeName,
            category = profile.businessCategory,
            address = profile.address,
            phone = profile.phoneNumber,
            qris = profile.staticQris,
            isMultiUser = profile.isMultiUserEnabled
        )
        db.storeProfileDao().upsert(profile.toEntity())
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
        val profile = StoreProfile(
            storeName = name,
            businessCategory = category,
            address = address,
            phoneNumber = phone,
            taxPercentage = category.defaultTax,
            staticQris = staticQris,
            isMultiUserEnabled = isMultiUser,
            isOnboardingCompleted = true
        )
        saveStoreProfile(profile)

        // Update Owner PIN
        if (ownerPin.isNotBlank()) {
            val salt = PinHasher.generateSalt()
            val hash = PinHasher.hash(ownerPin, salt)
            db.userDao().deleteByUsername("owner")
            db.userDao().insert(com.daunpos.app.data.local.UserEntity("owner", hash, salt, UserRole.OWNER.name))
        }

        // Update or insert Cashier PIN if multi-user
        if (isMultiUser && !cashierPin.isNullOrBlank()) {
            val salt = PinHasher.generateSalt()
            val hash = PinHasher.hash(cashierPin, salt)
            db.userDao().deleteByUsername("kasir")
            db.userDao().insert(com.daunpos.app.data.local.UserEntity("kasir", hash, salt, UserRole.KASIR.name))
        }

        // Replace product catalog with industry-specific catalog
        seedCategoryProducts(category)
    }

    suspend fun seedCategoryProducts(category: BusinessCategory) {
        val existing = db.productDao().getAll()
        existing.forEach { db.productDao().deleteById(it.id) }

        val templateProducts = when (category) {
            BusinessCategory.FNB -> listOf(
                Product("P001", "Es Kopi Daun", "Kopi", 22000, 8000, true, 45, "", 0, "☕", 0xFFE7F4EA, "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P002", "Americano", "Kopi", 20000, 7000, true, 30, "", 0, "🫘", 0xFFEFE7D6, "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P003", "Cappuccino", "Kopi", 28000, 10000, true, 25, "", 0, "🥛", 0xFFE7F4EA, "https://images.unsplash.com/photo-1587080413959-06b859fb107d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P004", "Cafe Latte", "Kopi", 30000, 11000, true, 35, "", 0, "☕", 0xFFF2E9DE, "https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P005", "Matcha Latte", "Non-Kopi", 28000, 10000, true, 20, "", 0, "🍵", 0xFFE8F6ED, "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P006", "Es Teh Manis", "Non-Kopi", 12000, 3000, true, 80, "", 0, "🧋", 0xFFE8F1FA, "https://images.unsplash.com/photo-1556679343-c7306c1976bc?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P007", "Nasi Goreng Spesial", "Makanan Berat", 35000, 15000, true, 15, "", 0, "🍛", 0xFFFBE9DC, "https://images.unsplash.com/photo-1603133872878-684f208fb84b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P008", "Butter Croissant", "Snack", 22000, 9000, true, 20, "", 0, "🥐", 0xFFF5EFDF, "https://images.unsplash.com/photo-1555507036-ab1f4038808a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400")
            )
            BusinessCategory.RETAIL -> listOf(
                Product("P001", "Kaos Polos Combed 30s", "Pakaian", 65000, 35000, true, 40, "899123456001", 0, "👕", 0xFFE7F4EA, "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P002", "Celana Chino Slim Fit", "Pakaian", 135000, 75000, true, 25, "899123456002", 0, "👖", 0xFFEFE7D6, "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P003", "Minuman Isotonik 500ml", "Minuman", 7500, 5000, true, 100, "899123456003", 0, "🥤", 0xFFE8F1FA, "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P004", "Kopi Kaleng Cold Brew", "Minuman", 15000, 9500, true, 40, "899123456004", 0, "☕", 0xFFF2E9DE, "https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P005", "Keripik Kentang Balado 85g", "Snack", 12500, 8000, true, 60, "899123456005", 0, "🍟", 0xFFFBF3D9, "https://images.unsplash.com/photo-1566478989037-eec170784d0b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P006", "Roti Sobek Cokelat Keju", "Snack", 16000, 11000, true, 25, "899123456006", 0, "🍞", 0xFFFBE9DC, "https://images.unsplash.com/photo-1509440159596-0249088772ff?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P007", "Baterai Alkaline AA Isi 4", "Aksesoris", 28000, 19000, true, 45, "899123456007", 0, "🔋", 0xFFEFE3DC, "https://images.unsplash.com/photo-1619725002198-6a689b72f41d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400")
            )
            BusinessCategory.SERVICE -> listOf(
                Product("P001", "Sesi Foto Studio (30 Mnt)", "Studio Foto", 150000, 0, true, 999, "", 30, "📸", 0xFFE7F4EA, "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P002", "Paket Wisuda / Grup (50 Mnt)", "Studio Foto", 250000, 0, true, 999, "", 50, "🎓", 0xFFEFE7D6, "https://images.unsplash.com/photo-1523050854058-8df90110c9f1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P003", "Cetak Foto + Frame 10R", "Cetak & Cetakan", 50000, 20000, true, 999, "FOTO10R", 0, "🖼️", 0xFFFBF3D9, "https://images.unsplash.com/photo-1513519245088-0e12902e5a38?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P004", "Gentlemen Haircut + Cuci", "Haircut", 45000, 0, true, 999, "", 30, "✂️", 0xFFE7F4EA, "https://images.unsplash.com/photo-1585747860715-2ba37e788b70?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P005", "Haircut Premium + Pijat Bahu", "Haircut", 65000, 0, true, 999, "", 45, "💈", 0xFFEFE7D6, "https://images.unsplash.com/photo-1503951914875-452162b0f3f1?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P006", "Cuci Rambut + Creambath Herbal", "Treatment", 55000, 10000, true, 999, "", 45, "💆", 0xFFE8F1FA, "https://images.unsplash.com/photo-1519699047748-de8e457a634e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"),
                Product("P007", "Servis Ringan / Pengecekan", "Servis & Reparasi", 75000, 0, true, 999, "SRV01", 60, "🛠️", 0xFFEFE3DC, "https://images.unsplash.com/photo-1581092160607-ee22621dd758?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400")
            )
        }

        templateProducts.forEach { upsertProduct(it) }

        // Seed default staff for SERVICE if empty
        if (category == BusinessCategory.SERVICE && db.storeStaffDao().getAll().isEmpty()) {
            db.storeStaffDao().upsert(com.daunpos.app.data.local.StoreStaffEntity("STF-1", "Dimas (Fotografer / Stylist)", "STAFF", "", "", true))
            db.storeStaffDao().upsert(com.daunpos.app.data.local.StoreStaffEntity("STF-2", "Sarah (Editor / Terapis)", "STAFF", "", "", true))
            db.storeStaffDao().upsert(com.daunpos.app.data.local.StoreStaffEntity("STF-3", "Budi (Teknisi / Operator)", "STAFF", "", "", true))
        }
    }

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

    suspend fun findProductByBarcodeOrId(code: String): Product? =
        db.productDao().getByBarcodeOrId(code.trim())?.toDomain()

    suspend fun addStaff(name: String, role: String = "STAFF"): StoreStaff {
        val id = "STF-${System.currentTimeMillis()}"
        val entity = com.daunpos.app.data.local.StoreStaffEntity(id = id, name = name, role = role, pinSalt = "", pinHash = "", isActive = true)
        db.storeStaffDao().upsert(entity)
        return entity.toDomain()
    }

    suspend fun deleteStaff(id: String) {
        db.storeStaffDao().deleteById(id)
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
