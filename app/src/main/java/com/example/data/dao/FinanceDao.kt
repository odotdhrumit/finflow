package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Account
import com.example.data.entity.AppSettings
import com.example.data.entity.Category
import com.example.data.entity.ConfirmationStatus
import com.example.data.entity.DetectedMessage
import com.example.data.entity.DetectedStatus
import com.example.data.entity.Investment
import com.example.data.entity.Loan
import com.example.data.entity.Reminder
import com.example.data.entity.SavingsGoal
import com.example.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY id ASC")
    fun getActiveAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountById(id: Long): Flow<Account?>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountByIdSuspend(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE isArchived = 0")
    suspend fun getActiveAccountsDirect(): List<Account>

    @Query("SELECT * FROM accounts WHERE accountNumberLast4 = :last4 AND isArchived = 0 LIMIT 1")
    suspend fun findAccountByLast4(last4: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("UPDATE accounts SET currentBalance = :newBalance WHERE id = :id")
    suspend fun updateAccountBalance(id: Long, newBalance: Double)

    // --- Transactions ---
    @Query("SELECT * FROM transactions WHERE confirmationStatus != 'IGNORED' ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE confirmationStatus = 'CONFIRMED' ORDER BY dateMillis DESC, id DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR toAccountId = :accountId) AND confirmationStatus = 'CONFIRMED' ORDER BY dateMillis DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE confirmationStatus = :status ORDER BY dateMillis DESC")
    fun getTransactionsByStatus(status: ConfirmationStatus): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE confirmationStatus = 'CONFIRMED' AND (merchant LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' OR categoryName LIKE '%' || :query || '%' OR accountName LIKE '%' || :query || '%') ORDER BY dateMillis DESC")
    fun searchTransactions(query: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun findTransactionByFingerprint(fingerprint: String): Transaction?

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions WHERE confirmationStatus = 'CONFIRMED'")
    suspend fun getAllConfirmedTransactionsDirect(): List<Transaction>

    // --- Categories ---
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesDirect(): List<Category>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // --- Savings Goals ---
    @Query("SELECT * FROM savings_goals ORDER BY targetDateMillis ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    // --- Investments ---
    @Query("SELECT * FROM investments ORDER BY dateMillis DESC")
    fun getAllInvestments(): Flow<List<Investment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment): Long

    @Update
    suspend fun updateInvestment(investment: Investment)

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    // --- Loans ---
    @Query("SELECT * FROM loans ORDER BY isClosed ASC, nextDueDateMillis ASC")
    fun getAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: Loan): Long

    @Update
    suspend fun updateLoan(loan: Loan)

    @Delete
    suspend fun deleteLoan(loan: Loan)

    // --- Reminders ---
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY dueDateMillis ASC")
    fun getActiveReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    // --- Detected Messages ---
    @Query("SELECT * FROM detected_messages ORDER BY detectedAtMillis DESC")
    fun getAllDetectedMessages(): Flow<List<DetectedMessage>>

    @Query("SELECT * FROM detected_messages WHERE status = 'PENDING_REVIEW' ORDER BY detectedAtMillis DESC")
    fun getPendingDetectedMessages(): Flow<List<DetectedMessage>>

    @Query("SELECT * FROM detected_messages WHERE duplicateFingerprint = :fingerprint LIMIT 1")
    suspend fun findDetectedByFingerprint(fingerprint: String): DetectedMessage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetectedMessage(message: DetectedMessage): Long

    @Update
    suspend fun updateDetectedMessage(message: DetectedMessage)

    @Delete
    suspend fun deleteDetectedMessage(message: DetectedMessage)

    // --- App Settings ---
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: AppSettings)

    // --- Reset / Demo Clearing ---
    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM savings_goals")
    suspend fun clearSavings()

    @Query("DELETE FROM investments")
    suspend fun clearInvestments()

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM detected_messages")
    suspend fun clearDetectedMessages()
}
