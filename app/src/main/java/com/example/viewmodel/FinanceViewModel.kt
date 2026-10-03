package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.FinFlowApplication
import com.example.data.entity.*
import com.example.data.repository.FinanceRepository
import com.example.parser.CategoryKeywordMatcher
import com.example.parser.SmsTransactionParser
import com.example.util.DateUtils
import com.example.util.ExportHelper
import com.example.util.ReminderScheduler
import com.example.util.SecurityHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File

data class DashboardSummary(
    val totalBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val totalSavings: Double = 0.0,
    val totalInvestments: Double = 0.0,
    val totalInvestmentGainLoss: Double = 0.0,
    val totalLoansDue: Double = 0.0,
    val pendingReviewCount: Int = 0
)

enum class DateFilterType {
    ALL,
    TODAY,
    THIS_WEEK,
    THIS_MONTH
}

class FinanceViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    // --- Core Flows ---
    val accounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeAccounts = repository.activeAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentTransactions = repository.getRecentTransactions(10).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val savingsGoals = repository.allSavingsGoals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val investments = repository.allInvestments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val loans = repository.allLoans.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val reminders = repository.allReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeReminders = repository.activeReminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val pendingDetections = repository.pendingDetectedMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allDetections = repository.allDetectedMessages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val appSettings = repository.appSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- App Lock State ---
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // --- Search & Filters for Transactions ---
    val searchQuery = MutableStateFlow("")
    val selectedTypeFilter = MutableStateFlow<TransactionType?>(null)
    val selectedDateFilter = MutableStateFlow(DateFilterType.ALL)

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        transactions,
        searchQuery,
        selectedTypeFilter,
        selectedDateFilter
    ) { txList, query, typeFilter, dateFilter ->
        val now = System.currentTimeMillis()
        val startOfToday = DateUtils.getStartOfDay(now)
        val startOfWeek = DateUtils.getStartOfWeek(now)
        val startOfMonth = DateUtils.getStartOfMonth(now)

        txList.filter { tx ->
            // Search query match
            val matchesQuery = query.isBlank() ||
                    tx.merchant.contains(query, ignoreCase = true) ||
                    tx.categoryName.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.accountName.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)

            // Type match
            val matchesType = typeFilter == null || tx.type == typeFilter

            // Date match
            val matchesDate = when (dateFilter) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> tx.dateMillis >= startOfToday
                DateFilterType.THIS_WEEK -> tx.dateMillis >= startOfWeek
                DateFilterType.THIS_MONTH -> tx.dateMillis >= startOfMonth
            }

            matchesQuery && matchesType && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Summary Flow ---
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        combine(activeAccounts, transactions) { acc, tx -> Pair(acc, tx) },
        combine(savingsGoals, investments) { sav, inv -> Pair(sav, inv) },
        combine(loans, pendingDetections) { ln, pend -> Pair(ln, pend) }
    ) { (accList, txList), (savingsList, invList), (loanList, pendingList) ->
        val totalBal = accList.sumOf { it.currentBalance }

        val startOfMonth = DateUtils.getStartOfMonth(System.currentTimeMillis())
        val monthlyTx = txList.filter { it.dateMillis >= startOfMonth && it.confirmationStatus == ConfirmationStatus.CONFIRMED }

        val income = monthlyTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = monthlyTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        val totalSavings = savingsList.sumOf { it.currentAmount }
        val totalInvValue = invList.sumOf { it.currentValue }
        val totalInvCost = invList.sumOf { it.investedAmount }
        val totalLoans = loanList.filter { !it.isClosed }.sumOf { it.remainingAmount }

        DashboardSummary(
            totalBalance = totalBal,
            monthlyIncome = income,
            monthlyExpenses = expenses,
            totalSavings = totalSavings,
            totalInvestments = totalInvValue,
            totalInvestmentGainLoss = totalInvValue - totalInvCost,
            totalLoansDue = totalLoans,
            pendingReviewCount = pendingList.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    init {
        checkAppLockInitial()
    }

    private fun checkAppLockInitial() {
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            if (settings.appLockEnabled && settings.pinHash.isNotEmpty()) {
                _isAppLocked.value = true
            }
        }
    }

    fun lockApp() {
        val settings = appSettings.value ?: runBlocking { repository.getSettingsDirect() }
        if (settings.appLockEnabled && settings.pinHash.isNotEmpty()) {
            _isAppLocked.value = true
        }
    }

    fun unlockApp(pin: String): Boolean {
        val settings = appSettings.value ?: runBlocking { repository.getSettingsDirect() }
        if (settings.appLockEnabled && settings.pinHash.isNotEmpty()) {
            if (SecurityHelper.verifyPin(pin, settings.pinHash)) {
                _isAppLocked.value = false
                return true
            }
            return false
        }
        _isAppLocked.value = false
        return true
    }

    fun onBiometricAuthenticationSuccess() {
        val settings = appSettings.value ?: runBlocking { repository.getSettingsDirect() }
        if (settings.appLockEnabled && settings.biometricEnabled) {
            _isAppLocked.value = false
        }
    }

    fun resetAppLock() {
        viewModelScope.launch {
            val settings = repository.getSettingsDirect()
            repository.saveSettings(settings.copy(appLockEnabled = false, pinHash = "", biometricEnabled = false))
            _isAppLocked.value = false
        }
    }

    // --- Transaction Actions ---
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        accountId: Long,
        toAccountId: Long? = null,
        merchant: String = "",
        notes: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val acc = activeAccounts.value.find { it.id == accountId }
            val toAcc = toAccountId?.let { id -> activeAccounts.value.find { it.id == id } }

            val tx = Transaction(
                amount = amount,
                type = type,
                categoryName = category,
                accountId = accountId,
                accountName = acc?.name ?: "Account",
                toAccountId = toAccountId,
                toAccountName = toAcc?.name,
                merchant = merchant.ifBlank { if (type == TransactionType.TRANSFER) "Transfer to ${toAcc?.name ?: "Account"}" else category },
                notes = notes,
                dateMillis = dateMillis,
                timeFormatted = DateUtils.formatTime(dateMillis),
                source = TransactionSource.MANUAL,
                confirmationStatus = ConfirmationStatus.CONFIRMED
            )
            repository.insertTransaction(tx)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    // --- Account Actions ---
    fun addAccount(
        name: String,
        bankName: String,
        accountNumberLast4: String,
        openingBalance: Double,
        accountType: AccountType,
        colorHex: String = "#10B981"
    ) {
        viewModelScope.launch {
            val acc = Account(
                name = name,
                bankName = bankName,
                accountNumberLast4 = accountNumberLast4,
                openingBalance = openingBalance,
                currentBalance = openingBalance,
                accountType = accountType,
                colorHex = colorHex
            )
            val id = repository.insertAccount(acc)
            repository.recalculateAccountBalance(id)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    // --- Savings Actions ---
    fun addSavingsGoal(name: String, targetAmount: Double, targetDateMillis: Long, notes: String, colorHex: String) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                name = name,
                targetAmount = targetAmount,
                targetDateMillis = targetDateMillis,
                notes = notes,
                colorHex = colorHex
            )
            repository.insertSavingsGoal(goal)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun addMoneyToSavings(goalId: Long, amount: Double, sourceAccountId: Long?) {
        viewModelScope.launch {
            repository.addMoneyToSavingsGoal(goalId, amount, sourceAccountId)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun withdrawMoneyFromSavings(goalId: Long, amount: Double, targetAccountId: Long?) {
        viewModelScope.launch {
            repository.withdrawMoneyFromSavingsGoal(goalId, amount, targetAccountId)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    // --- Investment Actions ---
    fun addInvestment(name: String, type: InvestmentType, investedAmount: Double, currentValue: Double, deductAccountId: Long?, notes: String) {
        viewModelScope.launch {
            val inv = Investment(
                name = name,
                type = type,
                investedAmount = investedAmount,
                currentValue = currentValue,
                notes = notes
            )
            repository.insertInvestment(inv, deductAccountId)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun updateInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.updateInvestment(investment)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    // --- Loan Actions ---
    fun addLoan(
        name: String,
        lender: String,
        originalAmount: Double,
        remainingAmount: Double,
        interestRate: Double,
        emiAmount: Double,
        dueDayOfMonth: Int,
        totalEmis: Int,
        notes: String = "",
        nextDueDateMillis: Long = System.currentTimeMillis(),
        enableReminder: Boolean = true
    ) {
        viewModelScope.launch {
            val loan = Loan(
                name = name,
                lender = lender,
                originalAmount = originalAmount,
                remainingAmount = remainingAmount,
                interestRate = interestRate,
                emiAmount = emiAmount,
                dueDayOfMonth = dueDayOfMonth,
                totalEmis = totalEmis,
                paidEmis = 0,
                remainingEmis = totalEmis,
                notes = notes,
                nextDueDateMillis = nextDueDateMillis
            )
            val loanId = repository.insertLoan(loan)

            // If reminder enabled, schedule single monthly recurring reminder
            if (enableReminder) {
                val reminder = Reminder(
                    title = "$name EMI",
                    amount = emiAmount,
                    dueDateMillis = nextDueDateMillis,
                    frequency = ReminderFrequency.MONTHLY,
                    reminderAdvanceDays = 0, // Remind on due date
                    linkedLoanId = loanId,
                    notes = "Monthly EMI payment to $lender (Due day: $dueDayOfMonth)"
                )
                val reminderId = repository.insertReminder(reminder)
                ReminderScheduler.scheduleReminder(getApplication(), reminder.copy(id = reminderId))
            }

            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun payLoanEmi(loanId: Long, emiAmount: Double, accountId: Long) {
        viewModelScope.launch {
            repository.recordEmiPayment(loanId, emiAmount, accountId)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            repository.deleteLoan(loan)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    // --- Reminder Actions ---
    fun addReminder(title: String, amount: Double, dueDateMillis: Long, frequency: ReminderFrequency, advanceDays: Int, notes: String) {
        viewModelScope.launch {
            val reminder = Reminder(
                title = title,
                amount = amount,
                dueDateMillis = dueDateMillis,
                frequency = frequency,
                reminderAdvanceDays = advanceDays,
                notes = notes
            )
            val id = repository.insertReminder(reminder)
            ReminderScheduler.scheduleReminder(getApplication(), reminder.copy(id = id))
        }
    }

    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
            ReminderScheduler.scheduleReminder(getApplication(), reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            ReminderScheduler.cancelReminder(getApplication(), reminder.id)
        }
    }

    fun toggleReminderCompleted(reminder: Reminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isCompleted = !reminder.isCompleted)
            repository.updateReminder(updated)
            if (updated.isCompleted) {
                ReminderScheduler.cancelReminder(getApplication(), reminder.id)
            } else {
                ReminderScheduler.scheduleReminder(getApplication(), updated)
            }
        }
    }

    // --- Detected Messages Actions ---
    fun confirmDetected(
        message: DetectedMessage,
        accountId: Long,
        category: String,
        amount: Double,
        type: TransactionType,
        merchant: String
    ) {
        viewModelScope.launch {
            repository.confirmDetectedMessage(message, accountId, category, amount, type, merchant)
            com.example.widget.FinFlowWidgetManager.updateAllWidgets(getApplication())
        }
    }

    fun ignoreDetected(message: DetectedMessage) {
        viewModelScope.launch {
            repository.ignoreDetectedMessage(message)
        }
    }

    fun testParseSms(rawSms: String): Boolean {
        val parsed = SmsTransactionParser.parse("TestBank", rawSms)
        if (parsed != null) {
            viewModelScope.launch {
                repository.processParsedTransaction(parsed, DetectedSourceType.SMS)
            }
            return true
        }
        return false
    }

    // --- Categories Actions ---
    fun addCategory(name: String, iconName: String, colorHex: String, isIncome: Boolean, keywords: String) {
        viewModelScope.launch {
            val cat = Category(
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isIncome = isIncome,
                keywords = keywords
            )
            repository.insertCategory(cat)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    // --- Settings Actions ---
    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.saveSettings(settings)
        }
    }

    fun updateAutoConfirm(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.saveSettings(current.copy(autoConfirmTrustedSms = enabled))
        }
    }

    fun updateSmsDetection(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.saveSettings(current.copy(enableSmsDetection = enabled))
        }
    }

    fun updateNotificationDetection(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.saveSettings(current.copy(enableNotificationDetection = enabled))
        }
    }

    fun updateAppLock(enabled: Boolean, pin: String = "", biometric: Boolean = false) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            val hash = if (pin.isNotBlank()) SecurityHelper.hashPin(pin) else current.pinHash
            repository.saveSettings(
                current.copy(
                    appLockEnabled = enabled,
                    pinHash = hash,
                    biometricEnabled = biometric
                )
            )
            if (!enabled) {
                _isAppLocked.value = false
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            repository.saveSettings(current.copy(hasCompletedOnboarding = true))
        }
    }

    // --- Demo & Data Management ---
    fun seedDemoData() {
        viewModelScope.launch {
            repository.seedDemoData()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun exportTransactionsCsv(onExportReady: (File?) -> Unit) {
        viewModelScope.launch {
            val all = transactions.value
            val file = ExportHelper.exportTransactionsToCsv(getApplication(), all)
            onExportReady(file)
        }
    }
}
