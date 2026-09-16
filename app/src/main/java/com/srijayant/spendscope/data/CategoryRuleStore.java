package com.srijayant.spendscope.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.srijayant.spendscope.domain.CategoryPipeline;
import com.srijayant.spendscope.domain.MerchantNormalizer;
import com.srijayant.spendscope.domain.MerchantRuleKey;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;

import java.util.Locale;
import java.util.Optional;

public final class CategoryRuleStore implements CategoryPipeline.RuleLookup {
    private static final String PREFERENCES_NAME = "merchant_category_rules";
    private static final String KEY_PREFIX = "key.";
    private static final String VPA_PREFIX = "vpa.";
    private static final String MANUAL_PREFIX = "manual.";

    private final SharedPreferences preferences;
    private final MerchantNormalizer normalizer = new MerchantNormalizer();

    public CategoryRuleStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public Expense apply(Expense expense) {
        Optional<ExpenseCategory> manual = getManualCategory(expense.getMessageId());
        if (manual.isPresent()) {
            return expense.withManualCategory(manual.get());
        }
        Optional<ExpenseCategory> rule = forVpa(expense.getVpa());
        if (rule.isEmpty()) {
            rule = forKey(normalizer.normalize(expense.getMerchant()));
        }
        return rule
                .map(expense::withUserCategory)
                .orElse(expense);
    }

    public Optional<ExpenseCategory> getCategory(String merchant) {
        return forKey(normalizer.normalize(merchant));
    }

    @Override
    public Optional<ExpenseCategory> forVpa(String vpa) {
        if (vpa == null || vpa.isBlank()) {
            return Optional.empty();
        }
        return read(VPA_PREFIX + privateKey(vpa.toLowerCase(Locale.ROOT)));
    }

    @Override
    public Optional<ExpenseCategory> forKey(String normalizedKey) {
        if (normalizedKey == null || normalizedKey.isBlank()) {
            return Optional.empty();
        }
        return read(KEY_PREFIX + privateKey(normalizedKey));
    }

    public Optional<ExpenseCategory> getManualCategory(long smsId) {
        return read(MANUAL_PREFIX + smsId);
    }

    private Optional<ExpenseCategory> read(String preferenceKey) {
        String value = preferences.getString(preferenceKey, null);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(ExpenseCategory.valueOf(value));
        } catch (IllegalArgumentException invalidStoredValue) {
            preferences.edit().remove(preferenceKey).apply();
            return Optional.empty();
        }
    }

    public void setCategory(String merchant, ExpenseCategory category) {
        setRule(null, normalizer.normalize(merchant), category);
    }

    public void setRule(String vpa, String normalizedKey, ExpenseCategory category) {
        if (vpa != null && !vpa.isBlank()) {
            preferences.edit()
                    .putString(VPA_PREFIX + privateKey(vpa.toLowerCase(Locale.ROOT)), category.name())
                    .apply();
        } else if (normalizedKey != null && !normalizedKey.isBlank()) {
            preferences.edit()
                    .putString(KEY_PREFIX + privateKey(normalizedKey), category.name())
                    .apply();
        }
    }

    public void setManualCategory(long smsId, ExpenseCategory category) {
        preferences.edit().putString(MANUAL_PREFIX + smsId, category.name()).apply();
    }

    public void removeManualCategory(long smsId) {
        preferences.edit().remove(MANUAL_PREFIX + smsId).apply();
    }

    public void removeCategory(String merchant) {
        String normalizedKey = normalizer.normalize(merchant);
        if (!normalizedKey.isBlank()) {
            preferences.edit().remove(KEY_PREFIX + privateKey(normalizedKey)).apply();
        }
    }

    public void removeRule(String vpa, String normalizedKey) {
        SharedPreferences.Editor editor = preferences.edit();
        if (vpa != null && !vpa.isBlank()) {
            editor.remove(VPA_PREFIX + privateKey(vpa.toLowerCase(Locale.ROOT)));
        }
        if (normalizedKey != null && !normalizedKey.isBlank()) {
            editor.remove(KEY_PREFIX + privateKey(normalizedKey));
        }
        editor.apply();
    }

    private String privateKey(String value) {
        return MerchantRuleKey.fromMerchant(value);
    }
}
