package com.srijayant.spendscope.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class MonthlyReport {
    private final YearMonth month;
    private final List<Expense> expenses;
    private final BigDecimal total;
    private final Map<ExpenseCategory, BigDecimal> categoryTotals;

    public MonthlyReport(YearMonth month, List<Expense> sourceExpenses) {
        this.month = month;

        List<Expense> sorted = new ArrayList<>(sourceExpenses);
        sorted.sort(Comparator.comparing(Expense::getTimestamp).reversed());
        this.expenses = Collections.unmodifiableList(sorted);

        EnumMap<ExpenseCategory, BigDecimal> totals = new EnumMap<>(ExpenseCategory.class);
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (Expense expense : sorted) {
            runningTotal = runningTotal.add(expense.getAmount());
            totals.merge(expense.getCategory(), expense.getAmount(), BigDecimal::add);
        }
        this.total = runningTotal;
        this.categoryTotals = Collections.unmodifiableMap(totals);
    }

    public YearMonth getMonth() {
        return month;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public int getTransactionCount() {
        return expenses.size();
    }

    public BigDecimal getAverage() {
        if (expenses.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return total.divide(BigDecimal.valueOf(expenses.size()), 2, RoundingMode.HALF_UP);
    }

    public Map<ExpenseCategory, BigDecimal> getCategoryTotals() {
        return categoryTotals;
    }

    public List<Map.Entry<ExpenseCategory, BigDecimal>> getCategoriesBySpend() {
        List<Map.Entry<ExpenseCategory, BigDecimal>> categories =
                new ArrayList<>(categoryTotals.entrySet());
        categories.sort(Map.Entry.<ExpenseCategory, BigDecimal>comparingByValue().reversed());
        return categories;
    }

    public ExpenseCategory getTopCategory() {
        List<Map.Entry<ExpenseCategory, BigDecimal>> categories = getCategoriesBySpend();
        return categories.isEmpty() ? ExpenseCategory.OTHER : categories.get(0).getKey();
    }
}
