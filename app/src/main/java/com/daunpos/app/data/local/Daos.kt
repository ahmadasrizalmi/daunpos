package com.daunpos.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name")
    suspend fun getAll(): List<ProductEntity>

    @Upsert
    suspend fun upsert(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity")
    suspend fun deductStock(id: String, quantity: Int): Int

    @Query("UPDATE products SET available = :available WHERE id = :id")
    suspend fun setAvailable(id: String, available: Boolean)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserEntity?

    @Insert
    suspend fun insert(user: UserEntity)

    @Query("DELETE FROM users WHERE username = :username")
    suspend fun deleteByUsername(username: String)
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    fun observeAll(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveShift(): ShiftEntity?

    @Upsert
    suspend fun upsert(shift: ShiftEntity)
}
