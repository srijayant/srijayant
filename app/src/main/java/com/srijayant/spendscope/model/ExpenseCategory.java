package com.srijayant.spendscope.model;

import java.util.Locale;
import java.util.Optional;

public enum ExpenseCategory {
    FOOD("Food & Dining"),
    GROCERIES("Groceries"),
    TRANSPORT("Transport"),
    FUEL("Fuel"),
    SHOPPING("Online Shopping"),
    BILLS("Bills & Utilities"),
    RENT("Housing"),
    EMI("EMI & Loans"),
    HEALTH("Health"),
    ENTERTAINMENT("Entertainment"),
    EDUCATION("Education"),
    TRAVEL("Travel"),
    CASH("Cash"),
    TRANSFER("Personal Transfers"),
    OTHER("Uncategorized"),
    ALCOHOL("Alcohol"),
    CLOTHING_FOOTWEAR("Clothing & Footwear"),
    ELECTRONICS("Electronics"),
    HOME_FURNITURE("Home & Furniture"),
    JEWELLERY_WATCHES("Jewellery & Watches"),
    KIDS("Kids"),
    PERSONAL_CARE("Personal Care"),
    HOME_SERVICES("Home Services"),
    VEHICLE("Vehicle"),
    PHOTO_PRINTING("Photo & Printing"),
    FAMILY_TRANSFER("Family Transfer"),
    SELF_TRANSFER("Self Transfer"),
    WALLET_TOP_UP("Wallet Top-up"),
    CREDIT_CARD_PAYMENT("Credit Card Payment"),
    DIVIDEND("Dividend"),
    INSURANCE("Insurance"),
    INVESTMENTS("Investments"),
    FEES_CHARGES("Fees & Charges"),
    REFUNDS("Refunds"),
    SUBSCRIPTIONS("Subscriptions");

    private final String displayName;

    ExpenseCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Optional<ExpenseCategory> fromDisplayName(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ExpenseCategory category : values()) {
            if (category.displayName.toLowerCase(Locale.ROOT).equals(normalized)
                    || category.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }
}
