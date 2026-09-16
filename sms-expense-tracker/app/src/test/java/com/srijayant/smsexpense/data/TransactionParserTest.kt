package com.srijayant.smsexpense.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionParserTest {

    private fun sms(body: String, sender: String = "VM-HDFCBK") =
        SmsMessage(sender = sender, body = body, timestampMillis = 1_757_900_000_000)

    @Test
    fun `parses UPI debit with VPA merchant`() {
        val txn = TransactionParser.parse(
            sms("Rs.450.00 debited from A/c XX1234 on 12-09-26 to VPA swiggy.upi@icici. UPI Ref 4256. Not you? Call 18002586161.")
        )
        assertNotNull(txn)
        assertEquals(450.0, txn!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, txn.type)
        assertEquals("Swiggy.upi", txn.merchant)
        assertEquals(Category.FOOD, txn.category)
    }

    @Test
    fun `parses card spend with merchant after at`() {
        val txn = TransactionParser.parse(
            sms("INR 2,499.00 spent on HDFC Bank Card xx5678 at AMAZON on 2026-09-10:14:22. Avl bal INR 55,000.")
        )
        assertNotNull(txn)
        assertEquals(2499.0, txn!!.amount, 0.001)
        assertEquals(TransactionType.DEBIT, txn.type)
        assertEquals("AMAZON", txn.merchant)
        assertEquals(Category.SHOPPING, txn.category)
    }

    @Test
    fun `parses salary credit`() {
        val txn = TransactionParser.parse(
            sms("Your A/c XX9876 is credited with Rs.85,000.00 on 01-09-26 by a/c linked to VPA acmecorp@ybl (UPI Ref no 425).")
        )
        assertNotNull(txn)
        assertEquals(85000.0, txn!!.amount, 0.001)
        assertEquals(TransactionType.CREDIT, txn.type)
    }

    @Test
    fun `debited-and-credited-to-beneficiary message is a debit`() {
        val txn = TransactionParser.parse(
            sms("Rs.1200 debited from A/c XX1111 and credited to raju@oksbi on 05-09-26. Ref 99.")
        )
        assertNotNull(txn)
        assertEquals(TransactionType.DEBIT, txn!!.type)
    }

    @Test
    fun `parses rupee symbol amounts`() {
        val txn = TransactionParser.parse(
            sms("Paid \u20B9199.00 to Netflix via UPI on 03-09-26. Ref 12345.")
        )
        assertNotNull(txn)
        assertEquals(199.0, txn!!.amount, 0.001)
        assertEquals(Category.ENTERTAINMENT, txn.category)
    }

    @Test
    fun `rejects OTP messages`() {
        assertNull(
            TransactionParser.parse(
                sms("764512 is your OTP for txn of Rs.5000 at Amazon. Do not share it with anyone.")
            )
        )
    }

    @Test
    fun `rejects promotional offers`() {
        assertNull(
            TransactionParser.parse(
                sms("Get a pre-approved loan of Rs.5,00,000 instantly! Apply now.")
            )
        )
    }

    @Test
    fun `rejects payment requests`() {
        assertNull(
            TransactionParser.parse(
                sms("Ramesh has requested Rs.750 from you on UPI. Approve in app.")
            )
        )
    }

    @Test
    fun `rejects failed transactions`() {
        assertNull(
            TransactionParser.parse(
                sms("Your payment of Rs.899 to Myntra failed. Amount will be refunded in 3 days.")
            )
        )
    }

    @Test
    fun `rejects messages from personal phone numbers`() {
        assertNull(
            TransactionParser.parse(
                sms("I paid Rs.500 to the plumber today", sender = "+919812345678")
            )
        )
    }

    @Test
    fun `rejects messages without amounts`() {
        assertNull(
            TransactionParser.parse(sms("Your account statement for August is ready."))
        )
    }

    @Test
    fun `ATM withdrawal categorised as cash`() {
        val txn = TransactionParser.parse(
            sms("Rs.10,000 withdrawn from HDFC Bank ATM at PUNE MG RD on 08-09-26. Avl bal Rs.42,000.")
        )
        assertNotNull(txn)
        assertEquals(Category.ATM, txn!!.category)
        assertEquals(TransactionType.DEBIT, txn.type)
    }

    @Test
    fun `fuel purchase categorised`() {
        val txn = TransactionParser.parse(
            sms("Rs.2000.00 spent on Card xx4321 at HPCL FILLING STATION on 09-09-26.")
        )
        assertNotNull(txn)
        assertEquals(Category.FUEL, txn!!.category)
    }

    @Test
    fun `uncategorised UPI payment falls back to transfers`() {
        val txn = TransactionParser.parse(
            sms("Rs.300 debited from A/c XX1234 to VPA friend123@oksbi via UPI. Ref 777.")
        )
        assertNotNull(txn)
        assertEquals(Category.TRANSFER, txn!!.category)
    }
}
