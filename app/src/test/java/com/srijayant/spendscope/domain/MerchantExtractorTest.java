package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class MerchantExtractorTest {
    private final MerchantExtractor extractor = new MerchantExtractor();

    @Test
    public void extractsAxisP2mAndP2aSlots() {
        MerchantExtraction p2m = extractor.extract(
                "INR 75 debited UPI/P2M/625854269826/PORTER Not you?"
        );
        MerchantExtraction p2a = extractor.extract(
                "INR 75 debited UPI/P2A/625854269827/ANITA"
        );

        assertEquals("PORTER", p2m.getMerchantRaw());
        assertFalse(p2m.isP2A());
        assertEquals("ANITA", p2a.getMerchantRaw());
        assertTrue(p2a.isP2A());
    }

    @Test
    public void extractsIciciCardDescriptor() {
        MerchantExtraction result = extractor.extract(
                "Card debited for UPI-625509901586-THESOULE. To dispute."
        );
        assertEquals("THESOULE", result.getMerchantRaw());
    }

    @Test
    public void extractsLongUpiNameVpaAndRemark() {
        MerchantExtraction result = extractor.extract(
                "UPI-KRITIKA-KRITIKAIT09@OKICICI-ICIC0000564-103412177351-RENT.AVL"
        );
        assertEquals("KRITIKA", result.getMerchantRaw());
        assertEquals("KRITIKAIT09@OKICICI", result.getVpa());
        assertEquals("RENT", result.getRemark());
    }

    @Test
    public void extractsCardPosAndRefundShapes() {
        assertEquals("MYNTRA DESIGNS PVT L", extractor.extract(
                "INR 500 spent at MYNTRA DESIGNS PVT L on 15-SEP"
        ).getMerchantRaw());
        assertEquals("ZOMATO", extractor.extract(
                "Refund from ZOMATO has been credited"
        ).getMerchantRaw());
    }

    @Test
    public void rejectsDocumentAndPromotionNoise() {
        String[] rejected = {
                "NAV OF mutual fund units is INR 20",
                "INVOICE DATED 15-SEP is ready",
                "Amount must BE PAID BY tomorrow",
                "AVOID LATE charges by paying now",
                "GET FLAT RS 500 off today",
                "Buy now & GET A FREE voucher",
                "WIN VOUCHERS in our contest",
                "PRE-APPROVED loan available",
                "RENEW THE SUBSCRIPTION today",
                "Complete WEB CHECK-IN now",
                "NON PAYMENT SUPPLY may be stopped"
        };
        for (String body : rejected) {
            MerchantExtraction result = extractor.extract(body);
            assertNull(body, result.getMerchantRaw());
        }
    }

    @Test
    public void rejectsUnsafeFreeTextCandidates() {
        String[] rejected = {
                "INR 100 paid to 123456",
                "INR 100 paid to TODAY",
                "INR 100 paid to 15-09-2026",
                "INR 100 paid to AB-BANK-S",
                "INR 100 paid to MG ROAD",
                "INR 100 paid to CARD 1234X"
        };
        for (String body : rejected) {
            assertNull(body, extractor.extract(body).getMerchantRaw());
        }
    }
}
