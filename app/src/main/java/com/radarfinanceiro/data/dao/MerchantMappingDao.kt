package com.radarfinanceiro.data.dao

import androidx.room.*
import com.radarfinanceiro.data.entity.MerchantMapping

@Dao
interface MerchantMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: MerchantMapping): Long

    @Update
    suspend fun update(mapping: MerchantMapping)

    @Query("SELECT * FROM merchant_mappings WHERE merchantPattern = :pattern LIMIT 1")
    suspend fun getByPattern(pattern: String): MerchantMapping?

    @Query("SELECT * FROM merchant_mappings WHERE :merchantName LIKE '%' || merchantPattern || '%' ORDER BY timesUsed DESC LIMIT 1")
    suspend fun findBestMatch(merchantName: String): MerchantMapping?

    @Query("SELECT * FROM merchant_mappings ORDER BY timesUsed DESC")
    suspend fun getAll(): List<MerchantMapping>

    @Query("DELETE FROM merchant_mappings WHERE id = :id")
    suspend fun deleteById(id: Long)
}
