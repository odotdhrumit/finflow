package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.FinanceRepository
import com.example.parser.DetectedTransactionType
import com.example.parser.SmsTransactionParser
import com.example.util.SecurityHelper
import com.example.viewmodel.FinanceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinFlowHardeningTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var application: FinFlowApplication

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        application = context as FinFlowApplication
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(database.financeDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    // --- TEST A: ₹400 credit ---
    @Test
    fun testA_AmountParsing_400() {
        val sms = "Your A/C ending 1234 has been credited by ₹400 on 01-Oct-26."
        val parsed = SmsTransactionParser.parse("SBI", sms)
        assertNotNull(parsed)
        assertEquals(400.0, parsed!!.amount, 0.001)
    }

    // --- TEST B: ₹4,000 credit (The critical bug was ₹4,000 parsed as ₹400) ---
    @Test
    fun testB_AmountParsing_4000_WithComma_And_WithoutComma() {
        // Formatted with comma
        val sms1 = "Dear SBI User, your A/C ending 1234 has been credited by ₹4,000 on 01-Oct-26."
        val parsed1 = SmsTransactionParser.parse("SBI", sms1)
        assertNotNull(parsed1)
        assertEquals(4000.0, parsed1!!.amount, 0.001)

        // Raw ₹4000 without comma
        val sms2 = "Your A/C 1234 is credited with ₹4000 via UPI."
        val parsed2 = SmsTransactionParser.parse("SBI", sms2)
        assertNotNull(parsed2)
        assertEquals(4000.0, parsed2!!.amount, 0.001)

        // ₹4,000.00 with decimals
        val sms3 = "Rs. 4,000.00 credited to A/c XX1234."
        val parsed3 = SmsTransactionParser.parse("HDFC", sms3)
        assertNotNull(parsed3)
        assertEquals(4000.0, parsed3!!.amount, 0.001)

        // INR 4,000 format
        val sms4 = "A/c 1234 credited with INR 4,000 via UPI."
        val parsed4 = SmsTransactionParser.parse("ICICI", sms4)
        assertNotNull(parsed4)
        assertEquals(4000.0, parsed4!!.amount, 0.001)

        // INR4000 without space or comma
        val sms5 = "INR4000 credited to account ending 1234."
        val parsed5 = SmsTransactionParser.parse("Axis", sms5)
        assertNotNull(parsed5)
        assertEquals(4000.0, parsed5!!.amount, 0.001)

        // Decimal amount ₹4,000.50
        val sms6 = "₹4,000.50 received from Amit via UPI Ref 888888."
        val parsed6 = SmsTransactionParser.parse("PhonePe", sms6)
        assertNotNull(parsed6)
        assertEquals(4000.50, parsed6!!.amount, 0.001)
    }

    // --- TEST C: ₹40,000 credit ---
    @Test
    fun testC_AmountParsing_40000() {
        val sms = "A/C ending 1234 credited with ₹40,000 on 01-Oct-26."
        val parsed = SmsTransactionParser.parse("SBI", sms)
        assertNotNull(parsed)
        assertEquals(40000.0, parsed!!.amount, 0.001)
    }

    // --- TEST D: ₹4,00,000 credit (Indian Lakh Format) ---
    @Test
    fun testD_AmountParsing_400000() {
        val sms = "A/C 1234 credited by ₹4,00,000.00 on 01-Oct-26 from Home Loan."
        val parsed = SmsTransactionParser.parse("SBI", sms)
        assertNotNull(parsed)
        assertEquals(400000.0, parsed!!.amount, 0.001)
    }

    // --- TEST E: Same SMS received twice -> Expected = ONE transaction ---
    @Test
    fun testE_SameSmsReceivedTwice_ResultsInOneTransaction() = runBlocking {
        // Setup active account and enable auto-confirm
        val accId = repository.insertAccount(
            Account(name = "SBI Savings", bankName = "SBI", accountNumberLast4 = "1234", openingBalance = 10000.0)
        )
        repository.saveSettings(AppSettings(autoConfirmTrustedSms = true))

        val sms = "Your A/C ending 1234 is credited by ₹4,000 on 01-Oct. Ref 987654321."
        val parsed1 = SmsTransactionParser.parse("SBI", sms, timestamp = 1700000000000L)
        val parsed2 = SmsTransactionParser.parse("SBI", sms, timestamp = 1700000005000L) // 5s later

        assertNotNull(parsed1)
        assertNotNull(parsed2)

        repository.processParsedTransaction(parsed1!!, DetectedSourceType.SMS)
        repository.processParsedTransaction(parsed2!!, DetectedSourceType.SMS)

        val transactions = database.financeDao().getAllConfirmedTransactionsDirect()
        assertEquals("Duplicate SMS must result in exactly ONE transaction", 1, transactions.size)
        assertEquals(4000.0, transactions[0].amount, 0.001)
    }

    // --- TEST F: Same transaction received through SMS + notification -> Expected = ONE transaction ---
    @Test
    fun testF_SmsAndNotification_MergedIntoOneTransaction() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "HDFC Bank", bankName = "HDFC", accountNumberLast4 = "5678", openingBalance = 5000.0)
        )
        repository.saveSettings(AppSettings(autoConfirmTrustedSms = true))

        val sms = "Your A/c 5678 is credited by ₹4,000 on 01-Oct. UPI Ref 123456789."
        val notif = "₹4,000 received from Rahul via UPI Ref 123456789."

        val parsedSms = SmsTransactionParser.parse("HDFC", sms, timestamp = 1700000000000L)
        val parsedNotif = SmsTransactionParser.parse("PhonePe", notif, timestamp = 1700000010000L)

        assertNotNull(parsedSms)
        assertNotNull(parsedNotif)

        repository.processParsedTransaction(parsedSms!!, DetectedSourceType.SMS)
        repository.processParsedTransaction(parsedNotif!!, DetectedSourceType.NOTIFICATION)

        val transactions = database.financeDao().getAllConfirmedTransactionsDirect()
        assertEquals("SMS + Notification must be merged into ONE transaction", 1, transactions.size)
        assertEquals(4000.0, transactions[0].amount, 0.001)
        assertEquals(TransactionSource.SMS_AND_NOTIFICATION, transactions[0].source)
        assertEquals("123456789", transactions[0].referenceNumber)
    }

    // --- TEST G: Two genuine ₹4,000 transactions with different reference IDs -> Expected = TWO transactions ---
    @Test
    fun testG_TwoGenuineTransactionsWithDifferentRef_ResultsInTwoTransactions() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "SBI Savings", bankName = "SBI", accountNumberLast4 = "1234", openingBalance = 10000.0)
        )
        repository.saveSettings(AppSettings(autoConfirmTrustedSms = true))

        val tx1 = "A/C ending 1234 credited with ₹4,000. Ref 111111."
        val tx2 = "A/C ending 1234 credited with ₹4,000. Ref 222222."

        val p1 = SmsTransactionParser.parse("SBI", tx1, timestamp = 1700000000000L)
        val p2 = SmsTransactionParser.parse("SBI", tx2, timestamp = 1700000100000L)

        assertNotNull(p1)
        assertNotNull(p2)

        repository.processParsedTransaction(p1!!, DetectedSourceType.SMS)
        repository.processParsedTransaction(p2!!, DetectedSourceType.SMS)

        val transactions = database.financeDao().getAllConfirmedTransactionsDirect()
        assertEquals("Two genuine transactions with different reference IDs must remain TWO transactions", 2, transactions.size)
    }

    // --- TEST H: ₹4,000 credit -> Expected type = CREDIT ---
    @Test
    fun testH_CreditTypeDetection() {
        val sms = "Your A/C ending 1234 has been credited by ₹4,000 on 01-Oct-26."
        val parsed = SmsTransactionParser.parse("SBI", sms)
        assertNotNull(parsed)
        assertEquals(DetectedTransactionType.CREDIT, parsed!!.detectedType)
        assertEquals(TransactionType.INCOME, parsed.transactionType)
    }

    // --- TEST I: ₹4,000 debit -> Expected type = DEBIT ---
    @Test
    fun testI_DebitTypeDetection() {
        val sms = "Your A/C ending 1234 is debited by ₹4,000 on 01-Oct-26 for UPI payment to Swiggy."
        val parsed = SmsTransactionParser.parse("SBI", sms)
        assertNotNull(parsed)
        assertEquals(DetectedTransactionType.DEBIT, parsed!!.detectedType)
        assertEquals(TransactionType.EXPENSE, parsed.transactionType)
    }

    // --- TEST J: Incorrect PIN -> Expected = app remains locked ---
    @Test
    fun testJ_IncorrectPin_RemainsLocked() = runBlocking {
        val pinHash = SecurityHelper.hashPin("1234")
        repository.saveSettings(AppSettings(appLockEnabled = true, pinHash = pinHash))

        val viewModel = FinanceViewModel(application, repository)
        viewModel.lockApp()

        // Try unlocking with incorrect PIN
        val success = viewModel.unlockApp("9999")
        assertFalse("Incorrect PIN must fail unlock", success)
        assertTrue("App must remain locked on incorrect PIN", viewModel.isAppLocked.value)
    }

    // --- TEST K & L: Cancel/Failed biometric authentication -> Expected = app remains locked ---
    @Test
    fun testKL_BiometricCancelOrFailure_RemainsLocked() = runBlocking {
        val pinHash = SecurityHelper.hashPin("1234")
        repository.saveSettings(AppSettings(appLockEnabled = true, pinHash = pinHash, biometricEnabled = true))

        val viewModel = FinanceViewModel(application, repository)
        viewModel.lockApp()

        // Before any biometric success callback:
        assertTrue("App starts in locked state", viewModel.isAppLocked.value)

        // If biometric callback is cancelled or failed: onBiometricAuthenticationSuccess() is NEVER called!
        // App must strictly remain locked:
        assertTrue("App remains locked when biometric is cancelled or failed", viewModel.isAppLocked.value)
    }

    // --- TEST M: Successful Android system biometric authentication -> Expected = app unlocks ---
    @Test
    fun testM_SuccessfulBiometricAuthentication_UnlocksApp() = runBlocking {
        val pinHash = SecurityHelper.hashPin("1234")
        repository.saveSettings(AppSettings(appLockEnabled = true, pinHash = pinHash, biometricEnabled = true))

        val viewModel = FinanceViewModel(application, repository)
        viewModel.lockApp()
        assertTrue("App is initially locked", viewModel.isAppLocked.value)

        // When Android BiometricPrompt invokes onAuthenticationSucceeded:
        viewModel.onBiometricAuthenticationSuccess()
        assertFalse("App must unlock on verified system biometric authentication success", viewModel.isAppLocked.value)
    }
}
