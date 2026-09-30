package com.example

import com.example.parser.CategoryKeywordMatcher
import com.example.parser.SmsTransactionParser
import com.example.util.CurrencyFormatter
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testCurrencyFormatter_IndianNumbering() {
        assertEquals("₹1,25,450", CurrencyFormatter.format(125450.0))
        assertEquals("₹45,000", CurrencyFormatter.format(45000.0))
        assertEquals("₹22,850", CurrencyFormatter.format(22850.0))
        assertEquals("₹850", CurrencyFormatter.format(850.0))
        assertEquals("₹10,00,000", CurrencyFormatter.format(1000000.0))
        assertEquals("-₹5,000", CurrencyFormatter.format(-5000.0))
    }

    @Test
    fun testCurrencyFormatter_Compact() {
        assertEquals("₹1.25 L", CurrencyFormatter.formatCompact(125000.0))
        assertEquals("₹1.50 Cr", CurrencyFormatter.formatCompact(15000000.0))
        assertEquals("₹50.0 k", CurrencyFormatter.formatCompact(50000.0))
    }

    @Test
    fun testSmsParser_DebitTransaction() {
        val sms = "Your A/C XX1234 is debited by Rs.850.00 on 12-Oct-26 at Zomato. UPI Ref 38291039."
        val parsed = SmsTransactionParser.parse("AD-SBIINB", sms)

        assertNotNull("Should parse valid debit SMS", parsed)
        assertEquals("SBI", parsed?.bank)
        assertEquals("1234", parsed?.accountLast4)
        assertEquals(850.0, parsed?.amount ?: 0.0, 0.01)
        assertTrue("Should be marked as debit", parsed?.isDebit == true)
        assertEquals("Food", parsed?.suggestedCategory)
        assertTrue("Fingerprint should not be empty", parsed?.fingerprint?.isNotEmpty() == true)
    }

    @Test
    fun testSmsParser_CreditTransaction() {
        val sms = "Your a/c no. XX6789 is credited with INR 35,000.00 by salary from Tech Corp."
        val parsed = SmsTransactionParser.parse("HDFCBK", sms)

        assertNotNull("Should parse valid credit SMS", parsed)
        assertEquals("HDFC", parsed?.bank)
        assertEquals("6789", parsed?.accountLast4)
        assertEquals(35000.0, parsed?.amount ?: 0.0, 0.01)
        assertFalse("Should be marked as credit", parsed?.isDebit == true)
        assertEquals("Salary", parsed?.suggestedCategory)
    }

    @Test
    fun testSmsParser_OtpMessage_MustBeIgnored() {
        val otpSms = "123456 is your OTP to login to SBI net banking. Do not share this code with anyone."
        val parsed = SmsTransactionParser.parse("SBIINB", otpSms)
        assertNull("OTP message must be ignored and not created as transaction", parsed)
    }

    @Test
    fun testSmsParser_DuplicateFingerprint_IdenticalForSameTransaction() {
        val sms1 = "A/c 1234 debited for Rs 500 at Swiggy Ref 9999"
        val sms2 = "A/c 1234 debited for Rs 500 at Swiggy Ref 9999"

        val p1 = SmsTransactionParser.parse("HDFC", sms1, 1000000L)
        val p2 = SmsTransactionParser.parse("HDFC", sms2, 1000000L)

        assertNotNull(p1)
        assertNotNull(p2)
        assertEquals("Fingerprints must be identical for duplicate detection", p1?.fingerprint, p2?.fingerprint)
    }

    @Test
    fun testCategoryKeywordMatcher() {
        assertEquals("Food", CategoryKeywordMatcher.suggestCategory("Paid to Swiggy UPI", false))
        assertEquals("Food", CategoryKeywordMatcher.suggestCategory("Zomato online order", false))
        assertEquals("Shopping", CategoryKeywordMatcher.suggestCategory("Amazon India retail purchase", false))
        assertEquals("Transport", CategoryKeywordMatcher.suggestCategory("Uber trip ride", false))
        assertEquals("Entertainment", CategoryKeywordMatcher.suggestCategory("Netflix subscription", false))
        assertEquals("Salary", CategoryKeywordMatcher.suggestCategory("Monthly payroll transfer", true))
    }
}
