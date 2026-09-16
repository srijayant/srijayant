package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.ClassificationSource;
import com.srijayant.spendscope.model.DerivedTransaction;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExpenseClassification;
import com.srijayant.spendscope.model.TransactionType;

import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MerchantCategorizationAcceptanceTest {
    private static final long TIMESTAMP =
            Instant.parse("2026-09-16T10:00:00Z").toEpochMilli();

    private final TransactionMessageParser parser =
            new TransactionMessageParser(TestMerchantSeeds.pipeline());

    @Test
    public void creditCardPaymentIsSelfTransferAndExcluded() {
        DerivedTransaction result = parse(
                "Payment of Rs 2,667.12 has been received on your ICICI Bank Credit "
                        + "Card XX2013 through Bharat Bill Payment System on 15-SEP-26."
        );

        assertEquals(TransactionType.TRANSFER_SELF, result.getType());
        assertEquals("Credit Card Payment", result.getSuggestedCategory());
        assertTrue(result.isExcludedFromSpend());
    }

    @Test
    public void axisP2mExtractsPorterAndTransport() {
        DerivedTransaction result = parse(
                "INR 75.00 debited A/c no. XX4819 15-09-26, 15:45:47 "
                        + "UPI/P2M/625854269826/PORTER Not you? Call the bank."
        );

        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals(7_500L, result.getAmountPaise());
        assertEquals("PORTER", result.getMerchant());
        assertEquals("Transport", result.getSuggestedCategory());
    }

    @Test
    public void iciciTruncationAndKnownBrandsCategorize() {
        assertEquals("Clothing & Footwear", parse(
                "ICICI Bank Credit Card XX2005 debited for INR 719.00 on 12-Sep-26 "
                        + "for UPI-625509901586-THESOULE. To dispute contact the bank."
        ).getSuggestedCategory());
        assertEquals("Food & Dining", parse(
                "ICICI Bank Credit Card XX2005 debited for INR 393.00 on 11-Sep-26 "
                        + "for UPI-662094772023-Swiggy. To dispute contact the bank."
        ).getSuggestedCategory());
        assertEquals("Clothing & Footwear", parse(
                "ICICI Bank Credit Card XX2005 debited for INR 229.00 on 12-Sep-26 "
                        + "for UPI-625534076896-JUST FEM. To dispute contact the bank."
        ).getSuggestedCategory());
    }

    @Test
    public void unresolvedMerchantLearnsFromNormalizedRule() {
        DerivedTransaction first = parse(
                "ICICI Bank Credit Card XX2005 debited for INR 880.00 on 11-Sep-26 "
                        + "for UPI-662036075644-LUCKY. To dispute contact the bank."
        );
        assertEquals("Uncategorized", first.getSuggestedCategory());
        assertTrue(first.needsReview());

        Map<String, ExpenseCategory> keyRules = new HashMap<>();
        keyRules.put(new MerchantNormalizer().normalize("LUCKY"), ExpenseCategory.STATIONERY);
        CategoryPipeline.RuleLookup rules = new MapRules(keyRules);
        CategoryPipeline learned = new CategoryPipeline(
                new MerchantNormalizer(),
                TestMerchantSeeds.load(),
                rules,
                new IdentityRules(List.of(), List.of(), true)
        );
        DerivedTransaction next = new TransactionMessageParser(learned).parse(
                2,
                "ICICIB",
                "INR 100 debited for UPI-662036075645-LUCKY.",
                TIMESTAMP
        ).orElseThrow();

        assertEquals("Stationery", next.getSuggestedCategory());
        assertEquals(ClassificationSource.USER_RULE.name(), next.getCategorySource());
    }

    @Test
    public void rejectsDueReminder() {
        assertFalse(parser.parse(
                1,
                "ICICIB",
                "Your total amount due is Rs 40,972.74 or minimum of Rs 7,420.00 "
                        + "is due by 20-SEP-26.",
                TIMESTAMP
        ).isPresent());
    }

    @Test
    public void remarkHintOutranksMerchantSeed() {
        ExpenseClassification result = TestMerchantSeeds.pipeline().classify(
                new CategoryPipeline.Input(
                        "UPI transfer",
                        TransactionType.DEBIT,
                        new MerchantExtraction(
                                "KRISHNA",
                                "shop@example",
                                "RENT",
                                false
                        ),
                        null
                )
        );

        assertEquals(ExpenseCategory.RENT, result.getCategory());
        assertEquals(ClassificationSource.REMARK_HINT, result.getSource());
    }

    @Test
    public void anyVpaContainingSelfIdentityIsExcluded() {
        ExpenseClassification result = TestMerchantSeeds.pipeline().classify(
                new CategoryPipeline.Input(
                        "INR 100 sent via UPI",
                        TransactionType.DEBIT,
                        new MerchantExtraction(
                                "SRIJAYANT",
                                "srijayantsingh-4@okicici",
                                null,
                                false
                        ),
                        null
                )
        );

        assertEquals(ExpenseCategory.SELF_TRANSFER, result.getCategory());
        assertEquals(ClassificationSource.IDENTITY, result.getSource());
        assertTrue(result.isExcludedFromSpend());
    }

    private DerivedTransaction parse(String body) {
        return parser.parse(1, "ICICIB", body, TIMESTAMP).orElseThrow();
    }

    private static final class MapRules implements CategoryPipeline.RuleLookup {
        private final Map<String, ExpenseCategory> keyRules;

        private MapRules(Map<String, ExpenseCategory> keyRules) {
            this.keyRules = keyRules;
        }

        @Override
        public Optional<ExpenseCategory> forVpa(String vpa) {
            return Optional.empty();
        }

        @Override
        public Optional<ExpenseCategory> forKey(String normalizedKey) {
            return Optional.ofNullable(keyRules.get(normalizedKey));
        }
    }
}
