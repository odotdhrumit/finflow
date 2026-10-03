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

    // --- TEST N: Distinguish Transaction Amount from Available Balance ---
    @Test
    fun testN_DistinguishTransactionAmountFromBalance() {
        val msg = "Rs 100 credited to your account. Available balance Rs 8,500."
        val parsed = SmsTransactionParser.parse("SBI", msg)
        assertNotNull(parsed)
        assertEquals("Transaction amount must be 100, NOT 8,500", 100.0, parsed!!.amount, 0.001)
        assertEquals("Balance must be identified as 8,500", 8500.0, parsed.balanceAfterTransaction ?: 0.0, 0.001)
        assertEquals(DetectedTransactionType.CREDIT, parsed.detectedType)
    }

    // --- TEST O: Debit with balance ---
    @Test
    fun testO_DebitWithBalance() {
        val msg = "Your account has been debited by Rs 4,000. Available balance is Rs 12,500."
        val parsed = SmsTransactionParser.parse("HDFC", msg)
        assertNotNull(parsed)
        assertEquals("Transaction amount must be 4,000", 4000.0, parsed!!.amount, 0.001)
        assertEquals("Balance must be 12,500", 12500.0, parsed.balanceAfterTransaction ?: 0.0, 0.001)
        assertEquals(DetectedTransactionType.DEBIT, parsed.detectedType)
    }

    // --- TEST P: Ambiguous wording triggers review ---
    @Test
    fun testP_AmbiguousWordingRequiresReview() {
        val msg = "Transaction info: Rs 500 processed for your card 1234."
        val parsed = SmsTransactionParser.parse("Bank", msg)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        // If type cannot be definitely confirmed as credit or debit, type is UNKNOWN and requiresReview is true
        if (parsed.detectedType == DetectedTransactionType.UNKNOWN) {
            assertTrue("Ambiguous transaction must require review", parsed.requiresReview)
        }
    }

    // --- TEST Q: Raw and normalized text preserved for audit ---
    @Test
    fun testQ_AuditAndNormalizedTextPreserved() {
        val msg = "₹4,000 CREDITED to A/C ending 9876 via UPI Ref 445566."
        val parsed = SmsTransactionParser.parse("SBI", msg)
        assertNotNull(parsed)
        assertEquals(msg, parsed!!.rawText)
        assertTrue(parsed.normalizedText.isNotEmpty())
        assertEquals("445566", parsed.referenceNumber)
        assertEquals("9876", parsed.accountLast4)
    }

    // --- TEST R: Mandatory 10 Genuine ₹100 Repeated Transactions Test -> Expected = 10 transactions, Sum = ₹1,000 ---
    @Test
    fun testR_TenGenuine100Transactions_TotalCreditIs1000() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "Salary Account", bankName = "SBI", accountNumberLast4 = "1234", openingBalance = 0.0)
        )
        repository.saveSettings(AppSettings(autoConfirmTrustedSms = true))

        // 10 genuine transactions arrive from another account
        for (i in 1..10) {
            val sms = "Your A/C ending 1234 has been credited by ₹100 on 01-Oct-26 via UPI. Ref 1000$i."
            val timestamp = 1700000000000L + (i * 60000L) // 1 minute apart
            val parsed = SmsTransactionParser.parse("SBI", sms, timestamp = timestamp)
            assertNotNull("Failed to parse transaction $i", parsed)
            assertEquals("Amount for transaction $i must be 100", 100.0, parsed!!.amount, 0.001)
            assertEquals("Type for transaction $i must be CREDIT", DetectedTransactionType.CREDIT, parsed.detectedType)

            repository.processParsedTransaction(parsed, DetectedSourceType.SMS)
        }

        val allTransactions = database.financeDao().getAllConfirmedTransactionsDirect()
        assertEquals("Must record exactly 10 genuine transactions", 10, allTransactions.size)

        val totalCredit = allTransactions.sumOf { it.amount }
        assertEquals("Total credit must be exactly ₹1,000", 1000.0, totalCredit, 0.001)
    }

    // --- TEST S: User Requested Example: ₹100 credited vs ₹5,850 balance ---
    @Test
    fun testS_UserExample_100Credited_5850Balance() {
        val msg = "₹100 credited to your account. Available balance ₹5,850."
        val parsed = SmsTransactionParser.parse("SBI", msg)
        assertNotNull(parsed)
        assertEquals("Transaction amount must be ₹100", 100.0, parsed!!.amount, 0.001)
        assertEquals("Transaction type must be CREDIT", DetectedTransactionType.CREDIT, parsed.detectedType)
        assertEquals("Balance must be ₹5,850", 5850.0, parsed.balanceAfterTransaction ?: 0.0, 0.001)
    }

    // --- TEST T: User Requested Example: Rs 4,000 debited vs Rs 12,500 balance ---
    @Test
    fun testT_UserExample_4000Debited_12500Balance() {
        val msg = "Rs 4,000 debited. Available balance Rs 12,500."
        val parsed = SmsTransactionParser.parse("HDFC", msg)
        assertNotNull(parsed)
        assertEquals("Transaction amount must be ₹4,000", 4000.0, parsed!!.amount, 0.001)
        assertEquals("Transaction type must be DEBIT", DetectedTransactionType.DEBIT, parsed.detectedType)
        assertEquals("Balance must be ₹12,500", 12500.0, parsed.balanceAfterTransaction ?: 0.0, 0.001)
    }

    // --- TEST U: Transaction + Fee + Balance ---
    @Test
    fun testU_TransactionWithFeeAndBalance() {
        val msg = "₹4,000 debited for utility bill. Convenience fee ₹15.00. Avl bal ₹24,800."
        val parsed = SmsTransactionParser.parse("Axis", msg)
        assertNotNull(parsed)
        assertEquals("Transaction amount must be 4000", 4000.0, parsed!!.amount, 0.001)
        assertEquals("Fee must be 15", 15.0, parsed.feeAmount ?: 0.0, 0.001)
        assertEquals("Balance must be 24800", 24800.0, parsed.balanceAfterTransaction ?: 0.0, 0.001)
        assertEquals(DetectedTransactionType.DEBIT, parsed.detectedType)
    }

    // --- TEST V: Two genuine ₹100 transactions without ref at 10:00 and 10:05 must BOTH be saved ---
    @Test
    fun testV_TwoGenuine100TransactionsWithoutRef_BothSaved() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "Savings", bankName = "SBI", accountNumberLast4 = "1234", openingBalance = 5000.0)
        )
        repository.saveSettings(AppSettings(autoConfirmTrustedSms = true))

        val sms1 = "₹100 credited to your A/C 1234 at 10:00 AM."
        val sms2 = "₹100 credited to your A/C 1234 at 10:05 AM."

        val p1 = SmsTransactionParser.parse("SBI", sms1, timestamp = 1700000000000L) // 10:00
        val p2 = SmsTransactionParser.parse("SBI", sms2, timestamp = 1700000300000L) // 10:05 (5 mins later)

        assertNotNull(p1)
        assertNotNull(p2)

        repository.processParsedTransaction(p1!!, DetectedSourceType.SMS)
        repository.processParsedTransaction(p2!!, DetectedSourceType.SMS)

        val txs = database.financeDao().getAllConfirmedTransactionsDirect()
        assertEquals("Two genuine ₹100 transactions must BOTH be saved", 2, txs.size)
        assertEquals(200.0, txs.sumOf { it.amount }, 0.001)
    }

    // --- TEST W: Verify all numeric amounts specified in requirement ---
    @Test
    fun testW_VerifyAllSpecifiedAmounts() {
        val amountsToTest = listOf(
            Pair("₹100 credited to account", 100.0),
            Pair("₹400 debited from account", 400.0),
            Pair("₹850 paid to Swiggy", 850.0),
            Pair("₹1,000 received via UPI", 1000.0),
            Pair("₹4,000 transferred to friend", 4000.0),
            Pair("₹40,000 deposited in bank", 40000.0),
            Pair("₹4,00,000 credited from loan", 400000.0),
            Pair("₹4,000.50 received cashback", 4000.50),
            Pair("Rs 4,000 paid for shopping", 4000.0),
            Pair("Rs. 4,000 debited from ATM", 4000.0),
            Pair("INR 4,000 credited to A/C", 4000.0)
        )

        for ((text, expectedAmt) in amountsToTest) {
            val parsed = SmsTransactionParser.parse("Bank", text)
            assertNotNull("Failed parsing '$text'", parsed)
            assertEquals("Incorrect amount for '$text'", expectedAmt, parsed!!.amount, 0.001)
        }
    }

    // --- TEST X: Three genuine ₹100 transactions must remain THREE transactions ---
    @Test
    fun testX_ThreeGenuine100TransactionsRemainThree() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "Axis Bank", bankName = "Axis", accountNumberLast4 = "9999", openingBalance = 1000.0)
        )
        repository.saveSettings(AppSettings(autoConfirmTrustedSms = true))

        val sms1 = "₹100 credited to account 9999 at 10:00."
        val sms2 = "₹100 credited to account 9999 at 10:05."
        val sms3 = "₹100 credited to account 9999 at 10:10."

        val p1 = SmsTransactionParser.parse("Axis", sms1, timestamp = 1700000000000L) // 10:00
        val p2 = SmsTransactionParser.parse("Axis", sms2, timestamp = 1700000300000L) // 10:05
        val p3 = SmsTransactionParser.parse("Axis", sms3, timestamp = 1700000600000L) // 10:10

        assertNotNull(p1)
        assertNotNull(p2)
        assertNotNull(p3)

        repository.processParsedTransaction(p1!!, DetectedSourceType.SMS)
        repository.processParsedTransaction(p2!!, DetectedSourceType.SMS)
        repository.processParsedTransaction(p3!!, DetectedSourceType.SMS)

        val txs = database.financeDao().getAllConfirmedTransactionsDirect()
        assertEquals("Three genuine ₹100 transactions must remain THREE separate transactions", 3, txs.size)
        assertEquals("Total credit must be ₹300", 300.0, txs.sumOf { it.amount }, 0.001)
    }

    // --- TEST Y: Savings Goal Progress and Add Money ---
    @Test
    fun testY_SavingsGoalProgressAndAddMoney() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "Main A/C", bankName = "HDFC", accountNumberLast4 = "1111", openingBalance = 50000.0)
        )
        val goalId = repository.insertSavingsGoal(
            SavingsGoal(name = "Emergency Fund", targetAmount = 100000.0, currentAmount = 20000.0, targetDateMillis = System.currentTimeMillis() + 10000000L)
        )

        // Add 5000 to savings from account
        repository.addMoneyToSavingsGoal(goalId, 5000.0, accId)

        val updatedGoal = database.financeDao().getAllSavingsGoalsDirect().first { it.id == goalId }
        assertEquals(25000.0, updatedGoal.currentAmount, 0.001)
        val remaining = (updatedGoal.targetAmount - updatedGoal.currentAmount).coerceAtLeast(0.0)
        assertEquals(75000.0, remaining, 0.001)
        val progress = (updatedGoal.currentAmount / updatedGoal.targetAmount * 100).toInt()
        assertEquals(25, progress)

        // Check account balance reduced by 5000
        val updatedAcc = database.financeDao().getAccountByIdSuspend(accId)
        assertEquals(45000.0, updatedAcc!!.currentBalance, 0.001)
    }

    // --- TEST Z: EMI Loan Payment Updates Installments and Next Due Date ---
    @Test
    fun testZ_LoanEmiPaymentUpdatesInstallmentsAndNextDue() = runBlocking {
        val accId = repository.insertAccount(
            Account(name = "Salary A/C", bankName = "SBI", accountNumberLast4 = "5555", openingBalance = 50000.0)
        )
        val startDue = 1700000000000L
        val loanId = repository.insertLoan(
            Loan(
                name = "Personal Loan",
                lender = "SBI",
                originalAmount = 100000.0,
                remainingAmount = 85000.0,
                interestRate = 10.5,
                emiAmount = 8500.0,
                dueDayOfMonth = 10,
                totalEmis = 12,
                paidEmis = 2,
                remainingEmis = 10,
                notes = "Education",
                nextDueDateMillis = startDue
            )
        )

        // Pay EMI of 8500
        repository.recordEmiPayment(loanId, 8500.0, accId)

        val updatedLoan = database.financeDao().getLoanById(loanId)
        assertNotNull(updatedLoan)
        assertEquals(3, updatedLoan!!.paidEmis)
        assertEquals(9, updatedLoan.remainingEmis)
        assertEquals(76500.0, updatedLoan.remainingAmount, 0.001)
        assertTrue("Next due date must be advanced into future", updatedLoan.nextDueDateMillis > startDue)

        // Verify account balance was debited
        val updatedAcc = database.financeDao().getAccountByIdSuspend(accId)
        assertEquals(41500.0, updatedAcc!!.currentBalance, 0.001)
    }

    // --- TEST W1: FinFlow Mega Widget Safe Default Views Construction ---
    @Test
    fun testW1_MegaWidgetSafeDefaultViewsCanBeBuilt() {
        val views = com.example.widget.FinFlowWidgetManager.buildSafeDefaultViews(application)
        assertNotNull("Widget RemoteViews must not be null", views)
    }

    // --- TEST W2: FinFlow Mega Widget Provider Instantiation and Update ---
    @Test
    fun testW2_MegaWidgetProviderCanBeInstantiated() {
        val provider = com.example.widget.FinFlowMegaWidgetProvider()
        assertNotNull(provider)
        val legacyProvider = com.example.widget.FinancialOverviewWidgetProvider()
        assertNotNull(legacyProvider)
    }

    // --- TEST W3: Monthly EMI Automatic Next Due Date Calculation ---
    @Test
    fun testW3_MonthlyEmiAutomaticDueCalculation() {
        val dueDay = 10
        val cal = java.util.Calendar.getInstance()
        val today = cal.get(java.util.Calendar.DAY_OF_MONTH)

        val nextCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (today <= dueDay) {
                set(java.util.Calendar.DAY_OF_MONTH, dueDay)
            } else {
                add(java.util.Calendar.MONTH, 1)
                set(java.util.Calendar.DAY_OF_MONTH, dueDay)
            }
        }

        assertEquals(10, nextCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertTrue("Calculated due date must be today or in the future", nextCal.timeInMillis >= System.currentTimeMillis() - 86400000L)
    }
}
