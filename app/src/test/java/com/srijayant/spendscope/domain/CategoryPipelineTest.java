package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.ClassificationConfidence;
import com.srijayant.spendscope.model.ClassificationSource;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExpenseClassification;
import com.srijayant.spendscope.model.TransactionType;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CategoryPipelineTest {
    @Test
    public void fullVpaRuleOutranksKeyIdentityAndMessageType() {
        CategoryPipeline pipeline = pipeline(
                new Rules(ExpenseCategory.EDUCATION, ExpenseCategory.TRAVEL),
                MerchantSeedData.empty()
        );
        ExpenseClassification result = pipeline.classify(input(
                "Refund has been credited",
                "SRIJAYANT",
                "srijayant@okicici",
                null,
                false
        ));

        assertEquals(ExpenseCategory.EDUCATION, result.getCategory());
        assertEquals(ClassificationSource.USER_RULE, result.getSource());
        assertEquals(ClassificationConfidence.CERTAIN, result.getConfidence());
    }

    @Test
    public void normalizedKeyRuleIsUsedWithoutVpa() {
        CategoryPipeline pipeline = pipeline(
                new Rules(null, ExpenseCategory.STATIONERY),
                MerchantSeedData.empty()
        );
        ExpenseClassification result = pipeline.classify(input(
                "INR 20 debited",
                "LUCKY",
                null,
                null,
                false
        ));

        assertEquals(ExpenseCategory.STATIONERY, result.getCategory());
        assertEquals(ClassificationSource.USER_RULE, result.getSource());
    }

    @Test
    public void longestPrefixWinsAtConfiguredConfidence() {
        Map<String, MerchantSeedData.Entry> exact = new LinkedHashMap<>();
        exact.put("MERCHANTLONG", new MerchantSeedData.Entry(
                ExpenseCategory.GROCERIES, "brand"
        ));
        exact.put("MERCHANT", new MerchantSeedData.Entry(
                ExpenseCategory.FOOD, "brand"
        ));
        CategoryPipeline pipeline = pipeline(
                new Rules(null, null),
                new MerchantSeedData(exact, List.of())
        );
        ExpenseClassification result = pipeline.classify(input(
                "INR 20 debited",
                "MERCHANTL",
                null,
                null,
                false
        ));

        assertEquals(ExpenseCategory.GROCERIES, result.getCategory());
        assertEquals(ClassificationSource.PREFIX_SEED, result.getSource());
        assertEquals(ClassificationConfidence.MEDIUM_HIGH, result.getConfidence());
    }

    @Test
    public void regexRulesAreOrderedFirstHitWins() {
        MerchantSeedData seeds = new MerchantSeedData(
                Map.of(),
                List.of(
                        new MerchantSeedData.RegexRule(ExpenseCategory.FOOD, "CAFE"),
                        new MerchantSeedData.RegexRule(ExpenseCategory.ENTERTAINMENT, "CAFE")
                )
        );
        ExpenseClassification result = pipeline(new Rules(null, null), seeds).classify(input(
                "INR 20 debited",
                "CAFE TEST",
                null,
                null,
                false
        ));

        assertEquals(ExpenseCategory.FOOD, result.getCategory());
        assertEquals(ClassificationSource.REGEX_SEED, result.getSource());
    }

    @Test
    public void gatewayUsesNearbyOrderBrandOrNeedsReview() {
        CategoryPipeline pipeline = TestMerchantSeeds.pipeline();
        ExpenseClassification correlated = pipeline.classify(new CategoryPipeline.Input(
                "Paid to RAZORPAY",
                TransactionType.DEBIT,
                new MerchantExtraction("RAZORPAY", "razorpay@icici", null, false),
                "Your SWIGGY order is confirmed"
        ));
        ExpenseClassification unresolved = pipeline.classify(new CategoryPipeline.Input(
                "Paid to PAYTMQR123",
                TransactionType.DEBIT,
                new MerchantExtraction("PAYTMQR123", "paytmqr123@paytm", null, false),
                null
        ));

        assertEquals(ExpenseCategory.FOOD, correlated.getCategory());
        assertEquals(ClassificationSource.ORDER_CORRELATION, correlated.getSource());
        assertFalse(correlated.needsReview());
        assertEquals(ExpenseCategory.OTHER, unresolved.getCategory());
        assertTrue(unresolved.needsReview());
    }

    @Test
    public void p2aIsPersonalTransferAndNeedsReview() {
        ExpenseClassification result = TestMerchantSeeds.pipeline().classify(input(
                "INR 100 debited",
                "ANITA",
                null,
                null,
                true
        ));
        assertEquals(ExpenseCategory.TRANSFER, result.getCategory());
        assertEquals(ClassificationSource.PERSON_HEURISTIC, result.getSource());
        assertTrue(result.needsReview());
    }

    private CategoryPipeline pipeline(
            CategoryPipeline.RuleLookup rules,
            MerchantSeedData seeds
    ) {
        return new CategoryPipeline(
                new MerchantNormalizer(),
                seeds,
                rules,
                new IdentityRules(List.of("SRIJAYANT"), List.of("KRITIKA"), true)
        );
    }

    private CategoryPipeline.Input input(
            String body,
            String merchant,
            String vpa,
            String remark,
            boolean p2a
    ) {
        return new CategoryPipeline.Input(
                body,
                TransactionType.DEBIT,
                new MerchantExtraction(merchant, vpa, remark, p2a),
                null
        );
    }

    private static final class Rules implements CategoryPipeline.RuleLookup {
        private final ExpenseCategory vpa;
        private final ExpenseCategory key;

        private Rules(ExpenseCategory vpa, ExpenseCategory key) {
            this.vpa = vpa;
            this.key = key;
        }

        @Override
        public Optional<ExpenseCategory> forVpa(String value) {
            return Optional.ofNullable(vpa);
        }

        @Override
        public Optional<ExpenseCategory> forKey(String value) {
            return Optional.ofNullable(key);
        }
    }
}
