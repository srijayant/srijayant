package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.ClassificationConfidence;
import com.srijayant.spendscope.model.ClassificationSource;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.MonthlyReport;

import org.junit.Test;

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
        assertEquals(124_950L, result.get().getAmountPaise());
        assertEquals("AMAZON", result.get().getMerchant());
        assertEquals(ExpenseCategory.SHOPPING, result.get().getCategory());
    }

    @Test
    public void extractsMaskedUpiDescriptorMerchantForRules() {
        Expense result = parser.parse(
                10,
                "ICICIB",
                "Credit Card XX2908 debited for INR 719.00 on 12-Sep-26 "
                        + "for UPI-62********86-THESOULE. To dispute contact the bank.",
                timestamp
        ).orElseThrow();

        assertEquals("THESOULE", result.getMerchant());
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
        assertEquals(42_500L, result.get().getAmountPaise());
        assertEquals("SWIGGY", result.get().getMerchant());
        assertEquals(ExpenseCategory.FOOD, result.get().getCategory());
        assertEquals(
                ClassificationConfidence.MEDIUM,
                result.get().getAutomaticClassification().getConfidence()
        );
        assertEquals(
                ClassificationSource.REGEX_SEED,
                result.get().getAutomaticClassification().getSource()
        );
    }

    @Test
    public void recognizesIndianGroceryAndFuelMerchants() {
        Expense groceries = parser.parse(
                6,
                "HDFCBK",
                "INR 850 paid to BIGBASKET via UPI",
                timestamp
        ).orElseThrow();
        Expense fuel = parser.parse(
                7,
                "ICICIB",
                "Rs 2,000 spent at INDIAN OIL on your card",
                timestamp
        ).orElseThrow();

        assertEquals(ExpenseCategory.GROCERIES, groceries.getCategory());
        assertEquals(ExpenseCategory.FUEL, fuel.getCategory());
    }

    @Test
    public void leavesUnknownMerchantForReview() {
        Expense result = parser.parse(
                9,
                "SBIBNK",
                "INR 1,500 paid to LOCAL PHARMACY using your card",
                timestamp
        ).orElseThrow();

        assertEquals(ExpenseCategory.OTHER, result.getCategory());
        assertEquals(
                ClassificationConfidence.LOW,
                result.getAutomaticClassification().getConfidence()
        );
        assertEquals(
                ClassificationSource.UNKNOWN,
                result.getAutomaticClassification().getSource()
        );
    }

    @Test
    public void extractsIndianUpiVpaForConsistentRules() {
        Expense result = parser.parse(
                8,
                "AXISBK",
                "Rs. 250 debited and transferred to VPA lunchbox@okhdfcbank (UPI Ref 12345)",
                timestamp
        ).orElseThrow();

        assertEquals("lunchbox@okhdfcbank", result.getMerchant());
        assertEquals(
                ClassificationConfidence.LOW,
                result.getAutomaticClassification().getConfidence()
        );
        assertEquals(
                ClassificationSource.PERSON_HEURISTIC,
                result.getAutomaticClassification().getSource()
        );
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
                30_000L,
                "Cafe",
                null,
                ExpenseCategory.FOOD,
                Instant.parse("2026-09-02T10:00:00Z")
        );
        Expense transport = new Expense(
                2,
                12_500L,
                "Metro",
                null,
                ExpenseCategory.TRANSPORT,
                Instant.parse("2026-09-03T10:00:00Z")
        );
        MonthlyReport report = new MonthlyReport(YearMonth.of(2026, 9), List.of(food, transport));

        assertEquals(42_500L, report.getTotalPaise());
        assertEquals(21_250L, report.getAveragePaise());
        assertEquals(ExpenseCategory.FOOD, report.getTopCategory());
        assertEquals(2, report.getTransactionCount());
        assertEquals(transport, report.getExpenses().get(0));
    }
}
