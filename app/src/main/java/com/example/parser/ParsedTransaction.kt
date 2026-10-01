package com.example.parser

import com.example.data.entity.TransactionType

enum class DetectedTransactionType {
    CREDIT,
    DEBIT,
    TRANSFER,
    UNKNOWN
}

data class ParsedTransaction(
    val bank: String,
    val accountLast4: String?,
    val detectedType: DetectedTransactionType,
    val amount: Double,
    val merchant: String,
    val referenceNumber: String,
    val suggestedCategory: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val rawText: String,
    val timestamp: Long,
    val fingerprint: String,
    val requiresReview: Boolean = false
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
