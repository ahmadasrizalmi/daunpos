package com.daunpos.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.daunpos.app.data.PinHasher
import com.daunpos.app.data.UserRole
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [ProductEntity::class, TransactionEntity::class, UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DaunPOSDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun userDao(): UserDao

    companion object {
        fun build(context: Context): DaunPOSDatabase =
            Room.databaseBuilder(context, DaunPOSDatabase::class.java, "daunpos.db")
                .addCallback(SeedCallback)
                .build()
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            seedProducts(db)
            seedUsers(db)
            seedTransactions(db)
        }

        private fun seedProducts(db: SupportSQLiteDatabase) {
            val products = listOf(
                arrayOf<Any?>("P001", "Espresso", "Kopi", 18000, 1, "☕", 0xFFE7F4EA),
                arrayOf<Any?>("P002", "Americano", "Kopi", 22000, 1, "🫘", 0xFFEFE7D6),
                arrayOf<Any?>("P003", "Cappuccino", "Kopi", 28000, 1, "🥛", 0xFFE7F4EA),
                arrayOf<Any?>("P004", "Cafe Latte", "Kopi", 30000, 1, "☕", 0xFFF2E9DE),
                arrayOf<Any?>("P005", "Es Teh Manis", "Non-Kopi", 12000, 1, "🧋", 0xFFE8F1FA),
                arrayOf<Any?>("P006", "Lemon Tea", "Non-Kopi", 18000, 1, "🍋", 0xFFFBF3D9),
                arrayOf<Any?>("P007", "Nasi Goreng Spesial", "Makanan Berat", 35000, 1, "🍛", 0xFFFBE9DC),
                arrayOf<Any?>("P008", "Mie Goreng Jawa", "Makanan Berat", 32000, 1, "🍜", 0xFFFBE9DC),
                arrayOf<Any?>("P009", "Croissant Butter", "Snack", 22000, 1, "🥐", 0xFFF5EFDF),
                arrayOf<Any?>("P010", "Kentang Goreng", "Snack", 25000, 0, "🍟", 0xFFFBF3D9),
                arrayOf<Any?>("P011", "Cheesecake Lemon", "Dessert", 32000, 1, "🍰", 0xFFFCE9E4),
                arrayOf<Any?>("P012", "Brownies Cokelat", "Dessert", 25000, 1, "🍫", 0xFFEFE3DC)
            )
            products.forEach { p ->
                db.execSQL(
                    "INSERT OR IGNORE INTO products (id, name, category, price, available, emoji, colorHex) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    p
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
                status: String
            ) {
                val subtotal = items.sumOf { it.getInt("price") * it.getInt("quantity") }
                val total = subtotal
                db.execSQL(
                    "INSERT OR IGNORE INTO transactions (id, dateTime, itemsJson, subtotal, tax, total, paymentMethod, cashReceived, `change`, status, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        id, time, JSONArray(items).toString(), subtotal, 0, total, method,
                        cash, if (cash != null) cash - total else null, status, ""
                    )
                )
            }

            tx(
                "#INV-0183", now - 1000 * 60 * 18,
                listOf(
                    item("P001", "Espresso", 18000, 1, "☕"),
                    item("P003", "Cappuccino", 28000, 1, "🥛"),
                    item("P009", "Croissant Butter", 22000, 1, "🥐"),
                    item("P005", "Es Teh Manis", 12000, 1, "🧋")
                ),
                "CASH", 100000, "SUKSES"
            )
            tx(
                "#INV-0182", now - 1000 * 60 * 60 * 2,
                listOf(
                    item("P007", "Nasi Goreng Spesial", 35000, 2, "🍛"),
                    item("P006", "Lemon Tea", 18000, 2, "🍋")
                ),
                "QRIS", null, "SUKSES"
            )
            tx(
                "#INV-0181", now - 1000 * 60 * 60 * 24,
                listOf(
                    item("P011", "Cheesecake Lemon", 32000, 1, "🍰"),
                    item("P002", "Americano", 22000, 1, "🫘")
                ),
                "CASH", 60000, "SUKSES"
            )
            tx(
                "#INV-0180", now - 1000 * 60 * 60 * 26,
                listOf(
                    item("P008", "Mie Goreng Jawa", 32000, 1, "🍜"),
                    item("P005", "Es Teh Manis", 12000, 1, "🧋"),
                    item("P012", "Brownies Cokelat", 25000, 1, "🍫")
                ),
                "TRANSFER", null, "SUKSES"
            )
            tx(
                "#INV-0179", now - 1000 * 60 * 60 * 48,
                listOf(
                    item("P010", "Kentang Goreng", 25000, 1, "🍟"),
                    item("P004", "Cafe Latte", 30000, 1, "☕")
                ),
                "QRIS", null, "SUKSES"
            )
            tx(
                "#INV-0178", now - 1000 * 60 * 60 * 50,
                listOf(
                    item("P001", "Espresso", 18000, 3, "☕"),
                    item("P009", "Croissant Butter", 22000, 2, "🥐")
                ),
                "CASH", 100000, "SUKSES"
            )
        }
    }
}
