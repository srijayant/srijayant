package com.srijayant.spendscope.model;

import java.util.Objects;

public final class ExpenseClassification {
    private final ExpenseCategory category;
    private final ClassificationConfidence confidence;
    private final ClassificationSource source;
    private final boolean needsReview;
    private final boolean excludedFromSpend;

    public ExpenseClassification(
            ExpenseCategory category,
            ClassificationConfidence confidence,
            ClassificationSource source
    ) {
        this(category, confidence, source, false, false);
    }

    public ExpenseClassification(
            ExpenseCategory category,
            ClassificationConfidence confidence,
            ClassificationSource source,
            boolean needsReview,
            boolean excludedFromSpend
    ) {
        this.category = Objects.requireNonNull(category);
        this.confidence = Objects.requireNonNull(confidence);
        this.source = Objects.requireNonNull(source);
        this.needsReview = needsReview;
        this.excludedFromSpend = excludedFromSpend;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public ClassificationConfidence getConfidence() {
        return confidence;
    }

    public ClassificationSource getSource() {
        return source;
    }

    public boolean needsReview() {
        return needsReview;
    }

    public boolean isExcludedFromSpend() {
        return excludedFromSpend;
    }
}
