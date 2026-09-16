package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.DerivedTransaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TransactionDeduplicator {
    public Result deduplicate(List<DerivedTransaction> transactions) {
        Map<String, DerivedTransaction> unique = new LinkedHashMap<>();
        int duplicatesRemoved = 0;
        for (DerivedTransaction transaction : transactions) {
            String key = key(transaction);
            DerivedTransaction existing = unique.get(key);
            if (existing == null) {
                unique.put(key, transaction);
            } else {
                unique.put(key, existing.withAdditionalDuplicate());
                duplicatesRemoved++;
            }
        }
        return new Result(new ArrayList<>(unique.values()), duplicatesRemoved);
    }

    private String key(DerivedTransaction transaction) {
        String reference = transaction.getReference();
        if (reference != null && !reference.isBlank()) {
            return "reference|"
                    + transaction.getSender() + "|"
                    + transaction.getType() + "|"
                    + transaction.getAmountPaise() + "|"
                    + reference.toUpperCase(Locale.ROOT);
        }
        return "body|" + transaction.getSender() + "|" + transaction.getBodyFingerprint();
    }

    public static final class Result {
        private final List<DerivedTransaction> transactions;
        private final int duplicatesRemoved;

        private Result(List<DerivedTransaction> transactions, int duplicatesRemoved) {
            this.transactions = Collections.unmodifiableList(transactions);
            this.duplicatesRemoved = duplicatesRemoved;
        }

        public List<DerivedTransaction> getTransactions() {
            return transactions;
        }

        public int getDuplicatesRemoved() {
            return duplicatesRemoved;
        }
    }
}
