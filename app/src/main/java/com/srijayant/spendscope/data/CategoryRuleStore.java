package com.srijayant.spendscope.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.srijayant.spendscope.domain.MerchantRuleKey;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;

import java.util.Optional;

public final class CategoryRuleStore {
    private static final String PREFERENCES_NAME = "merchant_category_rules";
    private static final String KEY_PREFIX = "merchant.";

    private final SharedPreferences preferences;

    public CategoryRuleStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public Expense apply(Expense expense) {
        return getCategory(expense.getMerchant())
                .map(expense::withUserCategory)
                .orElse(expense);
    }

    public Optional<ExpenseCategory> getCategory(String merchant) {
        String value = preferences.getString(key(merchant), null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ExpenseCategory.valueOf(value));
        } catch (IllegalArgumentException invalidStoredValue) {
            preferences.edit().remove(key(merchant)).apply();
            return Optional.empty();
        }
    }

    public void setCategory(String merchant, ExpenseCategory category) {
        preferences.edit().putString(key(merchant), category.name()).apply();
    }

    public void removeCategory(String merchant) {
        preferences.edit().remove(key(merchant)).apply();
    }

    private String key(String merchant) {
        return KEY_PREFIX + MerchantRuleKey.fromMerchant(merchant);
    }
}
