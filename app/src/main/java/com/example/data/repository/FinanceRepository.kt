package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.entity.*
import com.example.parser.CategoryKeywordMatcher
import com.example.parser.ParsedTransaction
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.Calendar

class FinanceRepository(private val dao: FinanceDao) {

    // --- Accounts ---
    val allAccounts: Flow<List<Account>> = dao.getAllAccounts()
    val activeAccounts: Flow<List<Account>> = dao.getActiveAccounts()

    suspend fun getAccountById(id: Long): Account? = withContext(Dispatchers.IO) {
        dao.getAccountByIdSuspend(id)
    }

    suspend fun insertAccount(account: Account): Long = withContext(Dispatchers.IO) {
        dao.insertAccount(account)
    }

    suspend fun updateAccount(account: Account) = withContext(Dispatchers.IO) {
        dao.updateAccount(account)
        recalculateAccountBalance(account.id)
    }

    suspend fun deleteAccount(account: Account) = withContext(Dispatchers.IO) {
        dao.deleteAccount(account)
    }

    // --- Transactions ---
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactions()
    fun getRecentTransactions(limit: Int = 10): Flow<List<Transaction>> = dao.getRecentTransactions(limit)
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>> = dao.getTransactionsByAccount(accountId)
    fun searchTransactions(query: String): Flow<List<Transaction>> = dao.searchTransactions(query)

    suspend fun insertTransaction(transaction: Transaction): Long = withContext(Dispatchers.IO) {
        // Prevent duplicate if fingerprint is set
        if (transaction.fingerprint.isNotEmpty()) {
            val existing = dao.findTransactionByFingerprint(transaction.fingerprint)
            if (existing != null) {
                return@withContext -1L // Duplicate rejected
            }
        }

        val id = dao.insertTransaction(transaction)
        if (transaction.confirmationStatus == ConfirmationStatus.CONFIRMED) {
            recalculateAccountBalance(transaction.accountId)
            if (transaction.type == TransactionType.TRANSFER && transaction.toAccountId != null) {
                recalculateAccountBalance(transaction.toAccountId)
            }
        }
        id
    }

    suspend fun updateTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val old = dao.getTransactionById(transaction.id)
        dao.updateTransaction(transaction)

        recalculateAccountBalance(transaction.accountId)
        if (transaction.toAccountId != null) {
            recalculateAccountBalance(transaction.toAccountId)
        }
        if (old != null && old.accountId != transaction.accountId) {
            recalculateAccountBalance(old.accountId)
        }
        if (old != null && old.toAccountId != null && old.toAccountId != transaction.toAccountId) {
            recalculateAccountBalance(old.toAccountId)
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(transaction)
        recalculateAccountBalance(transaction.accountId)
        if (transaction.toAccountId != null) {
            recalculateAccountBalance(transaction.toAccountId)
        }
    }

    suspend fun recalculateAccountBalance(accountId: Long) = withContext(Dispatchers.IO) {
        val account = dao.getAccountByIdSuspend(accountId) ?: return@withContext
        val allTx = dao.getAllConfirmedTransactionsDirect()

        var calculatedBalance = account.openingBalance
        for (tx in allTx) {
            when {
                // Outgoing transaction from this account
                tx.accountId == accountId -> {
                    when (tx.type) {
                        TransactionType.EXPENSE,
                        TransactionType.INVESTMENT,
                        TransactionType.LOAN_PAYMENT,
                        TransactionType.SAVINGS -> calculatedBalance -= tx.amount
                        TransactionType.INCOME -> calculatedBalance += tx.amount
                        TransactionType.TRANSFER -> calculatedBalance -= tx.amount
                    }
                }
                // Incoming transfer to this account
                tx.type == TransactionType.TRANSFER && tx.toAccountId == accountId -> {
                    calculatedBalance += tx.amount
                }
            }
        }

        dao.updateAccountBalance(accountId, calculatedBalance)
    }

    // --- Categories ---
    val allCategories: Flow<List<Category>> = dao.getAllCategories()

    suspend fun insertCategory(category: Category) = withContext(Dispatchers.IO) {
        dao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) = withContext(Dispatchers.IO) {
        dao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) = withContext(Dispatchers.IO) {
        dao.deleteCategory(category)
    }

    // --- Savings Goals ---
    val allSavingsGoals: Flow<List<SavingsGoal>> = dao.getAllSavingsGoals()

