package com.daunpos.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.daunpos.app.data.PinHasher
import com.daunpos.app.data.UserRole
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        UserEntity::class,
        ShiftEntity::class,
        StoreProfileEntity::class,
        StoreStaffEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class DaunPOSDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao
    abstract fun shiftDao(): ShiftDao
    abstract fun storeProfileDao(): StoreProfileDao
    abstract fun storeStaffDao(): StoreStaffDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alter products
                db.execSQL("ALTER TABLE products ADD COLUMN stock INTEGER NOT NULL DEFAULT 50")
                db.execSQL("ALTER TABLE products ADD COLUMN imageUrl TEXT DEFAULT NULL")

                // 2. Alter transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN discount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN tableNumber TEXT NOT NULL DEFAULT 'Take Away'")

                // 3. Create shifts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shifts` (
                        `id` TEXT NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER,
                        `startCash` INTEGER NOT NULL,
                        `endCash` INTEGER,
                        `totalCashSales` INTEGER NOT NULL DEFAULT 0,
                        `totalQrisSales` INTEGER NOT NULL DEFAULT 0,
                        `totalTransferSales` INTEGER NOT NULL DEFAULT 0,
                        `totalTransactions` INTEGER NOT NULL DEFAULT 0,
                        `status` TEXT NOT NULL DEFAULT 'OPEN',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Alter products for retail & service support
                db.execSQL("ALTER TABLE products ADD COLUMN costPrice INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE products ADD COLUMN barcodeSku TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 0")

                // 2. Create store_profile table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `store_profile` (
                        `id` INTEGER NOT NULL,
                        `storeName` TEXT NOT NULL,
                        `businessType` TEXT NOT NULL,
                        `address` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `logoUri` TEXT,
                        `taxPercentage` INTEGER NOT NULL DEFAULT 10,
                        `staticQrisRaw` TEXT NOT NULL DEFAULT '',
                        `merchantNmid` TEXT NOT NULL DEFAULT '',
                        `isMultiUserEnabled` INTEGER NOT NULL DEFAULT 0,
                        `isAutoVerificationEnabled` INTEGER NOT NULL DEFAULT 1,
                        `isOnboardingCompleted` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // 3. Create store_staff table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `store_staff` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `pinSalt` TEXT NOT NULL,
                        `pinHash` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // 4. Seed initial store profile row if absent
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO store_profile 
                    (id, storeName, businessType, address, phoneNumber, logoUri, taxPercentage, staticQrisRaw, merchantNmid, isMultiUserEnabled, isAutoVerificationEnabled, isOnboardingCompleted) 
                    VALUES (1, 'DaunPOS Coffee & Eatery', 'FNB', 'Jl. Daun Hijau No. 8, Bandung', '0812-3456-7890', NULL, 10, '', '', 0, 1, 0)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        fun build(context: Context): DaunPOSDatabase =
            Room.databaseBuilder(context, DaunPOSDatabase::class.java, "daunpos.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3)
                .addCallback(SeedCallback)
                .build()
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            seedStoreProfile(db)
            seedProducts(db)
            seedUsers(db)
            seedTransactions(db)
        }

        private fun seedStoreProfile(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO store_profile (id, storeName, businessType, address, phoneNumber, logoUri, taxPercentage, staticQrisRaw, merchantNmid, isMultiUserEnabled, isAutoVerificationEnabled, isOnboardingCompleted) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("DaunPOS Coffee & Eatery", "FNB", "Jl. Daun Hijau No. 8, Bandung", "0812-3456-7890", null, 10, "", "", 0, 1, 0)
            )
        }

        private fun seedProducts(db: SupportSQLiteDatabase) {
            val products = listOf(
                // id, name, category, price, available, stock, emoji, colorHex, imageUrl
                arrayOf<Any?>(
                    "P001", "Es Kopi Daun", "Kopi", 22000, 1, 45, "☕", 0xFFE7F4EA,
                    "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P002", "Americano", "Kopi", 20000, 1, 30, "🫘", 0xFFEFE7D6,
                    "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P003", "Cappuccino", "Kopi", 28000, 1, 25, "🥛", 0xFFE7F4EA,
                    "https://images.unsplash.com/photo-1587080413959-06b859fb107d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P004", "Cafe Latte", "Kopi", 30000, 1, 35, "☕", 0xFFF2E9DE,
                    "https://images.unsplash.com/photo-1570968915860-54d5c301fa9f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P005", "Matcha Latte", "Non-Kopi", 28000, 1, 20, "🍵", 0xFFE8F6ED,
                    "https://images.unsplash.com/photo-1536256263959-770b48d82b0a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P006", "Es Teh Manis", "Non-Kopi", 12000, 1, 80, "🧋", 0xFFE8F1FA,
                    "https://images.unsplash.com/photo-1556679343-c7306c1976bc?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P007", "Lemon Tea", "Non-Kopi", 18000, 1, 40, "🍋", 0xFFFBF3D9,
                    "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P008", "Nasi Goreng Spesial", "Makanan Berat", 35000, 1, 15, "🍛", 0xFFFBE9DC,
                    "https://images.unsplash.com/photo-1603133872878-684f208fb84b?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P009", "Mie Goreng Jawa", "Makanan Berat", 32000, 1, 18, "🍜", 0xFFFBE9DC,
                    "https://images.unsplash.com/photo-1612927601601-6638404737ce?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P010", "Butter Croissant", "Snack", 22000, 1, 20, "🥐", 0xFFF5EFDF,
                    "https://images.unsplash.com/photo-1555507036-ab1f4038808a?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P011", "Kentang Goreng", "Snack", 25000, 0, 0, "🍟", 0xFFFBF3D9,
                    "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P012", "Cheesecake Lemon", "Dessert", 32000, 1, 12, "🍰", 0xFFFCE9E4,
                    "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                ),
                arrayOf<Any?>(
                    "P013", "Brownies Cokelat", "Dessert", 25000, 1, 16, "🍫", 0xFFEFE3DC,
                    "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&q=80&w=400"
                )
            )
            products.forEach { p ->
                val fullParams = arrayOf<Any?>(
                    p[0], // id
                    p[1], // name
                    p[2], // category
                    p[3], // price
                    0,    // costPrice
                    p[4], // available
                    p[5], // stock
                    "",   // barcodeSku
                    0,    // durationMinutes
                    p[6], // emoji
                    p[7], // colorHex
                    p[8]  // imageUrl
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO products (id, name, category, price, costPrice, available, stock, barcodeSku, durationMinutes, emoji, colorHex, imageUrl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    fullParams
                )
            }
        }

        private fun seedUsers(db: SupportSQLiteDatabase) {
            listOf(
                "kasir" to UserRole.KASIR,
                "owner" to UserRole.OWNER
            ).forEach { (username, role) ->
                val salt = PinHasher.generateSalt()
                val hash = PinHasher.hash(if (role == UserRole.OWNER) "0000" else "1234", salt)
                db.execSQL(
                    "INSERT OR IGNORE INTO users (username, pinHash, pinSalt, role) VALUES (?, ?, ?, ?)",
                    arrayOf<Any?>(username, hash, salt, role.name)
                )
            }
        }

        private fun seedTransactions(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60 * 60 * 1000

            fun item(id: String, name: String, price: Int, qty: Int, emoji: String) =
                JSONObject().apply {
                    put("productId", id)
                    put("name", name)
                    put("price", price)
                    put("quantity", qty)
                    put("emoji", emoji)
                }

            fun tx(
                id: String,
                time: Long,
                items: List<JSONObject>,
                method: String,
                cash: Int?,
                status: String,
                table: String
            ) {
                val subtotal = items.sumOf { it.getInt("price") * it.getInt("quantity") }
                val total = subtotal
                db.execSQL(
                    "INSERT OR IGNORE INTO transactions (id, dateTime, itemsJson, subtotal, discount, tax, total, paymentMethod, cashReceived, `change`, status, note, tableNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        id, time, JSONArray(items).toString(), subtotal, 0, 0, total, method,
                        cash, if (cash != null) cash - total else null, status, "", table
                    )
                )
            }

            tx(
                "#INV-0181",
                now - dayMs,
                listOf(
                    item("P001", "Es Kopi Daun", 22000, 2, "☕"),
                    item("P010", "Butter Croissant", 22000, 1, "🥐")
                ),
                "CASH",
                100000,
                "SUKSES",
                "Meja 02"
            )
            tx(
                "#INV-0182",
                now - (dayMs / 2),
                listOf(
                    item("P003", "Cappuccino", 28000, 1, "🥛"),
                    item("P005", "Matcha Latte", 28000, 1, "🍵"),
                    item("P012", "Cheesecake Lemon", 32000, 1, "🍰")
                ),
                "QRIS",
                null,
                "SUKSES",
                "Meja 04"
            )
            tx(
                "#INV-0183",
                now - (15 * 60 * 1000),
                listOf(
                    item("P001", "Es Kopi Daun", 22000, 1, "☕"),
                    item("P003", "Cappuccino", 28000, 1, "🥛"),
                    item("P010", "Butter Croissant", 22000, 1, "🥐"),
                    item("P006", "Es Teh Manis", 12000, 1, "🧋")
                ),
                "CASH",
                100000,
                "SUKSES",
                "Meja 04"
            )
        }
    }
}
