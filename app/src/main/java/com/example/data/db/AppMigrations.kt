package com.example.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppMigrations {

    private fun addColumnIfNotExists(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnDefinition: String
    ) {
        val cursor = db.query("PRAGMA table_info(`$tableName`)")
        var exists = false
        val nameIndex = cursor.getColumnIndex("name")
        if (nameIndex != -1) {
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex).equals(columnName, ignoreCase = true)) {
                    exists = true
                    break
                }
            }
        }
        cursor.close()
        if (!exists) {
            db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
        }
    }

    fun migrateSchemaSafely(db: SupportSQLiteDatabase) {
        // 1. Ensure transactions table has all new columns
        addColumnIfNotExists(db, "transactions", "referenceNumber", "TEXT NOT NULL DEFAULT ''")
        addColumnIfNotExists(db, "transactions", "balanceAfterTransaction", "REAL")
        addColumnIfNotExists(db, "transactions", "feeAmount", "REAL")
        addColumnIfNotExists(db, "transactions", "upiId", "TEXT")
        addColumnIfNotExists(db, "transactions", "sender", "TEXT")
        addColumnIfNotExists(db, "transactions", "receiver", "TEXT")
        addColumnIfNotExists(db, "transactions", "currency", "TEXT NOT NULL DEFAULT 'INR'")
        addColumnIfNotExists(db, "transactions", "parseConfidence", "TEXT NOT NULL DEFAULT 'HIGH'")
        addColumnIfNotExists(db, "transactions", "rawSourceHash", "TEXT NOT NULL DEFAULT ''")
        addColumnIfNotExists(db, "transactions", "updatedAt", "INTEGER NOT NULL DEFAULT 0")

        // Ensure transactions indices exist
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_fingerprint` ON `transactions` (`fingerprint`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_dateMillis` ON `transactions` (`dateMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_type` ON `transactions` (`type`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_referenceNumber` ON `transactions` (`referenceNumber`)")

        // 2. Ensure detected_messages table has all new columns
        addColumnIfNotExists(db, "detected_messages", "parsedBalance", "REAL")
        addColumnIfNotExists(db, "detected_messages", "parsedFee", "REAL")
        addColumnIfNotExists(db, "detected_messages", "detectedReferenceNumber", "TEXT NOT NULL DEFAULT ''")
        addColumnIfNotExists(db, "detected_messages", "confidenceLevel", "TEXT NOT NULL DEFAULT 'HIGH'")
        addColumnIfNotExists(db, "detected_messages", "confidenceReason", "TEXT NOT NULL DEFAULT ''")

        // 3. Ensure app_settings table exists
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_settings` (" +
                "`id` INTEGER NOT NULL, " +
                "`currencySymbol` TEXT NOT NULL, " +
                "`autoConfirmTrustedSms` INTEGER NOT NULL, " +
                "`enableSmsDetection` INTEGER NOT NULL, " +
                "`enableNotificationDetection` INTEGER NOT NULL, " +
                "`appLockEnabled` INTEGER NOT NULL, " +
                "`pinHash` TEXT NOT NULL, " +
                "`biometricEnabled` INTEGER NOT NULL, " +
                "`hasCompletedOnboarding` INTEGER NOT NULL, " +
                "`lastBackupDateMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )

        // 4. Ensure all core entities exist in case of partial initialization
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
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateSchemaSafely(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateSchemaSafely(db)
        }
    }

    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            migrateSchemaSafely(db)
        }
    }
}
