package com.srijayant.spendscope.domain;

import com.srijayant.spendscope.model.ClassificationConfidence;
import com.srijayant.spendscope.model.ClassificationSource;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExpenseClassification;
import com.srijayant.spendscope.model.TransactionType;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class CategoryPipeline {
    private static final Pattern CC_PAYMENT = Pattern.compile(
            "\\b(?:CRED\\w*|DREAMPLUG\\w*|PAYTM CREDIT CARD BILL|"
                    + "PAYMENT OF RS\\.?\\s*[\\d,.]+ HAS BEEN RECEIVED ON YOUR .* CREDIT CARD)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern REFUND = Pattern.compile(
            "\\b(?:HAS BEEN CREDITED|HAS BEEN PROCESSED|WILL BE CREDITED BY|"
                    + "GPAYREFUND|REV-UPI-|REVERSAL|BHIMCASHBACK|REFUND)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern WALLET = Pattern.compile(
            "\\b(?:ADDMONEY|ADD-MONEY|AIRTELMONEY|OLAMONEY|GIFT CARD|GYFTR|EVOUCHERS)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CASH = Pattern.compile(
            "\\b(?:ATM|CASH ?WITHDRAWAL|WITHDRAWN)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern FEES = Pattern.compile(
            "\\b(?:BANK CHARGES|FEE|FEES|PENALTY)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern DIVIDEND = Pattern.compile(
            "\\b[A-Z][A-Z &]{2,}\\s+LTD\\s+\\d{1,2}-[A-Z]{3}-\\d{2}\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern GATEWAY = Pattern.compile(
            "(?:PAYTMQR|Q\\d+@YBL|GPAY-\\d+@OKBIZAXIS|BHARATPE\\d+@YESBANKLTD|"
                    + "PAYTM-\\d+@PAYTM|@AXL\\b|@PZ\\b|RAZORPAY|PAYU|CASHFREE|"
                    + "EASEBUZZ|INSTAMOJO|CCAVENUE|BILLDESK|MSWIPE|EURONETGPAY|"
                    + "PHONEPEMERCHANT|GOOGLEPAY|OKBIZ)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PERSONAL_VPA = Pattern.compile(
            "^(?:\\d{10}|[A-Z][A-Z0-9._-]*)@(?:OKSBI|OKAXIS|OKICICI|OKHDFCBANK|YBL|IBL|PAYTM)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern BUSINESS_TOKEN = Pattern.compile(
            "\\b(?:ENTE|ENTER|AGENC|DISTRIBU|STORE|MART|TRADE|SONS|RETA|M S|"
                    + "SHREE|SAI|PVT|LTD)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PERSON_NAME = Pattern.compile(
            "^(?:MR |MRS )?[A-Z]+(?:\\s+[A-Z]+){1,2}$",
            Pattern.CASE_INSENSITIVE
    );

    private final MerchantNormalizer normalizer;
    private final MerchantSeedData seeds;
    private final RuleLookup userRules;
    private final IdentityRules identityRules;

    public CategoryPipeline(
            MerchantNormalizer normalizer,
            MerchantSeedData seeds,
            RuleLookup userRules,
            IdentityRules identityRules
    ) {
        this.normalizer = normalizer;
        this.seeds = seeds;
        this.userRules = userRules;
        this.identityRules = identityRules;
    }

    public ExpenseClassification classify(Input input) {
        MerchantExtraction extraction = input.getExtraction();
        String merchant = extraction.bestIdentity();
        String key = normalizer.normalize(merchant);

        Optional<ExpenseCategory> vpaRule = userRules.forVpa(extraction.getVpa());
        if (vpaRule.isPresent()) {
            return result(vpaRule.get(), ClassificationConfidence.CERTAIN,
                    ClassificationSource.USER_RULE, false, excluded(vpaRule.get()));
        }
        Optional<ExpenseCategory> keyRule = userRules.forKey(key);
        if (keyRule.isPresent()) {
            return result(keyRule.get(), ClassificationConfidence.CERTAIN,
                    ClassificationSource.USER_RULE, false, excluded(keyRule.get()));
        }
        if (identityRules.isSelf(extraction)) {
            return result(ExpenseCategory.SELF_TRANSFER, ClassificationConfidence.CERTAIN,
                    ClassificationSource.IDENTITY, false, true);
        }
        if (identityRules.isFamily(extraction)) {
            return result(ExpenseCategory.FAMILY_TRANSFER, ClassificationConfidence.CERTAIN,
                    ClassificationSource.IDENTITY, false,
                    identityRules.isFamilyExcludedFromSpend());
        }

        String body = input.getBody() == null ? "" : input.getBody();
        String messageText = (body + " " + merchant).toUpperCase(Locale.ROOT);
        if (CC_PAYMENT.matcher(messageText).find()) {
            return result(ExpenseCategory.CREDIT_CARD_PAYMENT, ClassificationConfidence.CERTAIN,
                    ClassificationSource.MESSAGE_TYPE, false, true);
        }
        if (input.getType() == TransactionType.REFUND || REFUND.matcher(messageText).find()) {
            return result(ExpenseCategory.REFUNDS, ClassificationConfidence.CERTAIN,
                    ClassificationSource.MESSAGE_TYPE, false, true);
        }
        if (WALLET.matcher(messageText).find()) {
            return result(ExpenseCategory.WALLET_TOP_UP, ClassificationConfidence.CERTAIN,
                    ClassificationSource.MESSAGE_TYPE, false, true);
        }
        if (input.getType() == TransactionType.CASH_WITHDRAWAL
                || CASH.matcher(messageText).find()) {
            return result(ExpenseCategory.CASH, ClassificationConfidence.CERTAIN,
                    ClassificationSource.MESSAGE_TYPE, false, false);
        }
        if (FEES.matcher(messageText).find()) {
            return result(ExpenseCategory.FEES_CHARGES, ClassificationConfidence.CERTAIN,
                    ClassificationSource.MESSAGE_TYPE, false, false);
        }
        if (input.getType() == TransactionType.CREDIT && DIVIDEND.matcher(messageText).find()) {
            return result(ExpenseCategory.DIVIDEND, ClassificationConfidence.CERTAIN,
                    ClassificationSource.MESSAGE_TYPE, false, true);
        }

        Optional<ExpenseCategory> remark = remarkCategory(extraction.getRemark());
        if (remark.isPresent()) {
            ExpenseCategory category = remark.get();
            return result(category, ClassificationConfidence.HIGH,
                    ClassificationSource.REMARK_HINT, false, excluded(category));
        }

        MerchantSeedData.Entry exact = seeds.getExact().get(key);
        if (usableSeed(exact)) {
            return result(exact.getCategory(), ClassificationConfidence.HIGH,
                    ClassificationSource.EXACT_SEED, false, excluded(exact.getCategory()));
        }

        MerchantSeedData.Entry prefix = longestPrefix(key);
        if (prefix != null) {
            return result(prefix.getCategory(), ClassificationConfidence.MEDIUM_HIGH,
                    ClassificationSource.PREFIX_SEED, false, excluded(prefix.getCategory()));
        }

        String regexText = merchant.toUpperCase(Locale.ROOT);
        for (MerchantSeedData.RegexRule rule : seeds.getRegexRules()) {
            if (rule.matches(regexText) || rule.matches(key)) {
                return result(rule.getCategory(), ClassificationConfidence.MEDIUM,
                        ClassificationSource.REGEX_SEED, false, excluded(rule.getCategory()));
            }
        }

        if (isGateway(extraction, messageText)) {
            ExpenseClassification correlation = correlated(input.getCorrelatedMerchant());
            if (correlation != null) {
                return correlation;
            }
            return review(ExpenseCategory.OTHER);
        }
        if (isPerson(extraction)) {
            return result(ExpenseCategory.TRANSFER, ClassificationConfidence.LOW,
                    ClassificationSource.PERSON_HEURISTIC, true, false);
        }
        return review(ExpenseCategory.OTHER);
    }

    private ExpenseClassification correlated(String merchant) {
        String key = normalizer.normalize(merchant);
        MerchantSeedData.Entry exact = seeds.getExact().get(key);
        if (usableSeed(exact)) {
            return result(exact.getCategory(), ClassificationConfidence.MEDIUM,
                    ClassificationSource.ORDER_CORRELATION, false, excluded(exact.getCategory()));
        }
        MerchantSeedData.Entry prefix = longestPrefix(key);
        if (prefix != null) {
            return result(prefix.getCategory(), ClassificationConfidence.MEDIUM,
                    ClassificationSource.ORDER_CORRELATION, false, excluded(prefix.getCategory()));
        }
        String text = merchant == null ? "" : merchant.toUpperCase(Locale.ROOT);
        for (MerchantSeedData.RegexRule rule : seeds.getRegexRules()) {
            if (rule.matches(text) || rule.matches(key)) {
                return result(rule.getCategory(), ClassificationConfidence.MEDIUM,
                        ClassificationSource.ORDER_CORRELATION, false,
                        excluded(rule.getCategory()));
            }
        }
        return null;
    }

    private MerchantSeedData.Entry longestPrefix(String key) {
        if (key.length() < 5) {
            return null;
        }
        String bestKey = "";
        MerchantSeedData.Entry best = null;
        for (Map.Entry<String, MerchantSeedData.Entry> candidate : seeds.getExact().entrySet()) {
            String seedKey = candidate.getKey();
            if (!usableSeed(candidate.getValue())) {
                continue;
            }
            int commonLength = commonPrefixLength(key, seedKey);
            if (commonLength >= 5
                    && (key.startsWith(seedKey) || seedKey.startsWith(key))
                    && commonLength > bestKey.length()) {
                bestKey = seedKey.substring(0, commonLength);
                best = candidate.getValue();
            }
        }
        return best;
    }

    private int commonPrefixLength(String left, String right) {
        int length = Math.min(left.length(), right.length());
        int index = 0;
        while (index < length && left.charAt(index) == right.charAt(index)) {
            index++;
        }
        return index;
    }

    private boolean usableSeed(MerchantSeedData.Entry entry) {
        if (entry == null || entry.getCategory() == ExpenseCategory.OTHER) {
            return false;
        }
        String bucket = entry.getBucket();
        return !bucket.equals("gateway") && !bucket.equals("person")
                && !bucket.equals("noise") && !bucket.startsWith("you_tag_");
    }

    private Optional<ExpenseCategory> remarkCategory(String remark) {
        String value = normalizer.normalize(remark);
        if (value.isEmpty() || value.equals("UPI") || value.equals("FOR") || value.equals("TEST")) {
            return Optional.empty();
        }
        if (value.contains("RENT")) return Optional.of(ExpenseCategory.RENT);
        if (value.matches(".*LIC\\d*.*")) return Optional.of(ExpenseCategory.INSURANCE);
        if (value.contains("NPS")) return Optional.of(ExpenseCategory.INVESTMENTS);
        if (value.contains("LOAN") || value.contains("EMI")) return Optional.of(ExpenseCategory.EMI);
        if (value.contains("CAR")) return Optional.of(ExpenseCategory.VEHICLE);
        if (value.contains("TICKETS")) return Optional.of(ExpenseCategory.TRAVEL);
        if (value.contains("CHASMA")) return Optional.of(ExpenseCategory.HEALTH);
        if (value.contains("BED")) return Optional.of(ExpenseCategory.HOME_FURNITURE);
        if (value.contains("COOK")) return Optional.of(ExpenseCategory.HOME_SERVICES);
        if (value.contains("CASHWITHDRAWAL") || value.contains("WITHDRAW")) {
            return Optional.of(ExpenseCategory.CASH);
        }
        if (value.contains("MUMMY") || value.contains("MUM") || value.contains("MAA")) {
            return Optional.of(ExpenseCategory.FAMILY_TRANSFER);
        }
        return Optional.empty();
    }

    private boolean isGateway(MerchantExtraction extraction, String messageText) {
        return GATEWAY.matcher(messageText).find()
                || GATEWAY.matcher(extraction.getVpa() == null ? "" : extraction.getVpa()).find();
    }

    private boolean isPerson(MerchantExtraction extraction) {
        String merchant = extraction.getMerchantRaw() == null
                ? "" : extraction.getMerchantRaw().trim();
        String vpa = extraction.getVpa() == null ? "" : extraction.getVpa().trim();
        String combined = (merchant + " " + vpa).trim();
        if (BUSINESS_TOKEN.matcher(combined).find()) {
            return false;
        }
        return extraction.isP2A()
                || PERSONAL_VPA.matcher(vpa).matches()
                || merchant.toUpperCase(Locale.ROOT).startsWith("MR ")
                || merchant.toUpperCase(Locale.ROOT).startsWith("MRS ")
                || PERSON_NAME.matcher(merchant).matches();
    }

    private boolean excluded(ExpenseCategory category) {
        return category == ExpenseCategory.SELF_TRANSFER
                || category == ExpenseCategory.FAMILY_TRANSFER
                || category == ExpenseCategory.CREDIT_CARD_PAYMENT
                || category == ExpenseCategory.WALLET_TOP_UP
                || category == ExpenseCategory.REFUNDS
                || category == ExpenseCategory.DIVIDEND;
    }

    private ExpenseClassification review(ExpenseCategory category) {
        return result(category, ClassificationConfidence.LOW,
                ClassificationSource.UNKNOWN, true, excluded(category));
    }

    private ExpenseClassification result(
            ExpenseCategory category,
            ClassificationConfidence confidence,
            ClassificationSource source,
            boolean needsReview,
            boolean excludedFromSpend
    ) {
        return new ExpenseClassification(
                category, confidence, source, needsReview, excludedFromSpend
        );
    }

    public interface RuleLookup {
        Optional<ExpenseCategory> forVpa(String vpa);

        Optional<ExpenseCategory> forKey(String normalizedKey);
    }

    public static final class Input {
        private final String body;
        private final TransactionType type;
        private final MerchantExtraction extraction;
        private final String correlatedMerchant;

        public Input(
                String body,
                TransactionType type,
                MerchantExtraction extraction,
                String correlatedMerchant
        ) {
            this.body = body;
            this.type = type;
            this.extraction = extraction;
            this.correlatedMerchant = correlatedMerchant;
        }

        public String getBody() {
            return body;
        }

        public TransactionType getType() {
            return type;
        }

        public MerchantExtraction getExtraction() {
            return extraction;
        }

        public String getCorrelatedMerchant() {
            return correlatedMerchant;
        }
    }
}
