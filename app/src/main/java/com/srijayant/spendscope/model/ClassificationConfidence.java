package com.srijayant.spendscope.model;

public enum ClassificationConfidence {
    CERTAIN(1.0),
    HIGH(0.95),
    MEDIUM_HIGH(0.85),
    MEDIUM(0.8),
    LOW(0.5);

    private final double score;

    ClassificationConfidence(double score) {
        this.score = score;
    }

    public double getScore() {
        return score;
    }
}
