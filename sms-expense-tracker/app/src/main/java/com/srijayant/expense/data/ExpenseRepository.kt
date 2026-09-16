package com.srijayant.expense.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class ExpenseRepository(context: Context) {
    private val dao = ExpenseDatabase.get(context).expenseDao()
    private val smsReader = SmsReader(context)

    fun observeAll(): Flow<List<Expense>> = dao.observeAll()

    fun observeMonth(year: Int, month: Int): Flow<List<Expense>> {
        val (start, end) = monthBounds(year, month)
        return dao.observeBetween(start, end)
    }

    suspend fun getMonthlyReport(year: Int, month: Int): MonthlyReport {
        val (start, end) = monthBounds(year, month)
        val expenses = dao.getBetween(start, end)
        val categoryTotals = expenses
            .groupBy { it.category }
            .map { (category, items) ->
                CategoryTotal(
                    category = category,
                    total = items.sumOf { it.amount },
                    count = items.size
                )
            }
            .sortedByDescending { it.total }

        val dailyTotals = expenses
            .groupBy { dayOfMonth(it.timestamp) }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

        return MonthlyReport(
            year = year,
            month = month,
            totalSpent = expenses.sumOf { it.amount },
            transactionCount = expenses.size,
            categoryTotals = categoryTotals,
            dailyTotals = dailyTotals,
            expenses = expenses
        )
    }

    suspend fun syncFromSms(): SyncResult = withContext(Dispatchers.IO) {
        val knownIds = dao.getKnownSmsIds().toHashSet()
        val messages = smsReader.readInbox()
        val fresh = mutableListOf<Expense>()

        for (sms in messages) {
            if (sms.id in knownIds) continue
            val parsed = ExpenseParser.parse(sms.body, sms.address) ?: continue
            fresh += Expense(
                amount = parsed.amount,
                merchant = parsed.merchant,
                category = parsed.category,
                timestamp = sms.date,
                rawMessage = sms.body,
                sender = sms.address,
                smsId = sms.id,
                isManual = false
            )
        }

        val inserted = if (fresh.isNotEmpty()) {
            dao.insertAll(fresh).count { it != -1L }
        } else {
            0
        }

        SyncResult(
            scanned = messages.size,
            imported = inserted,
            skipped = messages.size - fresh.size
        )
    }

    suspend fun addManual(
        amount: Double,
        merchant: String,
        category: ExpenseCategory,
        timestamp: Long = System.currentTimeMillis()
    ) {
        dao.insert(
            Expense(
                amount = amount,
                merchant = merchant.ifBlank { "Manual entry" },
                category = category,
                timestamp = timestamp,
                rawMessage = "Manual entry",
                sender = "manual",
                smsId = null,
                isManual = true
            )
        )
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun monthBounds(year: Int, month: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = Calendar.getInstance().apply {
            timeInMillis = start
            add(Calendar.MONTH, 1)
        }.timeInMillis

        return start to end
    }

    private fun dayOfMonth(timestamp: Long): Int =
        Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
}

data class SyncResult(
    val scanned: Int,
    val imported: Int,
    val skipped: Int
)
