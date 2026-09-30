package com.example.parser

import com.example.data.entity.TransactionType

data class ParsedTransaction(
    val bank: String,
    val accountLast4: String?,
    val isDebit: Boolean,
    val amount: Double,
    val merchant: String,
    val referenceNumber: String,
    val suggestedCategory: String,
    val confidenceScore: Float, // 0.0 to 1.0
    val rawText: String,
    val timestamp: Long,
    val fingerprint: String
) {
    val transactionType: TransactionType
        get() = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME
}
