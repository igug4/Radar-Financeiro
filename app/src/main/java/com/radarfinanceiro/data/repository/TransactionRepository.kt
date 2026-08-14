package com.radarfinanceiro.data.repository

import com.radarfinanceiro.data.dao.MerchantMappingDao
import com.radarfinanceiro.data.dao.TransactionDao
import com.radarfinanceiro.data.entity.MerchantMapping
import com.radarfinanceiro.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao
) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllFlow()

    fun getByDateRange(start: String, end: String) = transactionDao.getByDateRange(start, end)

    fun getByCategory(category: String) = transactionDao.getByCategory(category)

    fun getMyTransactions() = transactionDao.getMyTransactions()

    fun getLucianaTransactions() = transactionDao.getLucianaTransactions()

    fun getPending() = transactionDao.getPendingConfirmation()

    fun search(query: String) = transactionDao.search(query)

    fun getCategorySummary(start: String, end: String) = transactionDao.getCategorySummary(start, end)

    fun getMyTotal(start: String, end: String) = transactionDao.getMyTotal(start, end)

    fun getLucianaTotal(start: String, end: String) = transactionDao.getLucianaTotal(start, end)

    suspend fun insert(transaction: Transaction): Long {
        // Evita duplicatas pela notificationId
        if (transaction.notificationId.isNotEmpty()) {
            val existing = transactionDao.getByNotificationId(transaction.notificationId)
            if (existing != null) return existing.id
        }
        return transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun getById(id: Long) = transactionDao.getById(id)

    // --- Aprendizado ---

    suspend fun suggestCategory(merchantName: String): String {
        val normalized = merchantName.uppercase().trim()

        // Busca exata primeiro
        val exactMatch = merchantMappingDao.getByPattern(normalized)
        if (exactMatch != null) return exactMatch.category

        // Busca parcial
        val partialMatch = merchantMappingDao.findBestMatch(normalized)
        if (partialMatch != null) return partialMatch.category

        // Busca por palavras-chave no nome
        val mappings = merchantMappingDao.getAll()
        for (mapping in mappings) {
            if (normalized.contains(mapping.merchantPattern.uppercase())) {
                return mapping.category
            }
        }

        return "Outros"
    }

    suspend fun learnCategory(merchantName: String, category: String, customName: String = "") {
        val normalized = merchantName.uppercase().trim()
        val existing = merchantMappingDao.getByPattern(normalized)

        if (existing != null) {
            merchantMappingDao.update(
                existing.copy(
                    category = category,
                    customName = customName.ifEmpty { existing.customName },
                    timesUsed = existing.timesUsed + 1,
                    lastUsed = System.currentTimeMillis()
                )
            )
        } else {
            merchantMappingDao.insert(
                MerchantMapping(
                    merchantPattern = normalized,
                    category = category,
                    customName = customName
                )
            )
        }
    }

    suspend fun getAllForExport() = transactionDao.getAllForExport()
}
