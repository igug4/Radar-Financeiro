package com.radarfinanceiro.data.dao

import androidx.room.*
import com.radarfinanceiro.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isLuciana = 1 ORDER BY timestamp DESC")
    fun getLucianaTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isLuciana = 0 ORDER BY timestamp DESC")
    fun getMyTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE confirmed = 0 ORDER BY timestamp DESC")
    fun getPendingConfirmation(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE notificationId = :notifId LIMIT 1")
    suspend fun getByNotificationId(notifId: String): Transaction?

    @Query("""
        SELECT category, SUM(amount) as total, COUNT(*) as count 
        FROM transactions 
        WHERE date BETWEEN :startDate AND :endDate 
        GROUP BY category 
        ORDER BY total DESC
    """)
    fun getCategorySummary(startDate: String, endDate: String): Flow<List<CategorySummary>>

    @Query("SELECT SUM(amount) FROM transactions WHERE date BETWEEN :startDate AND :endDate AND isLuciana = 0")
    fun getMyTotal(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE date BETWEEN :startDate AND :endDate AND isLuciana = 1")
    fun getLucianaTotal(startDate: String, endDate: String): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE merchantClean LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllForExport(): List<Transaction>
}

data class CategorySummary(
    val category: String,
    val total: Double,
    val count: Int
)
