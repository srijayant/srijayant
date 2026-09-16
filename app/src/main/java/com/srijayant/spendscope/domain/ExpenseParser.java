package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExpenseParser {
    private static final Pattern EXPENSE_SIGNAL = Pattern.compile(
            "\\b(debited|spent|paid|purchase(?:d)?|charged|withdrawn|withdrawal|"
                    + "payment\\s+of|sent\\s+(?:to|via)|transferred\\s+to|upi\\s+(?:txn|payment))\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CREDIT_SIGNAL = Pattern.compile(
            "\\b(credited\\s+to|payment\\s+received|amount\\s+received|refund(?:ed)?|"
                    + "reversal|reversed|cashback\\s+(?:received|credited)|deposited)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AMOUNT = Pattern.compile(
            "(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
                    + "|([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:₹|inr\\b)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern MERCHANT = Pattern.compile(
            "\\b(?:at|to|towards)\\s+([a-z0-9][a-z0-9 .&'@_-]{1,50}?)"
                    + "(?=\\s+(?:on|via|using|ref|reference|upi|txn|transaction|avl|"
                    + "available|bal|a/c|account|with)\\b|[().,;]|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public Optional<Expense> parse(
            long messageId,
            String sender,
            String body,
            long timestampMillis
    ) {
        if (body == null || body.isBlank() || timestampMillis <= 0) {
            return Optional.empty();
        }

        String normalized = WHITESPACE.matcher(body).replaceAll(" ").trim();
        if (!EXPENSE_SIGNAL.matcher(normalized).find() || CREDIT_SIGNAL.matcher(normalized).find()) {
            return Optional.empty();
        }

        Matcher amountMatcher = AMOUNT.matcher(normalized);
        if (!amountMatcher.find()) {
            return Optional.empty();
        }

        String amountText = amountMatcher.group(1) != null
                ? amountMatcher.group(1)
                : amountMatcher.group(2);
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        String merchant = extractMerchant(normalized, sender);
        ExpenseCategory category = categorize(normalized + " " + merchant);
        return Optional.of(new Expense(
                messageId,
                amount,
                merchant,
                category,
                Instant.ofEpochMilli(timestampMillis)
        ));
    }

    private String extractMerchant(String body, String sender) {
        Matcher matcher = MERCHANT.matcher(body);
        if (matcher.find()) {
            String merchant = cleanMerchant(matcher.group(1));
            if (!merchant.isBlank() && !merchant.equalsIgnoreCase("your account")) {
                return merchant;
            }
        }

        if (sender != null && !sender.isBlank()) {
            String cleanedSender = sender.replaceAll("[^A-Za-z0-9 -]", "").trim();
            if (cleanedSender.matches(".*[A-Za-z].*")) {
                return cleanedSender;
            }
        }
        return "Transaction alert";
    }

    private String cleanMerchant(String merchant) {
        String cleaned = merchant
                .replaceFirst("(?i)^vpa\\s+", "")
                .replaceAll("(?i)\\s+(?:has|for|dated)$", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 36) {
            cleaned = cleaned.substring(0, 36).trim();
        }
        if (cleaned.equals(cleaned.toUpperCase(Locale.ROOT))) {
            StringBuilder title = new StringBuilder();
            for (String word : cleaned.toLowerCase(Locale.ROOT).split(" ")) {
                if (!word.isEmpty()) {
                    if (title.length() > 0) {
                        title.append(' ');
                    }
                    title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
            }
            return title.toString();
        }
        return cleaned;
    }

    private ExpenseCategory categorize(String text) {
        String value = text.toLowerCase(Locale.ROOT);
        if (containsAny(value, "atm", "cash withdrawal", "withdrawn")) {
            return ExpenseCategory.CASH;
        }
        if (containsAny(value, "grocery", "groceries", "supermarket", "bigbasket",
                "blinkit", "zepto", "dmart", "jiomart", "more retail")) {
            return ExpenseCategory.GROCERIES;
        }
        if (containsAny(value, "swiggy", "zomato", "restaurant", "cafe", "coffee",
                "food", "bakery", "dhaba", "biryani")) {
            return ExpenseCategory.FOOD;
        }
        if (containsAny(value, "fuel", "petrol", "diesel", "iocl", "hpcl", "bpcl",
                "indian oil", "hindustan petroleum", "bharat petroleum", "fastag")) {
            return ExpenseCategory.FUEL;
        }
        if (containsAny(value, "irctc", "railway", "airline", "flight", "hotel",
                "makemytrip", "cleartrip", "goibibo", "yatra", "airbnb", "redbus")) {
            return ExpenseCategory.TRAVEL;
        }
        if (containsAny(value, "uber", "ola", "rapido", "metro", "cab", "auto fare",
                "namma yatri")) {
            return ExpenseCategory.TRANSPORT;
        }
        if (containsAny(value, "amazon", "flipkart", "myntra", "shopping", "retail",
                "store", "mall", "meesho", "ajio")) {
            return ExpenseCategory.SHOPPING;
        }
        if (containsAny(value, "house rent", "monthly rent", "rent payment")) {
            return ExpenseCategory.RENT;
        }
        if (containsAny(value, " emi ", "loan repayment", "loan payment", "home loan",
                "personal loan", "vehicle loan")) {
            return ExpenseCategory.EMI;
        }
        if (containsAny(value, "electricity", "broadband", "recharge", "utility",
                "insurance", "postpaid", "water bill", "gas bill", "mobile bill",
                "dth", "bescom", "mseb", "tata power")) {
            return ExpenseCategory.BILLS;
        }
        if (containsAny(value, "hospital", "pharmacy", "medical", "clinic", "doctor",
                "health", "apollo", "medplus")) {
            return ExpenseCategory.HEALTH;
        }
        if (containsAny(value, "netflix", "spotify", "cinema", "movie", "gaming",
                "bookmyshow", "hotstar", "prime video")) {
            return ExpenseCategory.ENTERTAINMENT;
        }
        if (containsAny(value, "school", "college", "tuition", "course", "udemy",
                "education", "exam fee", "books")) {
            return ExpenseCategory.EDUCATION;
        }
        if (containsAny(value, "upi", "imps", "neft", "transferred", "sent to")) {
            return ExpenseCategory.TRANSFER;
        }
        return ExpenseCategory.OTHER;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
