package com.srijayant.spendscope.model;

import java.time.Instant;
import java.util.Objects;

public final class DerivedTransaction {
    private final long smsId;
    private final String sender;
    private final Instant timestamp;
    private final long amountPaise;
    private final TransactionType type;
    private final String accountLast4;
    private final String instrument;
    private final String merchant;
    private final String vpa;
    private final String reference;
    private final Long balancePaise;
    private final String suggestedCategory;
    private final String categorySource;
    private final ClassificationConfidence confidence;
    private final boolean needsReview;
    private final boolean excludedFromSpend;
    private final String bodyFingerprint;
    private final int duplicateMessages;

    public DerivedTransaction(
            long smsId,
            String sender,
            Instant timestamp,
            long amountPaise,
            TransactionType type,
            String accountLast4,
            String instrument,
            String merchant,
            String vpa,
            String reference,
            Long balancePaise,
            String suggestedCategory,
            String categorySource,
            ClassificationConfidence confidence,
            boolean needsReview,
            boolean excludedFromSpend,
            String bodyFingerprint
    ) {
        this(
                smsId,
                sender,
                timestamp,
                amountPaise,
                type,
                accountLast4,
                instrument,
                merchant,
                vpa,
                reference,
                balancePaise,
                suggestedCategory,
                categorySource,
                confidence,
                needsReview,
                excludedFromSpend,
                bodyFingerprint,
                1
        );
    }

    private DerivedTransaction(
            long smsId,
            String sender,
            Instant timestamp,
            long amountPaise,
            TransactionType type,
            String accountLast4,
            String instrument,
            String merchant,
            String vpa,
            String reference,
            Long balancePaise,
            String suggestedCategory,
            String categorySource,
            ClassificationConfidence confidence,
            boolean needsReview,
            boolean excludedFromSpend,
            String bodyFingerprint,
            int duplicateMessages
    ) {
        this.smsId = smsId;
        this.sender = Objects.requireNonNull(sender);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.amountPaise = amountPaise;
        this.type = Objects.requireNonNull(type);
        this.accountLast4 = accountLast4;
        this.instrument = Objects.requireNonNull(instrument);
        this.merchant = merchant;
        this.vpa = vpa;
        this.reference = reference;
        this.balancePaise = balancePaise;
        this.suggestedCategory = Objects.requireNonNull(suggestedCategory);
        this.categorySource = Objects.requireNonNull(categorySource);
        this.confidence = Objects.requireNonNull(confidence);
        this.needsReview = needsReview;
        this.excludedFromSpend = excludedFromSpend;
        this.bodyFingerprint = Objects.requireNonNull(bodyFingerprint);
        this.duplicateMessages = duplicateMessages;
    }

    public long getSmsId() {
        return smsId;
    }

    public String getSender() {
        return sender;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public long getAmountPaise() {
        return amountPaise;
    }

    public TransactionType getType() {
        return type;
    }

    public String getAccountLast4() {
        return accountLast4;
    }

    public String getInstrument() {
        return instrument;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getVpa() {
        return vpa;
    }

    public String getReference() {
        return reference;
    }

    public Long getBalancePaise() {
        return balancePaise;
    }

    public String getSuggestedCategory() {
        return suggestedCategory;
    }

    public String getCategorySource() {
        return categorySource;
    }

    public ClassificationConfidence getConfidence() {
        return confidence;
    }

    public boolean needsReview() {
        return needsReview;
    }

    public boolean isExcludedFromSpend() {
        return excludedFromSpend;
    }

    public String getBodyFingerprint() {
        return bodyFingerprint;
    }

    public int getDuplicateMessages() {
        return duplicateMessages;
    }

    public DerivedTransaction withAdditionalDuplicate() {
        return new DerivedTransaction(
                smsId,
                sender,
                timestamp,
                amountPaise,
                type,
                accountLast4,
                instrument,
                merchant,
                vpa,
                reference,
                balancePaise,
                suggestedCategory,
                categorySource,
                confidence,
                needsReview,
                excludedFromSpend,
                bodyFingerprint,
                duplicateMessages + 1
        );
    }
}
