package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.srijayant.spendscope.model.ClassificationConfidence;
import com.srijayant.spendscope.model.DerivedTransaction;
import com.srijayant.spendscope.model.TransactionType;

import org.junit.Test;

import java.time.Instant;
import java.util.List;

public final class TransactionMessageParserTest {
    private final TransactionMessageParser parser = new TransactionMessageParser();
    private final long timestamp = Instant.parse("2026-09-16T10:00:00Z").toEpochMilli();

    @Test
    public void selectsDebitAmountInsteadOfEarlierBalance() {
        DerivedTransaction result = parser.parse(
                1,
                "VM-HDFCBK-S",
                "Avl Bal INR 10,000. INR 250 debited from A/c XX1234 at SWIGGY "
                        + "on 16-Sep. UPI Ref 123456789",
                timestamp
        ).orElseThrow();

        assertEquals(25_000L, result.getAmountPaise());
        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals("HDFCBK", result.getSender());
        assertEquals("1234", result.getAccountLast4());
        assertEquals("Swiggy", result.getMerchant());
        assertEquals("123456789", result.getReference());
        assertEquals("Food & dining", result.getSuggestedCategory());
        assertEquals(ClassificationConfidence.HIGH, result.getConfidence());
    }

    @Test
    public void parsesSalaryCreditAndDerivedDetails() {
        DerivedTransaction result = parser.parse(
                2,
                "AXISBK",
                "Salary of Rs. 85,000 credited to account XX7788 from ACME CORP "
                        + "on 16-Sep. Available balance INR 90,500",
                timestamp
        ).orElseThrow();

        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(8_500_000L, result.getAmountPaise());
        assertEquals(Long.valueOf(9_050_000L), result.getBalancePaise());
        assertEquals("Salary", result.getSuggestedCategory());
        assertEquals("Acme Corp", result.getMerchant());
        assertEquals(ClassificationConfidence.HIGH, result.getConfidence());
    }

    @Test
    public void parsesRefundAndUpiFields() {
        DerivedTransaction result = parser.parse(
                3,
                "ICICIB",
                "Refund of INR 799 received from shop@okicici. UPI Ref 99887766",
                timestamp
        ).orElseThrow();

        assertEquals(TransactionType.REFUND, result.getType());
        assertEquals(79_900L, result.getAmountPaise());
        assertEquals("shop@okicici", result.getVpa());
        assertEquals("Refunds", result.getSuggestedCategory());
    }

    @Test
    public void extractsMerchantFromMaskedIciciUpiDescriptor() {
        DerivedTransaction result = parser.parse(
                10,
                "ICICIB",
                "ICICI Bank Credit Card XX2908 debited for INR 719.00 on 12-Sep-26 "
                        + "for UPI-62********86-THESOULE. To dispute contact the bank.",
                timestamp
        ).orElseThrow();

        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals(71_900L, result.getAmountPaise());
        assertEquals("2908", result.getAccountLast4());
        assertEquals("UPI", result.getInstrument());
        assertEquals("Thesoule", result.getMerchant());
    }

    @Test
    public void rejectsOtpFailedAndNonTransactionMessages() {
        assertFalse(parser.parse(
                4,
                "HDFCBK",
                "OTP 123456 for transaction of INR 500",
                timestamp
        ).isPresent());
        assertFalse(parser.parse(
                5,
                "HDFCBK",
                "Your payment of INR 500 failed",
                timestamp
        ).isPresent());
        assertFalse(parser.parse(
                6,
                "SHOP",
                "Save INR 500 in our sale today",
                timestamp
        ).isPresent());
    }

    @Test
    public void keepsUnknownOptionalDetailsNull() {
        DerivedTransaction result = parser.parse(
                7,
                "SBIUPI",
                "Rs 100 sent via UPI",
                timestamp
        ).orElseThrow();

        assertNull(result.getAccountLast4());
        assertNull(result.getReference());
        assertTrue(result.getMerchant() == null || !result.getMerchant().isBlank());
    }

    @Test
    public void deduplicatesByReferenceAndCountsCollapsedMessages() {
        DerivedTransaction first = parser.parse(
                8,
                "HDFCBK",
                "INR 300 paid to CAFE via UPI. Ref No 76543210",
                timestamp
        ).orElseThrow();
        DerivedTransaction repeated = parser.parse(
                9,
                "HDFCBK",
                "INR 300 paid to CAFE via UPI; Ref No 76543210",
                timestamp + 1_000
        ).orElseThrow();

        TransactionDeduplicator.Result result =
                new TransactionDeduplicator().deduplicate(List.of(first, repeated));

        assertEquals(1, result.getTransactions().size());
        assertEquals(1, result.getDuplicatesRemoved());
        assertEquals(2, result.getTransactions().get(0).getDuplicateMessages());
    }
}
