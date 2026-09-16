package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.ClassificationConfidence;
import com.srijayant.spendscope.model.ClassificationSource;
import com.srijayant.spendscope.model.DerivedTransaction;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExpenseClassification;
import com.srijayant.spendscope.model.TransactionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TransactionMessageParser {
    private static final Pattern DEBIT_SIGNAL = Pattern.compile(
            "\\b(debited|spent|paid|sent|withdrawn|withdrawal|purchase(?:d)?|"
                    + "txn\\s+of|used\\s+for|transferred\\s+to|charged)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CREDIT_SIGNAL = Pattern.compile(
            "\\b(credited|received|deposited|added\\s+to)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REFUND_SIGNAL = Pattern.compile(
            "\\b(refund(?:ed)?|reversal|reversed|cashback\\s+credited)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REJECT_SIGNAL = Pattern.compile(
            "\\b(otp|one[ -]time password|declined|failed|unsuccessful|"
                    + "could not be processed|will be debited|scheduled|upcoming|"
                    + "mandate (?:created|approved)|payment due|amount due|credit limit|"
                    + "is due by|minimum of|nav of|invoice dated|be paid by)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern AMOUNT = Pattern.compile(
            "(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)"
                    + "|([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:₹|inr\\b)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BALANCE = Pattern.compile(
            "\\b(?:avl\\.?\\s*bal|available\\s+balance|balance|bal)\\s*(?:is|:)?\\s*"
                    + "(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern ACCOUNT = Pattern.compile(
            "\\b(?:a/c|acct|account|card)\\s*(?:no\\.?)?\\s*(?:ending\\s*)?"
                    + "[xX*\\s-]*([0-9]{3,4})\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern VPA = Pattern.compile(
            "\\b([a-zA-Z0-9._-]{2,}@[a-zA-Z0-9._-]{2,})\\b"
    );
    private static final Pattern UPI_DESCRIPTOR_MERCHANT = Pattern.compile(
            "\\bupi[-/]\\s*[a-z0-9*]{4,}[-/]\\s*"
                    + "([a-z][a-z0-9 .&'_-]{1,49}?)"
                    + "(?=\\s*(?:[.,;]|$|\\bto\\s+dispute\\b))",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REFERENCE = Pattern.compile(
            "\\b(?:upi\\s*ref(?:erence)?|ref(?:erence)?\\s*(?:no\\.?|number)?|rrn|"
                    + "txn\\s*(?:id|no\\.?))[:\\s#-]*([a-zA-Z0-9]{6,})\\b"
                    + "|\\bUPI/(?:P2M|P2A)/([A-Z0-9]{6,})/",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CREDIT_MERCHANT = Pattern.compile(
            "\\b(?:from|info:)\\s+(?!a/c\\b|acct\\b|account\\b)"
                    + "([a-z0-9][a-z0-9 .&'@_-]{1,50}?)"
                    + "(?=\\s+(?:on|via|using|ref|reference|upi|txn|transaction|to)\\b"
                    + "|[().,;]|$)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BALANCE_CONTEXT = Pattern.compile(
            "\\b(bal|balance|available|avl|limit|outstanding)\\b[^₹0-9]{0,16}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final MerchantExtractor merchantExtractor;
    private final CategoryPipeline categoryPipeline;

    public TransactionMessageParser() {
        this(defaultPipeline());
    }

    public TransactionMessageParser(CategoryPipeline categoryPipeline) {
        this.merchantExtractor = new MerchantExtractor();
        this.categoryPipeline = categoryPipeline;
    }

    public Optional<DerivedTransaction> parse(
            long smsId,
            String sender,
            String body,
            long timestampMillis
    ) {
        return parse(smsId, sender, body, timestampMillis, null);
    }

    public Optional<DerivedTransaction> parse(
            long smsId,
            String sender,
            String body,
            long timestampMillis,
            String correlatedMerchant
    ) {
        if (body == null || body.isBlank() || timestampMillis <= 0
                || REJECT_SIGNAL.matcher(body).find()
                || merchantExtractor.rejectsMessage(body)) {
            return Optional.empty();
        }

        String normalizedBody = WHITESPACE.matcher(body).replaceAll(" ").trim();
        TransactionType type = detectType(normalizedBody);
        if (type == null) {
            return Optional.empty();
        }

        Pattern primarySignal = type == TransactionType.CREDIT
                ? CREDIT_SIGNAL
                : type == TransactionType.REFUND ? REFUND_SIGNAL : DEBIT_SIGNAL;
        Long amountPaise = selectTransactionAmount(normalizedBody, primarySignal);
        if (amountPaise == null || amountPaise <= 0) {
            return Optional.empty();
        }

        String normalizedSender = normalizeSender(sender);
        MerchantExtraction extraction = merchantExtractor.extract(normalizedBody);
        String vpa = extraction.getVpa();
        String merchant = extraction.getMerchantRaw();
        ExpenseClassification classification = categoryPipeline.classify(
                new CategoryPipeline.Input(
                        normalizedBody,
                        type,
                        extraction,
                        correlatedMerchant
                )
        );
        if (classification.getCategory() == ExpenseCategory.CREDIT_CARD_PAYMENT
                || classification.getCategory() == ExpenseCategory.SELF_TRANSFER
                || classification.getCategory() == ExpenseCategory.WALLET_TOP_UP) {
            type = TransactionType.TRANSFER_SELF;
        } else if (classification.getCategory() == ExpenseCategory.FAMILY_TRANSFER) {
            type = TransactionType.FAMILY_TRANSFER;
        }

        return Optional.of(new DerivedTransaction(
                smsId,
                normalizedSender,
                Instant.ofEpochMilli(timestampMillis),
                amountPaise,
                type,
                find(ACCOUNT, normalizedBody),
                detectInstrument(normalizedBody, type),
                merchant,
                vpa,
                findReference(normalizedBody),
                findBalance(normalizedBody),
                classification.getCategory().getDisplayName(),
                classification.getSource().name(),
                classification.getConfidence(),
                classification.needsReview(),
                classification.isExcludedFromSpend(),
                MerchantRuleKey.fromMerchant(normalizedSender + "|" + normalizedBody)
        ));
    }

    private static CategoryPipeline defaultPipeline() {
        List<MerchantSeedData.RegexRule> rules = List.of(
                new MerchantSeedData.RegexRule(ExpenseCategory.FOOD, "SWIGGY|ZOMATO"),
                new MerchantSeedData.RegexRule(
                        ExpenseCategory.CLOTHING_FOOTWEAR,
                        "THESOULE|THE SOUL|JUST ?FEM"
                ),
                new MerchantSeedData.RegexRule(ExpenseCategory.TRANSPORT, "PORTER|UBER|RAPIDO"),
                new MerchantSeedData.RegexRule(ExpenseCategory.GROCERIES, "BIGBASKET|BLINKIT|ZEPTO"),
                new MerchantSeedData.RegexRule(ExpenseCategory.FUEL, "INDIAN OIL|IOCL|HPCL|BPCL"),
                new MerchantSeedData.RegexRule(ExpenseCategory.SHOPPING, "AMAZON|FLIPKART|MYNTRA")
        );
        CategoryPipeline.RuleLookup noRules = new CategoryPipeline.RuleLookup() {
            @Override
            public Optional<ExpenseCategory> forVpa(String vpa) {
                return Optional.empty();
            }

            @Override
            public Optional<ExpenseCategory> forKey(String normalizedKey) {
                return Optional.empty();
            }
        };
        return new CategoryPipeline(
                new MerchantNormalizer(),
                new MerchantSeedData(Collections.emptyMap(), rules),
                noRules,
                new IdentityRules(
                        List.of("SRIJAYANT", "JAYANT-SRIJAYANTSINGH"),
                        List.of("KRITIKA", "KRITIKAIT09"),
                        true
                )
        );
    }

    private TransactionType detectType(String body) {
        if (REFUND_SIGNAL.matcher(body).find()) {
            return TransactionType.REFUND;
        }
        if (DEBIT_SIGNAL.matcher(body).find()) {
            if (containsAny(body.toLowerCase(Locale.ROOT), "atm", "withdrawn", "withdrawal")) {
                return TransactionType.CASH_WITHDRAWAL;
            }
            return TransactionType.DEBIT;
        }
        if (CREDIT_SIGNAL.matcher(body).find()) {
            return TransactionType.CREDIT;
        }
        return null;
    }

    private Long selectTransactionAmount(String body, Pattern signalPattern) {
        List<AmountCandidate> candidates = new ArrayList<>();
        Matcher matcher = AMOUNT.matcher(body);
        while (matcher.find()) {
            String value = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            int contextStart = Math.max(0, matcher.start() - 24);
            String precedingContext = body.substring(contextStart, matcher.start());
            if (!BALANCE_CONTEXT.matcher(precedingContext).find()) {
                Long paise = toPaise(value);
                if (paise != null) {
                    candidates.add(new AmountCandidate(matcher.start(), paise));
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }

        List<Integer> signalPositions = new ArrayList<>();
        Matcher signals = signalPattern.matcher(body);
        while (signals.find()) {
            signalPositions.add(signals.start());
        }
        if (signalPositions.isEmpty()) {
            return candidates.get(0).paise;
        }

        AmountCandidate best = candidates.get(0);
        int bestDistance = distanceToNearestSignal(best.position, signalPositions);
        for (int i = 1; i < candidates.size(); i++) {
            AmountCandidate candidate = candidates.get(i);
            int distance = distanceToNearestSignal(candidate.position, signalPositions);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best.paise;
    }

    private int distanceToNearestSignal(int position, List<Integer> signalPositions) {
        int minimum = Integer.MAX_VALUE;
        for (int signalPosition : signalPositions) {
            minimum = Math.min(minimum, Math.abs(position - signalPosition));
        }
        return minimum;
    }

    private Long findBalance(String body) {
        Matcher matcher = BALANCE.matcher(body);
        return matcher.find() ? toPaise(matcher.group(1)) : null;
    }

    private Long toPaise(String amount) {
        try {
            return new BigDecimal(amount.replace(",", ""))
                    .setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException | NumberFormatException invalidAmount) {
            return null;
        }
    }

    private String find(Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String findReference(String body) {
        Matcher matcher = REFERENCE.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private String findAndCleanMerchant(String body) {
        Matcher matcher = CREDIT_MERCHANT.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return cleanMerchantValue(matcher.group(1));
    }

    private String findAndCleanUpiDescriptor(String body) {
        Matcher matcher = UPI_DESCRIPTOR_MERCHANT.matcher(body);
        return matcher.find() ? cleanMerchantValue(matcher.group(1)) : null;
    }

    private String cleanMerchantValue(String merchant) {
        String value = WHITESPACE.matcher(merchant).replaceAll(" ").trim();
        if (value.equals(value.toUpperCase(Locale.ROOT))) {
            StringBuilder title = new StringBuilder();
            for (String word : value.toLowerCase(Locale.ROOT).split(" ")) {
                if (!word.isEmpty()) {
                    if (title.length() > 0) {
                        title.append(' ');
                    }
                    title.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                }
            }
            value = title.toString();
        }
        return MerchantRuleKey.isEligibleMerchant(value) ? value : null;
    }

    private String normalizeSender(String sender) {
        if (sender == null || sender.isBlank()) {
            return "UNKNOWN";
        }
        return sender.trim()
                .replaceFirst("(?i)^[A-Z]{2}-", "")
                .replaceFirst("(?i)-[A-Z]$", "")
                .toUpperCase(Locale.ROOT);
    }

    private String detectInstrument(String body, TransactionType type) {
        String value = body.toLowerCase(Locale.ROOT);
        if (value.contains("upi") || VPA.matcher(body).find()) {
            return "UPI";
        }
        if (value.contains("imps")) {
            return "IMPS";
        }
        if (value.contains("neft")) {
            return "NEFT";
        }
        if (value.contains("atm") || type == TransactionType.CASH_WITHDRAWAL) {
            return "ATM";
        }
        if (value.contains("card")) {
            return "CARD";
        }
        return "ACCOUNT";
    }

    private String incomeCategory(TransactionType type, String body) {
        String value = body.toLowerCase(Locale.ROOT);
        if (type == TransactionType.REFUND) {
            return "Refunds";
        }
        if (value.contains("salary")) {
            return "Salary";
        }
        if (value.contains("interest")) {
            return "Interest";
        }
        return "Other Income";
    }

    private ClassificationConfidence incomeConfidence(TransactionType type, String body) {
        if (type == TransactionType.REFUND) {
            return ClassificationConfidence.HIGH;
        }
        String value = body.toLowerCase(Locale.ROOT);
        if (value.contains("salary")) {
            return ClassificationConfidence.HIGH;
        }
        if (value.contains("interest")) {
            return ClassificationConfidence.MEDIUM;
        }
        return ClassificationConfidence.LOW;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private static final class AmountCandidate {
        private final int position;
        private final long paise;

        private AmountCandidate(int position, long paise) {
            this.position = position;
            this.paise = paise;
        }
    }
}
