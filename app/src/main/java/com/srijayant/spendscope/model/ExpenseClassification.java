package com.srijayant.spendscope.model;

import java.util.Objects;

public final class ExpenseClassification {
    private final ExpenseCategory category;
    private final ClassificationConfidence confidence;
    private final ClassificationSource source;

    public ExpenseClassification(
            ExpenseCategory category,
            ClassificationConfidence confidence,
            ClassificationSource source
    ) {
        this.category = Objects.requireNonNull(category);
        this.confidence = Objects.requireNonNull(confidence);
        this.source = Objects.requireNonNull(source);
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
}
