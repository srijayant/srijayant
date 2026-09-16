package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.ExpenseCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MerchantSeedData {
    private final Map<String, Entry> exact;
    private final List<RegexRule> regexRules;

    public MerchantSeedData(Map<String, Entry> exact, List<RegexRule> regexRules) {
        this.exact = Collections.unmodifiableMap(new LinkedHashMap<>(exact));
        this.regexRules = Collections.unmodifiableList(new ArrayList<>(regexRules));
    }

    public Map<String, Entry> getExact() {
        return exact;
    }

    public List<RegexRule> getRegexRules() {
        return regexRules;
    }

    public static MerchantSeedData empty() {
        return new MerchantSeedData(Collections.emptyMap(), Collections.emptyList());
    }

    public static final class Entry {
        private final ExpenseCategory category;
        private final String bucket;

        public Entry(ExpenseCategory category, String bucket) {
            this.category = Objects.requireNonNull(category);
            this.bucket = Objects.requireNonNull(bucket);
        }

        public ExpenseCategory getCategory() {
            return category;
        }

        public String getBucket() {
            return bucket;
        }
    }

    public static final class RegexRule {
        private final ExpenseCategory category;
        private final Pattern pattern;

        public RegexRule(ExpenseCategory category, String pattern) {
            this.category = Objects.requireNonNull(category);
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }

        public ExpenseCategory getCategory() {
            return category;
        }

        public boolean matches(String value) {
            return pattern.matcher(value).find();
        }
    }
}
