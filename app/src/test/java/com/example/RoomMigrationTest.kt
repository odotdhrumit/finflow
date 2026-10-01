package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.AppMigrations
import com.example.data.entity.ConfirmationStatus
import com.example.data.entity.Transaction
import com.example.data.entity.TransactionSource
import com.example.data.entity.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomMigrationTest {

    private val testDbName = "migration_test_db"

    @Test
    fun testMigration1To2PreservesData() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(testDbName)

        // 1. Create a version 1 database using raw SQLite
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(testDbName)
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create tables with Version 1 schema (before adding new columns)
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `accounts` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`bankName` TEXT NOT NULL, " +
                                "`accountNumberLast4` TEXT NOT NULL, " +
                                "`openingBalance` REAL NOT NULL, " +
                                "`currentBalance` REAL NOT NULL, " +
                                "`accountType` TEXT NOT NULL, " +
                                "`colorHex` TEXT NOT NULL, " +
                                "`isArchived` INTEGER NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `transactions` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`amount` REAL NOT NULL, " +
                                "`type` TEXT NOT NULL, " +
                                "`categoryName` TEXT NOT NULL, " +
                                "`accountId` INTEGER NOT NULL, " +
                                "`accountName` TEXT NOT NULL, " +
                                "`toAccountId` INTEGER, " +
                                "`toAccountName` TEXT, " +
                                "`dateMillis` INTEGER NOT NULL, " +
                                "`timeFormatted` TEXT NOT NULL, " +
                                "`merchant` TEXT NOT NULL, " +
                                "`notes` TEXT NOT NULL, " +
                                "`source` TEXT NOT NULL, " +
                                "`confirmationStatus` TEXT NOT NULL, " +
                                "`fingerprint` TEXT NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL, " +
                                "`updatedAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `categories` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`iconName` TEXT NOT NULL, " +
                                "`colorHex` TEXT NOT NULL, " +
                                "`isIncome` INTEGER NOT NULL, " +
                                "`isDefault` INTEGER NOT NULL, " +
                                "`keywords` TEXT NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `savings_goals` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`targetAmount` REAL NOT NULL, " +
                                "`currentAmount` REAL NOT NULL, " +
                                "`targetDateMillis` INTEGER NOT NULL, " +
                                "`notes` TEXT NOT NULL, " +
                                "`colorHex` TEXT NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `investments` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`type` TEXT NOT NULL, " +
                                "`investedAmount` REAL NOT NULL, " +
                                "`currentValue` REAL NOT NULL, " +
                                "`dateMillis` INTEGER NOT NULL, " +
                                "`notes` TEXT NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `loans` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`lender` TEXT NOT NULL, " +
                                "`originalAmount` REAL NOT NULL, " +
                                "`remainingAmount` REAL NOT NULL, " +
                                "`interestRate` REAL NOT NULL, " +
                                "`emiAmount` REAL NOT NULL, " +
                                "`dueDayOfMonth` INTEGER NOT NULL, " +
                                "`totalEmis` INTEGER NOT NULL, " +
                                "`paidEmis` INTEGER NOT NULL, " +
                                "`remainingEmis` INTEGER NOT NULL, " +
                                "`frequency` TEXT NOT NULL, " +
                                "`notes` TEXT NOT NULL, " +
                                "`nextDueDateMillis` INTEGER NOT NULL, " +
                                "`isClosed` INTEGER NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `reminders` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`title` TEXT NOT NULL, " +
                                "`amount` REAL NOT NULL, " +
                                "`dueDateMillis` INTEGER NOT NULL, " +
                                "`frequency` TEXT NOT NULL, " +
                                "`reminderAdvanceDays` INTEGER NOT NULL, " +
                                "`isCompleted` INTEGER NOT NULL, " +
                                "`linkedLoanId` INTEGER, " +
                                "`notes` TEXT NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL)"
                    )

                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `detected_messages` (" +
                                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`sourceType` TEXT NOT NULL, " +
                                "`senderOrApp` TEXT NOT NULL, " +
                                "`rawText` TEXT NOT NULL, " +
                                "`detectedAtMillis` INTEGER NOT NULL, " +
                                "`parsedAmount` REAL, " +
                                "`parsedType` TEXT, " +
                                "`parsedBank` TEXT, " +
                                "`parsedAccountLast4` TEXT, " +
                                "`parsedMerchant` TEXT, " +
                                "`suggestedCategory` TEXT, " +
                                "`status` TEXT NOT NULL, " +
                                "`duplicateFingerprint` TEXT NOT NULL, " +
                                "`linkedTransactionId` INTEGER)"
                    )

                    // Insert identity hash for version 1
                    db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
                    db.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '4aa97b2df4fcd43bfc88845fd7b22e24')")

                    // 2. Insert test data that MUST be preserved
                    db.execSQL(
                        "INSERT INTO `accounts` (`id`, `name`, `bankName`, `accountNumberLast4`, `openingBalance`, `currentBalance`, `accountType`, `colorHex`, `isArchived`, `createdAt`) " +
                                "VALUES (1, 'SBI Salary', 'State Bank of India', '4321', 50000.0, 48500.0, 'BANK', '#1E40AF', 0, 1000)"
                    )

                    db.execSQL(
                        "INSERT INTO `transactions` (`id`, `amount`, `type`, `categoryName`, `accountId`, `accountName`, `toAccountId`, `toAccountName`, `dateMillis`, `timeFormatted`, `merchant`, `notes`, `source`, `confirmationStatus`, `fingerprint`, `createdAt`, `updatedAt`) " +
                                "VALUES (1, 1500.0, 'EXPENSE', 'Groceries', 1, 'SBI Salary', NULL, NULL, 1600000, '02:30 PM', 'Supermarket', 'Weekly groceries', 'MANUAL', 'CONFIRMED', 'fp123', 1000, 1000)"
                    )

                    db.execSQL(
                        "INSERT INTO `savings_goals` (`id`, `name`, `targetAmount`, `currentAmount`, `targetDateMillis`, `notes`, `colorHex`, `createdAt`) " +
                                "VALUES (1, 'Emergency Fund', 100000.0, 25000.0, 2000000, 'Rainy day', '#047857', 1000)"
                    )

                    db.execSQL(
                        "INSERT INTO `investments` (`id`, `name`, `type`, `investedAmount`, `currentValue`, `dateMillis`, `notes`, `createdAt`) " +
                                "VALUES (1, 'Nifty 50 Index', 'MUTUAL_FUND', 30000.0, 34500.0, 1500000, 'SIP', 1000)"
                    )

                    db.execSQL(
                        "INSERT INTO `loans` (`id`, `name`, `lender`, `originalAmount`, `remainingAmount`, `interestRate`, `emiAmount`, `dueDayOfMonth`, `totalEmis`, `paidEmis`, `remainingEmis`, `frequency`, `notes`, `nextDueDateMillis`, `isClosed`, `createdAt`) " +
                                "VALUES (1, 'Car Loan', 'HDFC Bank', 500000.0, 320000.0, 8.5, 12500.0, 5, 48, 16, 32, 'MONTHLY', 'Honda City', 1700000, 0, 1000)"
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val rawDb = helper.writableDatabase
        rawDb.close()

        // 3. Now open the database with Room and MIGRATIONS
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, testDbName)
            .addMigrations(
                AppMigrations.MIGRATION_1_2,
                AppMigrations.MIGRATION_2_3,
                AppMigrations.MIGRATION_1_3
            )
            .build()

        val dao = roomDb.financeDao()

        // 4. Verify existing data remains completely intact!
        val accounts = dao.getActiveAccountsDirect()
        assertEquals(1, accounts.size)
        assertEquals("SBI Salary", accounts[0].name)
        assertEquals(48500.0, accounts[0].currentBalance, 0.001)

        val txList = dao.getAllConfirmedTransactionsDirect()
        assertEquals(1, txList.size)
        val existingTx = txList[0]
        assertEquals(1500.0, existingTx.amount, 0.001)
        assertEquals("Supermarket", existingTx.merchant)
        assertEquals("Groceries", existingTx.categoryName)
        // Verify migrated fields have safe defaults
        assertEquals("", existingTx.referenceNumber)
        assertEquals("INR", existingTx.currency)
        assertEquals("HIGH", existingTx.parseConfidence)
        assertNull(existingTx.balanceAfterTransaction)

        val savings = dao.getAllSavingsGoals().first()
        assertEquals(1, savings.size)
        assertEquals("Emergency Fund", savings[0].name)
        assertEquals(25000.0, savings[0].currentAmount, 0.001)

        val investments = dao.getAllInvestments().first()
        assertEquals(1, investments.size)
        assertEquals("Nifty 50 Index", investments[0].name)
        assertEquals(34500.0, investments[0].currentValue, 0.001)

        val loans = dao.getAllLoans().first()
        assertEquals(1, loans.size)
        assertEquals("Car Loan", loans[0].name)
        assertEquals(320000.0, loans[0].remainingAmount, 0.001)

        // 5. Verify new transactions with the new schema fields can be inserted and queried
        val newTx = Transaction(
            amount = 250.0,
            type = TransactionType.EXPENSE,
            categoryName = "Food",
            accountId = 1,
            accountName = "SBI Salary",
            merchant = "Starbucks",
            referenceNumber = "UPI/1234567890",
            balanceAfterTransaction = 48250.0,
            feeAmount = 0.0,
            currency = "INR",
            parseConfidence = "HIGH",
            source = TransactionSource.SMS,
            confirmationStatus = ConfirmationStatus.CONFIRMED
        )
        val insertedId = dao.insertTransaction(newTx)
        assertTrue(insertedId > 0)

        val fetchedTx = dao.getTransactionById(insertedId)
        assertNotNull(fetchedTx)
        assertEquals("UPI/1234567890", fetchedTx!!.referenceNumber)
        assertEquals(48250.0, fetchedTx.balanceAfterTransaction!!, 0.001)

        roomDb.close()
        context.deleteDatabase(testDbName)
        }
    }
}
