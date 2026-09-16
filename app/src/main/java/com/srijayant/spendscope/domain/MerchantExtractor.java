package com.srijayant.spendscope.domain;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MerchantExtractor {
    private static final Pattern MESSAGE_REJECT = Pattern.compile(
            "\\b(?:NAV OF|INVOICE DATED|BE PAID BY|"
                    + "AVOID (?:LATE|CHARGES|DISCONNECTION)|GET (?:FLAT )?RS|"
                    + "& GET A FREE|WIN VOUCHERS|PRE-APPROVED|RENEW THE SUBSCRIPTION|"
                    + "WEB CHECK-IN|NON PAYMENT SUPPLY)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AXIS_UPI = Pattern.compile(
            "\\bUPI/(P2M|P2A)/[A-Z0-9*]+/([^.,;]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ICICI_CC_UPI = Pattern.compile(
            "\\bUPI-[A-Z0-9*]{4,}-([A-Z][A-Z0-9 &'_-]{1,60}?)(?=\\s*(?:[.,;]|$))",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern LONG_UPI = Pattern.compile(
            "(?:\\bUPI-)?([A-Z][A-Z ]{1,40})-"
                    + "([A-Z0-9._-]{2,}@[A-Z0-9._-]{2,})-"
                    + "([A-Z]{4}0[A-Z0-9]{6})-(\\d{6,})-([A-Z0-9 ]+?)(?:\\.AVL)?"
                    + "(?=\\s|[.,;]|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CARD_POS = Pattern.compile(
            "\\bat\\s+([A-Z0-9][A-Z0-9 .&'_-]{1,60}?)\\s+on\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REFUND = Pattern.compile(
            "\\b(?:REFUND\\s+(?:OF\\s+)?(?:RS\\.?|INR|₹)?\\s*[\\d,.]*\\s*)?"
                    + "(?:FROM\\s+)?([A-Z0-9][A-Z0-9 .&'@_-]{1,60}?)\\s+"
                    + "(?:HAS BEEN|WILL BE)\\s+(?:CREDITED|PROCESSED)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VPA = Pattern.compile(
            "\\b([A-Z0-9._-]{2,}@[A-Z0-9._-]{2,})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FREE_TEXT = Pattern.compile(
            "\\b(?:AT|TO|TOWARDS)\\s+([A-Z0-9][A-Z0-9 .&'@_-]{1,60}?)"
                    + "(?=\\s+(?:ON|VIA|USING|REF|REFERENCE|UPI|TXN|TRANSACTION|AVL|"
                    + "AVAILABLE|BAL|A/C|ACCOUNT|WITH)\\b|[().,;]|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SENDER_ID = Pattern.compile("^[A-Z]{2}-[A-Z0-9]{3,8}(?:-[A-Z])?$");
    private static final Pattern MASKED_CARD = Pattern.compile("^CARD\\s+\\d{4}X$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADDRESS = Pattern.compile(
            "\\b(?:ROAD|RD|CHOWK|STATION|BRANCH)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DATE = Pattern.compile(
            "^(?:\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}|\\d{1,2}-[A-Z]{3}-\\d{2,4})$",
            Pattern.CASE_INSENSITIVE
    );

    public MerchantExtraction extract(String body) {
        if (body == null || body.isBlank() || MESSAGE_REJECT.matcher(body).find()) {
            return new MerchantExtraction(null, null, null, false);
        }
        String normalized = body.replaceAll("\\s+", " ").trim();
        String vpa = find(VPA, normalized, 1);

        Matcher longUpi = LONG_UPI.matcher(normalized);
        if (longUpi.find()) {
            return new MerchantExtraction(
                    clean(longUpi.group(1)),
                    longUpi.group(2),
                    cleanRemark(longUpi.group(5)),
                    false
            );
        }

        Matcher axis = AXIS_UPI.matcher(normalized);
        if (axis.find()) {
            boolean p2a = axis.group(1).equalsIgnoreCase("P2A");
            return new MerchantExtraction(clean(axis.group(2)), vpa, null, p2a);
        }

        String structured = find(ICICI_CC_UPI, normalized, 1);
        if (structured == null) {
            structured = find(CARD_POS, normalized, 1);
        }
        if (structured == null) {
            structured = find(REFUND, normalized, 1);
        }
        if (structured != null) {
            String cleaned = clean(structured);
            return new MerchantExtraction(
                    isRejectedCandidate(cleaned) ? null : cleaned,
                    vpa,
                    null,
                    false
            );
        }

        String fallback = find(FREE_TEXT, normalized, 1);
        fallback = clean(fallback);
        if (isRejectedCandidate(fallback)) {
            fallback = null;
        }
        if (fallback == null && vpa != null) {
            fallback = vpa;
        }
        return new MerchantExtraction(fallback, vpa, null, false);
    }

    public boolean rejectsMessage(String body) {
        return body == null || MESSAGE_REJECT.matcher(body).find();
    }

    private String find(Pattern pattern, String body, int group) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(group) : null;
    }

    private String cleanRemark(String value) {
        String cleaned = clean(value);
        return cleaned == null ? null : cleaned.replaceFirst("(?i)\\.AVL$", "").trim();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value
                .replaceFirst("(?i)^VPA\\s+", "")
                .replaceFirst("(?i)\\s+NOT\\s+YOU\\??.*$", "")
                .replaceAll("(?i)\\s+(?:HAS|FOR|DATED)$", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private boolean isRejectedCandidate(String value) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String uppercase = value.toUpperCase(Locale.ROOT);
        return uppercase.matches("\\d+")
                || DATE.matcher(uppercase).matches()
                || uppercase.matches("(?:PAY|RS|YOU|US|WWW|STOP|TODAY)")
                || SENDER_ID.matcher(uppercase).matches()
                || MASKED_CARD.matcher(uppercase).matches()
                || ADDRESS.matcher(uppercase).find();
    }
}