    suspend fun insertSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        dao.insertSavingsGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        dao.updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        dao.deleteSavingsGoal(goal)
    }

    suspend fun addMoneyToSavingsGoal(goalId: Long, amount: Double, sourceAccountId: Long?) = withContext(Dispatchers.IO) {
        val goals = dao.getAllSavingsGoals().firstOrNull() ?: return@withContext
        val goal = goals.find { it.id == goalId } ?: return@withContext
        val updated = goal.copy(currentAmount = goal.currentAmount + amount)
        dao.updateSavingsGoal(updated)

        if (sourceAccountId != null) {
            val account = dao.getAccountByIdSuspend(sourceAccountId)
            val tx = Transaction(
                amount = amount,
                type = TransactionType.SAVINGS,
                categoryName = "Savings",
                accountId = sourceAccountId,
                accountName = account?.name ?: "Account",
                merchant = "Savings: ${goal.name}",
                notes = "Deposit to savings goal ${goal.name}",
                source = TransactionSource.MANUAL,
                confirmationStatus = ConfirmationStatus.CONFIRMED,
                dateMillis = System.currentTimeMillis(),
                timeFormatted = DateUtils.formatTime(System.currentTimeMillis())
            )
            insertTransaction(tx)
        }
    }

    suspend fun withdrawMoneyFromSavingsGoal(goalId: Long, amount: Double, targetAccountId: Long?) = withContext(Dispatchers.IO) {
        val goals = dao.getAllSavingsGoals().firstOrNull() ?: return@withContext
        val goal = goals.find { it.id == goalId } ?: return@withContext
        val newAmt = (goal.currentAmount - amount).coerceAtLeast(0.0)
        val updated = goal.copy(currentAmount = newAmt)
        dao.updateSavingsGoal(updated)

        if (targetAccountId != null) {
            val account = dao.getAccountByIdSuspend(targetAccountId)
            val tx = Transaction(
                amount = amount,
                type = TransactionType.INCOME,
                categoryName = "Savings",
                accountId = targetAccountId,
                accountName = account?.name ?: "Account",
                merchant = "Withdrawal: ${goal.name}",
                notes = "Withdrawal from savings goal ${goal.name}",
                source = TransactionSource.MANUAL,
                confirmationStatus = ConfirmationStatus.CONFIRMED,
                dateMillis = System.currentTimeMillis(),
                timeFormatted = DateUtils.formatTime(System.currentTimeMillis())
            )
            insertTransaction(tx)
        }
    }

    // --- Investments ---
    val allInvestments: Flow<List<Investment>> = dao.getAllInvestments()

    suspend fun insertInvestment(investment: Investment, deductAccountId: Long? = null) = withContext(Dispatchers.IO) {
        dao.insertInvestment(investment)
        if (deductAccountId != null) {
            val account = dao.getAccountByIdSuspend(deductAccountId)
            val tx = Transaction(
                amount = investment.investedAmount,
                type = TransactionType.INVESTMENT,
                categoryName = "Investment",
                accountId = deductAccountId,
                accountName = account?.name ?: "Account",
                merchant = investment.name,
                notes = "Investment into ${investment.type.name}",
                source = TransactionSource.MANUAL,
                confirmationStatus = ConfirmationStatus.CONFIRMED,
                dateMillis = investment.dateMillis,
                timeFormatted = DateUtils.formatTime(investment.dateMillis)
            )
            insertTransaction(tx)
        }
    }

    suspend fun updateInvestment(investment: Investment) = withContext(Dispatchers.IO) {
        dao.updateInvestment(investment)
    }

    suspend fun deleteInvestment(investment: Investment) = withContext(Dispatchers.IO) {
        dao.deleteInvestment(investment)
    }

    // --- Loans ---
    val allLoans: Flow<List<Loan>> = dao.getAllLoans()

    suspend fun insertLoan(loan: Loan) = withContext(Dispatchers.IO) {
        dao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: Loan) = withContext(Dispatchers.IO) {
        dao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: Loan) = withContext(Dispatchers.IO) {
        dao.deleteLoan(loan)
    }

    suspend fun recordEmiPayment(loanId: Long, emiAmount: Double, accountId: Long) = withContext(Dispatchers.IO) {
        val loan = dao.getLoanById(loanId) ?: return@withContext
        val newPaid = loan.paidEmis + 1
        val newRemainingEmis = (loan.remainingEmis - 1).coerceAtLeast(0)
        val newRemainingAmt = (loan.remainingAmount - emiAmount).coerceAtLeast(0.0)

        // Calculate next month's due date
        val nextDueCal = Calendar.getInstance().apply {
            timeInMillis = loan.nextDueDateMillis
            add(Calendar.MONTH, 1)
        }

        val updatedLoan = loan.copy(
            paidEmis = newPaid,
            remainingEmis = newRemainingEmis,
            remainingAmount = newRemainingAmt,
            isClosed = newRemainingAmt <= 0 || newRemainingEmis == 0,
            nextDueDateMillis = nextDueCal.timeInMillis
        )
        dao.updateLoan(updatedLoan)

        val account = dao.getAccountByIdSuspend(accountId)
        val tx = Transaction(
            amount = emiAmount,
            type = TransactionType.LOAN_PAYMENT,
            categoryName = "Loan",
            accountId = accountId,
            accountName = account?.name ?: "Account",
            merchant = "${loan.name} EMI",
            notes = "EMI payment ${newPaid} of ${loan.totalEmis} to ${loan.lender}",
            source = TransactionSource.MANUAL,
            confirmationStatus = ConfirmationStatus.CONFIRMED,
            dateMillis = System.currentTimeMillis(),
            timeFormatted = DateUtils.formatTime(System.currentTimeMillis())
        )
        insertTransaction(tx)
    }

    // --- Reminders ---
    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()
    val activeReminders: Flow<List<Reminder>> = dao.getActiveReminders()

    suspend fun insertReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        dao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        dao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: Reminder) = withContext(Dispatchers.IO) {
        dao.deleteReminder(reminder)
    }

    // --- Detected Messages (SMS & Notification) ---
    val allDetectedMessages: Flow<List<DetectedMessage>> = dao.getAllDetectedMessages()
    val pendingDetectedMessages: Flow<List<DetectedMessage>> = dao.getPendingDetectedMessages()

    suspend fun processParsedTransaction(parsed: ParsedTransaction, sourceType: DetectedSourceType): Boolean = withContext(Dispatchers.IO) {
        // Duplicate check
        val existingDetected = dao.findDetectedByFingerprint(parsed.fingerprint)
        if (existingDetected != null) {
            return@withContext false
        }
        val existingTx = dao.findTransactionByFingerprint(parsed.fingerprint)
        if (existingTx != null) {
            return@withContext false
        }

        val settings = dao.getSettingsDirect() ?: AppSettings()

        // Check if account matches by last 4 digits
        val matchingAccount = parsed.accountLast4?.let { dao.findAccountByLast4(it) }
            ?: dao.getActiveAccountsDirect().firstOrNull()

        if (settings.autoConfirmTrustedSms && matchingAccount != null && parsed.confidenceScore >= 0.7f) {
            // Auto confirm!
            val tx = Transaction(
                amount = parsed.amount,
                type = parsed.transactionType,
                categoryName = parsed.suggestedCategory,
                accountId = matchingAccount.id,
                accountName = matchingAccount.name,
                dateMillis = parsed.timestamp,
                timeFormatted = DateUtils.formatTime(parsed.timestamp),
                merchant = parsed.merchant,
                notes = "Auto-detected from ${sourceType.name} (${parsed.bank})",
                source = if (sourceType == DetectedSourceType.SMS) TransactionSource.SMS else TransactionSource.NOTIFICATION,
                confirmationStatus = ConfirmationStatus.CONFIRMED,
                fingerprint = parsed.fingerprint,
                referenceNumber = parsed.referenceNumber
            )
            val txId = insertTransaction(tx)

            val detectedMsg = DetectedMessage(
                sourceType = sourceType,
                senderOrApp = parsed.bank,
                rawText = parsed.rawText,
                detectedAtMillis = parsed.timestamp,
                parsedAmount = parsed.amount,
                parsedType = parsed.transactionType,
                parsedBank = parsed.bank,
                parsedAccountLast4 = parsed.accountLast4,
                parsedMerchant = parsed.merchant,
                suggestedCategory = parsed.suggestedCategory,
                status = DetectedStatus.CONFIRMED,
                duplicateFingerprint = parsed.fingerprint,
                linkedTransactionId = txId
            )
            dao.insertDetectedMessage(detectedMsg)
            return@withContext true
        } else {
            // Send to review queue
            val detectedMsg = DetectedMessage(
                sourceType = sourceType,
                senderOrApp = parsed.bank,
                rawText = parsed.rawText,
                detectedAtMillis = parsed.timestamp,
                parsedAmount = parsed.amount,
                parsedType = parsed.transactionType,
                parsedBank = parsed.bank,
                parsedAccountLast4 = parsed.accountLast4,
                parsedMerchant = parsed.merchant,
                suggestedCategory = parsed.suggestedCategory,
                status = DetectedStatus.PENDING_REVIEW,
                duplicateFingerprint = parsed.fingerprint
            )
            dao.insertDetectedMessage(detectedMsg)
            return@withContext true
        }
    }

    suspend fun confirmDetectedMessage(
        detected: DetectedMessage,
        accountId: Long,
        category: String,
        amount: Double,
        type: TransactionType,
        merchant: String
    ) = withContext(Dispatchers.IO) {
        val account = dao.getAccountByIdSuspend(accountId)
        val tx = Transaction(
            amount = amount,
            type = type,
            categoryName = category,
            accountId = accountId,
            accountName = account?.name ?: "Account",
            dateMillis = detected.detectedAtMillis,
            timeFormatted = DateUtils.formatTime(detected.detectedAtMillis),
            merchant = merchant,
            notes = "Confirmed from ${detected.sourceType.name} (${detected.parsedBank ?: "Bank"})",
            source = if (detected.sourceType == DetectedSourceType.SMS) TransactionSource.SMS else TransactionSource.NOTIFICATION,
            confirmationStatus = ConfirmationStatus.CONFIRMED,
            fingerprint = detected.duplicateFingerprint
        )
        val txId = insertTransaction(tx)

        val updated = detected.copy(
            status = DetectedStatus.CONFIRMED,
            linkedTransactionId = txId,
            parsedAmount = amount,
            parsedType = type,
            parsedMerchant = merchant,
            suggestedCategory = category
        )
        dao.updateDetectedMessage(updated)
    }

    suspend fun ignoreDetectedMessage(detected: DetectedMessage) = withContext(Dispatchers.IO) {
        val updated = detected.copy(status = DetectedStatus.IGNORED)
        dao.updateDetectedMessage(updated)
    }

    // --- App Settings ---
    val appSettings: Flow<AppSettings?> = dao.getSettingsFlow()

    suspend fun getSettingsDirect(): AppSettings = withContext(Dispatchers.IO) {
        dao.getSettingsDirect() ?: AppSettings()
    }

    suspend fun saveSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.saveSettings(settings)
    }

    // --- Demo Data Seeder ---
    suspend fun seedDemoData() = withContext(Dispatchers.IO) {
        // Ensure categories exist
        val existingCats = dao.getAllCategoriesDirect()
        if (existingCats.isEmpty()) {
            dao.insertCategories(CategoryKeywordMatcher.getDefaultCategories())
        }

        // Accounts: SBI (₹75,450), HDFC (₹40,000), Cash (₹10,000)
        val sbiId = dao.insertAccount(
            Account(
                name = "SBI Savings",
                bankName = "State Bank of India",
                accountNumberLast4 = "1234",
                openingBalance = 75450.0,
                currentBalance = 75450.0,
                accountType = AccountType.BANK,
                colorHex = "#1E40AF"
            )
        )

        val hdfcId = dao.insertAccount(
            Account(
                name = "HDFC Salary A/C",
                bankName = "HDFC Bank",
                accountNumberLast4 = "6789",
                openingBalance = 40000.0,
                currentBalance = 40000.0,
                accountType = AccountType.BANK,
                colorHex = "#047857"
            )
        )

        val cashId = dao.insertAccount(
            Account(
                name = "Cash Wallet",
                bankName = "Cash in Hand",
                accountNumberLast4 = "",
                openingBalance = 10000.0,
                currentBalance = 10000.0,
                accountType = AccountType.CASH,
                colorHex = "#F59E0B"
            )
        )

        val now = System.currentTimeMillis()
        val oneDay = 86400000L

        // Realistic transactions
        val txList = listOf(
            Transaction(
                amount = 35000.0,
                type = TransactionType.INCOME,
                categoryName = "Salary",
                accountId = hdfcId,
                accountName = "HDFC Salary A/C",
                merchant = "Tech Corp Inc",
                notes = "Monthly Salary Deposit",
                dateMillis = now - (oneDay * 5),
                timeFormatted = "10:30 AM",
                source = TransactionSource.SMS,
                confirmationStatus = ConfirmationStatus.CONFIRMED
            ),
            Transaction(
                amount = 350.0,
                type = TransactionType.EXPENSE,
                categoryName = "Food",
                accountId = sbiId,
                accountName = "SBI Savings",
                merchant = "Swiggy",
                notes = "Lunch order",
                dateMillis = now - (oneDay * 1),
                timeFormatted = "01:15 PM",
                source = TransactionSource.SMS,
                confirmationStatus = ConfirmationStatus.CONFIRMED
            ),
            Transaction(
                amount = 1299.0,
                type = TransactionType.EXPENSE,
                categoryName = "Shopping",
                accountId = hdfcId,
                accountName = "HDFC Salary A/C",
                merchant = "Amazon India",
                notes = "Noise Headphones case",
                dateMillis = now - (oneDay * 2),
                timeFormatted = "04:45 PM",
                source = TransactionSource.SMS,
                confirmationStatus = ConfirmationStatus.CONFIRMED
            ),
            Transaction(
                amount = 450.0,
                type = TransactionType.EXPENSE,
                categoryName = "Transport",
                accountId = cashId,
                accountName = "Cash Wallet",
                merchant = "Metro Card & Cab",
                notes = "Daily commute",
                dateMillis = now - (oneDay * 3),
                timeFormatted = "06:20 PM",
                source = TransactionSource.MANUAL,
                confirmationStatus = ConfirmationStatus.CONFIRMED
            ),
            Transaction(
                amount = 2500.0,
                type = TransactionType.TRANSFER,
                categoryName = "Transfer",
                accountId = sbiId,
                accountName = "SBI Savings",
                toAccountId = hdfcId,
                toAccountName = "HDFC Salary A/C",
                merchant = "Self Transfer",
                notes = "Transfer to Salary A/C for bills",
                dateMillis = now - (oneDay * 4),
                timeFormatted = "11:00 AM",
                source = TransactionSource.MANUAL,
                confirmationStatus = ConfirmationStatus.CONFIRMED
            )
        )

        for (tx in txList) {
            dao.insertTransaction(tx)
        }

        // Recalculate balances
        recalculateAccountBalance(sbiId)
        recalculateAccountBalance(hdfcId)
        recalculateAccountBalance(cashId)

        // Savings: New Phone Target ₹50,000, Saved ₹18,500
        val targetPhoneDate = Calendar.getInstance().apply { add(Calendar.MONTH, 6) }.timeInMillis
        dao.insertSavingsGoal(
            SavingsGoal(
                name = "New Phone",
                targetAmount = 50000.0,
                currentAmount = 18500.0,
                targetDateMillis = targetPhoneDate,
                notes = "Flagship upgrade fund",
                colorHex = "#3B82F6"
            )
        )

        // Loan: Personal Loan ₹45,000 remaining, ₹5,000 EMI
        val nextDueLoan = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 12)
            if (timeInMillis < now) add(Calendar.MONTH, 1)
        }.timeInMillis

        dao.insertLoan(
            Loan(
                name = "Personal Loan",
                lender = "SBI Finance",
                originalAmount = 60000.0,
                remainingAmount = 45000.0,
                interestRate = 10.5,
                emiAmount = 5000.0,
                dueDayOfMonth = 12,
                totalEmis = 12,
                paidEmis = 3,
                remainingEmis = 9,
                frequency = "Monthly",
                notes = "Laptop purchase financing",
                nextDueDateMillis = nextDueLoan
            )
        )

        // Investment: Mutual Fund SIP
        dao.insertInvestment(
            Investment(
                name = "Nifty 50 Index Fund",
                type = InvestmentType.MUTUAL_FUND,
                investedAmount = 25000.0,
                currentValue = 31250.0,
                notes = "Monthly SIP"
            )
        )

        // Reminder for Loan EMI
        dao.insertReminder(
            Reminder(
                title = "Personal Loan EMI",
                amount = 5000.0,
                dueDateMillis = nextDueLoan,
                frequency = ReminderFrequency.MONTHLY,
                reminderAdvanceDays = 1,
                notes = "Auto-debit from SBI A/C"
            )
        )

        // Detected message pending review
        dao.insertDetectedMessage(
            DetectedMessage(
                sourceType = DetectedSourceType.SMS,
                senderOrApp = "SBI",
                rawText = "Your A/C XX1234 is debited by Rs.850.00 on 12-Oct-26 at Zomato. UPI Ref 38291039.",
                detectedAtMillis = now - 3600000L,
                parsedAmount = 850.0,
                parsedType = TransactionType.EXPENSE,
                parsedBank = "SBI",
                parsedAccountLast4 = "1234",
                parsedMerchant = "Zomato",
                suggestedCategory = "Food",
                status = DetectedStatus.PENDING_REVIEW,
                duplicateFingerprint = "sample-sbi-fingerprint-1"
            )
        )
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dao.clearAccounts()
        dao.clearTransactions()
        dao.clearSavings()
        dao.clearInvestments()
        dao.clearLoans()
        dao.clearReminders()
        dao.clearDetectedMessages()
    }
}
