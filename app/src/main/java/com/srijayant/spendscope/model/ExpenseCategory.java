package com.srijayant.spendscope.model;

public enum ExpenseCategory {
    FOOD("Food & dining"),
    GROCERIES("Groceries"),
    TRANSPORT("Transport"),
    FUEL("Fuel"),
    SHOPPING("Shopping"),
    BILLS("Bills & recharge"),
    RENT("Rent"),
    EMI("EMI & loans"),
    HEALTH("Healthcare"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    TRAVEL("Travel"),
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
