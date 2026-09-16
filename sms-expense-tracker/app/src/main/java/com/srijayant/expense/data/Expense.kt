package com.srijayant.expense.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory(val label: String) {
    FOOD("Food & Dining"),
    SHOPPING("Shopping"),
    TRAVEL("Travel"),
    BILLS("Bills & Utilities"),
    ENTERTAINMENT("Entertainment"),
    HEALTH("Health"),
    TRANSFER("Transfers"),
    ATM("ATM / Cash"),
    OTHER("Other")
}

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["smsId"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val merchant: String,
    val category: ExpenseCategory,
    val timestamp: Long,
    val rawMessage: String,
    val sender: String,
    val smsId: String?,
    val isManual: Boolean = false,
    val currency: String = "INR"
)

data class CategoryTotal(
    val category: ExpenseCategory,
    val total: Double,
    val count: Int
)

data class MonthlyReport(
    val year: Int,
    val month: Int,
    val totalSpent: Double,
    val transactionCount: Int,
    val categoryTotals: List<CategoryTotal>,
    val dailyTotals: Map<Int, Double>,
    val expenses: List<Expense>
)
