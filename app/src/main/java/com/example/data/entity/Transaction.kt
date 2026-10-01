package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    INVESTMENT,
    LOAN_PAYMENT,
    SAVINGS
}

enum class TransactionSource {
    MANUAL,
    SMS,
    NOTIFICATION,
    SMS_AND_NOTIFICATION,
    IMPORTED
}

enum class ConfirmationStatus {
    CONFIRMED,
    PENDING_REVIEW,
    IGNORED
}

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["fingerprint"], unique = false),
        Index(value = ["dateMillis"]),
        Index(value = ["accountId"]),
        Index(value = ["type"]),
        Index(value = ["referenceNumber"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryName: String,
    val accountId: Long,
    val accountName: String = "",
    val toAccountId: Long? = null,
    val toAccountName: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val timeFormatted: String = "",
    val merchant: String = "",
    val notes: String = "",
    val source: TransactionSource = TransactionSource.MANUAL,
    val confirmationStatus: ConfirmationStatus = ConfirmationStatus.CONFIRMED,
    val fingerprint: String = "", // Used for duplicate detection (amount + account + date/ref)
    val referenceNumber: String = "",
    val balanceAfterTransaction: Double? = null,
    val feeAmount: Double? = null,
    val upiId: String? = null,
    val sender: String? = null,
    val receiver: String? = null,
    val currency: String = "INR",
    val parseConfidence: String = "HIGH",
    val rawSourceHash: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
