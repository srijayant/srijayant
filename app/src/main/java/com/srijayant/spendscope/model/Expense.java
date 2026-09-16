package com.srijayant.spendscope.model;

import java.time.Instant;
import java.util.Objects;

public final class Expense {
    private final long messageId;
    private final long amountPaise;
    private final String merchant;
    private final String vpa;
    private final ExpenseCategory category;
    private final ExpenseClassification automaticClassification;
    private final Instant timestamp;
    private final boolean userCategorized;
    private final boolean manual;

    public Expense(
            long messageId,
            long amountPaise,
            String merchant,
            String vpa,
            ExpenseCategory category,
            Instant timestamp
    ) {
        this(
                messageId,
                amountPaise,
                merchant,
                vpa,
                new ExpenseClassification(
                        category,
                        ClassificationConfidence.LOW,
                        ClassificationSource.UNKNOWN
                ),
                category,
                timestamp,
                false,
                false
        );
    }

    public Expense(
            long messageId,
            long amountPaise,
            String merchant,
            String vpa,
            ExpenseClassification automaticClassification,
            Instant timestamp
    ) {
        this(
                messageId,
                amountPaise,
                merchant,
                vpa,
                automaticClassification,
                automaticClassification.getCategory(),
                timestamp,
                false,
                false
        );
    }

    private Expense(
            long messageId,
            long amountPaise,
            String merchant,
            String vpa,
            ExpenseClassification automaticClassification,
            ExpenseCategory category,
            Instant timestamp,
            boolean userCategorized,
            boolean manual
    ) {
        this.messageId = messageId;
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("amountPaise must be positive");
        }
        this.amountPaise = amountPaise;
        this.merchant = Objects.requireNonNull(merchant);
        this.vpa = vpa;
        this.automaticClassification = Objects.requireNonNull(automaticClassification);
        this.category = Objects.requireNonNull(category);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.userCategorized = userCategorized;
        this.manual = manual;
    }

    public long getMessageId() {
        return messageId;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getVpa() {
        return vpa;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public ExpenseClassification getAutomaticClassification() {
        return automaticClassification;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public boolean isUserCategorized() {
        return userCategorized;
    }

    public boolean isManual() {
        return manual;
    }

    public boolean isExcludedFromSpend() {
        return automaticClassification.isExcludedFromSpend() && !userCategorized;
    }

    public Expense withUserCategory(ExpenseCategory userCategory) {
        return new Expense(
                messageId,
                amountPaise,
                merchant,
                vpa,
                automaticClassification,
                userCategory,
                timestamp,
                true,
                false
        );
    }

    public Expense withManualCategory(ExpenseCategory manualCategory) {
        return new Expense(
                messageId,
                amountPaise,
                merchant,
                vpa,
                automaticClassification,
                manualCategory,
                timestamp,
                true,
                true
        );
    }
}
