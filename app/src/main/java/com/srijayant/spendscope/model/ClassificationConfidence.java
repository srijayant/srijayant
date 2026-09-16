package com.srijayant.spendscope.model;

public enum ClassificationConfidence {
    HIGH(3),
    MEDIUM(2),
    LOW(1);

    private final int score;

    ClassificationConfidence(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }
}
