package com.srijayant.spendscope.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public final class Expense {
    private final long messageId;
    private final BigDecimal amount;
    private final String merchant;
    private final ExpenseCategory category;
    private final Instant timestamp;

    public Expense(
            long messageId,
            BigDecimal amount,
            String merchant,
            ExpenseCategory category,
            Instant timestamp
    ) {
        this.messageId = messageId;
        this.amount = Objects.requireNonNull(amount);
        this.merchant = Objects.requireNonNull(merchant);
        this.category = Objects.requireNonNull(category);
        this.timestamp = Objects.requireNonNull(timestamp);
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

    public Instant getTimestamp() {
        return timestamp;
    }
}
