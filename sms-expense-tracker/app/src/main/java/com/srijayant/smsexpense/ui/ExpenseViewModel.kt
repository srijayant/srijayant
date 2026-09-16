package com.srijayant.smsexpense.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srijayant.smsexpense.data.Category
import com.srijayant.smsexpense.data.SmsReader
import com.srijayant.smsexpense.data.Transaction
import com.srijayant.smsexpense.data.TransactionParser
import com.srijayant.smsexpense.data.TransactionType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CategoryTotal(val category: Category, val total: Double, val share: Float)

data class MonthlyReport(
    val month: YearMonth,
    val totalSpent: Double,
    val totalReceived: Double,
    val categoryTotals: List<CategoryTotal>,
    val transactions: List<Transaction>
)

data class UiState(
    val permissionGranted: Boolean = false,
    val loading: Boolean = false,
    val scannedSmsCount: Int = 0,
    val selectedMonth: YearMonth = YearMonth.now(),
    val availableMonths: List<YearMonth> = emptyList(),
    val report: MonthlyReport? = null
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    /** How far back in the inbox to scan. */
    private val monthsOfHistory = 6L

    private val zone: ZoneId = ZoneId.systemDefault()
    private var allTransactions: List<Transaction> = emptyList()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(permissionGranted = granted) }
        if (granted) refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val (transactions, smsCount) = withContext(Dispatchers.IO) {
                val from = YearMonth.now().minusMonths(monthsOfHistory - 1)
                    .atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val messages = SmsReader(getApplication()).readInbox(from)
                messages.mapNotNull(TransactionParser::parse) to messages.size
            }
            allTransactions = transactions
            val months = transactions
                .map { YearMonth.from(Instant.ofEpochMilli(it.timestampMillis).atZone(zone)) }
                .distinct()
                .sortedDescending()
                .ifEmpty { listOf(YearMonth.now()) }
            val selected = _uiState.value.selectedMonth
                .takeIf { months.contains(it) } ?: months.first()
            _uiState.update {
                it.copy(
                    loading = false,
                    scannedSmsCount = smsCount,
                    availableMonths = months,
                    selectedMonth = selected,
                    report = buildReport(selected)
                )
            }
        }
    }

    fun selectMonth(month: YearMonth) {
        _uiState.update { it.copy(selectedMonth = month, report = buildReport(month)) }
    }

    fun previousMonth() = shiftMonth(-1)
    fun nextMonth() = shiftMonth(+1)

    private fun shiftMonth(delta: Int) {
        val months = _uiState.value.availableMonths
        if (months.isEmpty()) return
        // Months are sorted descending, so "previous" moves toward the end.
        val idx = months.indexOf(_uiState.value.selectedMonth)
        val newIdx = idx - delta
        if (idx == -1 || newIdx !in months.indices) return
        selectMonth(months[newIdx])
    }

    private fun buildReport(month: YearMonth): MonthlyReport {
        val inMonth = allTransactions.filter {
            YearMonth.from(Instant.ofEpochMilli(it.timestampMillis).atZone(zone)) == month
        }
        val debits = inMonth.filter { it.type == TransactionType.DEBIT }
        val totalSpent = debits.sumOf { it.amount }
        val totalReceived = inMonth.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }

        val categoryTotals = debits
            .groupBy { it.category }
            .map { (category, txns) -> category to txns.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .map { (category, total) ->
                CategoryTotal(
                    category = category,
                    total = total,
                    share = if (totalSpent > 0) (total / totalSpent).toFloat() else 0f
                )
            }

        return MonthlyReport(
            month = month,
            totalSpent = totalSpent,
            totalReceived = totalReceived,
            categoryTotals = categoryTotals,
            transactions = inMonth.sortedByDescending { it.timestampMillis }
        )
    }
}
