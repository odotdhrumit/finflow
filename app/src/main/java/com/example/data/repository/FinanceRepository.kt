package com.example.data.repository

import com.example.data.dao.FinanceDao
import com.example.data.entity.*
import com.example.parser.CategoryKeywordMatcher
import com.example.parser.ParsedTransaction
import com.example.parser.ParseConfidence
import com.example.parser.SmsTransactionParser
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import com.example.parser.DetectedTransactionType
import java.util.Calendar

class FinanceRepository(private val dao: FinanceDao) {

    private val transactionProcessingMutex = Mutex()

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

        // Update linked reminder with the new next due date
        val linkedReminder = dao.getReminderByLinkedLoanId(loanId)
        if (linkedReminder != null) {
            if (updatedLoan.isClosed) {
                dao.updateReminder(linkedReminder.copy(isCompleted = true))
            } else {
                dao.updateReminder(
                    linkedReminder.copy(
                        dueDateMillis = nextDueCal.timeInMillis,
                        isCompleted = false
                    )
                )
            }
        }

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
        transactionProcessingMutex.withLock {
            val rawClean = parsed.rawText.trim()
            val rawHash = SmsTransactionParser.sha256(rawClean.lowercase(java.util.Locale.ENGLISH))

            // 0. EXACT REPEATED MESSAGE / BROADCAST RETRY PROTECTION
            // If the exact same raw message was already processed in detected_messages:
            val exactDuplicate = dao.findDetectedByExactRawText(rawClean)
            if (exactDuplicate != null) {
                // Telecom duplicate retry of identical SMS broadcast
                return@withLock true
            }

            // 1. DEDUPLICATION & MERGING IN CONFIRMED/EXISTING TRANSACTIONS
            var matchedTx: Transaction? = null

            // Priority 1: Bank/UPI reference ID (UTR / RRN / Txn ID)
            if (parsed.referenceNumber.isNotBlank()) {
                matchedTx = dao.findTransactionByReference(parsed.referenceNumber)
            }

            // Priority 2: Stable fingerprint match
            if (matchedTx == null && parsed.fingerprint.isNotBlank()) {
                matchedTx = dao.findTransactionByFingerprint(parsed.fingerprint)
            }

            // Priority 3: Cross-channel SMS + Notification merging when reference ID is missing
            // Requirements:
            // - Sources must differ (e.g. one SMS, one Notification)
            // - Within tight burst window (<= 90 seconds)
            // - Same transaction amount (diff < 0.001) & same direction
            if (matchedTx == null && parsed.referenceNumber.isBlank()) {
                val candidates = dao.findMatchingTransactions(
                    type = parsed.transactionType,
                    amount = parsed.amount,
                    timestamp = parsed.timestamp,
                    windowMillis = 90000L // 90 seconds tight window for SMS + Notification burst
                )
                matchedTx = candidates.firstOrNull { cand ->
                    val isDifferentChannel = (cand.source == TransactionSource.SMS && sourceType == DetectedSourceType.NOTIFICATION) ||
                            (cand.source == TransactionSource.NOTIFICATION && sourceType == DetectedSourceType.SMS)
                    val accMatch = parsed.accountLast4 == null || cand.accountName.contains(parsed.accountLast4)
                    val refMatch = cand.referenceNumber.isBlank()
                    val balMatch = parsed.balanceAfterTransaction == null || cand.balanceAfterTransaction == null ||
                            kotlin.math.abs(cand.balanceAfterTransaction - parsed.balanceAfterTransaction) < 0.001
                    isDifferentChannel && accMatch && refMatch && balMatch
                }
            }

            if (matchedTx != null) {
                // Duplicate transaction found! Merge metadata into canonical transaction
                val isSms = sourceType == DetectedSourceType.SMS
                val currentSource = matchedTx.source
                val newSource = when {
                    (currentSource == TransactionSource.SMS && !isSms) || (currentSource == TransactionSource.NOTIFICATION && isSms) ->
                        TransactionSource.SMS_AND_NOTIFICATION
                    currentSource == TransactionSource.SMS_AND_NOTIFICATION ->
                        TransactionSource.SMS_AND_NOTIFICATION
                    else -> currentSource
                }

                val updatedRef = if (matchedTx.referenceNumber.isBlank() && parsed.referenceNumber.isNotBlank()) {
                    parsed.referenceNumber
                } else matchedTx.referenceNumber

                val updatedMerchant = if ((matchedTx.merchant.isBlank() || matchedTx.merchant == "Bank") && parsed.merchant.isNotBlank()) {
                    parsed.merchant
                } else matchedTx.merchant

                val updatedBal = parsed.balanceAfterTransaction ?: matchedTx.balanceAfterTransaction

                val mergedTx = matchedTx.copy(
                    source = newSource,
                    referenceNumber = updatedRef,
                    merchant = updatedMerchant,
                    balanceAfterTransaction = updatedBal,
                    feeAmount = parsed.feeAmount ?: matchedTx.feeAmount,
                    upiId = parsed.upiId ?: matchedTx.upiId,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateTransaction(mergedTx)

                // Record in detected_messages linked to the merged transaction
                val detectedMsg = DetectedMessage(
                    sourceType = sourceType,
                    senderOrApp = parsed.bank ?: "Bank",
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
                    linkedTransactionId = matchedTx.id,
                    parsedBalance = parsed.balanceAfterTransaction,
                    parsedFee = parsed.feeAmount,
                    detectedReferenceNumber = parsed.referenceNumber,
                    confidenceLevel = parsed.confidence.name,
                    confidenceReason = "Merged with existing transaction #${matchedTx.id}"
                )
                dao.insertDetectedMessage(detectedMsg)
                return@withLock true
            }

            // 2. DEDUPLICATION IN PENDING REVIEW QUEUE
            var matchedPending: DetectedMessage? = null
            if (parsed.referenceNumber.isNotBlank()) {
                matchedPending = dao.findDetectedByReference(parsed.referenceNumber)
            }
            if (matchedPending == null && parsed.fingerprint.isNotBlank()) {
                matchedPending = dao.findDetectedByFingerprint(parsed.fingerprint)
            }

            if (matchedPending != null) {
                // Merge into existing pending message without creating duplicate
                val updatedRaw = if (!matchedPending.rawText.contains(parsed.rawText)) {
                    "${matchedPending.rawText}\n---\n${parsed.rawText}"
                } else matchedPending.rawText

                val updatedPending = matchedPending.copy(
                    rawText = updatedRaw,
                    parsedMerchant = if (matchedPending.parsedMerchant.isNullOrBlank() || matchedPending.parsedMerchant == "Bank") parsed.merchant else matchedPending.parsedMerchant,
                    parsedAccountLast4 = matchedPending.parsedAccountLast4 ?: parsed.accountLast4,
                    parsedBalance = parsed.balanceAfterTransaction ?: matchedPending.parsedBalance,
                    detectedReferenceNumber = if (matchedPending.detectedReferenceNumber.isBlank()) parsed.referenceNumber else matchedPending.detectedReferenceNumber
                )
                dao.updateDetectedMessage(updatedPending)
                return@withLock true
            }

            // 3. NEW TRANSACTION: Check auto-confirm vs review queue
            val settings = dao.getSettingsDirect() ?: AppSettings()
            val matchingAccount = parsed.accountLast4?.let { dao.findAccountByLast4(it) }
                ?: dao.getActiveAccountsDirect().firstOrNull()

            val canAutoConfirm = settings.autoConfirmTrustedSms &&
                    !parsed.requiresReview &&
                    matchingAccount != null &&
                    parsed.confidence != ParseConfidence.LOW &&
                    parsed.detectedType != DetectedTransactionType.UNKNOWN &&
                    parsed.amount > 0.0

            if (canAutoConfirm && matchingAccount != null) {
                val tx = Transaction(
                    amount = parsed.amount,
                    type = parsed.transactionType,
                    categoryName = parsed.suggestedCategory,
                    accountId = matchingAccount.id,
                    accountName = matchingAccount.name,
                    dateMillis = parsed.timestamp,
                    timeFormatted = DateUtils.formatTime(parsed.timestamp),
                    merchant = parsed.merchant,
                    notes = "Auto-detected from ${sourceType.name} (${parsed.bank ?: "Bank"})",
                    source = if (sourceType == DetectedSourceType.SMS) TransactionSource.SMS else TransactionSource.NOTIFICATION,
                    confirmationStatus = ConfirmationStatus.CONFIRMED,
                    fingerprint = parsed.fingerprint,
                    referenceNumber = parsed.referenceNumber,
                    balanceAfterTransaction = parsed.balanceAfterTransaction,
                    feeAmount = parsed.feeAmount,
                    upiId = parsed.upiId,
                    sender = parsed.sender,
                    receiver = parsed.receiver,
                    parseConfidence = parsed.confidence.name,
                    rawSourceHash = rawHash
                )
                val txId = insertTransaction(tx)

                val detectedMsg = DetectedMessage(
                    sourceType = sourceType,
                    senderOrApp = parsed.bank ?: "Bank",
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
                    linkedTransactionId = txId,
                    parsedBalance = parsed.balanceAfterTransaction,
                    parsedFee = parsed.feeAmount,
                    detectedReferenceNumber = parsed.referenceNumber,
                    confidenceLevel = parsed.confidence.name,
                    confidenceReason = parsed.confidenceReason
                )
                dao.insertDetectedMessage(detectedMsg)
                return@withLock true
            } else {
                // Send to Review Queue
                val detectedMsg = DetectedMessage(
                    sourceType = sourceType,
                    senderOrApp = parsed.bank ?: "Bank",
                    rawText = parsed.rawText,
                    detectedAtMillis = parsed.timestamp,
                    parsedAmount = if (parsed.amount > 0.0) parsed.amount else null,
                    parsedType = parsed.transactionType,
                    parsedBank = parsed.bank,
                    parsedAccountLast4 = parsed.accountLast4,
                    parsedMerchant = parsed.merchant,
                    suggestedCategory = parsed.suggestedCategory,
                    status = DetectedStatus.PENDING_REVIEW,
                    duplicateFingerprint = parsed.fingerprint,
                    parsedBalance = parsed.balanceAfterTransaction,
                    parsedFee = parsed.feeAmount,
                    detectedReferenceNumber = parsed.referenceNumber,
                    confidenceLevel = parsed.confidence.name,
                    confidenceReason = parsed.confidenceReason
                )
                dao.insertDetectedMessage(detectedMsg)
                return@withLock true
            }
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
            fingerprint = detected.duplicateFingerprint,
            referenceNumber = detected.detectedReferenceNumber,
            balanceAfterTransaction = detected.parsedBalance,
            feeAmount = detected.parsedFee
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

    suspend fun removeDemoDataIfPresent() = withContext(Dispatchers.IO) {
        dao.deleteDemoTransactions()
        dao.deleteDemoAccounts()
        dao.deleteDemoSavings()
        dao.deleteDemoInvestments()
        dao.deleteDemoLoans()
        dao.deleteDemoReminders()
        dao.deleteDemoDetectedMessages()
    }
}
