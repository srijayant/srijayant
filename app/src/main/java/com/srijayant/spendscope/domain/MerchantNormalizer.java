package com.srijayant.spendscope.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class MerchantNormalizer {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PREFIX = Pattern.compile("^(?:(?:VPA|WWW)\\s+|PAYTM-|UPI-|LTD-|TP-)+");
    private static final Pattern SUFFIX = Pattern.compile(
            "\\s+(?:FROM PAYTM (?:BALANCE|WALLET)|HAS BEEN.*|"
                    + "FOR (?:RS|INR)\\s*[\\d,.]+|IN BANGALORE|"
                    + "INR BANGALORE IND|MUMBAI IND)$"
    );
    private static final Pattern VPA_PROCESSOR = Pattern.compile("\\.(?:PAYU|RZP|EBZ|CF)$");
    private static final Pattern TRAILING_ORDER_OR_DIGITS = Pattern.compile("(?:-ORDER)?\\d+$");
    private static final Pattern NON_KEY = Pattern.compile("[^A-Z0-9]");

    public String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toUpperCase(Locale.ROOT);
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ").trim();
        normalized = PREFIX.matcher(normalized).replaceFirst("");
        normalized = SUFFIX.matcher(normalized).replaceFirst("");
        int at = normalized.indexOf('@');
        if (at >= 0) {
            normalized = normalized.substring(0, at);
            normalized = VPA_PROCESSOR.matcher(normalized).replaceFirst("");
        }
        normalized = TRAILING_ORDER_OR_DIGITS.matcher(normalized).replaceFirst("");
        normalized = NON_KEY.matcher(normalized).replaceAll("");

        if (normalized.equals("THESOUL") || normalized.startsWith("THESOULE")) {
            return "THESOULE";
        }
        if (normalized.startsWith("ZOMATO")) {
            return "ZOMATO";
        }
        return normalized;
    }
}
