package com.radarfinanceiro.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radarfinanceiro.data.dao.CategorySummary
import com.radarfinanceiro.data.database.AppDatabase
import com.radarfinanceiro.data.entity.Transaction
import com.radarfinanceiro.data.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    val repository = TransactionRepository(db.transactionDao(), db.merchantMappingDao())

    private val _currentMonth = MutableStateFlow(getCurrentMonth())
    val currentMonth: StateFlow<String> = _currentMonth

    val transactions: Flow<List<Transaction>> = _currentMonth.flatMapLatest { month ->
        val (start, end) = getMonthRange(month)
        repository.getByDateRange(start, end)
    }

    val categorySummary: Flow<List<CategorySummary>> = _currentMonth.flatMapLatest { month ->
        val (start, end) = getMonthRange(month)
        repository.getCategorySummary(start, end)
    }

    val myTotal: Flow<Double?> = _currentMonth.flatMapLatest { month ->
        val (start, end) = getMonthRange(month)
        repository.getMyTotal(start, end)
    }

    val lucianaTotal: Flow<Double?> = _currentMonth.flatMapLatest { month ->
        val (start, end) = getMonthRange(month)
        repository.getLucianaTotal(start, end)
    }

    val pendingCount: Flow<Int> = repository.getPending().map { it.size }

    private val _searchQuery = MutableStateFlow("")
    val searchResults: Flow<List<Transaction>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.length >= 2) repository.search(query)
            else flowOf(emptyList())
        }

    fun setMonth(month: String) {
        _currentMonth.value = month
    }

    fun nextMonth() {
        _currentMonth.value = adjustMonth(_currentMonth.value, 1)
    }

    fun previousMonth() {
        _currentMonth.value = adjustMonth(_currentMonth.value, -1)
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(transaction)
            if (!transaction.isLuciana) {
                repository.learnCategory(
                    transaction.merchantClean.ifEmpty { transaction.merchantName },
                    transaction.category,
                    transaction.note
                )
            }
        }
    }

    // --- Utilitarios de data ---

    private fun getCurrentMonth(): String {
        return SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
    }

    private fun getMonthRange(month: String): Pair<String, String> {
        val parts = month.split("/")
        val m = parts[0].toInt()
        val y = parts[1].toInt()

        val cal = Calendar.getInstance()
        cal.set(y, m - 1, 1)
        val start = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(cal.time)

        return start to end
    }

    private fun adjustMonth(month: String, delta: Int): String {
        val parts = month.split("/")
        val cal = Calendar.getInstance()
        cal.set(parts[1].toInt(), parts[0].toInt() - 1, 1)
        cal.add(Calendar.MONTH, delta)
        return SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(cal.time)
    }
}
