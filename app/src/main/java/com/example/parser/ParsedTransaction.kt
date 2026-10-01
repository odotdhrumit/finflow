package com.example.parser

import com.example.data.entity.TransactionType

enum class DetectedTransactionType {
    CREDIT,
    DEBIT,
    TRANSFER,
    UNKNOWN
}

enum class ParseConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class MoneyRole {
    TRANSACTION,
    BALANCE,
    FEE,
    LIMIT,
    TOTAL,
    OTHER
}

data class MoneyCandidate(
    val rawValue: String,
    val amount: Double,
    val startIndex: Int,
    val endIndex: Int,
    val role: MoneyRole,
    val score: Float,
    val reasoning: String
)

data class ParsedTransaction(
    val bank: String?,
    val accountLast4: String?,
    val detectedType: DetectedTransactionType,
    val amount: Double,
    val balanceAfterTransaction: Double? = null,
    val feeAmount: Double? = null,
    val merchant: String = "",
    val sender: String? = null,
    val receiver: String? = null,
    val upiId: String? = null,
    val referenceNumber: String = "",
    val suggestedCategory: String = "Other",
    val confidence: ParseConfidence = ParseConfidence.HIGH,
    val confidenceScore: Float = 1.0f,
    val confidenceReason: String = "",
    val rawText: String,
    val normalizedText: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fingerprint: String,
    val requiresReview: Boolean = false,
    val allDetectedMoney: List<MoneyCandidate> = emptyList()
) {
    val isDebit: Boolean
        get() = detectedType == DetectedTransactionType.DEBIT

    val transactionType: TransactionType
        get() = when (detectedType) {
            DetectedTransactionType.CREDIT -> TransactionType.INCOME
            DetectedTransactionType.DEBIT -> TransactionType.EXPENSE
            DetectedTransactionType.TRANSFER -> TransactionType.TRANSFER
            DetectedTransactionType.UNKNOWN -> TransactionType.EXPENSE
        }
}
