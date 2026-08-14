package com.radarfinanceiro.data.dao

import androidx.room.*
import com.radarfinanceiro.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<Category>)

    @Query("SELECT * FROM categories WHERE isActive = 1 ORDER BY sortOrder")
    fun getAllActive(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun getAll(): Flow<List<Category>>

    @Update
    suspend fun update(category: Category)
}
