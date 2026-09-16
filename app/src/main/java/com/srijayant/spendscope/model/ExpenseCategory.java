package com.srijayant.spendscope.model;

public enum ExpenseCategory {
    FOOD("Food & dining"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills & utilities"),
    HEALTH("Health"),
    ENTERTAINMENT("Entertainment"),
    CASH("Cash withdrawal"),
    TRANSFER("Transfers"),
    OTHER("Other");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
