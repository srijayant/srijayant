package com.srijayant.spendscope.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Expense {
    private final long messageId;
    private final BigDecimal amount;
    private final String merchant;
    private final ExpenseCategory category;
    private final ExpenseClassification automaticClassification;
    private final Instant timestamp;
    private final boolean userCategorized;

    public Expense(
            long messageId,
            BigDecimal amount,
            String merchant,
            ExpenseCategory category,
            Instant timestamp
    ) {
        this(
                messageId,
                amount,
                merchant,
                new ExpenseClassification(
                        category,
                        ClassificationConfidence.LOW,
                        ClassificationSource.UNKNOWN
                ),
                category,
                timestamp,
                false
        );
    }

    public Expense(
            long messageId,
            BigDecimal amount,
            String merchant,
            ExpenseClassification automaticClassification,
            Instant timestamp
    ) {
        this(
                messageId,
                amount,
                merchant,
                automaticClassification,
                automaticClassification.getCategory(),
                timestamp,
                false
        );
    }

    private Expense(
            long messageId,
            BigDecimal amount,
            String merchant,
            ExpenseClassification automaticClassification,
            ExpenseCategory category,
            Instant timestamp,
            boolean userCategorized
    ) {
        this.messageId = messageId;
        this.amount = Objects.requireNonNull(amount);
        this.merchant = Objects.requireNonNull(merchant);
        this.automaticClassification = Objects.requireNonNull(automaticClassification);
        this.category = Objects.requireNonNull(category);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.userCategorized = userCategorized;
    }

    public long getMessageId() {
        return messageId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMerchant() {
        return merchant;
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

    public Expense withUserCategory(ExpenseCategory userCategory) {
        return new Expense(
                messageId,
                amount,
                merchant,
                automaticClassification,
                userCategory,
                timestamp,
                true
        );
    }
}
