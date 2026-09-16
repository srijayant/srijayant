package com.srijayant.spendscope.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public final class MerchantNormalizerTest {
    @Parameterized.Parameters(name = "{index}: {0} -> {1}")
    public static Collection<Object[]> values() {
        return Arrays.asList(new Object[][]{
                {"THE SOUL", "THESOULE"},
                {"THESOULE", "THESOULE"},
                {"VPA THESOULEDSTORE", "THESOULE"},
                {"ZOMATO1", "ZOMATO"},
                {"ZOMATO@HDFCBANK", "ZOMATO"},
                {"ZOMATO-ORDER@PAYTM", "ZOMATO"},
                {"SWIGGY FROM PAYTM BALANCE", "SWIGGY"},
                {"SWIGGY FROM PAYTM WALLET", "SWIGGY"},
                {"VPA SWIGGYSTORES@ICICI", "SWIGGYSTORES"},
                {"WWW AMAZON", "AMAZON"},
                {"PAYTM-BILLDESK", "BILLDESK"},
                {"UPI-PORTER", "PORTER"},
                {"LTD-MERCHANT", "MERCHANT"},
                {"TP-MERCHANT", "MERCHANT"},
                {"BEWAKOOF1", "BEWAKOOF"},
                {"BIGBASKET.99313006", "BIGBASKET"},
                {"SHOP.PAYU@HDFCBANK", "SHOP"},
                {"SHOP.RZP@ICICI", "SHOP"},
                {"SHOP.EBZ@YBL", "SHOP"},
                {"SHOP.CF@AXIS", "SHOP"},
                {"MERCHANT HAS BEEN CREDITED", "MERCHANT"},
                {"MERCHANT HAS BEEN PROCESSED TODAY", "MERCHANT"},
                {"MERCHANT FOR RS 500", "MERCHANT"},
                {"MERCHANT FOR INR 1,250.00", "MERCHANT"},
                {"MERCHANT IN BANGALORE", "MERCHANT"},
                {"MERCHANT INR BANGALORE IND", "MERCHANT"},
                {"MERCHANT MUMBAI IND", "MERCHANT"},
                {" just fem ", "JUSTFEM"},
                {"M S SAPTRISHI", "MSSAPTRISHI"},
                {"A&B STORE", "ABSTORE"},
                {"O'BRIEN CAFE", "OBRIENCAFE"},
                {"SHOP-ONLINE", "SHOPONLINE"},
                {"SHOP_ONLINE", "SHOPONLINE"},
                {"SHOP.ONLINE", "SHOPONLINE"},
                {"  MULTIPLE   SPACES  ", "MULTIPLESPACES"},
                {"ＰＯＲＴＥＲ", "PORTER"},
                {"123 SHOP", "123SHOP"},
                {"SHOP123", "SHOP"},
                {"PAYTM-UPI-MERCHANT", "MERCHANT"},
                {"VPA ZEPTO@YESPAY", "ZEPTO"},
                {"WWW FLIPKART MUMBAI IND", "FLIPKART"},
                {"TP-ZOMATO9", "ZOMATO"},
                {"LTD-THESOULEDSTORE", "THESOULE"},
                {"VPA SWIGGYUPI@AXISBANK", "SWIGGYUPI"},
                {"", ""},
                {null, ""}
        });
    }

    private final String input;
    private final String expected;

    public MerchantNormalizerTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void normalizesMerchantIdentity() {
        assertEquals(expected, new MerchantNormalizer().normalize(input));
    }
}
