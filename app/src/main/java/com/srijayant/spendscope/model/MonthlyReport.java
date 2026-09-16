package com.srijayant.spendscope.model;

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
    private final long totalPaise;
    private final Map<ExpenseCategory, Long> categoryTotals;

    public MonthlyReport(YearMonth month, List<Expense> sourceExpenses) {
        this.month = month;

        List<Expense> sorted = new ArrayList<>(sourceExpenses);
        sorted.sort(Comparator.comparing(Expense::getTimestamp).reversed());
        this.expenses = Collections.unmodifiableList(sorted);

        EnumMap<ExpenseCategory, Long> totals = new EnumMap<>(ExpenseCategory.class);
        long runningTotal = 0L;
        for (Expense expense : sorted) {
            runningTotal = Math.addExact(runningTotal, expense.getAmountPaise());
            totals.merge(expense.getCategory(), expense.getAmountPaise(), Math::addExact);
        }
        this.totalPaise = runningTotal;
        this.categoryTotals = Collections.unmodifiableMap(totals);
    }

    public YearMonth getMonth() {
        return month;
    }

    public List<Expense> getExpenses() {
        return expenses;
    }

    public long getTotalPaise() {
        return totalPaise;
    }

    public int getTransactionCount() {
        return expenses.size();
    }

    public long getAveragePaise() {
        if (expenses.isEmpty()) {
            return 0L;
        }
        return Math.round((double) totalPaise / expenses.size());
    }

    public Map<ExpenseCategory, Long> getCategoryTotals() {
        return categoryTotals;
    }

    public List<Map.Entry<ExpenseCategory, Long>> getCategoriesBySpend() {
        List<Map.Entry<ExpenseCategory, Long>> categories =
                new ArrayList<>(categoryTotals.entrySet());
        categories.sort(Map.Entry.<ExpenseCategory, Long>comparingByValue().reversed());
        return categories;
    }

    public ExpenseCategory getTopCategory() {
        List<Map.Entry<ExpenseCategory, Long>> categories = getCategoriesBySpend();
        return categories.isEmpty() ? ExpenseCategory.OTHER : categories.get(0).getKey();
    }
}
