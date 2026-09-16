package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.MonthlyReport;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public final class ExpenseParserTest {
    private final ExpenseParser parser = new ExpenseParser();
    private final long timestamp = Instant.parse("2026-09-10T12:30:00Z").toEpochMilli();

    @Test
    public void parsesBankDebitAndMerchant() {
        Optional<Expense> result = parser.parse(
                1,
                "HDFCBK",
                "Rs. 1,249.50 debited from A/c XX1234 at AMAZON on 10-Sep. Avl Bal Rs 8,000",
                timestamp
        );

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("1249.50"), result.get().getAmount());
        assertEquals("Amazon", result.get().getMerchant());
        assertEquals(ExpenseCategory.SHOPPING, result.get().getCategory());
    }

    @Test
    public void parsesUpiPaymentAndCategorizesFood() {
        Optional<Expense> result = parser.parse(
                2,
                "AXISBK",
                "UPI payment of INR 425 paid to SWIGGY via Axis Bank. Ref 123456",
                timestamp
        );

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("425"), result.get().getAmount());
        assertEquals("Swiggy", result.get().getMerchant());
        assertEquals(ExpenseCategory.FOOD, result.get().getCategory());
    }

    @Test
    public void ignoresIncomingCreditAndRefunds() {
        assertFalse(parser.parse(
                3,
                "ICICIB",
                "INR 5,000 credited to your account via NEFT",
                timestamp
        ).isPresent());
        assertFalse(parser.parse(
                4,
                "HDFCBK",
                "Refund of Rs 799 credited to your account",
                timestamp
        ).isPresent());
    }

    @Test
    public void ignoresMessagesWithoutExpenseLanguage() {
        assertFalse(parser.parse(
                5,
                "SHOP",
                "Your cart total is ₹999. Complete checkout today.",
                timestamp
        ).isPresent());
    }

    @Test
    public void monthlyReportCalculatesTotalsAndTopCategory() {
        Expense food = new Expense(
                1,
                new BigDecimal("300"),
                "Cafe",
                ExpenseCategory.FOOD,
                Instant.parse("2026-09-02T10:00:00Z")
        );
        Expense transport = new Expense(
                2,
                new BigDecimal("125"),
                "Metro",
                ExpenseCategory.TRANSPORT,
                Instant.parse("2026-09-03T10:00:00Z")
        );
        MonthlyReport report = new MonthlyReport(YearMonth.of(2026, 9), List.of(food, transport));

        assertEquals(new BigDecimal("425"), report.getTotal());
        assertEquals(new BigDecimal("212.50"), report.getAverage());
        assertEquals(ExpenseCategory.FOOD, report.getTopCategory());
        assertEquals(2, report.getTransactionCount());
        assertEquals(transport, report.getExpenses().get(0));
    }
}
