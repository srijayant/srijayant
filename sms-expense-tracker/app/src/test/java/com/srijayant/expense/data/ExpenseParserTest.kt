package com.srijayant.expense.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseParserTest {

    @Test
    fun parsesHdfcDebit() {
        val sms = "Dear Customer, Rs.1,250.00 debited from A/c XX1234 on 15-Sep-26 at SWIGGY BANGALORE. Avl Bal Rs.24,500.00"
        val parsed = ExpenseParser.parse(sms, "HDFCBK")
        assertNotNull(parsed)
        assertEquals(1250.0, parsed!!.amount, 0.01)
        assertEquals(ExpenseCategory.FOOD, parsed.category)
    }

    @Test
    fun parsesUpiSpend() {
        val sms = "INR 499.00 spent on your SBI Card XX9876 at NETFLIX on 10-09-26. Not you? Call 1800."
        val parsed = ExpenseParser.parse(sms, "SBICRD")
        assertNotNull(parsed)
        assertEquals(499.0, parsed!!.amount, 0.01)
        assertEquals(ExpenseCategory.ENTERTAINMENT, parsed.category)
    }

    @Test
    fun ignoresCreditAlerts() {
        val sms = "INR 5,000.00 credited to your A/c XX4321 on 12-Sep-26. Avl Bal INR 30,000.00"
        assertNull(ExpenseParser.parse(sms, "ICICIB"))
    }

    @Test
    fun categorizesTravel() {
        val sms = "Rs.320 paid via UPI to UBER INDIA on 14-Sep-26. UPI Ref 123456."
        val parsed = ExpenseParser.parse(sms, "AXISBK")
        assertNotNull(parsed)
        assertEquals(ExpenseCategory.TRAVEL, parsed!!.category)
    }
}
