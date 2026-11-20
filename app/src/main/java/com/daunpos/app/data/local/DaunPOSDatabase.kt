package com.daunpos.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        UserEntity::class,
        ShiftEntity::class,
        StoreProfileEntity::class,
        StoreStaffEntity::class
    ],
    version = 5,
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

        // The v3 catalog and invoices were demonstration fixtures, not merchant records.
        // Clear them once during upgrade; future app versions must never clear live data.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM transactions")
                db.execSQL("DELETE FROM products")
                db.execSQL("DELETE FROM shifts")
                db.execSQL("DELETE FROM store_staff")
                db.execSQL("DELETE FROM users")
                db.execSQL(
                    "UPDATE store_profile SET storeName = 'Nama Toko', address = '', phoneNumber = '', taxPercentage = 0, staticQrisRaw = '', merchantNmid = '', isMultiUserEnabled = 0, isAutoVerificationEnabled = 0, isOnboardingCompleted = 0 WHERE id = 1 AND storeName = 'DaunPOS Coffee & Eatery' AND address = 'Jl. Daun Hijau No. 8, Bandung' AND phoneNumber = '0812-3456-7890'"
                )
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE products ADD COLUMN unitLabel TEXT NOT NULL DEFAULT 'unit'")
            }
        }

        fun build(context: Context): DaunPOSDatabase =
            Room.databaseBuilder(context, DaunPOSDatabase::class.java, "daunpos.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4, MIGRATION_1_4, MIGRATION_4_5)
                .addCallback(SeedCallback)
                .build()
    }

    private object SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "INSERT OR IGNORE INTO store_profile (id, storeName, businessType, address, phoneNumber, logoUri, taxPercentage, staticQrisRaw, merchantNmid, isMultiUserEnabled, isAutoVerificationEnabled, isOnboardingCompleted) VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>("Nama Toko", "FNB", "", "", null, 0, "", "", 0, 0, 0)
            )
        }
    }
}