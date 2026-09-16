package com.srijayant.spendscope.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.srijayant.spendscope.domain.IdentityRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class IdentityRuleStore {
    private static final String PREFERENCES = "identity_rules";
    private static final String SELF = "self";
    private static final String FAMILY = "family";
    private static final String FAMILY_EXCLUDED = "family_excluded";
    private static final String SEPARATOR = "\u001f";
    private static final List<String> DEFAULT_SELF = List.of(
            "SRIJAYANT",
            "JAYANT-SRIJAYANTSINGH"
    );
    private static final List<String> DEFAULT_FAMILY = List.of(
            "KRITIKA",
            "KRITIKAIT09"
    );

    private final SharedPreferences preferences;

    public IdentityRuleStore(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        if (!preferences.contains(SELF)) {
            preferences.edit()
                    .putString(SELF, encode(DEFAULT_SELF))
                    .putString(FAMILY, encode(DEFAULT_FAMILY))
                    .putBoolean(FAMILY_EXCLUDED, true)
                    .apply();
        }
    }

    public IdentityRules load() {
        return new IdentityRules(
                decode(preferences.getString(SELF, encode(DEFAULT_SELF))),
                decode(preferences.getString(FAMILY, encode(DEFAULT_FAMILY))),
                preferences.getBoolean(FAMILY_EXCLUDED, true)
        );
    }

    public void save(List<String> self, List<String> family, boolean familyExcluded) {
        preferences.edit()
                .putString(SELF, encode(self))
                .putString(FAMILY, encode(family))
                .putBoolean(FAMILY_EXCLUDED, familyExcluded)
                .apply();
    }

    private String encode(List<String> values) {
        return String.join(SEPARATOR, values);
    }

    private List<String> decode(String value) {
        if (value == null || value.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(value.split(SEPARATOR, -1)));
    }
}
