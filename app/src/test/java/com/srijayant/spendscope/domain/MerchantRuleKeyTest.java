package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;

public final class MerchantRuleKeyTest {
    @Test
    public void matchingMerchantVariantsProduceSamePrivateKey() {
        String upper = MerchantRuleKey.fromMerchant("SWIGGY!");
        String lower = MerchantRuleKey.fromMerchant(" swiggy ");

        assertEquals(upper, lower);
        assertEquals(64, upper.length());
        assertFalse(upper.contains("swiggy"));
        assertNotEquals(upper, MerchantRuleKey.fromMerchant("Zomato"));
    }

    @Test
    public void userCategoryPreservesExpenseAndMarksOverride() {
        Expense original = new Expense(
                42,
                new BigDecimal("799"),
                "Merchant A",
                ExpenseCategory.OTHER,
                Instant.parse("2026-09-16T10:00:00Z")
        );

        Expense categorized = original.withUserCategory(ExpenseCategory.FOOD);

        assertEquals(original.getMessageId(), categorized.getMessageId());
        assertEquals(original.getAmount(), categorized.getAmount());
        assertEquals(original.getMerchant(), categorized.getMerchant());
        assertEquals(ExpenseCategory.FOOD, categorized.getCategory());
        assertTrue(categorized.isUserCategorized());
        assertFalse(original.isUserCategorized());
    }
}
