package com.srijayant.expense.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srijayant.expense.data.Expense
import com.srijayant.expense.data.ExpenseCategory
import com.srijayant.expense.data.ExpenseRepository
import com.srijayant.expense.data.MonthlyReport
import com.srijayant.expense.data.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class UiState(
    val year: Int = Calendar.getInstance().get(Calendar.YEAR),
    val month: Int = Calendar.getInstance().get(Calendar.MONTH),
    val hasSmsPermission: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSync: SyncResult? = null,
    val error: String? = null,
    val report: MonthlyReport? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository(application)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val monthKey = MutableStateFlow(
        _ui.value.year to _ui.value.month
    )

    val expenses: StateFlow<List<Expense>> = monthKey
        .flatMapLatest { (year, month) -> repository.observeMonth(year, month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshReport()
    }

    fun setSmsPermission(granted: Boolean) {
        _ui.update { it.copy(hasSmsPermission = granted) }
        if (granted) syncSms()
    }

    fun shiftMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _ui.value.year)
            set(Calendar.MONTH, _ui.value.month)
            add(Calendar.MONTH, delta)
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        _ui.update { it.copy(year = year, month = month) }
        monthKey.value = year to month
        refreshReport()
    }

    fun syncSms() {
        if (!_ui.value.hasSmsPermission) return
        viewModelScope.launch {
            _ui.update { it.copy(isSyncing = true, error = null) }
            runCatching { repository.syncFromSms() }
                .onSuccess { result ->
                    _ui.update { it.copy(isSyncing = false, lastSync = result) }
                    refreshReport()
                }
                .onFailure { e ->
                    _ui.update {
                        it.copy(isSyncing = false, error = e.message ?: "SMS sync failed")
                    }
                }
        }
    }

    fun addManual(amount: Double, merchant: String, category: ExpenseCategory) {
        viewModelScope.launch {
            repository.addManual(amount, merchant, category)
            refreshReport()
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            refreshReport()
        }
    }

    fun clearError() {
        _ui.update { it.copy(error = null) }
    }

    private fun refreshReport() {
        viewModelScope.launch {
            val report = repository.getMonthlyReport(_ui.value.year, _ui.value.month)
            _ui.update { it.copy(report = report) }
        }
    }
}
